// This is the AccountProfileScreen.kt file
// java/com/nagpurpulse/ui/screens/settings/AccountProfileScreen.kt

package com.nagpurpulse.ui.screens.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.nagpurpulse.ui.screens.profile.RandomImages
import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.material.icons.automirrored.filled.Message
import android.net.Uri
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountProfileUiState(
    val profile: Profile? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val displayName: String = "",
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val hideComments: Boolean = false,
    val hidePosts: Boolean = false,
    val hideProfile: Boolean = false,
    val allowDms: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val isSaving: Boolean = false,
    val isAvatarUploading: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null

)

@HiltViewModel
class AccountProfileViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {
    private val _s = MutableStateFlow(AccountProfileUiState())
    val state: StateFlow<AccountProfileUiState> = _s

    init { load() }

    private fun load() {
        viewModelScope.launch {
            authRepository.getCurrentProfile().fold(
                onSuccess = { p ->

                    _s.value = _s.value.copy(
                        profile = p,
                        avatarUrl = p.avatarUrl,
                        bannerUrl = p.coverUrl,

                        displayName = p.displayName ?: p.username,
                        bio = p.tagline ?: "",
                        location = p.location ?: "Nagpur, Maharashtra",

                        hideComments = p.hideComments,
                        hidePosts = p.hidePosts,
                        hideProfile = p.hideProfile,
                        allowDms = p.allowDms,
                        showOnlineStatus = p.showOnlineStatus
                    )
                },
                onFailure = {}
            )
        }
    }

    fun setDisplayName(v: String) { _s.value = _s.value.copy(displayName = v) }
    fun setBio(v: String)         { _s.value = _s.value.copy(bio = v) }
    fun setLocation(v: String)    { _s.value = _s.value.copy(location = v) }
    fun setWebsite(v: String)     { _s.value = _s.value.copy(website = v) }
    fun setHideComments(v: Boolean) { _s.value = _s.value.copy(hideComments = v); savePrivacy() }
    fun setHidePosts(v: Boolean)    { _s.value = _s.value.copy(hidePosts = v);    savePrivacy() }
    fun setHideProfile(v: Boolean)  { _s.value = _s.value.copy(hideProfile = v);  savePrivacy() }
    fun setAllowDms(v: Boolean)     { _s.value = _s.value.copy(allowDms = v);     savePrivacy() }
    fun setShowOnline(v: Boolean)   { _s.value = _s.value.copy(showOnlineStatus = v); savePrivacy() }

