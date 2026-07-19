package com.example.repository

import android.content.Context
import com.example.api.GeminiApiClient
import com.example.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentChange
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Await extension for Firebase Tasks
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: Exception("Firebase Task Failed"))
        }
    }
}

class ChatRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.chatDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var firebaseAuth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set

    // Live Flows
    val conversations: Flow<List<Conversation>> = dao.observeConversations()
    val currentUser: Flow<Profile?> = dao.observeCurrentUser()
    val callLogs: Flow<List<CallLog>> = dao.observeCallLogs()
    val blockedAccounts: Flow<List<BlockedAccount>> = dao.observeBlockedAccounts()
    val settings: Flow<List<UserSetting>> = dao.observeUserSettings()

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize default settings and simulated profiles on background thread
        coroutineScope.launch {
            prepopulateDatabase()
            startDisappearingMessagesWorker()
        }
    }

    private suspend fun prepopulateDatabase() {
        // Initialize Default User Settings if empty
        val defaultSettings = mapOf(
            "theme" to "dark",
            "wallpaper" to "default",
            "font_size" to "medium",
            "language" to "en",
            "animations" to "true",
            "high_contrast" to "false",
            "security_notifications" to "true",
            "disappearing_time" to "off",
            "message_tone" to "default",
            "group_tone" to "default",
            "call_tone" to "default",
            "auto_download_wifi" to "true",
            "auto_download_cellular" to "false",
            "usage_msgs_sent" to "0",
            "usage_msgs_received" to "0",
            "usage_bytes_sent" to "0",
            "usage_bytes_received" to "0"
        )
        for ((key, value) in defaultSettings) {
            if (dao.getSettingValue(key) == null) {
                dao.insertSetting(UserSetting(key, value))
            }
        }

        // Initialize Simulated Profiles
        val simulatedProfiles = listOf(
            Profile("+18005550199", "Aero Customer Care", "🏢", "Official support of Aero Strange International Platforms.", isCurrentUser = false, isSimulated = true),
            Profile("+14155550100", "Strange Explorer", "🛸", "Seeking out weird anomalies. Let's chat!", isCurrentUser = false, isSimulated = true),
            Profile("+15105550123", "Alice Vance", "👩‍💻", "Android developer exploring secure solutions.", isCurrentUser = false, isSimulated = true),
            Profile("+16505550145", "Bob Sterling", "🕵️", "Focusing on security notifications and end-to-end encryption.", isCurrentUser = false, isSimulated = true),
            Profile("+19005550999", "Aero Strange AI", "🤖", "Powered by Aero Strange AI. Ask me anything!", isCurrentUser = false, isSimulated = true)
        )
        dao.insertProfiles(simulatedProfiles)
    }

    private fun startDisappearingMessagesWorker() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    dao.deleteExpiredMessages(System.currentTimeMillis())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    // --- Authentication & Registration Flow ---
    suspend fun registerOrLogin(phone: String, name: String): Result<Profile> {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) return Result.failure(Exception("Phone number cannot be empty"))

        // Check if number is a simulated user
        val existingProfile = dao.getProfileByPhone(cleanPhone)
        if (existingProfile != null) {
            if (existingProfile.isSimulated) {
                return Result.failure(Exception("This number is already registered by another Aero Strange subscriber on this platform."))
            } else if (!existingProfile.isCurrentUser) {
                return Result.failure(Exception("Another subscriber is already registered with this phone number."))
            } else {
                // Already current user, update name if needed
                val updated = existingProfile.copy(name = name)
                dao.insertProfile(updated)
                return Result.success(updated)
            }
        }

        // Create new profile as Current User
        val newProfile = Profile(
            phone = cleanPhone,
            name = name,
            avatar = "👤",
            about = "Hey there! I am using Aero Strange Chats.",
            isCurrentUser = true,
            isSimulated = false
        )
        dao.insertProfile(newProfile)
        
        // Track stats
        incrementBytesSent(512)

        return Result.success(newProfile)
    }

    suspend fun updateCurrentUserProfile(name: String, about: String, avatar: String) {
        val user = dao.getCurrentUser() ?: return
        dao.updateProfile(user.phone, name, about, avatar)
    }

    suspend fun signOut() {
        val user = dao.getCurrentUser() ?: return
        dao.insertProfile(user.copy(isCurrentUser = false))
    }

    suspend fun authenticateWithFirebase(
        email: String,
        password: String,
        isSignUp: Boolean,
        name: String,
        phone: String
    ): Result<Profile> {
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val localPhone = if (cleanPhone.isNotEmpty()) cleanPhone else "+1555" + Math.abs(cleanEmail.hashCode())

        val auth = firebaseAuth
        if (auth != null && cleanEmail.isNotEmpty() && password.isNotEmpty()) {
            return try {
                val authResult = if (isSignUp) {
                    auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                } else {
                    auth.signInWithEmailAndPassword(cleanEmail, password).await()
                }
                
                val firebaseUser = authResult.user
                val finalName = if (name.isNotEmpty()) name else (firebaseUser?.displayName ?: cleanEmail.substringBefore("@"))
                
                val profile = Profile(
                    phone = localPhone,
                    name = finalName,
                    avatar = "👤",
                    about = "Aero Strange Profile verified by Firebase Auth",
                    isCurrentUser = true,
                    isSimulated = false
                )
                dao.insertProfile(profile)
                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local fallback simulation if Firebase not initialized
            delay(800)
            if (cleanEmail.isEmpty()) {
                return Result.failure(Exception("Email cannot be empty"))
            }
            if (password.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters"))
            }
            val finalName = if (name.isNotEmpty()) name else cleanEmail.substringBefore("@")
            val profile = Profile(
                phone = localPhone,
                name = finalName,
                avatar = "👤",
                about = "Aero Secure Local Gateway (No Firebase Server Detected)",
                isCurrentUser = true,
                isSimulated = false
            )
            dao.insertProfile(profile)
            return Result.success(profile)
        }
    }

    private val activeListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    fun startRealtimeFirestoreSync(conversationId: String) {
        val fs = firestore ?: return
        if (activeListeners.containsKey(conversationId)) return

        try {
            val listener = fs.collection("chats")
                .document(conversationId)
                .collection("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        coroutineScope.launch {
                            val user = dao.getCurrentUser()
                            for (docChange in snapshots.documentChanges) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
                                    val doc = docChange.document
                                    val msgId = doc.getString("id") ?: continue
                                    val senderPhone = doc.getString("senderPhone") ?: ""
                                    
                                    // Only insert if it's from another user
                                    if (senderPhone != user?.phone) {
                                        val message = Message(
                                            id = msgId,
                                            conversationId = doc.getString("conversationId") ?: conversationId,
                                            senderPhone = senderPhone,
                                            senderName = doc.getString("senderName") ?: "Unknown",
                                            text = doc.getString("text") ?: "",
                                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                            isRead = doc.getBoolean("isRead") ?: false
                                        )
                                        dao.insertMessage(message)
                                        dao.updateConversationLastMessage(conversationId, message.text, message.timestamp)
                                    }
                                }
                            }
                        }
                    }
                }
            activeListeners[conversationId] = listener
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Chat & Group Management ---
    fun observeMessages(conversationId: String): Flow<List<Message>> {
        startRealtimeFirestoreSync(conversationId)
        return dao.observeLast50MessagesForConversation(conversationId)
    }

    fun observeConversationMembers(conversationId: String): Flow<List<String>> {
        return dao.observeMemberPhones(conversationId)
    }

    suspend fun getProfile(phone: String): Profile? {
        return dao.getProfileByPhone(phone)
    }

    suspend fun createDirectChat(targetPhone: String): Result<Conversation> {
        val currentUser = dao.getCurrentUser() ?: return Result.failure(Exception("Not authenticated"))
        val targetProfile = dao.getProfileByPhone(targetPhone) ?: return Result.failure(Exception("Contact not found"))

        if (targetPhone == currentUser.phone) {
            return Result.failure(Exception("You cannot start a chat with yourself"))
        }

        val convId = if (currentUser.phone < targetPhone) {
            "${currentUser.phone}_${targetPhone}"
        } else {
            "${targetPhone}_${currentUser.phone}"
        }

        val existing = dao.getConversationById(convId)
        if (existing != null) {
            return Result.success(existing)
        }

        val newConv = Conversation(
            id = convId,
            name = null,
            isGroup = false,
            groupAvatar = null,
            lastMessageText = "Chat started",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        dao.insertConversation(newConv)

        dao.insertConversationMembers(listOf(
            ConversationMember(convId, currentUser.phone),
            ConversationMember(convId, targetPhone)
        ))

        return Result.success(newConv)
    }

    suspend fun createGroupChat(name: String, members: List<String>, avatar: String = "👥"): Result<Conversation> {
        val currentUser = dao.getCurrentUser() ?: return Result.failure(Exception("Not authenticated"))
        if (name.trim().isEmpty()) return Result.failure(Exception("Group name cannot be empty"))

        val convId = "group_${UUID.randomUUID()}"
        val newConv = Conversation(
            id = convId,
            name = name,
            isGroup = true,
            groupAvatar = avatar,
            lastMessageText = "Group created",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        dao.insertConversation(newConv)

        val memberList = (members + currentUser.phone).distinct().map {
            ConversationMember(convId, it)
        }
        dao.insertConversationMembers(memberList)

        sendMessage(convId, "Group created with members", isSystem = true)

        return Result.success(newConv)
    }

    suspend fun addMemberToGroup(conversationId: String, phone: String) {
        dao.insertConversationMembers(listOf(ConversationMember(conversationId, phone)))
        val profile = dao.getProfileByPhone(phone)
        sendMessage(conversationId, "${profile?.name ?: phone} was added to the group.", isSystem = true)
    }

    suspend fun removeMemberFromGroup(conversationId: String, phone: String) {
        dao.removeMember(conversationId, phone)
        val profile = dao.getProfileByPhone(phone)
        sendMessage(conversationId, "${profile?.name ?: phone} was removed from the group.", isSystem = true)
    }

    // --- Messaging Flows ---
    suspend fun sendMessage(conversationId: String, text: String, isSystem: Boolean = false) {
        val currentUser = dao.getCurrentUser()
        val senderPhone = if (isSystem) "system" else (currentUser?.phone ?: "unknown")
        val senderName = if (isSystem) "System" else (currentUser?.name ?: "Unknown")

        // Check disappearing message time
        val disappearingOption = dao.getSettingValue("disappearing_time") ?: "off"
        val disappearingAt = getDisappearingTimestamp(disappearingOption)

        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPhone = senderPhone,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = isSystem,
            disappearingAt = disappearingAt
        )
        dao.insertMessage(message)
        dao.updateConversationLastMessage(conversationId, text, System.currentTimeMillis())

        // Real-time Sync with Firestore if active
        firestore?.let { fs ->
            coroutineScope.launch {
                try {
                    val firestoreMsg = mapOf(
                        "id" to message.id,
                        "conversationId" to message.conversationId,
                        "senderPhone" to message.senderPhone,
                        "senderName" to message.senderName,
                        "text" to message.text,
                        "timestamp" to message.timestamp,
                        "isRead" to message.isRead
                    )
                    fs.collection("chats")
                        .document(conversationId)
                        .collection("messages")
                        .document(message.id)
                        .set(firestoreMsg)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (!isSystem) {
            incrementSentMessages()
            incrementBytesSent(text.length * 2)

            // Trigger AI response if the conversation involves a simulated contact
            triggerSimulatedResponses(conversationId, text)
        }
    }

    private fun getDisappearingTimestamp(option: String): Long? {
        val now = System.currentTimeMillis()
        return when (option) {
            "1m" -> now + 60_000
            "5m" -> now + 5 * 60_000
            "1h" -> now + 3600_000
            "1d" -> now + 24 * 3600_000
            else -> null
        }
    }

    private fun triggerSimulatedResponses(conversationId: String, userMessage: String) {
        coroutineScope.launch {
            delay(1000) // Delay to feel like natural typing speed
            val currentUser = dao.getCurrentUser() ?: return@launch
            val conv = dao.getConversationById(conversationId) ?: return@launch
            val members = dao.getMembersByConversationId(conversationId)

            val otherMembers = members.filter { it.userPhone != currentUser.phone }
            if (otherMembers.isEmpty()) return@launch

            if (conv.isGroup) {
                // In group, pick a random simulated member to reply
                val simulatedMemberPhones = otherMembers.map { it.userPhone }
                val targetPhone = simulatedMemberPhones.random()
                val senderProfile = dao.getProfileByPhone(targetPhone) ?: return@launch

                if (senderProfile.isSimulated) {
                    val history = getConversationHistory(conversationId)
                    val aiReply = GeminiApiClient.generateAIResponse(senderProfile.name, senderProfile.about, history)
                    deliverSimulatedMessage(conversationId, senderProfile, aiReply)
                }
            } else {
                // Direct chat
                val targetPhone = otherMembers.first().userPhone
                val senderProfile = dao.getProfileByPhone(targetPhone) ?: return@launch

                if (senderProfile.isSimulated) {
                    val history = getConversationHistory(conversationId)
                    val aiReply = GeminiApiClient.generateAIResponse(senderProfile.name, senderProfile.about, history)
                    deliverSimulatedMessage(conversationId, senderProfile, aiReply)
                }
            }
        }
    }

    private suspend fun getConversationHistory(conversationId: String): List<Pair<String, String>> {
        val list = dao.getLatestMessage(conversationId)?.let {
            listOf(it)
        } ?: emptyList()
        // For simple history mapping
        return list.map { it.senderName to it.text }
    }

    private suspend fun deliverSimulatedMessage(conversationId: String, sender: Profile, text: String) {
        val disappearingOption = dao.getSettingValue("disappearing_time") ?: "off"
        val disappearingAt = getDisappearingTimestamp(disappearingOption)

        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPhone = sender.phone,
            senderName = sender.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            disappearingAt = disappearingAt
        )
        dao.insertMessage(message)
        dao.updateConversationLastMessage(conversationId, text, System.currentTimeMillis())

        incrementReceivedMessages()
        incrementBytesReceived(text.length * 2)
    }

    // --- Block List Operations ---
    suspend fun blockUser(phone: String) {
        dao.blockPhone(BlockedAccount(phone))
    }

    suspend fun unblockUser(phone: String) {
        dao.unblockPhone(phone)
    }

    suspend fun isUserBlocked(phone: String): Boolean {
        return dao.isBlocked(phone)
    }

    // --- Call Log Operations ---
    suspend fun logCall(callerPhone: String, receiverPhone: String, isVoice: Boolean, status: String, durationSeconds: Int = 0) {
        val callLog = CallLog(
            id = UUID.randomUUID().toString(),
            callerPhone = callerPhone,
            receiverPhone = receiverPhone,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            isVoice = isVoice,
            status = status
        )
        dao.insertCallLog(callLog)
    }

    suspend fun clearCallHistory() {
        dao.clearAllCallLogs()
    }

    // --- Settings Persistence ---
    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(UserSetting(key, value))
    }

    suspend fun getSetting(key: String): String? {
        return dao.getSettingValue(key)
    }

    suspend fun clearAllChats() {
        val conversationsList = dao.observeConversations().first()
        for (c in conversationsList) {
            dao.clearChatMessages(c.id)
            dao.updateConversationLastMessage(c.id, "Chat cleared", System.currentTimeMillis())
        }
    }

    // --- Statistics Helper functions ---
    private suspend fun incrementSentMessages() {
        val current = dao.getSettingValue("usage_msgs_sent")?.toIntOrNull() ?: 0
        dao.insertSetting(UserSetting("usage_msgs_sent", (current + 1).toString()))
    }

    private suspend fun incrementReceivedMessages() {
        val current = dao.getSettingValue("usage_msgs_received")?.toIntOrNull() ?: 0
        dao.insertSetting(UserSetting("usage_msgs_received", (current + 1).toString()))
    }

    private suspend fun incrementBytesSent(bytes: Int) {
        val current = dao.getSettingValue("usage_bytes_sent")?.toIntOrNull() ?: 0
        dao.insertSetting(UserSetting("usage_bytes_sent", (current + bytes).toString()))
    }

    private suspend fun incrementBytesReceived(bytes: Int) {
        val current = dao.getSettingValue("usage_bytes_received")?.toIntOrNull() ?: 0
        dao.insertSetting(UserSetting("usage_bytes_received", (current + bytes).toString()))
    }

    suspend fun resetNetworkUsage() {
        dao.insertSetting(UserSetting("usage_msgs_sent", "0"))
        dao.insertSetting(UserSetting("usage_msgs_received", "0"))
        dao.insertSetting(UserSetting("usage_bytes_sent", "0"))
        dao.insertSetting(UserSetting("usage_bytes_received", "0"))
    }

    suspend fun clearChatHistory(conversationId: String) {
        dao.clearChatMessages(conversationId)
        dao.updateConversationLastMessage(conversationId, "Chat history cleared", System.currentTimeMillis())
    }

    suspend fun registerSimulatedContact(profile: Profile) {
        dao.insertProfile(profile)
    }

    suspend fun getSimulatedProfiles(): List<Profile> {
        return dao.getSimulatedProfiles()
    }
}
