//This is the CreateThreadScreen.kt file
//java/com/nagpurpulse/ui/screens/thread/CreateThreadScreen.kt

package com.nagpurpulse.ui.screens.thread

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

private const val MAX_TITLE_CHARS = 120
private const val MAX_BODY_CHARS = 1500
private const val CITY = "Nagpur"

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThreadScreen(
    initialCategory: String = "",
    postType: String = "normal",
    editingPostId: String? = null,
    onClose: () -> Unit,
    onPostSuccess: () -> Unit,
    viewModel: CreateThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditing = editingPostId != null
    val userId = viewModel.currentUserId
    val draftStore = remember { PostDraftStore(context) }

    // ── Content state ──────────────────────────────────────────────────────
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable {
        mutableStateOf(initialCategory.ifBlank { "community" })
    }
    // A category the user picked (or that was passed in) is never overridden by auto-suggest.
    var categoryManuallyChanged by rememberSaveable { mutableStateOf(initialCategory.isNotBlank()) }
    var isAnonymous by rememberSaveable { mutableStateOf(false) }
    var selectedArea by rememberSaveable { mutableStateOf(CITY) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    var removedExistingImage by remember { mutableStateOf(false) }

    // Edit-mode baseline, used to detect unsaved changes
    var baselineTitle by remember { mutableStateOf("") }
    var baselineBody by remember { mutableStateOf("") }

    // ── UI state ───────────────────────────────────────────────────────────
    var titleFocused by remember { mutableStateOf(false) }
    var bodyFocused by remember { mutableStateOf(false) }
    var lastFocusedField by remember { mutableStateOf("title") }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAreaSheet by remember { mutableStateOf(false) }
    var showEmojiRow by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }

    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }

    // ── Edit mode: load the existing post (once) ───────────────────────────
    LaunchedEffect(editingPostId) {
        if (editingPostId == null) return@LaunchedEffect
        viewModel.getPostById(editingPostId) { post ->
            title = post.title
            body = post.body ?: ""
            baselineTitle = post.title
            baselineBody = post.body ?: ""
            existingImageUrl = post.imageUrl
            // Keep the post's current settings instead of silently resetting them
            if (post.category.isNotBlank()) selectedCategory = post.category
            post.areaTag?.takeIf { it.isNotBlank() }?.let { selectedArea = it }
            isAnonymous = post.isAnonymous
            categoryManuallyChanged = true
        }
    }

    // ── Draft restore / autosave (new posts only) ──────────────────────────
    var pendingDraft by remember {
        mutableStateOf(
            if (!isEditing && title.isBlank() && body.isBlank()) draftStore.load(userId) else null
        )
    }

    LaunchedEffect(title, body, selectedCategory, selectedArea, isAnonymous) {
        if (isEditing || showSuccess) return@LaunchedEffect
        if (pendingDraft != null) {
            // Wait for the user's decision; typing something new replaces the old draft.
            if (title.isBlank() && body.isBlank()) return@LaunchedEffect
            pendingDraft = null
        }
        delay(500)
        draftStore.save(
            userId,
            PostDraftStore.Draft(title, body, selectedCategory, selectedArea, isAnonymous)
        )
    }

    // ── Auto-focus the title so the keyboard is already up ─────────────────
    LaunchedEffect(Unit) {
        if (!isEditing) {
            delay(150)
            try { titleFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    // ── Location (requested only when the user asks for it) ────────────────
    var locationGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var currentArea by remember { mutableStateOf<String?>(null) }
    var locationTick by remember { mutableIntStateOf(0) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result.values.any { it }
        if (!locationGranted) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Location permission is off. You can still pick an area manually.")
            }
        }
    }

    LaunchedEffect(locationGranted, locationTick) {
        if (!locationGranted) return@LaunchedEffect
        val location = try {
            lastKnownLocation(context)
        } catch (_: SecurityException) {
            null
        } ?: return@LaunchedEffect
        userLat = location.latitude
        userLng = location.longitude
        currentArea = resolveSubLocality(context, location.latitude, location.longitude)
    }

    fun useCurrentLocation() {
        haptic.tap()
        val area = currentArea
        when {
            !locationGranted -> locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            area != null -> selectedArea = area
            else -> {
                locationTick++
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Finding your area…")
                }
            }
        }
    }

    val areaPills = remember(userLat, userLng, currentArea, selectedArea) {
        buildAreaOptions(userLat, userLng, currentArea, selectedArea, limit = 6)
    }
    val allAreas = remember(userLat, userLng, currentArea) {
        buildAreaOptions(userLat, userLng, currentArea, selectedArea = null, limit = null)
    }

    // ── Photos (permission is asked only when the user wants recent photos) ─
    var hasMediaPermission by remember { mutableStateOf(hasPermission(context, mediaPermission())) }
    var recentImages by remember { mutableStateOf<List<Uri>?>(null) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMediaPermission = granted }

    LaunchedEffect(hasMediaPermission) {
        recentImages = if (hasMediaPermission) loadRecentImages(context) else emptyList()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Cancelling the picker must not clear a photo that is already attached.
        if (uri != null) {
            selectedImageUri = uri
            haptic.tap()
        }
    }

    fun openPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // ── Insights (social proof, reach, trending) ───────────────────────────
    LaunchedEffect(selectedArea) { viewModel.loadInsights(selectedArea) }

    // ── Category auto-suggest (debounced) ──────────────────────────────────
    var suggestedCategory by remember { mutableStateOf("community") }
    LaunchedEffect(title, body) {
        delay(400)
        suggestedCategory = suggestCategory(title, body)
        if (!categoryManuallyChanged && suggestedCategory != selectedCategory) {
            selectedCategory = suggestedCategory
        }
    }

    // ── Prompts ────────────────────────────────────────────────────────────
    val prompts = remember(currentArea) {
        composerPrompts(currentArea, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    // ── Derived ────────────────────────────────────────────────────────────
    val hasImage = selectedImageUri != null || existingImageUrl != null
    val progress = postQualityProgress(title, body, hasImage, selectedArea != CITY)
    val nudge = postQualityNudge(title, body, hasImage, selectedArea != CITY)

    val postState = when {
        showSuccess            -> PostButtonState.Success
        uiState.isLoading      -> PostButtonState.Loading
        title.isNotBlank()     -> PostButtonState.Ready
        else                   -> PostButtonState.Disabled
    }

    // ── Shake (invalid submit / error) ─────────────────────────────────────
    val shake = remember { Animatable(0f) }
    var shakeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(shakeTick) {
        if (shakeTick == 0) return@LaunchedEffect
        repeat(3) {
            shake.animateTo(12f, tween(45))
            shake.animateTo(-12f, tween(45))
        }
        shake.animateTo(0f, tween(45))
    }

    // ── Errors → snackbar + haptic + shake ─────────────────────────────────
    LaunchedEffect(uiState.errorNonce) {
        val message = uiState.error
        if (uiState.errorNonce > 0 && message != null) {
            haptic.error()
            shakeTick++
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ── Success → celebrate briefly, then leave ────────────────────────────
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(if (isEditing) 700L else 1500L)
            onPostSuccess()
        }
    }

    // ── Closing / discarding ───────────────────────────────────────────────
    fun hasUnsavedChanges(): Boolean =
        if (isEditing)
            title != baselineTitle || body != baselineBody ||
                    selectedImageUri != null || removedExistingImage
        else
            title.isNotBlank() || body.isNotBlank() || selectedImageUri != null

    fun attemptClose() {
        if (uiState.isLoading || showSuccess) return
        if (hasUnsavedChanges()) {
            focusManager.clearFocus()
            showDiscardDialog = true
        } else {
            onClose()
        }
    }

    BackHandler(enabled = !showSuccess) { attemptClose() }

    // ── Submit ─────────────────────────────────────────────────────────────
    fun submit() {
        if (title.isBlank()) {
            haptic.error()
            shakeTick++
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Add a title to share your post")
            }
            return
        }
        if (uiState.isLoading || showSuccess) return
        focusManager.clearFocus()
        viewModel.createPost(
            context = context,
            title = title,
            body = body,
            category = selectedCategory,
            areaTag = selectedArea,
            isAnonymous = isAnonymous,
            imageUri = selectedImageUri,
            postType = postType,
            editingPostId = editingPostId,
            clearImage = removedExistingImage && selectedImageUri == null,
            onSuccess = {
                haptic.success()
                streak = if (isEditing) 0 else draftStore.recordPost()
                draftStore.clear()
                showSuccess = true
            }
        )
    }

    fun insertEmoji(emoji: String) {
        haptic.tap()
        if (lastFocusedField == "body") {
            if (body.length + emoji.length <= MAX_BODY_CHARS) body += emoji
        } else {
            if (title.length + emoji.length <= MAX_TITLE_CHARS) title += emoji
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },

            // ── TOP BAR ─────────────────────────────────────────────────────
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .pointerInput(Unit) {
                            // Pull down to dismiss, like a sheet
                            var dragged = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragged = 0f },
                                onDragCancel = { dragged = 0f },
                                onDragEnd = { if (dragged > 200f) attemptClose() },
                                onVerticalDrag = { change, dy ->
                                    dragged += dy
                                    change.consume()
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pressScale { attemptClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close, "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (isEditing) "Edit Post" else "Create Post",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isEditing) "Editing your post" else "Share with ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                if (!isEditing) {
                                    Text(
                                        "NagpurPulse",
                                        color = OrangeMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = title.isNotBlank() && !showSuccess,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(150))
                        ) {
                            QualityRing(progress, Modifier.padding(end = 10.dp))
                        }

                        PostButton(
                            state = postState,
                            label = if (isEditing) "Save" else "Post",
                            shakeOffset = shake.value,
                            onClick = { submit() }
                        )
                    }
                }
            },

            // ── BOTTOM BAR (pinned above the keyboard) ──────────────────────
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    QuickEmojiRow(visible = showEmojiRow) { insertEmoji(it) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolButton(Icons.Filled.Image, "Add photo", hasImage) { openPhotoPicker() }
                        ToolButton(
                            Icons.Filled.Tag, "Choose topic",
                            selectedCategory != "community"
                        ) { showCategorySheet = true }
                        ToolButton(
                            Icons.Filled.LocationOn, "Choose area",
                            selectedArea != CITY
                        ) { showAreaSheet = true }
                        ToolButton(
                            if (isAnonymous) Icons.Filled.Lock else Icons.Filled.VisibilityOff,
                            "Post anonymously", isAnonymous
                        ) {
                            haptic.tap()
                            isAnonymous = !isAnonymous
                        }
                        ToolButton(
                            Icons.Filled.EmojiEmotions, "Emoji", showEmojiRow
                        ) { showEmojiRow = !showEmojiRow }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Shield, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Be kind. Be local. Follow ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            "Community Guidelines",
                            color = OrangeMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            " ›",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }

        ) { paddingValues ->

            val entry = remember { MutableTransitionState(false).apply { targetState = true } }

            AnimatedVisibility(
                visibleState = entry,
                enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { 24 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {

                    Spacer(Modifier.height(8.dp))

                    // ── Draft restore ──────────────────────────────────────
                    AnimatedVisibility(
                        visible = pendingDraft != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val draft = pendingDraft
                        Column {
                            DraftBanner(
                                preview = draft?.let { it.title.ifBlank { it.body } }.orEmpty(),
                                onRestore = {
                                    draft?.let {
                                        title = it.title
                                        body = it.body
                                        selectedCategory = it.category
                                        selectedArea = it.area
                                        isAnonymous = it.isAnonymous
                                        categoryManuallyChanged = true
                                    }
                                    pendingDraft = null
                                    haptic.tap()
                                },
                                onDiscard = {
                                    draftStore.clear()
                                    pendingDraft = null
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Social proof + trending ────────────────────────────
                    if (!isEditing) {
                        TrendingStrip(
                            postsToday = uiState.insights.postsToday,
                            trending = uiState.insights.trendingCategories,
                            onTrendingClick = { cat ->
                                selectedCategory = cat
                                categoryManuallyChanged = true
                                haptic.tap()
                            }
                        )
                        if (uiState.insights.postsToday != null ||
                            uiState.insights.trendingCategories.isNotEmpty()
                        ) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── COMPOSER CARD ──────────────────────────────────────
                    val cardBorder by animateColorAsState(
                        if (titleFocused || bodyFocused) OrangeMain.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        tween(200), label = "card_border"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (titleFocused || bodyFocused) 1.5.dp else 0.5.dp,
                            cardBorder
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            ComposerHeader(
                                displayName = uiState.displayName,
                                avatarUrl = uiState.avatarUrl,
                                isAnonymous = isAnonymous
                            )

                            Spacer(Modifier.height(14.dp))

                            // Title
                            BasicTextField(
                                value = title,
                                onValueChange = { if (it.length <= MAX_TITLE_CHARS) title = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(titleFocus)
                                    .onFocusChanged {
                                        titleFocused = it.isFocused
                                        if (it.isFocused) lastFocusedField = "title"
                                    },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(OrangeMain),
                                minLines = 2,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { bodyFocus.requestFocus() }
                                ),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (title.isEmpty()) {
                                            TypewriterPlaceholder(
                                                prompts = prompts,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                showCursor = !titleFocused
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            if (title.length >= MAX_TITLE_CHARS * 0.8f) {
                                Text(
                                    "${MAX_TITLE_CHARS - title.length} left",
                                    color = if (title.length >= MAX_TITLE_CHARS) MaterialTheme.colorScheme.error
                                    else OrangeMain,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 4.dp)
                                )
                            }

                            // One-tap starters while the composer is empty
                            AnimatedVisibility(
                                visible = title.isEmpty() && body.isEmpty() && !isEditing,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    StarterChips { starter ->
                                        haptic.tap()
                                        title = starter.template
                                        selectedCategory = starter.category
                                        categoryManuallyChanged = true
                                        scope.launch {
                                            delay(50)
                                            try { titleFocus.requestFocus() } catch (_: Exception) {}
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Description (always available, so category suggestions work early)
                            Row(Modifier.height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (body.isNotBlank() || bodyFocused) OrangeMain
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = body,
                                    onValueChange = { if (it.length <= MAX_BODY_CHARS) body = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(bodyFocus)
                                        .onFocusChanged {
                                            bodyFocused = it.isFocused
                                            if (it.isFocused) lastFocusedField = "body"
                                        },
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    ),
                                    cursorBrush = SolidColor(OrangeMain),
                                    minLines = 3,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    decorationBox = { inner ->
                                        Box(Modifier.fillMaxWidth()) {
                                            if (body.isEmpty()) {
                                                Text(
                                                    "Add more details (optional)",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }

                            if (body.length >= MAX_BODY_CHARS * 0.7f) {
                                val remaining = MAX_BODY_CHARS - body.length
                                val counterColor by animateColorAsState(
                                    if (body.length > MAX_BODY_CHARS * 0.9f)
                                        MaterialTheme.colorScheme.error
                                    else OrangeMain,
                                    tween(300), label = "body_counter"
                                )
                                Text(
                                    "$remaining left",
                                    color = counterColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 4.dp)
                                )
                            }

                            // Attached photo
                            AnimatedVisibility(
                                visible = hasImage,
                                enter = fadeIn(tween(250)) + expandVertically(),
                                exit = fadeOut(tween(150)) + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(14.dp))
                                    AttachedImagePreview(
                                        model = selectedImageUri ?: existingImageUrl ?: "",
                                        onRemove = {
                                            haptic.tap()
                                            if (existingImageUrl != null) removedExistingImage = true
                                            selectedImageUri = null
                                            existingImageUrl = null
                                        },
                                        onReplace = { openPhotoPicker() }
                                    )
                                }
                            }
                        }
                    }

                    // ── Category suggestion + quality nudge ────────────────
                    Spacer(Modifier.height(10.dp))

                    CategorySuggestion(
                        suggested = suggestedCategory,
                        visible = suggestedCategory != "community" &&
                                suggestedCategory != selectedCategory,
                        onApply = {
                            haptic.tap()
                            selectedCategory = suggestedCategory
                            categoryManuallyChanged = false
                        }
                    )

                    AnimatedContent(
                        targetState = nudge,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "nudge"
                    ) { text ->
                        if (text != null) {
                            Row(
                                modifier = Modifier.padding(bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Lightbulb, contentDescription = null,
                                    tint = OrangeMain, modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Spacer(Modifier.height(0.dp))
                        }
                    }

                    // ── Topic ──────────────────────────────────────────────
                    SectionLabel("Topic")
                    Spacer(Modifier.height(8.dp))
                    CategoryChipRow(
                        selected = selectedCategory,
                        onSelect = {
                            haptic.tap()
                            selectedCategory = it
                            categoryManuallyChanged = true
                        },
                        onMore = { showCategorySheet = true }
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Recent photos ──────────────────────────────────────
                    RecentPhotosSection(
                        hasPermission = hasMediaPermission,
                        images = recentImages,
                        selectedUri = selectedImageUri,
                        onRequestPermission = {
                            mediaPermissionLauncher.launch(mediaPermission())
                        },
                        onBrowse = { openPhotoPicker() },
                        onToggle = { uri ->
                            haptic.tap()
                            selectedImageUri = if (selectedImageUri == uri) null else uri
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Area ───────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Navigation, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Nearby areas",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .pressScale { useCurrentLocation() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Use current location",
                                color = OrangeMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.MyLocation, contentDescription = null,
                                tint = OrangeMain, modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    AreaPills(
                        options = areaPills,
                        selected = selectedArea,
                        onSelect = {
                            haptic.tap()
                            selectedArea = it
                        },
                        onMore = { showAreaSheet = true }
                    )

                    // Reach preview
                    Spacer(Modifier.height(10.dp))
                    val reach = uiState.insights.reachCount
                    Text(
                        text = when {
                            reach != null && reach >= 10 && selectedArea == CITY ->
                                "Visible to ~${if (reach >= 1000) "1,000+" else reach.toString()} neighbours across Nagpur"
                            reach != null && reach >= 10 ->
                                "Reaches ~${if (reach >= 1000) "1,000+" else reach.toString()} people following $selectedArea"
                            selectedArea == CITY -> "Posting to all of Nagpur"
                            else -> "Posting to $selectedArea"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Anonymous ──────────────────────────────────────────
                    AnonymousCard(
                        isAnonymous = isAnonymous,
                        onToggle = {
                            haptic.tap()
                            isAnonymous = it
                        }
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // ── Success moment ─────────────────────────────────────────────────
        SuccessOverlay(visible = showSuccess, isEdit = isEditing, streak = streak)
    }

    // ── Sheets ─────────────────────────────────────────────────────────────
    if (showCategorySheet) {
        CategorySheet(
            selected = selectedCategory,
            onSelect = {
                haptic.tap()
                selectedCategory = it
                categoryManuallyChanged = true
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showAreaSheet) {
        AreaSheet(
            options = allAreas,
            selected = selectedArea,
            onSelect = {
                haptic.tap()
                selectedArea = it
                showAreaSheet = false
            },
            onDismiss = { showAreaSheet = false }
        )
    }

    // ── Leave / discard confirmation ───────────────────────────────────────
    if (showDiscardDialog) {
        if (isEditing) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard changes?") },
                text = { Text("Your edits to this post won't be saved.") },
                confirmButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep editing", color = OrangeMain, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        onClose()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Leave this post?") },
                text = { Text("Save it as a draft to pick up where you left off. Tap outside to keep writing.") },
                confirmButton = {
                    TextButton(onClick = {
                        draftStore.save(
                            userId,
                            PostDraftStore.Draft(title, body, selectedCategory, selectedArea, isAnonymous)
                        )
                        showDiscardDialog = false
                        onClose()
                    }) { Text("Save draft", color = OrangeMain, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        draftStore.clear()
                        showDiscardDialog = false
                        onClose()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Permissions & location helpers ────────────────────────────────────────────
private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun hasLocationPermission(context: Context): Boolean =
    hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

/** READ_MEDIA_IMAGES only exists on Android 13+; older versions use READ_EXTERNAL_STORAGE. */
private fun mediaPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

@SuppressLint("MissingPermission")
private suspend fun lastKnownLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)

    val last = suspendCancellableCoroutine<Location?> { cont ->
        client.lastLocation
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }
    if (last != null) return last

    return suspendCancellableCoroutine { cont ->
        val cancellation = CancellationTokenSource()
        cont.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }
}

/** Reverse geocoding is blocking I/O — always run it off the main thread. */
@Suppress("DEPRECATION")
private suspend fun resolveSubLocality(context: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        try {
            Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
                ?.subLocality
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

/**
 * Builds the area options. When [limit] is set (the pill row) only the nearest areas are kept;
 * when null (the full sheet) every area is listed. [selectedArea] is pinned so a choice made
 * in the sheet is always visible in the row.
 */
private fun buildAreaOptions(
    lat: Double?,
    lng: Double?,
    currentArea: String?,
    selectedArea: String?,
    limit: Int?
): List<AreaOption> {
    val result = mutableListOf(AreaOption(CITY, "Entire city", isCity = true))

    currentArea?.let {
        result += AreaOption(it, "Your location", isCurrent = true)
    }

    val others = nagpurAreas
        .filter { !it.name.equals(currentArea, ignoreCase = true) }
        .let { areas ->
            if (lat != null && lng != null) {
                areas
                    .map { it to distanceKm(lat, lng, it.latitude, it.longitude) }
                    .sortedBy { it.second }
                    .map { (area, km) -> AreaOption(area.name, formatDistance(km)) }
            } else {
                areas.sortedBy { it.name }.map { AreaOption(it.name, "Nagpur") }
            }
        }

    result += if (limit != null) others.take(limit) else others

    if (selectedArea != null && result.none { it.name == selectedArea }) {
        result.add(1.coerceAtMost(result.size), AreaOption(selectedArea, "Selected"))
    }
    return result
}

data class NagpurArea(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private val nagpurAreas = listOf(

    NagpurArea("Sitabuldi", 21.1458, 79.0882),
    NagpurArea("Dharampeth", 21.1345, 79.0628),
    NagpurArea("Sadar", 21.1645, 79.0810),
    NagpurArea("Civil Lines", 21.1540, 79.0740),
    NagpurArea("Ramdaspeth", 21.1332, 79.0765),
    NagpurArea("Dhantoli", 21.1360, 79.0829),
    NagpurArea("Manish Nagar", 21.0924, 79.0722),
    NagpurArea("Pratap Nagar", 21.1158, 79.0526),
    NagpurArea("Narendra Nagar", 21.1030, 79.0768),
    NagpurArea("Trimurti Nagar", 21.1165, 79.0463),
    NagpurArea("Khamla", 21.1118, 79.0569),
    NagpurArea("Mahal", 21.1415, 79.1008),
    NagpurArea("Itwari", 21.1518, 79.1065),
    NagpurArea("Nandanvan", 21.1219, 79.1178),
    NagpurArea("Sakkardara", 21.1221, 79.1042),
    NagpurArea("Jaripatka", 21.1963, 79.0827),
    NagpurArea("Besa", 21.0772, 79.0984),
    NagpurArea("Medical Square", 21.1287, 79.0954),
    NagpurArea("Ajni", 21.1211, 79.0822),
    NagpurArea("Shankar Nagar", 21.1381, 79.0607),
    NagpurArea("Gokulpeth", 21.1408, 79.0558),
    NagpurArea("Seminary Hills", 21.1661, 79.0505),
    NagpurArea("Lakadganj", 21.1462, 79.1172),
    NagpurArea("Mankapur", 21.1896, 79.0655),
    NagpurArea("Wardha Road", 21.0853, 79.0601),
    NagpurArea("Hudkeshwar", 21.1285, 79.1318),
    NagpurArea("Pardi", 21.1498, 79.1443)
)
