package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.CallLog
import com.example.database.Conversation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChatViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Chats, 1 = Calls, 2 = Settings
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    val conversations by viewModel.conversations.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage = settings["language"] ?: "en"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.getTranslation("title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.getTranslation("subtitle").uppercase(),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Direct Chat")
                    }
                    IconButton(onClick = { showNewGroupDialog = true }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("main_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                    label = { Text(viewModel.getTranslation("tab_chats")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Dialpad, contentDescription = null) },
                    label = { Text(viewModel.getTranslation("tab_calls")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(viewModel.getTranslation("tab_settings")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "New Chat")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatsTab(
                    viewModel = viewModel,
                    conversations = conversations,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it }
                )
                1 -> CallsTab(
                    viewModel = viewModel,
                    callLogs = callLogs
                )
                2 -> SettingsTab(viewModel = viewModel)
            }
        }
    }

    // Direct Chat Creation Dialog
    if (showNewChatDialog) {
        var phoneInput by remember { mutableStateOf("") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showNewChatDialog = false
                dialogError = null
            },
            title = { Text(viewModel.getTranslation("new_chat_title")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(viewModel.getTranslation("phone_placeholder")) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInput.trim().isEmpty()) {
                            dialogError = "Please enter a phone number"
                            return@Button
                        }
                        coroutineScope.launch {
                            val result = viewModel.repository.createDirectChat(phoneInput.trim())
                            if (result.isSuccess) {
                                showNewChatDialog = false
                                viewModel.selectConversation(result.getOrThrow().id)
                            } else {
                                dialogError = result.exceptionOrNull()?.message ?: "Could not start chat"
                            }
                        }
                    }
                ) {
                    Text("Secure Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Group Chat Creation Dialog
    if (showNewGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        var simulatedContacts by remember { mutableStateOf<List<com.example.database.Profile>>(emptyList()) }
        val selectedMembers = remember { mutableStateListOf<String>() }

        // Fetch contacts on open
        LaunchedEffect(Unit) {
            simulatedContacts = viewModel.repository.getSimulatedProfilesList()
        }

        AlertDialog(
            onDismissRequest = {
                showNewGroupDialog = false
                dialogError = null
            },
            title = { Text(viewModel.getTranslation("new_group_title")) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text(viewModel.getTranslation("group_name_placeholder")) },
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Contacts to Add:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(simulatedContacts) { contact ->
                            val isSelected = selectedMembers.contains(contact.phone)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selectedMembers.remove(contact.phone)
                                        } else {
                                            selectedMembers.add(contact.phone)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) {
                                            selectedMembers.remove(contact.phone)
                                        } else {
                                            selectedMembers.add(contact.phone)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(contact.avatar, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(contact.name, fontWeight = FontWeight.Medium)
                                    Text(contact.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupNameInput.trim().isEmpty()) {
                            dialogError = "Please enter a group name"
                            return@Button
                        }
                        if (selectedMembers.isEmpty()) {
                            dialogError = "Please select at least one contact"
                            return@Button
                        }
                        coroutineScope.launch {
                            val result = viewModel.repository.createGroupChat(groupNameInput.trim(), selectedMembers.toList())
                            if (result.isSuccess) {
                                showNewGroupDialog = false
                                viewModel.selectConversation(result.getOrThrow().id)
                            } else {
                                dialogError = result.exceptionOrNull()?.message ?: "Could not create group"
                            }
                        }
                    }
                ) {
                    Text("Create Group")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Simulated repository getter helper
private suspend fun com.example.repository.ChatRepository.getSimulatedProfilesList(): List<com.example.database.Profile> {
    return getSimulatedProfiles()
}

@Composable
fun ChatsTab(
    viewModel: ChatViewModel,
    conversations: List<Conversation>,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    val filteredConv = conversations.filter {
        (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.lastMessageText ?: "").contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search chats or messages on Aero secure index...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("chats_search_input")
        )

        // Security Banner (Sleek Interface)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "End-to-End Encrypted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your messages and calls are secured by Aero Strange International Platforms.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        lineHeight = 12.sp
                    )
                }
            }
        }

        if (filteredConv.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.getTranslation("empty_chats"),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredConv) { conv ->
                    ConversationItem(viewModel, conv)
                }
            }
        }
    }
}