    fun saveProfile() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _s.value = _s.value.copy(isSaving = true)
            authRepository.updateFullProfile(
                userId      = uid,
                displayName = _s.value.displayName.ifBlank { null },
                bio         = _s.value.bio.ifBlank { null },
                location    = _s.value.location.ifBlank { null },
                website     = _s.value.website.ifBlank { null }
            ).fold(
                onSuccess = {
                    _s.value = _s.value.copy(
                        isSaving = false,
                                saveSuccess = true
                    )
                },
                onFailure = { e -> _s.value = _s.value.copy(isSaving = false, error = e.message) }
            )
        }
    }

    private fun savePrivacy() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            authRepository.updatePrivacySettings(
                userId           = uid,
                hideComments     = _s.value.hideComments,
                hidePosts        = _s.value.hidePosts,
                hideProfile      = _s.value.hideProfile,
                allowDms         = _s.value.allowDms,
                showOnlineStatus = _s.value.showOnlineStatus
            )
        }
    }

    fun uploadAvatar(
        context: android.content.Context,
        uri: Uri
    ) {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            try {

                _s.value = _s.value.copy(
                    isAvatarUploading = true
                )

                val bytes =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.readBytes()
                        ?: return@launch

                authRepository.uploadAvatar(
                    userId = uid,
                    bytes = bytes
                ).fold(

                    onSuccess = { url ->

                        _s.value = _s.value.copy(
                            avatarUrl = url,
                            isAvatarUploading = false
                        )

                        _s.value = _s.value.copy(
                            avatarUrl = url
                        )

                        authRepository.updateFullProfile(
                            userId = uid,
                            avatarUrl = url
                        ).fold(
                            onSuccess = {
                                load()

                                _s.value = _s.value.copy(
                                    saveSuccess = true
                                )
                            },
                            onFailure = {

                                _s.value = _s.value.copy(
                                    isAvatarUploading = false
                                )
                            }
                        )
                    },

                    onFailure = {

                        _s.value = _s.value.copy(
                            isAvatarUploading = false
                        )
                    }
                )

            } catch (e: Exception) {
            }
        }
    }

    fun uploadBanner(
        context: android.content.Context,
        uri: Uri
    ) {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            try {

                val bytes =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.readBytes()
                        ?: return@launch

                authRepository
                    .uploadProfileImage(
                        userId = uid,
                        bytes = bytes,
                        isBanner = true
                    )
                    .fold(

                        onSuccess = { url ->

                            authRepository.updateFullProfile(
                                userId = uid,
                                coverUrl = url
                            ).fold(
                                onSuccess = {
                                    load()
                                },
                                onFailure = {
                                }
                            )
                        },

                        onFailure = {
                        }
                    )

            } catch (e: Exception) {
            }
        }
    }


    fun randomAvatar() {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            val url = RandomImages.avatars.random()

            authRepository.updateFullProfile(
                userId = uid,
                avatarUrl = url
            )

            load()
        }
    }

    /*
    fun randomBanner() {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            val url = RandomImages.banners.random()

            authRepository.updateFullProfile(
                userId = uid,
                coverUrl = url
            )

            load()
        }
    }

    */

    fun removeAvatar() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _s.value = _s.value.copy(isSaving = true)
            authRepository.updateFullProfile(userId = uid, avatarUrl = "").fold(
                onSuccess = {
                    _s.value = _s.value.copy(
                        avatarUrl = null,
                        isSaving = false,
                        saveSuccess = true
                    )
                },
                onFailure = { error ->
                    _s.value = _s.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unable to remove avatar."
                    )
                }
            )
        }
    }

    /*
    fun removeBanner() {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            authRepository.updateFullProfile(
                userId = uid,
                coverUrl = ""
            )

            load()
        }
    }

    */

    fun saveRandomAvatar(url: String) {

        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {

            authRepository.updateFullProfile(
                userId = uid,
                avatarUrl = url
            ).fold(
                onSuccess = {
                    load()

                    _s.value = _s.value.copy(
                        saveSuccess = true
                    )
                },
                onFailure = { }
            )
        }
    }


    fun changeEmail(newEmail: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.changeEmail(newEmail.trim()).fold(
                onSuccess = {
                    onComplete(true, "Email update requested. Check your inbox to confirm the new address.")
                },
                onFailure = {
                    onComplete(false, it.message ?: "Unable to update email. Please try again.")
                }
            )
        }
    }

    fun changePassword(newPassword: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.changePassword(newPassword).fold(
                onSuccess = {
                    onComplete(true, "Password updated successfully.")
                },
                onFailure = {
                    onComplete(false, it.message ?: "Unable to update password. Please try again.")
                }
            )
        }
    }

    fun deactivateAccount(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.deactivateAccount().fold(
                onSuccess = { onComplete(true, "Your account has been deactivated. Sign in again whenever you're ready to reactivate it.") },
                onFailure = { onComplete(false, it.message ?: "Unable to deactivate account. Please try again.") }
            )
        }
    }

    fun deleteAccount(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccount().fold(
                onSuccess = {
                    onComplete(true, "Your account has been permanently deleted.")
                },
                onFailure = { error ->
                    onComplete(
                        false,
                        error.message ?: "Unable to delete your account. Please try again."
                    )
                }
            )
        }
    }
}

