package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SettingsTab(viewModel: ChatViewModel) {
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "settings_subscreen"
    ) { subScreen ->
        if (subScreen == null) {
            MainSettingsList(
                viewModel = viewModel,
                currentUser = currentUser,
                onSubScreenSelect = { activeSubScreen = it }
            )
        } else {
            SubSettingsScreen(
                viewModel = viewModel,
                screenKey = subScreen,
                settings = settings,
                onBack = { activeSubScreen = null }
            )
        }
    }
}

@Composable
fun MainSettingsList(
    viewModel: ChatViewModel,
    currentUser: com.example.database.Profile?,
    onSubScreenSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isEditingProfile by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(currentUser?.name ?: "") }
    var editAbout by remember { mutableStateOf(currentUser?.about ?: "") }
    var editAvatar by remember { mutableStateOf(currentUser?.avatar ?: "👤") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingProfile) {
                    // Profile edit view
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👤", "😎", "🛡️", "🛸", "🤖", "🦊", "👩‍💻", "🕵️").forEach { emoji ->
                            Text(
                                emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clickable { editAvatar = emoji }
                                    .background(
                                        if (editAvatar == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editAbout,
                        onValueChange = { editAbout = it },
                        label = { Text("About status") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditingProfile = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.repository.updateCurrentUserProfile(editName, editAbout, editAvatar)
                                isEditingProfile = false
                            }
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    // Profile display view
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentUser?.avatar ?: "👤", fontSize = 42.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.name ?: "Secure Subscriber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = currentUser?.phone ?: "",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = currentUser?.about ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            editName = currentUser?.name ?: ""
                            editAbout = currentUser?.about ?: ""
                            editAvatar = currentUser?.avatar ?: "👤"
                            isEditingProfile = true
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile Status")
                    }
                }
            }
        }

        // Settings items list
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_account"),
            icon = Icons.Default.Security,
            onClick = { onSubScreenSelect("account") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_privacy"),
            icon = Icons.Default.PrivacyTip,
            onClick = { onSubScreenSelect("privacy") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_chats"),
            icon = Icons.Default.Wallpaper,
            onClick = { onSubScreenSelect("chats") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_notifications"),
            icon = Icons.Default.NotificationsActive,
            onClick = { onSubScreenSelect("notifications") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_storage"),
            icon = Icons.Default.Storage,
            onClick = { onSubScreenSelect("storage") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_accessibility"),
            icon = Icons.Default.Accessibility,
            onClick = { onSubScreenSelect("accessibility") }
        )
        SettingsMenuItem(
            title = viewModel.getTranslation("settings_about"),
            icon = Icons.Default.CorporateFare,
            onClick = { onSubScreenSelect("about") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Button(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings_sign_out_btn")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(viewModel.getTranslation("sign_out"), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubSettingsScreen(
    viewModel: ChatViewModel,
    screenKey: String,
    settings: Map<String, String>,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenKey) {
                            "account" -> viewModel.getTranslation("settings_account")
                            "privacy" -> viewModel.getTranslation("settings_privacy")
                            "chats" -> viewModel.getTranslation("settings_chats")
                            "notifications" -> viewModel.getTranslation("settings_notifications")
                            "storage" -> viewModel.getTranslation("settings_storage")
                            "accessibility" -> viewModel.getTranslation("settings_accessibility")
                            else -> viewModel.getTranslation("settings_about")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (screenKey) {
                "account" -> {
                    // Security notifications
                    val isSecNotif = settings["security_notifications"] == "true"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(viewModel.getTranslation("sec_notif_title"), fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = isSecNotif,
                                    onCheckedChange = {
                                        viewModel.updateSetting("security_notifications", it.toString())
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.getTranslation("sec_notif_desc"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Aero Terminal Encryption Keys", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Status: Active & Secure\n" +
                                "Algorithm: SHA-512 Terminal Encrypted\n" +
                                "Hardware Root: Android Keystore Signed",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                "privacy" -> {
                    // Disappearing messages timer
                    val disappearVal = settings["disappearing_time"] ?: "off"
                    Text(viewModel.getTranslation("disappearing_title"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = viewModel.getTranslation("disappearing_desc"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("off", "1m", "5m", "1h", "1d").forEach { option ->
                            Button(
                                onClick = { viewModel.updateSetting("disappearing_time", option) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (disappearVal == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = option.uppercase(),
                                    fontSize = 11.sp,
                                    color = if (disappearVal == option) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Blocked accounts view
                    val blockedAccounts by viewModel.blockedAccounts.collectAsState()
                    Text("Blocked Accounts (${blockedAccounts.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (blockedAccounts.isEmpty()) {
                        Text("No accounts are blocked.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        blockedAccounts.forEach { blocked ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(blocked.blockedPhone, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.unblockUser(blocked.blockedPhone)
                                    }
                                }) {
                                    Text("Unblock", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                "chats" -> {
                    // Wallpaper picker
                    Text("Chat Wallpapers", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val activeWall = settings["wallpaper"] ?: "default"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        mapOf(
                            "default" to Color.Gray,
                            "solid_teal" to WallpaperTeal,
                            "solid_blue" to WallpaperBlue,
                            "solid_dark" to WallpaperDark,
                            "solid_green" to WallpaperGreen,
                            "solid_burgundy" to WallpaperBurgundy
                        ).forEach { (key, color) ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .clickable { viewModel.updateSetting("wallpaper", key) }
                                    .background(
                                        if (activeWall == key) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activeWall == key) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font size picker
                    val activeFont = settings["font_size"] ?: "medium"
                    Text("Message Font Size", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("small", "medium", "large").forEach { size ->
                            Button(
                                onClick = { viewModel.updateSetting("font_size", size) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeFont == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = size.uppercase(),
                                    color = if (activeFont == size) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Chat history logs
                    Text("Chat Operations", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Button(
                        onClick = { viewModel.clearAllChatsAndHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Chats & History Logs")
                    }
                }

                "notifications" -> {
                    // Notification tones
                    val mTone = settings["message_tone"] ?: "default"
                    val gTone = settings["group_tone"] ?: "default"
                    val cTone = settings["call_tone"] ?: "default"

                    Text("Customize Alert Sound Tones", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    NotificationTonePicker(
                        label = "Message Tone",
                        current = mTone,
                        options = listOf("default", "chime", "bell", "none"),
                        onSelect = { viewModel.updateSetting("message_tone", it) }
                    )

                    NotificationTonePicker(
                        label = "Group Chat Tone",
                        current = gTone,
                        options = listOf("default", "whistle", "bounce", "none"),
                        onSelect = { viewModel.updateSetting("group_tone", it) }
                    )

                    NotificationTonePicker(
                        label = "VoIP Call Ringtone",
                        current = cTone,
                        options = listOf("default", "ringtone_classic", "ringtone_modern", "none"),
                        onSelect = { viewModel.updateSetting("call_tone", it) }
                    )
                }

                "storage" -> {
                    // Network usage statistics
                    val sentMsgs = settings["usage_msgs_sent"] ?: "0"
                    val recMsgs = settings["usage_msgs_received"] ?: "0"
                    val sentBytes = settings["usage_bytes_sent"]?.toLongOrNull() ?: 0L
                    val recBytes = settings["usage_bytes_received"]?.toLongOrNull() ?: 0L

                    val wifiAuto = settings["auto_download_wifi"] == "true"
                    val cellularAuto = settings["auto_download_cellular"] == "true"

                    Text("Aero Terminal Traffic Index", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Outgoing Secured Messages", fontSize = 13.sp)
                                Text(sentMsgs, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Incoming Decrypted Messages", fontSize = 13.sp)
                                Text(recMsgs, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Uploaded Telemetry Bytes", fontSize = 13.sp)
                                Text(formatBytes(sentBytes), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Downloaded Decrypted Bytes", fontSize = 13.sp)
                                Text(formatBytes(recBytes), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.resetNetworkStats() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Traffic Counters")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Auto Download media settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(viewModel.getTranslation("auto_wifi"), fontSize = 13.sp)
                                Checkbox(checked = wifiAuto, onCheckedChange = {
                                    viewModel.updateSetting("auto_download_wifi", it.toString())
                                })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(viewModel.getTranslation("auto_cell"), fontSize = 13.sp)
                                Checkbox(checked = cellularAuto, onCheckedChange = {
                                    viewModel.updateSetting("auto_download_cellular", it.toString())
                                })
                            }
                        }
                    }
                }

                "accessibility" -> {
                    // Increase contrast
                    val isHighContrast = settings["high_contrast"] == "true"
                    val isAnimations = settings["animations"] == "true"
                    val activeLang = settings["language"] ?: "en"

                    Text("Accessibility Toggles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(viewModel.getTranslation("contrast_title"), fontSize = 13.sp)
                                Switch(checked = isHighContrast, onCheckedChange = {
                                    viewModel.updateSetting("high_contrast", it.toString())
                                })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(viewModel.getTranslation("animations_title"), fontSize = 13.sp)
                                Switch(checked = isAnimations, onCheckedChange = {
                                    viewModel.updateSetting("animations", it.toString())
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(viewModel.getTranslation("language_title"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    val langs = mapOf(
                        "en" to "English",
                        "es" to "Español",
                        "fr" to "Français",
                        "de" to "Deutsch",
                        "ar" to "العربية",
                        "hi" to "हिन्दी",
                        "zh" to "中文",
                        "ja" to "日本語"
                    )

                    LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        langs.toList().forEach { (code, name) ->
                            item {
                                Button(
                                    onClick = { viewModel.updateSetting("language", code) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeLang == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        color = if (activeLang == code) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                "about" -> {
                    // Aero Strange branding
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
                        }
                    }

                    Text(
                        text = "AERO STRANGE CHATS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "VERSION 4.2.0 SECURED PROTOTYPE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "This software is fully engineered and operated by Aero Strange International Platforms.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "All digital assets, telemetry packets, and encrypted communication pipes transit through certified Aero Secure Routing Nodes. Direct WebRTC-simulated peer channels ensure extreme speed over Wi-Fi and mobile networks.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationTonePicker(
    label: String,
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var previewText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { opt ->
                Button(
                    onClick = {
                        onSelect(opt)
                        if (opt != "none") {
                            previewText = "Previewing: $opt alert! 🔔"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (current == opt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = opt.uppercase(),
                        fontSize = 9.sp,
                        color = if (current == opt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        AnimatedVisibility(visible = previewText != null) {
            previewText?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.2f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}

// Grid helper
@Composable
fun LazyVerticalGrid(
    columns: androidx.compose.foundation.lazy.grid.GridCells,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit
) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
