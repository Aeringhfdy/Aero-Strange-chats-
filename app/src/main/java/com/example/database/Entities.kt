package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val phone: String,
    val name: String,
    val avatar: String, // String representation of avatar (e.g. emoji, local identifier, or image)
    val about: String,
    val isCurrentUser: Boolean = false,
    val isSimulated: Boolean = false
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String, // Combination of phone numbers for direct chats, or UUID for groups
    val name: String?, // Group name, null for direct chats
    val isGroup: Boolean,
    val groupAvatar: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?
)

@Entity(tableName = "conversation_members", primaryKeys = ["conversationId", "userPhone"])
data class ConversationMember(
    val conversationId: String,
    val userPhone: String
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderPhone: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val disappearingAt: Long? = null // Timestamp when the message disappears
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey val id: String,
    val callerPhone: String,
    val receiverPhone: String, // Can be user phone or group ID
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isVoice: Boolean = true, // true for voice, false for video
    val status: String // "Missed", "Completed", "Declined", "Outgoing"
)

@Entity(tableName = "blocked_accounts")
data class BlockedAccount(
    @PrimaryKey val blockedPhone: String,
    val blockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSetting(
    @PrimaryKey val key: String,
    val value: String
)