@Composable
fun ConversationItem(viewModel: ChatViewModel, conv: Conversation) {
    var contactProfile by remember { mutableStateOf<com.example.database.Profile?>(null) }

    LaunchedEffect(conv) {
        if (!conv.isGroup) {
            val user = viewModel.currentUser.value ?: return@LaunchedEffect
            val phones = conv.id.split("_")
            val targetPhone = phones.find { it != user.phone } ?: phones.first()
            contactProfile = viewModel.repository.getProfile(targetPhone)
        }
    }

    val title = if (conv.isGroup) conv.name ?: "Aero Group" else contactProfile?.name ?: "Secure User"
    val avatar = if (conv.isGroup) conv.groupAvatar ?: "👥" else contactProfile?.avatar ?: "👤"
    val timestamp = conv.lastMessageTimestamp?.let { formatTime(it) } ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectConversation(conv.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(avatar, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = timestamp,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conv.lastMessageText ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Encryption Lock Badge
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(start = 86.dp))
    }
}

@Composable
fun CallsTab(viewModel: ChatViewModel, callLogs: List<CallLog>) {
    var dialNumberInput by remember { mutableStateOf("") }
    val settings by viewModel.settings.collectAsState()
    val highContrast = settings["high_contrast"] == "true"

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Segment: Standalone Numeric Dial Pad (Fully requested!)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Number screen
                OutlinedTextField(
                    value = dialNumberInput,
                    onValueChange = { dialNumberInput = it },
                    placeholder = { Text("Dial Securely via Internet...") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    trailingIcon = {
                        if (dialNumberInput.isNotEmpty()) {
                            IconButton(onClick = { dialNumberInput = dialNumberInput.dropLast(1) }) {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dial_screen_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Standard 12-key Layout
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowKeys.forEach { key ->
                            IconButton(
                                onClick = { dialNumberInput += key },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Call Action buttons (Voice and Video)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (dialNumberInput.isNotEmpty()) {
                                viewModel.dialNumber(dialNumberInput, isVoice = true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Voice", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (dialNumberInput.isNotEmpty()) {
                                viewModel.dialNumber(dialNumberInput, isVoice = false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 2.dp)

        // Lower Segment: Calls History list
        Text(
            text = "CALL HISTORY LOGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.getTranslation("empty_calls"),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(callLogs) { log ->
                    CallLogItem(viewModel, log)
                }
            }
        }
    }
}

@Composable
fun CallLogItem(viewModel: ChatViewModel, log: CallLog) {
    var contactProfile by remember { mutableStateOf<com.example.database.Profile?>(null) }

    LaunchedEffect(log) {
        contactProfile = viewModel.repository.getProfile(log.receiverPhone)
            ?: viewModel.repository.getProfile(log.callerPhone)
    }

    val title = contactProfile?.name ?: log.receiverPhone
    val avatar = contactProfile?.avatar ?: "👤"
    val timestamp = formatTime(log.timestamp)

    val iconColor = when (log.status) {
        "Missed", "Declined" -> MaterialTheme.colorScheme.error
        "Completed" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val icon = when {
        log.status == "Missed" -> Icons.Default.CallMissed
        log.status == "Outgoing" -> Icons.Default.CallMade
        else -> Icons.Default.CallReceived
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${log.status} • $timestamp",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        IconButton(onClick = { viewModel.dialNumber(log.receiverPhone, isVoice = log.isVoice) }) {
            Icon(
                imageVector = if (log.isVoice) Icons.Default.Call else Icons.Default.VideoCall,
                contentDescription = "Redial",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
