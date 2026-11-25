package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AccountStatus
import com.example.data.model.BOOTSTRAPPED_ADMIN_EMAIL
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.UserRole
import com.example.data.model.safeEnum
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class AppWorkspaceMode(val label: String, val subtitle: String) {
    CUSTOMER("Customer", "Find medicines, AI guide & Google Maps"),
    PHARMACY("My Pharmacy Store", "Verified partner portal to manage store & update stock"),
    ADMIN("Admin Governance", "Verify pharmacies, moderate catalogue & inspect logs")
}

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(
        val uid: String,
        val displayName: String,
        val email: String,
        val isEmailVerified: Boolean,
        val role: UserRole,
        val accountStatus: AccountStatus,
        val hasApprovedPharmacy: Boolean
    ) : AuthUiState {
        val canAccessPharmacyPortal: Boolean
            get() = role == UserRole.ADMIN || hasApprovedPharmacy
    }
}

class AuthenticationViewModel(
    private val db: FirebaseFirestore,
    appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val prefs = appContext.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _workspaceMode = MutableStateFlow(
        safeEnum(prefs.getString(KEY_WORKSPACE_MODE, null), AppWorkspaceMode.CUSTOMER)
    )
    val workspaceMode: StateFlow<AppWorkspaceMode> = _workspaceMode.asStateFlow()

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private var userDocListener: ListenerRegistration? = null
    private var pharmacyOwnerListener: ListenerRegistration? = null
    private var activeUid: String? = null

    private var profileRole: UserRole = UserRole.CUSTOMER
    private var accountStatus: AccountStatus = AccountStatus.ACTIVE
    private var hasAdminRecord: Boolean = false
    private var hasApprovedPharmacy: Boolean = false
    private var autoSignInResolved: Boolean = auth.currentUser != null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            stopRoleListeners()
            activeUid = null
            profileRole = UserRole.CUSTOMER
            accountStatus = AccountStatus.ACTIVE
            hasAdminRecord = false
            hasApprovedPharmacy = false
            _workspaceMode.value = AppWorkspaceMode.CUSTOMER
            _authState.value = if (autoSignInResolved) AuthUiState.Unauthenticated else AuthUiState.Loading
        } else {
            autoSignInResolved = true
            startObservingUserRole(user)
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun setWorkspaceMode(mode: AppWorkspaceMode) {
        val currentAuth = _authState.value as? AuthUiState.Authenticated
        val resolvedMode = if (currentAuth == null) {
            AppWorkspaceMode.CUSTOMER
        } else {
            resolveAuthorizedMode(mode, currentAuth.role, currentAuth.canAccessPharmacyPortal)
        }
        _workspaceMode.value = resolvedMode
        prefs.edit().putString(KEY_WORKSPACE_MODE, resolvedMode.name).apply()
    }

    private fun resolveAuthorizedMode(
        requested: AppWorkspaceMode,
        role: UserRole,
        canAccessPharmacyPortal: Boolean
    ): AppWorkspaceMode = when (requested) {
        AppWorkspaceMode.ADMIN -> if (role == UserRole.ADMIN) AppWorkspaceMode.ADMIN else AppWorkspaceMode.CUSTOMER
        AppWorkspaceMode.PHARMACY -> if (canAccessPharmacyPortal) AppWorkspaceMode.PHARMACY else AppWorkspaceMode.CUSTOMER
        AppWorkspaceMode.CUSTOMER -> AppWorkspaceMode.CUSTOMER
    }

    fun markAutoSignInResolved() {
        autoSignInResolved = true
        if (auth.currentUser == null) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    private fun startObservingUserRole(user: FirebaseUser) {
        if (activeUid == user.uid && userDocListener != null) {
            emitAuthenticatedState(user)
            return
        }
        stopRoleListeners()
        activeUid = user.uid
        profileRole = UserRole.CUSTOMER
        accountStatus = AccountStatus.ACTIVE
        hasAdminRecord = false
        hasApprovedPharmacy = false

        val isBootstrappedAdmin =
            user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
        if (isBootstrappedAdmin) {
            hasAdminRecord = true
            if (prefs.getString(KEY_WORKSPACE_MODE, null) == null) {
                _workspaceMode.value = AppWorkspaceMode.ADMIN
            }
        }
        emitAuthenticatedState(user)

        viewModelScope.launch {
            try {
                val adminDocExists = db.collection("admins").document(user.uid).get().await().exists()
                if (activeUid == user.uid && adminDocExists) {
                    hasAdminRecord = true
                    emitAuthenticatedState(user)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Admin record lookup skipped: ${e.message}")
            }
        }

        val userPath = "users/${user.uid}"
        userDocListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (auth.currentUser?.uid == user.uid) {
                        handleFirestoreError(error, OperationType.GET, userPath)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    profileRole = safeEnum(snapshot.getString("role"), UserRole.CUSTOMER)
                    accountStatus = safeEnum(snapshot.getString("status"), AccountStatus.ACTIVE)
                    emitAuthenticatedState(user)
                }
            }

        pharmacyOwnerListener = db.collection("pharmacies")
            .whereEqualTo("ownerUserId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (auth.currentUser?.uid == user.uid) {
                        handleFirestoreError(error, OperationType.LIST, "pharmacies")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    hasApprovedPharmacy = snapshot.documents.any { doc ->
                        doc.getString("verificationStatus") == PharmacyVerificationStatus.APPROVED.name
                    }
                    emitAuthenticatedState(user)
                }
            }
    }

    private fun emitAuthenticatedState(user: FirebaseUser) {
        val effectiveRole = when {
            hasAdminRecord -> UserRole.ADMIN
            hasApprovedPharmacy -> UserRole.PHARMACY
            else -> UserRole.CUSTOMER
        }
        val canAccessPharmacy = effectiveRole == UserRole.ADMIN || hasApprovedPharmacy
        val resolvedMode = resolveAuthorizedMode(_workspaceMode.value, effectiveRole, canAccessPharmacy)
        if (_workspaceMode.value != resolvedMode) {
            _workspaceMode.value = resolvedMode
            prefs.edit().putString(KEY_WORKSPACE_MODE, resolvedMode.name).apply()
        }
        _authState.value = AuthUiState.Authenticated(
            uid = user.uid,
            displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "MedFind User",
            email = user.email.orEmpty(),
            isEmailVerified = user.isEmailVerified,
            role = effectiveRole,
            accountStatus = accountStatus,
            hasApprovedPharmacy = hasApprovedPharmacy
        )
    }

    fun signOut(credentialManager: CredentialManager) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                stopRoleListeners()
                activeUid = null
                _workspaceMode.value = AppWorkspaceMode.CUSTOMER
                prefs.edit().remove(KEY_WORKSPACE_MODE).apply()
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    _authState.value = AuthUiState.Unauthenticated
                }
            }
        }
    }

    private fun stopRoleListeners() {
        userDocListener?.remove()
        userDocListener = null
        pharmacyOwnerListener?.remove()
        pharmacyOwnerListener = null
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        stopRoleListeners()
    }

    private companion object {
        const val TAG = "AuthVM"
        const val PREFS_NAME = "medfind_workspace_prefs"
        const val KEY_WORKSPACE_MODE = "workspace_mode"
    }
}
