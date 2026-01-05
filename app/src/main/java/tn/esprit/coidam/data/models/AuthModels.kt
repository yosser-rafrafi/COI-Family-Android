package tn.esprit.coidam.data.models
data class AuthResponse(
    val access_token: String? = null,
    val user: UserResponse? = null,
    val companion: UserResponse? = null,
    val blind: UserResponse? = null,
    val message: String? = null,
    val error: String? = null,
    val options: List<UserOption>? = null
)

data class UserResponse(
    val _id: String? = null,
    val id: String? = null, // Some responses might use id
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val userType: String? = null,
    val linkedUserId: String? = null, // ✅ AJOUTÉ
    val photoUrl: String? = null
) {
    fun getUserId(): String {
        return _id ?: id ?: ""
    }
}

data class UserOption(
    val userId: String,
    val userType: String,
    val email: String? = null
)

data class SignUpResponse(
    val companion: UserResponse? = null,
    val blind: UserResponse? = null,
    val message: String? = null
)

data class ApiError(
    val error: String? = null,
    val message: String? = null,
    val statusCode: Int? = null
)

