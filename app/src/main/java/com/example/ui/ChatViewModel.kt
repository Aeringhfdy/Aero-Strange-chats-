package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ChatRepository(application)

    // State Holders
    val currentUser = repository.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val conversations = repository.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val callLogs = repository.callLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedAccounts = repository.blockedAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.settings.map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // UI Navigation State
    private val _currentScreen = MutableStateFlow("auth") // auth, main_chats, chat_view, settings, call_history, dial_pad, group_create
    val currentScreen: StateFlow<String> = _currentScreen

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId

    // Calling States
    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall

    // Simulated Ringing Sound State (vibration pattern trigger or visual flashing)
    private val _ringingState = MutableStateFlow(false)
    val ringingState: StateFlow<Boolean> = _ringingState

    // Authentication Helper states
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var callTimerJob: Job? = null

    init {
        // Automatically switch screen when authenticated
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    _currentScreen.value = "main_chats"
                } else {
                    _currentScreen.value = "auth"
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectConversation(id: String) {
        _selectedConversationId.value = id
        _currentScreen.value = "chat_view"
    }

    // --- Authentication Actions ---
    fun registerOrLogin(phone: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            delay(1000) // Realistic loading spinner
            val result = repository.registerOrLogin(phone, name)
            if (result.isSuccess) {
                _currentScreen.value = "main_chats"
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "An error occurred"
            }
            _isLoading.value = false
        }
    }

    fun authenticateWithFirebase(email: String, password: String, isSignUp: Boolean, name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            delay(1000)
            val result = repository.authenticateWithFirebase(email, password, isSignUp, name, phone)
            if (result.isSuccess) {
                _currentScreen.value = "main_chats"
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Authentication failed"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _selectedConversationId.value = null
            _currentScreen.value = "auth"
        }
    }

    // --- Message Actions ---
    fun getMessagesForSelected(): Flow<List<Message>> {
        val convId = _selectedConversationId.value ?: return emptyFlow()
        return repository.observeMessages(convId)
    }

    fun sendMessage(text: String) {
        val convId = _selectedConversationId.value ?: return
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            repository.sendMessage(convId, text)
        }
    }

    fun clearChat(conversationId: String) {
        viewModelScope.launch {
            repository.clearChatHistory(conversationId)
        }
    }

    // --- Call System Actions ---
    fun dialNumber(number: String, isVoice: Boolean) {
        viewModelScope.launch {
            val contact = repository.getProfile(number)
            if (contact != null) {
                startCall(contact, isVoice, isOutgoing = true)
            } else {
                // If contact is not found, dynamically register a mock profile so call can connect!
                val randomProfile = Profile(
                    phone = number,
                    name = "Aero Contact $number",
                    avatar = "📞",
                    about = "Aero Strange Subscriber",
                    isCurrentUser = false,
                    isSimulated = true
                )
                repository.registerSimulatedContact(randomProfile)
                startCall(randomProfile, isVoice, isOutgoing = true)
            }
        }
    }

    fun startCall(contact: Profile, isVoice: Boolean, isOutgoing: Boolean = true) {
        val userPhone = currentUser.value?.phone ?: "me"
        val callId = java.util.UUID.randomUUID().toString()

        _activeCall.value = ActiveCallState(
            id = callId,
            contactPhone = contact.phone,
            contactName = contact.name,
            contactAvatar = contact.avatar,
            isVoice = isVoice,
            status = if (isOutgoing) "Dialing" else "Ringing",
            durationSeconds = 0,
            isMuted = false,
            isVideoOff = false,
            isSpeakerOn = false
        )

        _ringingState.value = true

        if (isOutgoing) {
            viewModelScope.launch {
                delay(3000) // Ringing transition to Connected
                _ringingState.value = false
                _activeCall.value = _activeCall.value?.copy(status = "Connected")
                startCallTimer()
            }
        }
    }

    fun acceptCall() {
        val call = _activeCall.value ?: return
        _ringingState.value = false
        _activeCall.value = call.copy(status = "Connected")
        startCallTimer()
    }

    fun declineOrEndCall() {
        val call = _activeCall.value ?: return
        _ringingState.value = false
        callTimerJob?.cancel()

        viewModelScope.launch {
            val duration = call.durationSeconds
            val status = when (call.status) {
                "Dialing", "Ringing" -> if (call.status == "Ringing") "Declined" else "Missed"
                else -> "Completed"
            }

            val userPhone = currentUser.value?.phone ?: "me"
            repository.logCall(
                callerPhone = if (status == "Declined" || status == "Missed") call.contactPhone else userPhone,
                receiverPhone = if (status == "Declined" || status == "Missed") userPhone else call.contactPhone,
                isVoice = call.isVoice,
                status = status,
                durationSeconds = duration
            )

            _activeCall.value = null
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _activeCall.value ?: break
                _activeCall.value = current.copy(durationSeconds = current.durationSeconds + 1)
            }
        }
    }

    fun toggleMute() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isMuted = !current.isMuted)
    }

    fun toggleVideo() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isVideoOff = !current.isVideoOff)
    }

    fun toggleSpeaker() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    }

    fun toggleScreenSharing() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isScreenSharing = !current.isScreenSharing)
    }

    fun startGroupCall(groupName: String, isVoice: Boolean) {
        val callId = java.util.UUID.randomUUID().toString()

        _activeCall.value = Companion.ActiveCallState(
            id = callId,
            contactPhone = "group_call",
            contactName = groupName,
            contactAvatar = "👥",
            isVoice = isVoice,
            status = "Dialing",
            durationSeconds = 0,
            isMuted = false,
            isVideoOff = false,
            isSpeakerOn = true,
            isScreenSharing = false,
            isGroupCall = true,
            groupParticipants = listOf("Strange Explorer", "Alice Vance", "Bob Sterling")
        )

        _ringingState.value = true

        viewModelScope.launch {
            delay(3000) // Simulated connection latency
            _ringingState.value = false
            _activeCall.value = _activeCall.value?.copy(status = "Connected")
            startCallTimer()
        }
    }

    // --- Settings Actions ---
    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    fun resetNetworkStats() {
        viewModelScope.launch {
            repository.resetNetworkUsage()
        }
    }

    fun clearAllChatsAndHistory() {
        viewModelScope.launch {
            repository.clearAllChats()
        }
    }

    // --- Translation Dictionary for Aero Strange Language Selection ---
    fun getTranslation(key: String): String {
        val lang = settings.value["language"] ?: "en"
        return translationMap[lang]?.get(key) ?: translationMap["en"]?.get(key) ?: key
    }

    companion object {
        data class ActiveCallState(
            val id: String,
            val contactPhone: String,
            val contactName: String,
            val contactAvatar: String,
            val isVoice: Boolean,
            val status: String, // Dialing, Ringing, Connected, Ended
            val durationSeconds: Int,
            val isMuted: Boolean,
            val isVideoOff: Boolean,
            val isSpeakerOn: Boolean,
            val isScreenSharing: Boolean = false,
            val isGroupCall: Boolean = false,
            val groupParticipants: List<String> = emptyList()
        )

        private val translationMap = mapOf(
            "en" to mapOf(
                "title" to "Aero Strange Chats",
                "subtitle" to "Operated by Aero Strange International Platforms",
                "phone_placeholder" to "Phone Number (fake accepted)",
                "name_placeholder" to "Display Name",
                "btn_register" to "Register Securely",
                "btn_signin" to "Secure Login",
                "tab_chats" to "Chats",
                "tab_calls" to "Calls",
                "tab_settings" to "Settings",
                "empty_chats" to "No secure chats yet. Search or enter a phone number in dial pad to start!",
                "empty_calls" to "No Call History on Aero Strange network.",
                "new_chat_title" to "Secure Direct Chat",
                "new_group_title" to "Create Aero Group",
                "group_name_placeholder" to "Group Name",
                "settings_account" to "Account & Security",
                "settings_privacy" to "Privacy & Disappearing",
                "settings_chats" to "Chat Background & History",
                "settings_notifications" to "Alerts & Ringtones",
                "settings_storage" to "Data & Network Statistics",
                "settings_accessibility" to "Accessibility & Language",
                "settings_about" to "About Platforms",
                "sign_out" to "Disconnect Account",
                "disappearing_title" to "Disappearing Messages",
                "disappearing_desc" to "Automatically delete sent messages from devices after configured times.",
                "contrast_title" to "Increase Visual Contrast",
                "animations_title" to "Interface Animations",
                "language_title" to "Aero System Language",
                "auto_wifi" to "Auto-download media over Wi-Fi",
                "auto_cell" to "Auto-download over cellular",
                "sec_notif_title" to "Security Notifications",
                "sec_notif_desc" to "Receive alerts when security configurations or user devices change on Aero platforms."
            ),
            "es" to mapOf(
                "title" to "Aero Strange Chats",
                "subtitle" to "Operado por Aero Strange International Platforms",
                "phone_placeholder" to "Número de teléfono",
                "name_placeholder" to "Nombre a mostrar",
                "btn_register" to "Registrarse Seguro",
                "btn_signin" to "Iniciar sesión",
                "tab_chats" to "Chats",
                "tab_calls" to "Llamadas",
                "tab_settings" to "Ajustes",
                "empty_chats" to "No hay chats seguros aún.",
                "empty_calls" to "Sin historial de llamadas.",
                "new_chat_title" to "Chat directo seguro",
                "new_group_title" to "Crear grupo Aero",
                "group_name_placeholder" to "Nombre de grupo",
                "settings_account" to "Cuenta y Seguridad",
                "settings_privacy" to "Privacidad y Desaparición",
                "settings_chats" to "Fondo de chat e Historial",
                "settings_notifications" to "Alertas y Tonos",
                "settings_storage" to "Datos y Estadísticas",
                "settings_accessibility" to "Accesibilidad e Idioma",
                "settings_about" to "Acerca de Aero",
                "sign_out" to "Desconectar cuenta",
                "disappearing_title" to "Mensajes temporales",
                "disappearing_desc" to "Eliminar automáticamente mensajes después del tiempo configurado.",
                "contrast_title" to "Aumentar contraste",
                "animations_title" to "Animaciones de interfaz",
                "language_title" to "Idioma del sistema Aero",
                "auto_wifi" to "Descarga automática con Wi-Fi",
                "auto_cell" to "Descarga automática con datos móviles",
                "sec_notif_title" to "Notificaciones de seguridad",
                "sec_notif_desc" to "Recibir alertas sobre la seguridad de las plataformas Aero."
            ),
            "fr" to mapOf(
                "title" to "Aero Strange Chats",
                "subtitle" to "Géré par Aero Strange International Platforms",
                "phone_placeholder" to "Numéro de téléphone",
                "name_placeholder" to "Nom d'affichage",
                "btn_register" to "S'enregistrer",
                "btn_signin" to "Connexion sécurisée",
                "tab_chats" to "Discussions",
                "tab_calls" to "Appels",
                "tab_settings" to "Paramètres",
                "empty_chats" to "Aucun chat sécurisé.",
                "empty_calls" to "Pas d'historique d'appels.",
                "new_chat_title" to "Nouveau chat sécurisé",
                "new_group_title" to "Créer un groupe Aero",
                "group_name_placeholder" to "Nom du groupe",
                "settings_account" to "Compte et Sécurité",
                "settings_privacy" to "Confidentialité et messages éphémères",
                "settings_chats" to "Arrière-plan et historique",
                "settings_notifications" to "Alertes et sonneries",
                "settings_storage" to "Utilisation des données",
                "settings_accessibility" to "Accessibilité et Langue",
                "settings_about" to "À propos",
                "sign_out" to "Se déconnecter",
                "disappearing_title" to "Messages éphémères",
                "disappearing_desc" to "Supprime automatiquement les messages après le délai.",
                "contrast_title" to "Augmenter le contraste",
                "animations_title" to "Animations de l'interface",
                "language_title" to "Langue du système Aero",
                "auto_wifi" to "Téléchargement automatique en Wi-Fi",
                "auto_cell" to "Téléchargement automatique en données mobiles",
                "sec_notif_title" to "Notifications de sécurité",
                "sec_notif_desc" to "Recevoir des alertes sur la sécurité des plateformes Aero."
            ),
            "de" to mapOf(
                "title" to "Aero Strange Chats",
                "subtitle" to "Betrieben von Aero Strange International Platforms",
                "phone_placeholder" to "Telefonnummer",
                "name_placeholder" to "Anzeigename",
                "btn_register" to "Sicher registrieren",
                "btn_signin" to "Sicherer Login",
                "tab_chats" to "Chats",
                "tab_calls" to "Anrufe",
                "tab_settings" to "Einstellungen",
                "empty_chats" to "Noch keine sicheren Chats.",
                "empty_calls" to "Kein Anrufverlauf.",
                "new_chat_title" to "Sicherer Direktchat",
                "new_group_title" to "Aero Gruppe erstellen",
                "group_name_placeholder" to "Gruppenname",
                "settings_account" to "Konto & Sicherheit",
                "settings_privacy" to "Datenschutz & Selbstlöschung",
                "settings_chats" to "Chathintergrund & Verlauf",
                "settings_notifications" to "Meldungen & Töne",
                "settings_storage" to "Daten- & Netzwerknutzung",
                "settings_accessibility" to "Barrierefreiheit & Sprache",
                "settings_about" to "Über Aero",
                "sign_out" to "Konto trennen",
                "disappearing_title" to "Selbstlöschende Nachrichten",
                "disappearing_desc" to "Nachrichten automatisch nach Ablauf der Zeit löschen.",
                "contrast_title" to "Kontrast erhöhen",
                "animations_title" to "Benutzeroberflächen-Animationen",
                "language_title" to "Aero System Sprache",
                "auto_wifi" to "Automatischer Download über WLAN",
                "auto_cell" to "Automatischer Download über Mobilfunk",
                "sec_notif_title" to "Sicherheitsbenachrichtigungen",
                "sec_notif_desc" to "Erhalten Sie Warnungen bei Änderungen an Aero-Plattformen."
            ),
            "ar" to mapOf(
                "title" to "دردشات إيرو سترينج",
                "subtitle" to "مُدار بواسطة منصات إيرو سترينج الدولية",
                "phone_placeholder" to "رقم الهاتف",
                "name_placeholder" to "الاسم المستعار",
                "btn_register" to "تسجيل آمن",
                "btn_signin" to "دخول آمن",
                "tab_chats" to "المحادثات",
                "tab_calls" to "المكالمات",
                "tab_settings" to "الإعدادات",
                "empty_chats" to "لا توجد محادثات آمنة بعد.",
                "empty_calls" to "سجل المكالمات فارغ.",
                "new_chat_title" to "محادثة مباشرة آمنة",
                "new_group_title" to "إنشاء مجموعة إيرو",
                "group_name_placeholder" to "اسم المجموعة",
                "settings_account" to "الحساب والأمان",
                "settings_privacy" to "الخصوصية والرسائل المختفية",
                "settings_chats" to "خلفية وتاريخ الدردشة",
                "settings_notifications" to "التنبيهات والنغمات",
                "settings_storage" to "البيانات والشبكة",
                "settings_accessibility" to "سهولة الوصول واللغة",
                "settings_about" to "حول المنصة",
                "sign_out" to "تسجيل الخروج",
                "disappearing_title" to "الرسائل ذاتية الاختفاء",
                "disappearing_desc" to "حذف الرسائل تلقائيًا بعد وقت محدد.",
                "contrast_title" to "زيادة التباين البصري",
                "animations_title" to "تأثيرات الحركة",
                "language_title" to "لغة نظام إيرو",
                "auto_wifi" to "تحميل تلقائي عبر الواي فاي",
                "auto_cell" to "تحميل تلقائي عبر البيانات",
                "sec_notif_title" to "إشعارات الأمان",
                "sec_notif_desc" to "استلام إشعارات عند حدوث أي تعديلات أمنية."
            ),
            "hi" to mapOf(
                "title" to "एयरो स्ट्रेंज चैट्स",
                "subtitle" to "एयरो स्ट्रेंज इंटरनेशनल प्लेटफॉर्म्स द्वारा संचालित",
                "phone_placeholder" to "फ़ोन नंबर (नकली स्वीकार्य)",
                "name_placeholder" to "प्रदर्शित नाम",
                "btn_register" to "सुरक्षित पंजीकरण",
                "btn_signin" to "सुरक्षित लॉगिन",
                "tab_chats" to "चैट",
                "tab_calls" to "कॉल",
                "tab_settings" to "सेटिंग्स",
                "empty_chats" to "कोई सुरक्षित चैट उपलब्ध नहीं है।",
                "empty_calls" to "कॉल इतिहास खाली है।",
                "new_chat_title" to "सुरक्षित डायरेक्ट चैट",
                "new_group_title" to "एयरो ग्रुप बनाएं",
                "group_name_placeholder" to "ग्रुप का नाम",
                "settings_account" to "खाता और सुरक्षा",
                "settings_privacy" to "गोपनीयता और गायब संदेश",
                "settings_chats" to "चैट वॉलपेपर और इतिहास",
                "settings_notifications" to "अलर्ट और रिंगटोन",
                "settings_storage" to "डेटा और नेटवर्क उपयोग",
                "settings_accessibility" to "अभिगम्यता और भाषा",
                "settings_about" to "प्लेटफॉर्म के बारे में",
                "sign_out" to "खाता बंद करें",
                "disappearing_title" to "गायब होने वाले संदेश",
                "disappearing_desc" to "तय समय के बाद संदेशों को स्वचालित रूप से हटाएं।",
                "contrast_title" to "दृश्य कंट्रास्ट बढ़ाएं",
                "animations_title" to "एनिमेशन सक्षम करें",
                "language_title" to "एयरो सिस्टम भाषा",
                "auto_wifi" to "वाई-फाई पर ऑटो-डाउनलोड",
                "auto_cell" to "सेलुलर पर ऑटो-डाउनलोड",
                "sec_notif_title" to "सुरक्षा सूचनाएं",
                "sec_notif_desc" to "एयरो प्लेटफॉर्म पर सुरक्षा अपडेट मिलने पर अलर्ट प्राप्त करें।"
            ),
            "zh" to mapOf(
                "title" to "Aero Strange 加密聊天",
                "subtitle" to "由 Aero Strange 国际平台运营",
                "phone_placeholder" to "手机号码 (支持任意号码)",
                "name_placeholder" to "昵称",
                "btn_register" to "安全注册",
                "btn_signin" to "安全登录",
                "tab_chats" to "聊天",
                "tab_calls" to "通话",
                "tab_settings" to "设置",
                "empty_chats" to "暂无加密聊天。",
                "empty_calls" to "暂无通话记录。",
                "new_chat_title" to "发起安全聊天",
                "new_group_title" to "创建 Aero 群组",
                "group_name_placeholder" to "群组名称",
                "settings_account" to "账户与安全",
                "settings_privacy" to "隐私与阅后即焚",
                "settings_chats" to "聊天背景与历史",
                "settings_notifications" to "通知与铃声",
                "settings_storage" to "数据与网络流量",
                "settings_accessibility" to "无障碍与语言",
                "settings_about" to "关于平台",
                "sign_out" to "断开账户连接",
                "disappearing_title" to "阅后即焚消息",
                "disappearing_desc" to "在设定时间后自动从设备删除消息。",
                "contrast_title" to "增加视觉对比度",
                "animations_title" to "界面动画效果",
                "language_title" to "Aero 系统语言",
                "auto_wifi" to "通过 Wi-Fi 自动下载",
                "auto_cell" to "通过移动数据自动下载",
                "sec_notif_title" to "安全警报通知",
                "sec_notif_desc" to "当 Aero 平台上的安全配置发生变化时接收通知。"
            ),
            "ja" to mapOf(
                "title" to "Aero Strange チャット",
                "subtitle" to "Aero Strange International Platforms 運営",
                "phone_placeholder" to "電話番号 (ダミー可)",
                "name_placeholder" to "表示名",
                "btn_register" to "安全に登録",
                "btn_signin" to "安全なログイン",
                "tab_chats" to "チャット",
                "tab_calls" to "通話",
                "tab_settings" to "設定",
                "empty_chats" to "安全なチャットはありません。",
                "empty_calls" to "通話履歴はありません。",
                "new_chat_title" to "ダイレクトチャットを開始",
                "new_group_title" to "Aero グループ作成",
                "group_name_placeholder" to "グループ名",
                "settings_account" to "アカウントとセキュリティ",
                "settings_privacy" to "プライバシーと消えるメッセージ",
                "settings_chats" to "壁紙とチャット履歴",
                "settings_notifications" to "通知と着信音",
                "settings_storage" to "ストレージとネットワーク使用量",
                "settings_accessibility" to "アクセシビリティと言語",
                "settings_about" to "Aero について",
                "sign_out" to "ログアウト",
                "disappearing_title" to "消えるメッセージ",
                "disappearing_desc" to "設定された時間経過後にメッセージを自動的に削除します。",
                "contrast_title" to "ハイコントラストを有効化",
                "animations_title" to "UIアニメーションを有効化",
                "language_title" to "Aero システム言語",
                "auto_wifi" to "Wi-Fi接続時に自動ダウンロード",
                "auto_cell" to "モバイル回線時に自動ダウンロード",
                "sec_notif_title" to "セキュリティ通知",
                "sec_notif_desc" to "セキュリティ関連の変更が発生した際に通知します。"
            )
        )
    }
}
