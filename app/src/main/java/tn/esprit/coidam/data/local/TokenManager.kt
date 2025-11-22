package tn.esprit.coidam.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        private val LINKED_USER_ID_KEY = stringPreferencesKey("linked_user_id") // ✅ AJOUTÉ

    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    suspend fun getTokenSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first()
    }

    suspend fun saveUserId(userId: String?) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId as String
        }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }

    suspend fun getUserIdSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }

    suspend fun saveUserType(userType: String?) {
        context.dataStore.edit { preferences ->
            preferences[USER_TYPE_KEY] = userType as String
        }
    }

    fun getUserType(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]
        }
    }

    suspend fun getUserTypeSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]
        }.first()
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
    }

    suspend fun getUserEmailSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }.first()
    }

    suspend fun saveLinkedUserId(linkedUserId: String) {
        context.dataStore.edit { preferences ->
            preferences[LINKED_USER_ID_KEY] = linkedUserId
        }
    }

    suspend fun getLinkedUserIdSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[LINKED_USER_ID_KEY]
        }.first()
    }


    suspend fun isLoggedIn(): Boolean {
        val token = getTokenSync()
        val userId = getUserIdSync()
        return !token.isNullOrEmpty() && !userId.isNullOrEmpty()
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

