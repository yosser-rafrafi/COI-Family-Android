package tn.esprit.coidam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.screens.*

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var navController: NavHostController? = null

    // ✅ ÉTAT GLOBAL POUR LE LOADING GOOGLE SIGN-IN
    private val _isGoogleSignInLoading = mutableStateOf(false)
    val isGoogleSignInLoading: State<Boolean> = _isGoogleSignInLoading

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("230808302553-bht9c8jnbjsftpuphd53mjm9lve3333s.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d("GoogleSignIn", "✅ GoogleSignInClient initialisé")

        setContent {
            val navController = rememberNavController()
            this@MainActivity.navController = navController

            // ✅ PASSER L'ÉTAT DE LOADING AU COMPOSABLE
            AppNavHost(navController, isGoogleSignInLoading.value)
        }
    }

    // ✅ AFFICHER LE LOADING AVANT DE LANCER GOOGLE SIGN-IN
    fun signInWithGoogle() {
        Log.d("GoogleSignIn", "🟡 Lancement du flux Google Sign-In")
        _isGoogleSignInLoading.value = true // ✅ ACTIVER LE LOADING

        val signInIntent = googleSignInClient.signInIntent
        Log.d("GoogleSignIn", "➡️ Démarrage de l'intent Google Sign-In")
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            // ✅ CACHER LE LOADING APRÈS RÉSULTAT (SUCCESS OU ERREUR)
            _isGoogleSignInLoading.value = false

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        Log.d("GoogleSignIn", "🔸 handleSignInResult appelé")
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "✅ Connexion réussie avec : ${account.email}")

            val idToken = account.idToken
            Log.d("GoogleSignIn", "🧩 idToken récupéré : $idToken")

            idToken?.let { token ->
                sendIdTokenToBackend(token, account.email)
            }

        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "❌ Erreur ApiException : ${e.statusCode}", e)
            _isGoogleSignInLoading.value = false // ✅ CACHER LE LOADING EN CAS D'ERREUR
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "❌ Exception inconnue :", e)
            _isGoogleSignInLoading.value = false
        }
    }

    private fun sendIdTokenToBackend(idToken: String, email: String?) {
        if (idToken.isEmpty()) {
            Log.e("GoogleSignIn", "🚨 idToken est vide")
            return
        }
        Log.d("GoogleSignIn", "🚀 Envoi du token au backend : $idToken")

        val authRepository = AuthRepository(this)
        lifecycleScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { authResponse ->
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {
                    val companionOption = authResponse.options.find { it.userType == "companion" }
                        ?: authResponse.options.first()
                    val loginResult = authRepository.loginAs(companionOption.userId, companionOption.userType)
                    loginResult.onSuccess {
                        navController?.navigate("blind_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        Log.d("GoogleSignIn", "✅ Login backend réussi avec profil companion")
                    }.onFailure { e ->
                        Log.e("GoogleSignIn", "❌ Erreur lors de la connexion au profil : ${e.message}")
                    }
                } else if (authResponse.access_token != null) {
                    navController?.navigate("blind_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                    Log.d("GoogleSignIn", "✅ Login backend réussi")
                } else {
                    Log.e("GoogleSignIn", "❌ Pas de token ni d'options disponibles")
                }
            }.onFailure { e ->
                if (e is AuthRepository.GoogleSignInException && e.statusCode == 404) {
                    Log.d("GoogleSignIn", "👤 Utilisateur non trouvé avec Google")
                    email?.let { userEmail ->
                        handleGoogleUserNotFound(userEmail, idToken, authRepository)
                    }
                } else {
                    Log.e("GoogleSignIn", "❌ Login backend erreur : ${e.message}")
                }
            }
        }
    }

    private fun handleGoogleUserNotFound(email: String, idToken: String, authRepository: AuthRepository) {
        lifecycleScope.launch {
            val randomPassword = "Google${System.currentTimeMillis()}"

            val signUpResult = authRepository.signUp(email, randomPassword)
            signUpResult.onSuccess {
                Log.d("GoogleSignIn", "✅ Nouveau compte créé avec succès")
                val signInResult = authRepository.signInWithGoogle(idToken)
                signInResult.onSuccess { authResponse ->
                    if (authResponse.access_token != null) {
                        navController?.navigate("blind_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        Log.d("GoogleSignIn", "✅ Connexion réussie après création du compte")
                    }
                }
            }.onFailure { exception ->
                Log.e("GoogleSignIn", "❌ Erreur lors de la création du compte : ${exception.message}")
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, isGoogleLoading: Boolean) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("login") {
            // ✅ PASSER L'ÉTAT DE LOADING AU LOGIN SCREEN
            LoginScreen(navController, isGoogleLoading)
        }
        composable("register") {
            SignupScreen(navController, isGoogleLoading)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController)
        }
        composable("profil") {
            ProfilScreen(navController)
        }
        composable("update_profile") {
            UpdateProfilScreen(navController)
        }
        composable("companion_dashboard") {
            DashboardScreen(navController)
        }
        composable("blind_dashboard") {
            BlindDashboardScreen(navController)
        }
        composable("known_persons") {
            KnownPersonListScreen(navController)
        }
        composable("known_person_create") {
            KnownPersonCreateScreen(navController)
        }
        composable("known_person_detail/{id}") { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("id") ?: ""
            KnownPersonDetailScreen(navController, personId)
        }
        composable("alerts") {
            CompanionAlertsScreen(navController)
        }
        composable("alert_detail/{alertId}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            AlertDetailScreen(navController, alertId)
        }

        // ✅ NOUVELLE ROUTE POUR ENVOYER DES ALERTES
        composable("send_alert") {
            CreateAlertScreen(navController)
        }
    }
}