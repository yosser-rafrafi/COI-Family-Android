package tn.esprit.coidam

import android.content.ContentValues.TAG
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
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
                    sendIdTokenToBackend(token)
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
            sendIdTokenToBackend(idToken)

        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "❌ Erreur ApiException : ${e.statusCode}", e)
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "❌ Exception inconnue :", e)
        }
    }

    private fun sendIdTokenToBackend(idToken: String?) {
        if (idToken.isNullOrEmpty()) {
            Log.e("GoogleSignIn", "🚨 idToken est nul ou vide, rien à envoyer")
            return
        }
        Log.d("GoogleSignIn", "🚀 Envoi du token au backend : $idToken")

        // TODO : ajouter ici l'appel réseau Retrofit ou HttpURLConnection
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
    }
}