@Composable
fun AccountProfileScreen(
    navController: NavController,
    vm: AccountProfileViewModel = hiltViewModel()
) {
    val s       = vm.state.collectAsState().value
    val context = LocalContext.current
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAvatarUrl by remember {
        mutableStateOf<String?>(null)
    }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarExpanded by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var accountActionBusy by remember { mutableStateOf(false) }
    var accountActionMessage by remember { mutableStateOf<String?>(null) }
    var accountActionError by remember { mutableStateOf<String?>(null) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    val pickAvatar =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            uri?.let {

                avatarUri = it

            }
        }



    val pickBanner =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            uri?.let {

                bannerUri = it

                vm.uploadBanner(
                    context = context,
                    uri = it
                )
            }
        }

    LaunchedEffect(s.saveSuccess) {
        if (s.saveSuccess) navController.popBackStack()
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(Modifier.background(Brush.verticalGradient(listOf(Surface, Background), 0f, 130f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText) }
                    Column(Modifier.weight(1f)) {
                        Text("Account & Profile", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Manage your identity and public profile", color = SecondaryText, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(OrangePrimary)
                            .pressScale {

                                when {

                                    avatarUri != null -> {
                                        vm.uploadAvatar(
                                            context = context,
                                            uri = avatarUri!!
                                        )
                                    }

                                    pendingAvatarUrl == "" -> {
                                        vm.removeAvatar()
                                    }

                                    pendingAvatarUrl != null -> {
                                        vm.saveRandomAvatar(
                                            pendingAvatarUrl!!
                                        )
                                    }

                                    else -> {
                                        vm.saveProfile()
                                    }
                                }

                                avatarUri = null
                                pendingAvatarUrl = null
                            }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (s.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Profile card preview
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Surface)
                        .border(
                            1.dp,
                            OrangePrimary.copy(0.12f),
                            RoundedCornerShape(18.dp)
                        )
                ) {

                    // Below is the banner

                    /*
                    val bannerModel = s.bannerUrl

                    if (bannerModel != null) {
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = "Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            OrangePrimary.copy(alpha = 0.25f),
                                            Surface
                                        )
                                    )
                                )
                        )
                    }  */

                    Row(
                        modifier = Modifier.padding(DensityManager.cardPadding.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(0.12f))
                                    .border(
                                        2.dp,
                                        OrangePrimary.copy(0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                if (s.isAvatarUploading) {

                                    CircularProgressIndicator(
                                        color = OrangePrimary,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(28.dp)
                                    )

                                } else {

                                    val avatarModel = s.avatarUrl

                                    if (avatarModel != null) {

                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(avatarModel)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )

                                    } else {

                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            Box(Modifier.size(24.dp).clip(CircleShape).background(OrangePrimary).pressScale { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Alignment.Center) {
                                Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.displayName.ifBlank { "Your Name" },
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Incognito User", color = SecondaryText, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = OrangePrimary, modifier = Modifier.size(12.dp))
                                Text(
                                    " ${s.location}",
                                    color = OrangePrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(OrangePrimary.copy(0.15f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text("Pulse Member", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // PROFILE INFORMATION section
            item { SectionHeader("PROFILE INFORMATION") }
            item {
                SettingsGroup {
                    ProfileEditRow(
                        "Display Name",
                        s.displayName,
                        Icons.Filled.Person,
                        OrangePrimary
                    ) {
                        vm.setDisplayName(it)
                    }


                    SettingsDivider()

                    SettingsRow(
                        "Username",
                        "u/${s.profile?.username ?: ""}",
                        Icons.Filled.AlternateEmail,
                        BlueInfo
                    ) {}

                    SettingsDivider()

                    ProfileEditRow("Bio",           s.bio,         Icons.Filled.Edit,         PurpleNight)    { vm.setBio(it) }
                    SettingsDivider()
                    LocationPickerRow(value = s.location, onValueChange = vm::setLocation, context = context)
                    SettingsDivider()

/*

                    SettingsRow(
                        "Profile Banner",
                        "Upload, generate or remove banner",
                        Icons.Filled.Image,
                        BlueInfo
                    ) {
                        showBannerMenu = true
                    }

                    DropdownMenu(
                        expanded = showBannerMenu,
                        onDismissRequest = { showBannerMenu = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text("🖼 Upload Banner") },
                            onClick = {
                                showBannerMenu = false
                                pickBanner.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("🎲 Random Banner") },
                            onClick = {
                                showBannerMenu = false
                                vm.randomBanner()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("🗑 Remove Banner") },
                            onClick = {
                                showBannerMenu = false
                                vm.removeBanner()
                            }
                        )

                    } // Close Banner DropdownMenu

                    SettingsDivider()
*/

                    AvatarManagementCard(
                        avatarUrl = s.avatarUrl,
                        localAvatarUri = avatarUri,
                        pendingAvatarUrl = pendingAvatarUrl,
                        expanded = avatarExpanded,
                        onExpandToggle = {
                            avatarExpanded = !avatarExpanded
                        },
                        onUpload = {
                            pickAvatar.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onRandom = {
                            pendingAvatarUrl = RandomImages.avatars.random()
                        },
                        onRemove = {
                            pendingAvatarUrl = ""
                        }

                    )


                }
            }

            // Privacy controls live in the dedicated Privacy Settings screen to avoid duplicate controls.

            // ACCOUNT section
            item { SectionHeader("ACCOUNT") }
            item {
                SettingsGroup {
                    SettingsRow("Change Email",       "Update your email address",            Icons.Filled.Email,  BlueInfo)   { showChangeEmailDialog = true; accountActionMessage = null; accountActionError = null }
                    SettingsDivider()
                    SettingsRow("Deactivate Account", "Temporarily disable your account",     Icons.Filled.PauseCircle, SecondaryText) { showDeactivateDialog = true }
                    SettingsDivider()
                    SettingsRow(
                        "Delete Account",
                        "Permanently delete your account and all data",
                        Icons.Filled.Delete,
                        RedAlert
                    ) {
                        showDeleteDialog = true
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }


    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { if (!accountActionBusy) showDeactivateDialog = false },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Temporarily deactivate account?") },
            text = {
                Text("Your account will be marked deactivated and you'll be signed out on this device. Signing in again with your credentials reactivates it. Your content is not deleted.")
            },
            confirmButton = {
                Button(
                    enabled = !accountActionBusy,
                    onClick = {
                        accountActionBusy = true
                        vm.deactivateAccount { success, message ->
                            accountActionBusy = false
                            showDeactivateDialog = false
                            if (success) {
                                navController.navigate("login") { popUpTo(0) }
                            } else {
                                accountActionError = message
                            }
                        }
                    }
                ) { Text(if (accountActionBusy) "Deactivating…" else "Deactivate") }
            },
            dismissButton = {
                TextButton(enabled = !accountActionBusy, onClick = { showDeactivateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (accountActionMessage != null || accountActionError != null) {
        AlertDialog(
            onDismissRequest = { accountActionMessage = null; accountActionError = null },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (accountActionError == null) "Account update" else "Could not update account") },
            text = { Text(accountActionMessage ?: accountActionError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { accountActionMessage = null; accountActionError = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showChangeEmailDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!accountActionBusy) showChangeEmailDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !accountActionBusy,
                dismissOnClickOutside = !accountActionBusy
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .heightIn(max = maxHeight * 0.88f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF24191A), Color(0xFF121116), Color(0xFF1B151A))
                            )
                        )
                        .border(
                            1.dp,
                            Color(0xFFFF7A24).copy(alpha = 0.72f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFD2C7CA).copy(alpha = 0.85f))
                    )

                    Spacer(Modifier.height(28.dp))

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF9B45).copy(alpha = 0.32f), Color(0xFF7D310D).copy(alpha = 0.24f))
                                )
                            )
                            .border(1.dp, Color(0xFFFF8A35).copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = Color(0xFFFFA04E),
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = "Change email",
                        color = Color(0xFFF8F6F7),
                        fontSize = 27.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "We’ll send a confirmation link to your new address.",
                        color = Color(0xFFC4BEC8),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        placeholder = {
                            Text("name@example.com", color = Color(0xFF89858F), fontSize = 15.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF89858F))
                        },
                        singleLine = true,
                        enabled = !accountActionBusy,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF7F3F5),
                            unfocusedTextColor = Color(0xFFF7F3F5),
                            focusedBorderColor = Color(0xFFFF8A35),
                            unfocusedBorderColor = Color(0xFF625B64),
                            cursorColor = Color(0xFFFF8A35),
                            focusedContainerColor = Color(0xFF17151B).copy(alpha = 0.55f),
                            unfocusedContainerColor = Color(0xFF17151B).copy(alpha = 0.55f),
                            focusedLeadingIconColor = Color(0xFF89858F),
                            unfocusedLeadingIconColor = Color(0xFF89858F)
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF8E8A9C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Your current email stays active until you confirm.",
                            color = Color(0xFFAAA5B5),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(Modifier.height(26.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF17151A).copy(alpha = 0.55f))
                                .border(1.dp, Color(0xFF514A54), RoundedCornerShape(50))
                                .pressScale { if (!accountActionBusy) showChangeEmailDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color(0xFFFF792E), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (!accountActionBusy && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail.trim()).matches())
                                        Brush.horizontalGradient(listOf(Color(0xFFB94B12), Color(0xFF8F390F)))
                                    else Brush.horizontalGradient(listOf(Color(0xFF71300F), Color(0xFF67300F)))
                                )
                                .pressScale {
                                    if (!accountActionBusy && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail.trim()).matches()) {
                                        accountActionBusy = true
                                        vm.changeEmail(newEmail) { success, message ->
                                            accountActionBusy = false
                                            if (success) {
                                                showChangeEmailDialog = false
                                                newEmail = ""
                                                accountActionMessage = message
                                                accountActionError = null
                                            } else {
                                                accountActionError = message
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accountActionBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Update email",
                                    color = if (android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail.trim()).matches()) Color.White else Color(0xFFB7A39A),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!accountActionBusy) showChangePasswordDialog = false },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Change password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        singleLine = true,
                        enabled = !accountActionBusy,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        enabled = !accountActionBusy,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    if (newPassword.isNotEmpty() && newPassword.length < 8) {
                        Text("Use at least 8 characters.", color = RedAlert, fontSize = 12.sp)
                    }
                    if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Text("Passwords do not match.", color = RedAlert, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !accountActionBusy, onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = !accountActionBusy && newPassword.length >= 8 && newPassword == confirmPassword,
                    onClick = {
                        accountActionBusy = true
                        vm.changePassword(newPassword) { success, message ->
                            accountActionBusy = false
                            showChangePasswordDialog = false
                            newPassword = ""
                            confirmPassword = ""
                            if (success) accountActionMessage = message else accountActionError = message
                        }
                    }
                ) { Text(if (accountActionBusy) "Updating…" else "Update password") }
            }
        )
    }

    if (showDeleteDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!accountActionBusy) showDeleteDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !accountActionBusy,
                dismissOnClickOutside = !accountActionBusy
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .heightIn(max = maxHeight * 0.88f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF211A1D),
                                    Color(0xFF171416),
                                    Color(0xFF21191B)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color(0xFF49383B).copy(alpha = 0.72f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .width(24.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF777174).copy(alpha = 0.7f))
                    )

                    Spacer(Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFA493F).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFA493F).copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF5A50),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Delete your account?",
                        color = Color(0xFFF8F6F6),
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = "Your posts, comments, votes and saved items will be permanently removed.",
                        color = Color(0xFFC2BBBE),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(Modifier.height(17.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2A201E), Color(0xFF261D1C))
                                )
                            )
                            .border(
                                1.dp,
                                Color(0xFF4A342E).copy(alpha = 0.8f),
                                RoundedCornerShape(20.dp)
                            )
                            .pressScale {
                                if (!accountActionBusy) {
                                    showDeleteDialog = false
                                    showDeactivateDialog = true
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8D8582).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PauseCircle,
                                contentDescription = null,
                                tint = Color(0xFFC9C2C0),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Need a break?",
                                color = Color(0xFFF5F1F1),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Deactivate your account instead.",
                                color = Color(0xFFAAA2A5),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Deactivate account instead",
                            tint = Color(0xFFAAA2A5),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1D1B1D))
                                .border(1.dp, Color(0xFF484346), RoundedCornerShape(50))
                                .pressScale {
                                    if (!accountActionBusy) showDeleteDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Keep account",
                                color = Color(0xFFE0DCDE),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF514A), Color(0xFFFF4038))
                                    )
                                )
                                .pressScale {
                                    if (!accountActionBusy) {
                                        accountActionBusy = true
                                        vm.deleteAccount { success, message ->
                                            accountActionBusy = false
                                            if (success) {
                                                showDeleteDialog = false
                                                navController.navigate("login") {
                                                    popUpTo(0)
                                                }
                                            } else {
                                                accountActionError = message
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accountActionBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Delete forever",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LocationPickerRow(
    value: String,
    onValueChange: (String) -> Unit,
    context: android.content.Context
) {
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadAreas() {
        if (loading) return
        loading = true
        lookupError = null
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                try {
                    if (!Geocoder.isPresent()) emptyList()
                    else {
                        @Suppress("DEPRECATION")
                        Geocoder(context, java.util.Locale.getDefault())
                            .getFromLocationName("Nagpur, Maharashtra, India", 25)
                            .orEmpty()
                            .mapNotNull { address ->
                                listOfNotNull(address.subLocality, address.locality, address.subAdminArea)
                                    .firstOrNull { it.isNotBlank() }
                                    ?.let { "$it, Nagpur" }
                            }
                            .distinct()
                    }
                } catch (_: Exception) { emptyList() }
            }
            suggestions = results
            loading = false
            if (results.isEmpty()) lookupError = "Couldn't load area suggestions. Try again later."
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(GreenSuccess.copy(0.12f)), Alignment.Center) {
                Icon(Icons.Filled.LocationOn, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Location", color = SecondaryText, fontSize = 11.sp)
                Text(value.ifBlank { "Choose an area in Nagpur" }, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { expanded = true; if (suggestions.isEmpty()) loadAreas() }) {
                Icon(Icons.Filled.ArrowDropDown, "Choose Nagpur location", tint = OrangePrimary)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF26191A), Color(0xFF111116), Color(0xFF21171B))
                    ),
                    RoundedCornerShape(18.dp)
                )
                .border(1.dp, Color(0xFFFF6848).copy(alpha = 0.78f), RoundedCornerShape(18.dp))
        ) {
            when {
                loading -> DropdownMenuItem(text = { Text("Loading Nagpur areas…", color = TertiaryText) }, onClick = {}, enabled = false)
                suggestions.isEmpty() -> {
                    DropdownMenuItem(text = { Text(lookupError ?: "No locations found", color = TertiaryText) }, onClick = {}, enabled = false)
                    DropdownMenuItem(text = { Text("Retry", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }, onClick = { loadAreas() })
                }
                else -> suggestions.forEach { area ->
                    DropdownMenuItem(text = { Text(area, color = if (area == value) OrangePrimary else PrimaryText, fontWeight = if (area == value) FontWeight.SemiBold else FontWeight.Normal) }, onClick = { onValueChange(area); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ProfileEditRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onValueChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().pressScale { editing = true }.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.12f)), Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = SecondaryText, fontSize = 11.sp)
                if (editing) {
                    OutlinedTextField(
                        value = value, onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary, unfocusedBorderColor = Divider,
                            focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText, cursorColor = OrangePrimary,
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SecondaryText,
                            focusedContainerColor = SurfaceAlt,
                            unfocusedContainerColor = SurfaceAlt
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                } else {
                    Text(value.ifBlank { "—" }, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pressScale { editing = !editing },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (editing) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = if (editing) "Finish editing $label" else "Edit $label",
                    tint = if (editing) OrangePrimary else TertiaryText,
                    modifier = Modifier.size(if (editing) 24.dp else 16.dp)
                )
            }
        }

}}

@Composable
private fun AvatarActionButton(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .pressScale(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            title,
            color = PrimaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AvatarManagementCard(
    avatarUrl: String?,
    localAvatarUri: Uri?,
    pendingAvatarUrl: String?,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onUpload: () -> Unit,
    onRandom: () -> Unit,
    onRemove: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        SettingsRow(
            "Profile Avatar",
            if (expanded)
                "Hide avatar options"
            else
                "Manage your profile avatar",
            Icons.Filled.AccountCircle,
            OrangePrimary
        ) {
            onExpandToggle()
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceAlt)
                    .border(
                        1.dp,
                        OrangePrimary.copy(alpha = 0.15f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Selected Avatar Preview",
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(alpha = 0.10f))
                        .border(
                            2.dp,
                            OrangePrimary.copy(alpha = 0.40f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {


                    val previewAvatar = when {
                        localAvatarUri != null -> localAvatarUri.toString()
                        pendingAvatarUrl != null -> pendingAvatarUrl
                        else -> avatarUrl
                    }

                    if (!previewAvatar.isNullOrBlank()) {

                        AsyncImage(
                            model = previewAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(70.dp)
                        )
                    }

                }
                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = onUpload,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose New Avatar")
                    }

                    OutlinedButton(
                        onClick = onRandom,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Random Avatar")
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Avatar")
                    }
                }
            }
        }
    }
}
