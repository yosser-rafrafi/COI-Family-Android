package tn.esprit.coidam

import android.content.ContentValues.TAG
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import tn.esprit.coidam.screens.*

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.AuthRepository

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var navController: NavHostController? = null

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("230808302553-bht9c8jnbjsftpuphd53mjm9lve3333s.apps.googleusercontent.com") // 🔸 à remplacer
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d("GoogleSignIn", "✅ GoogleSignInClient initialisé")

        setContent {
            val navController = rememberNavController()
            this@MainActivity.navController = navController
            AppNavHost(navController)
        }
    }

    // Cette fonction sera appelée par ton bouton Google
    fun signInWithGoogle() {
        Log.d("GoogleSignIn", "🟡 Lancement du flux Google Sign-In")
        val signInIntent = googleSignInClient.signInIntent
        Log.d("GoogleSignIn", "➡️ Démarrage de l’intent Google Sign-In")
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Connexion réussie avec : ${account.email}")
                val idToken = account.idToken
                Log.d(TAG, "idToken récupéré : $idToken")

                // 🚀 Appel backend
                idToken?.let { token ->
                    sendIdTokenToBackend(token, account.email)
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Erreur Google Sign-In : ${e.statusCode}")
            }
        }
    }


    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        Log.d("GoogleSignIn", "🔸 handleSignInResult appelé")
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "✅ Connexion réussie avec : ${account.email}")

            val idToken = account.idToken
            Log.d("GoogleSignIn", "🧩 idToken récupéré : $idToken")

            // 🔹 Simulation de l’envoi au backend
            //sendIdTokenToBackend(idToken)

        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "❌ Erreur ApiException : ${e.statusCode}", e)
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "❌ Exception inconnue :", e)
        }
    }

    private fun sendIdTokenToBackend(idToken: String, email: String?) {
        if (idToken.isNullOrEmpty()) {
            Log.e("GoogleSignIn", "🚨 idToken est nul ou vide, rien à envoyer")
            return
        }
        Log.d("GoogleSignIn", "🚀 Envoi du token au backend : $idToken")

        val authRepository = AuthRepository(this)
        lifecycleScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { authResponse ->
                // Automatically login as companion profile if options exist
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {
                    val companionOption = authResponse.options.find { it.userType == "companion" } 
                        ?: authResponse.options.first()
                    val loginResult = authRepository.loginAs(companionOption.userId, companionOption.userType)
                    loginResult.onSuccess {
                        navController?.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        Log.d("GoogleSignIn", "✅ Login backend réussi avec profil companion")
                    }.onFailure { e ->
                        Log.e("GoogleSignIn", "❌ Erreur lors de la connexion au profil : ${e.message}")
                    }
                } else if (authResponse.access_token != null) {
                    navController?.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                    Log.d("GoogleSignIn", "✅ Login backend réussi, access_token = ${authResponse.access_token}")
                } else {
                    Log.e("GoogleSignIn", "❌ Pas de token ni d'options disponibles")
                }
            }.onFailure { e ->
                // Check if it's a 404 error (user not found)
                if (e is AuthRepository.GoogleSignInException && e.statusCode == 404) {
                    Log.d("GoogleSignIn", "👤 Utilisateur non trouvé avec Google, vérification si compte existe...")
                    // User might exist but not linked to Google
                    // Try to create account or link existing account
                    email?.let { userEmail ->
                        handleGoogleUserNotFound(userEmail, idToken, authRepository)
                    } ?: run {
                        Log.e("GoogleSignIn", "❌ Email non disponible pour créer le compte")
                    }
                } else {
                    Log.e("GoogleSignIn", "❌ Login backend erreur : ${e.message}")
                }
            }
        }
    }
    
    private fun handleGoogleUserNotFound(email: String, idToken: String, authRepository: AuthRepository) {
        lifecycleScope.launch {
            // Try to create account with Google
            // Generate a random password for the account (user won't need it for Google sign-in)
            val randomPassword = "Google${System.currentTimeMillis()}"
            
            val signUpResult = authRepository.signUp(email, randomPassword)
            signUpResult.onSuccess {
                Log.d("GoogleSignIn", "✅ Nouveau compte créé avec succès, tentative de connexion Google...")
                // After signup, try to sign in with Google again
                val signInResult = authRepository.signInWithGoogle(idToken)
                signInResult.onSuccess { authResponse ->
                    if (authResponse.access_token != null) {
                        navController?.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        Log.d("GoogleSignIn", "✅ Connexion réussie après création du compte")
                    }
                }.onFailure { e ->
                    Log.e("GoogleSignIn", "❌ Erreur lors de la connexion Google après création : ${e.message}")
                    // If Google sign-in still fails, user might need to use email/password
                    // For now, just log the error
                }
            }.onFailure { exception ->
                // If signup fails, it might be because user already exists
                val errorMessage = exception.message ?: ""
                Log.d("GoogleSignIn", "📝 Message d'erreur signup: $errorMessage")
                
                // Check if user already exists (common error messages)
                val userExists = errorMessage.contains("already exists", ignoreCase = true) || 
                                errorMessage.contains("email", ignoreCase = true) ||
                                errorMessage.contains("duplicate", ignoreCase = true) ||
                                errorMessage.contains("409", ignoreCase = false)
                
                if (userExists) {
                    Log.d("GoogleSignIn", "ℹ️ Compte existe déjà dans la base de données")
                    Log.d("GoogleSignIn", "💡 Le backend ne lie pas automatiquement Google aux comptes existants")
                    Log.d("GoogleSignIn", "💡 Solution: L'utilisateur doit se connecter avec email/password")
                    Log.d("GoogleSignIn", "💡 OU le backend doit être modifié pour créer automatiquement le lien")
                    
                    // The user exists but Google account is not linked
                    // The backend needs to be modified to:
                    // 1. Check if user exists by email when Google sign-in fails with 404
                    // 2. Automatically link the Google account to the existing user
                    // OR create an endpoint to link Google account to existing user
                    
                    // For now, we can't proceed without backend support
                    // User needs to login with email/password first
                } else {
                    Log.e("GoogleSignIn", "❌ Erreur lors de la création du compte : ${exception.message}")
                }
            }
        }
    }

}


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { SignupScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("profil") { ProfilScreen(navController) }
        composable("update_profile") { UpdateProfilScreen(navController) }
        composable("dashboard") { DashboardScreen(navController) }
        composable("known_persons") { KnownPersonListScreen(navController) }
        composable("known_person_create") { KnownPersonCreateScreen(navController) }
        composable("known_person_detail/{id}") { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("id") ?: ""
            KnownPersonDetailScreen(navController, personId)
        }
    }
}


