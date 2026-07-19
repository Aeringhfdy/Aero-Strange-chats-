package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Message
import com.example.database.Profile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val convId by viewModel.selectedConversationId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val messages by viewModel.getMessagesForSelected().collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showInfoSheet by remember { mutableStateOf(false) }

    val activeConv = conversations.find { it.id == convId } ?: return
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Retrieve other profile if direct chat
    var otherProfile by remember { mutableStateOf<Profile?>(null) }
    var isOtherBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(convId, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(activeConv, currentUser) {
        val user = currentUser
        if (!activeConv.isGroup && user != null) {
            val phones = activeConv.id.split("_")
            val targetPhone = phones.find { it != user.phone } ?: phones.first()
            otherProfile = viewModel.repository.getProfile(targetPhone)
            isOtherBlocked = otherProfile?.let { viewModel.repository.isUserBlocked(it.phone) } ?: false
        }
    }

    // Load Wallpaper Background from Settings
    val wallpaperKey = settings["wallpaper"] ?: "default"
    val wallpaperColor = when (wallpaperKey) {
        "solid_green" -> WallpaperGreen
        "solid_blue" -> WallpaperBlue
        "solid_dark" -> WallpaperDark
        "solid_teal" -> WallpaperTeal
        "solid_burgundy" -> WallpaperBurgundy
        else -> MaterialTheme.colorScheme.background
    }

    // Load custom font size
    val fontSizeKey = settings["font_size"] ?: "medium"
    val fontSize = when (fontSizeKey) {
        "small" -> 13.sp
        "large" -> 18.sp
        else -> 15.sp
    }

    val themeKey = settings["theme"] ?: "dark"
    val isDarkTheme = themeKey == "dark"

    val title = if (activeConv.isGroup) activeConv.name ?: "Aero Group" else otherProfile?.name ?: "Secure Chat"
    val avatar = if (activeConv.isGroup) activeConv.groupAvatar ?: "👥" else otherProfile?.avatar ?: "👤"
    val subInfo = if (activeConv.isGroup) "Group • Aero platform" else if (isOtherBlocked) "Blocked on your list" else "Secure Terminal Online"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInfoSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatar, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subInfo,
                                fontSize = 11.sp,
                                color = if (isOtherBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("main_chats") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (activeConv.isGroup) {
                                viewModel.startGroupCall(activeConv.name ?: "Group Call", isVoice = true)
                            } else if (otherProfile != null) {
                                viewModel.startCall(otherProfile!!, isVoice = true)
                            }
                        },
                        enabled = activeConv.isGroup || (!isOtherBlocked && otherProfile != null)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call")
                    }
                    IconButton(
                        onClick = {
                            if (activeConv.isGroup) {
                                viewModel.startGroupCall(activeConv.name ?: "Group Call", isVoice = false)
                            } else if (otherProfile != null) {
                                viewModel.startCall(otherProfile!!, isVoice = false)
                            }
                        },
                        enabled = activeConv.isGroup || (!isOtherBlocked && otherProfile != null)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                    }
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info Details")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("chat_top_bar")
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOtherBlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "You have blocked this contact. Unblock them in Contact Info to type a message.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Aero encrypted message...") },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (textInput.isNotEmpty()) {
                                            viewModel.sendMessage(textInput)
                                            textInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(wallpaperColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // System E2EE Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Messages are end-to-end encrypted by Aero Strange cryptographic keys.",
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(messages) { msg ->
                    MessageBubble(
                        msg = msg,
                        isCurrentUser = msg.senderPhone == currentUser?.phone,
                        fontSize = fontSize,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }

    // Info Drawer / Bottom Sheet simulation
    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatar, fontSize = 54.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    text = if (activeConv.isGroup) "Group ID: ${activeConv.id}" else otherProfile?.phone ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (activeConv.isGroup) "Aero Strange Group Chat Channel" else otherProfile?.about ?: "",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))

                // Disappearing message current setting status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Disappearing Messages", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = settings["disappearing_time"]?.uppercase() ?: "OFF",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                if (!activeConv.isGroup && otherProfile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    if (isOtherBlocked) {
                                        viewModel.repository.unblockUser(otherProfile!!.phone)
                                        isOtherBlocked = false
                                    } else {
                                        viewModel.repository.blockUser(otherProfile!!.phone)
                                        isOtherBlocked = true
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOtherBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isOtherBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isOtherBlocked) "Unblock ${otherProfile!!.name}" else "Block ${otherProfile!!.name}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOtherBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.clearChat(activeConv.id)
                            showInfoSheet = false
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Clear Chat History", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message, isCurrentUser: Boolean, fontSize: androidx.compose.ui.unit.TextUnit, isDarkTheme: Boolean) {
    val bubbleBg = if (isCurrentUser) {
        if (isDarkTheme) BubbleCurrentUserDark else BubbleCurrentUserLight
    } else {
        if (isDarkTheme) BubbleOtherUserDark else BubbleOtherUserLight
    }

    val bubbleTextColor = if (isCurrentUser) {
        if (isDarkTheme) Color.White else Color.Black
    } else {
        if (isDarkTheme) Color.White else Color.Black
    }

    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Sender name (only in group chats when not current user)
            if (!isCurrentUser && msg.senderPhone != "system" && msg.senderPhone != "unknown") {
                Text(
                    text = msg.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                color = bubbleBg,
                shape = shape,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = msg.text,
                        fontSize = fontSize,
                        color = bubbleTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = formatTime(msg.timestamp),
                            fontSize = 9.sp,
                            color = bubbleTextColor.copy(alpha = 0.5f)
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = AeroLightGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
