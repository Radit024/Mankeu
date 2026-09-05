package com.arinahub.arinaagri.calculator.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface AuthService {
    val currentUser: StateFlow<AuthUser?>
    suspend fun signInWithCredential(credential: AuthCredential): Result<AuthUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    fun setCustomUser(user: AuthUser?)
    fun signOut()
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
}

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthService {
    private val _currentUser = MutableStateFlow<AuthUser?>(auth.currentUser?.toAuthUser())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser?.toAuthUser()
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<AuthUser> {
        return runCatching {
            val authResult = auth.signInWithCredential(credential).awaitTask()
            val user = authResult.user?.toAuthUser() ?: error("Sign in succeeded but user is null.")
            _currentUser.value = user
            user
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    override fun setCustomUser(user: AuthUser?) {
        _currentUser.value = user
    }

    override fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )
}
