package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Profiles
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): Profile?

    @Query("SELECT * FROM profiles WHERE isCurrentUser = 1")
    fun observeCurrentUser(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE phone = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): Profile?

    @Query("SELECT * FROM profiles WHERE isSimulated = 1")
    suspend fun getSimulatedProfiles(): List<Profile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<Profile>)

    @Query("UPDATE profiles SET name = :name, about = :about, avatar = :avatar WHERE phone = :phone")
    suspend fun updateProfile(phone: String, name: String, about: String, avatar: String)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun observeConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTimestamp = :timestamp WHERE id = :id")
    suspend fun updateConversationLastMessage(id: String, text: String, timestamp: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    // Conversation Members
    @Query("SELECT * FROM conversation_members WHERE conversationId = :conversationId")
    suspend fun getMembersByConversationId(conversationId: String): List<ConversationMember>

    @Query("SELECT userPhone FROM conversation_members WHERE conversationId = :conversationId")
    fun observeMemberPhones(conversationId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversationMembers(members: List<ConversationMember>)

    @Query("DELETE FROM conversation_members WHERE conversationId = :conversationId AND userPhone = :userPhone")
    suspend fun removeMember(conversationId: String, userPhone: String)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 50) ORDER BY timestamp ASC")
    fun observeLast50MessagesForConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId AND senderPhone != :currentUserPhone")
    suspend fun markMessagesAsRead(conversationId: String, currentUserPhone: String)

    @Query("DELETE FROM messages WHERE disappearingAt IS NOT NULL AND disappearingAt <= :now")
    suspend fun deleteExpiredMessages(now: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearChatMessages(conversationId: String)

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun observeCallLogs(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()

    // Block List
    @Query("SELECT * FROM blocked_accounts")
    fun observeBlockedAccounts(): Flow<List<BlockedAccount>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_accounts WHERE blockedPhone = :phone)")
    suspend fun isBlocked(phone: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockPhone(blocked: BlockedAccount)

    @Query("DELETE FROM blocked_accounts WHERE blockedPhone = :phone")
    suspend fun unblockPhone(phone: String)

    // Settings
    @Query("SELECT * FROM user_settings")
    fun observeUserSettings(): Flow<List<UserSetting>>

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSetting)
}
