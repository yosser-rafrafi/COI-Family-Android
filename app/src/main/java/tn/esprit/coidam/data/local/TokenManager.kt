package tn.esprit.coidam.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        private val LINKED_USER_ID_KEY = stringPreferencesKey("linked_user_id")
        
        // Remember Me functionality
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        private val SAVED_EMAIL_KEY = stringPreferencesKey("saved_email")
        private val SAVED_PASSWORD_KEY = stringPreferencesKey("saved_password")

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
    
    // Remember Me functionality
    suspend fun saveRememberMe(remember: Boolean, email: String?, password: String?) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = remember
            if (remember && email != null) {
                preferences[SAVED_EMAIL_KEY] = email
                if (password != null) {
                    preferences[SAVED_PASSWORD_KEY] = password
                }
            } else {
                preferences.remove(SAVED_EMAIL_KEY)
                preferences.remove(SAVED_PASSWORD_KEY)
            }
        }
    }
    
    suspend fun getRememberMeSync(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[REMEMBER_ME_KEY] ?: false
        }.first()
    }
    
    suspend fun getSavedEmailSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[SAVED_EMAIL_KEY]
        }.first()
    }
    
    suspend fun getSavedPasswordSync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[SAVED_PASSWORD_KEY]
        }.first()
    }
    
    suspend fun clearRememberMe() {
        context.dataStore.edit { preferences ->
            preferences.remove(REMEMBER_ME_KEY)
            preferences.remove(SAVED_EMAIL_KEY)
            preferences.remove(SAVED_PASSWORD_KEY)
        }
    }
}

