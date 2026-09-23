package com.example.chatapp.data.remote

import com.example.chatapp.data.model.Chat
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ---- DTOs (match FastAPI JSON keys) ----

data class ApiUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: String = ""
) {
    fun toDomain() = User(uid, displayName, email, photoUrl, online, lastSeen)
}

data class ApiChat(
    val chatId: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val lastSenderId: String = "",
    val photoUrl: String = "",
    val createdBy: String = ""
) {
    fun toDomain() = Chat(chatId, name, isGroup, members, lastMessage, lastMessageTime,
        lastSenderId, photoUrl, createdBy)
}

data class ApiMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: String = "",
    val seenBy: List<String> = emptyList(),
    val fileName: String = "",
    val fileSize: Long = 0L
) {
    fun toDomain() = Message(messageId, chatId, senderId, senderName, text,
        if (imageUrl.isNotBlank()) ServerConfig.absolute(imageUrl) else "", timestamp,
        seenBy = seenBy, fileName = fileName, fileSize = fileSize)
}

data class AuthResponse(val token: String = "", val user: ApiUser = ApiUser())

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class UpdateNameRequest(val displayName: String)
data class PresenceRequest(val online: Boolean)
data class DeviceRequest(val fcmToken: String)
data class DirectRequest(val otherUid: String)
data class GroupRequest(val name: String, val memberUids: List<String>)
data class TextRequest(val text: String)

// ---- Endpoints ----

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("api/me")
    suspend fun me(): ApiUser

    @PUT("api/me")
    suspend fun updateMe(@Body body: UpdateNameRequest): ApiUser

    @POST("api/presence")
    suspend fun presence(@Body body: PresenceRequest): Map<String, Boolean>

    @POST("api/device")
    suspend fun device(@Body body: DeviceRequest): Map<String, Boolean>

    @GET("api/users")
    suspend fun users(): List<ApiUser>

    @GET("api/chats")
    suspend fun chats(): List<ApiChat>

    @GET("api/chats/{id}")
    suspend fun chat(@Path("id") chatId: String): ApiChat

    @GET("api/users/{uid}")
    suspend fun user(@Path("uid") uid: String): ApiUser

    @POST("api/chats/direct")
    suspend fun direct(@Body body: DirectRequest): ApiChat

    @POST("api/chats/group")
    suspend fun group(@Body body: GroupRequest): ApiChat

    @GET("api/chats/{id}/messages")
    suspend fun messages(@Path("id") chatId: String, @Query("limit") limit: Int = 200): List<ApiMessage>

    @POST("api/chats/{id}/messages")
    suspend fun sendText(@Path("id") chatId: String, @Body body: TextRequest): ApiMessage

    @Multipart
    @POST("api/chats/{id}/image")
    suspend fun sendImage(@Path("id") chatId: String, @Part file: MultipartBody.Part): ApiMessage

    @Multipart
    @POST("api/chats/{id}/file")
    suspend fun sendFile(@Path("id") chatId: String, @Part file: MultipartBody.Part): ApiMessage
}
