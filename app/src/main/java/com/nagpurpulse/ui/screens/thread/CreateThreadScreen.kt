//This is the CreateThreadScreen.kt file
//java/com/nagpurpulse/ui/screens/thread/CreateThreadScreen.kt

package com.nagpurpulse.ui.screens.thread


import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import com.nagpurpulse.ui.components.pressScale
import androidx.compose.foundation.shape.RoundedCornerShape
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.util.Locale

private fun getDynamicPrompt(): String {

    val hour = java.util.Calendar.getInstance().get(
        java.util.Calendar.HOUR_OF_DAY
    )

    return when {

        hour in 5..11 -> listOf(
            "Good morning Nagpur",
            "Any updates from your area?",
            "What's happening today?"
        ).random()

        hour in 12..17 -> listOf(
            "What's happening in Nagpur?",
            "Any traffic updates?",
            "Share a local update..."
        ).random()

        hour in 18..22 -> listOf(
            "Any events tonight?",
            "Recommend a place to eat",
            "What's trending this evening?"
        ).random()

        else -> listOf(
            "Late night thoughts?",
            "Anything happening nearby?",
            "Share something interesting..."
        ).random()
    }
}

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
    val uiState  by viewModel.uiState.collectAsState()
    val context   = LocalContext.current



    // ── Permissions ────────────────────────────────────────────────────────
    val imagePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        imagePermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
    }

    val recentImages = remember { getRecentImages(context) }

    // ── Location ───────────────────────────────────────────────────────────
    var currentArea by remember { mutableStateOf<String?>(null) }

    var userLatitude by remember {
        mutableStateOf<Double?>(null)
    }

    var userLongitude by remember {
        mutableStateOf<Double?>(null)
    }

    var selectedArea by remember {
        mutableStateOf("Nagpur")
    }

    LaunchedEffect(Unit) {
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation
                .addOnSuccessListener { location ->

                    android.util.Log.d(
                        "LOCATION_TEST",
                        "Lat=${location?.latitude} Lng=${location?.longitude}"
                    )
                    if (location != null) {

                        userLatitude = location.latitude
                        userLongitude = location.longitude

                        try {
                            val addr = Geocoder(context, Locale.getDefault())
                                .getFromLocation(location.latitude, location.longitude, 1)
                                ?.firstOrNull()?.subLocality

                            currentArea = addr

                            android.util.Log.d(
                                "LOCATION",
                                "Area=$currentArea Lat=$userLatitude Lng=$userLongitude"
                            )

                        } catch (_: Exception) {}
                    }
                }
        } catch (_: Exception) {}
    }

    // ── State ──────────────────────────────────────────────────────────────
    var title by remember { mutableStateOf("") }

    var dynamicPrompt by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }

    LaunchedEffect(title.isEmpty()) {

        if (!title.isEmpty()) return@LaunchedEffect

        var currentIndex = dynamicPrompts.indices.random()

        while (title.isEmpty()) {

            val targetText = dynamicPrompts[currentIndex]

            dynamicPrompt = ""

            targetText.forEach { char ->

                if (title.isNotEmpty()) return@LaunchedEffect

                dynamicPrompt += char
                delay((35..65).random().toLong())
            }

            delay(1800)

            while (dynamicPrompt.isNotEmpty()) {

                if (title.isNotEmpty()) return@LaunchedEffect

                dynamicPrompt = dynamicPrompt.dropLast(1)
                delay((15..30).random().toLong())
            }

            delay(250)

            currentIndex = (0 until dynamicPrompts.size)
                .filter { it != currentIndex }
                .random()
        }
    }

    LaunchedEffect(title.isEmpty()) {

        if (!title.isEmpty()) return@LaunchedEffect

        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    val titleFocusRequester = remember { FocusRequester() }
    var body             by remember { mutableStateOf("") }


    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }



    var existingImageUrl by remember {

        mutableStateOf<String?>(null)

    }

    LaunchedEffect(editingPostId) {

        if (editingPostId == null) return@LaunchedEffect

        viewModel.getPostById(editingPostId) { post ->

            title = post.title
            body = post.body ?: ""
        }

        viewModel.getPostById(editingPostId) { post ->

            title = post.title
            body = post.body ?: ""

            existingImageUrl = post.imageUrl
        }

    }


    var selectedCategory by remember {
        mutableStateOf(
            if (initialCategory.isNotBlank()) initialCategory else "community"
        )
    }

    var categoryManuallyChanged by remember {
        mutableStateOf(false)
    }

    val suggestedCategory = remember(title, body) {
        suggestCategory(title, body)
    }

    LaunchedEffect(suggestedCategory) {

        if (categoryManuallyChanged) return@LaunchedEffect

        delay(500)

        if (suggestedCategory == suggestCategory(title, body)) {
            selectedCategory = suggestedCategory
        }
    }

    var isAnonymous      by remember { mutableStateOf(false) }

    val anonymousIconScale by animateFloatAsState(
        targetValue = if (isAnonymous) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "anonymous_icon_scale"
    )

    val anonymousIconColor by animateColorAsState(
        targetValue =
            if (isAnonymous)
                OrangeMain
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        label = "anonymous_icon_color"
    )

    val maxBodyChars     = 1500

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        contentVisible = true
    }

    val canPost =
        title.isNotBlank() &&
                !uiState.isLoading

    // ── Scaffold ───────────────────────────────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        // ── TOP BAR ──────────────────────────────────────────────────────
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close — small circle button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close, "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center title
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Create Post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Share with ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "NagpurPulse",
                            color = OrangeMain,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = OrangeMain,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // POST button — large orange pill
                val postScale by animateFloatAsState(
                    if (canPost) 1f else 0.94f,
                    spring(Spring.DampingRatioMediumBouncy), label = "postscale"
                )
                Box(
                    modifier = Modifier
                        .scale(postScale)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (canPost) OrangeGradient
                            else Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .pressScale {
                            if (canPost) viewModel.createPost(
                                context = context,
                                title = title,
                                body = body,
                                category = selectedCategory,
                                areaTag = selectedArea,
                                isAnonymous = isAnonymous,
                                imageUri = selectedImageUri,
                                postType = postType,
                                editingPostId = editingPostId,
                                onSuccess = onPostSuccess
                            )
                        }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        uiState.isLoading,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "postbtn"
                    ) { loading ->
                        if (loading)
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else
                            Text(
                                "Post",
                                color =
                                    if (canPost)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                    }
                }
            }
        },

        // ── BOTTOM BAR ───────────────────────────────────────────────────
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn, // using shield-like icon
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Be kind. Be local. Follow ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Community Guidelines",
                    color = OrangeMain,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }

    ) { paddingValues ->

        AnimatedVisibility(
            contentVisible,
            enter = fadeIn(tween(350)) + slideInVertically { 30 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp)
            ) {

                Spacer(Modifier.height(8.dp))

                if (
                    suggestedCategory != "community" &&
                    selectedCategory != suggestedCategory
                ) {

                    Text(
                        text = "Suggested: ${categoryChipLabel(suggestedCategory)}",
                        color = OrangeMain,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

// ── CATEGORY CHIPS ─────────────────────────
                Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                    postCategories.forEach { cat ->

                        val isSelected = selectedCategory == cat

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (isSelected)
                                        OrangeGradient
                                    else
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) OrangeMain else BorderGray,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .shadow(
                                    elevation = if (isSelected) 4.dp else 0.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    ambientColor = OrangeMain.copy(alpha = 0.15f),
                                    spotColor = OrangeMain.copy(alpha = 0.2f)
                                )
                                .pressScale {

                                    categoryManuallyChanged = true

                                    selectedCategory = cat
                                }
                                .padding(horizontal = 18.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = categoryChipIcon(cat),
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    text = categoryChipLabel(cat),
                                    color =
                                        if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight =
                                        if (isSelected)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
                                )
                            }
                        }

                    }
                    }




                Spacer(Modifier.height(8.dp))

                // ── MAIN POST CARD ────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border   = BorderStroke(0.5.dp, BorderGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        // Header row: "✨ What's happening in Nagpur?" + location pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    categoryManuallyChanged = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(105.dp)
                                    .focusRequester(titleFocusRequester),

                                placeholder = {

                                    if (title.isEmpty()) {
                                        Text(
                                            text = dynamicPrompt + if (showCursor) "|" else "",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },

                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    cursorColor = OrangeMain,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )



                        }



                        Spacer(Modifier.height(10.dp))

                        AnimatedVisibility(
                            visible = title.isNotBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Orange vertical bar (visible when focused or has text)
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (body.isNotBlank())
                                                OrangeMain
                                            else
                                                Color.Transparent
                                        )
                                )
                                Spacer(Modifier.width(10.dp))
                                OutlinedTextField(
                                    value = body,
                                    onValueChange = {
                                        if (it.length <= maxBodyChars) {
                                            body = it
                                            categoryManuallyChanged = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp),
                                    placeholder = {
                                        Text(
                                            "Add description..."
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = OrangeMain,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Add Topic
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "#",
                                    color = OrangeMain,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.width(5.dp))

                                Text(
                                    categoryChipLabel(selectedCategory),
                                    color = if (selectedCategory == "community")
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        OrangeMain,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Add Photo
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
                                    .pressScale {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text("Add Photo")
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Nagpur Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = OrangeMain,
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    selectedArea,
                                    color = TextGray,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(Modifier.width(2.dp))

                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        val counterColor by animateColorAsState(
                            when {
                                body.length > maxBodyChars * 0.9f -> Color(0xFFFF3B30)
                                body.length > maxBodyChars * 0.7f -> OrangeMain
                                else -> TextGray
                            },
                            tween(300),
                            label = "counter"
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${body.length}/$maxBodyChars",
                                color = counterColor,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                }
                }

                // ── SELECTED IMAGE PREVIEW ────────────────────────────────
                if (selectedImageUri != null || existingImageUrl != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri ?: existingImageUrl,
                            contentDescription  = "Selected image",
                            modifier            = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.65f))
                                .pressScale {
                                    selectedImageUri = null
                                    existingImageUrl = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))


                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, BorderGray),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = anonymousIconColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(anonymousIconScale)
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Post Anonymously",
                                color = anonymousIconColor,
                                fontWeight = if (isAnonymous)
                                    FontWeight.Bold
                                else
                                    FontWeight.SemiBold
                            )

                            Text(
                                "Hide your username from others",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = isAnonymous,
                            onCheckedChange = {
                                isAnonymous = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangeMain,
                                checkedBorderColor = OrangeMain,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = BorderGray
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
// ── RECENT PHOTOS ─────────────────────────────────────

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Recent Photos",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentImages.take(10)) { imageUri ->

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                if (selectedImageUri == imageUri) 2.dp else 1.dp,
                                if (selectedImageUri == imageUri)
                                    OrangeMain
                                else
                                    BorderGray,
                                RoundedCornerShape(14.dp)
                            )
                            .pressScale {
                                selectedImageUri = imageUri
                            }
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))




                Spacer(Modifier.height(8.dp))

                // ── NEARBY AREAS ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation/compass icon
                    Icon(
                        Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Nearby Areas",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Use current location",
                        color      = OrangeMain,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    // Target circle icon
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(1.5.dp, OrangeMain, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OrangeMain)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Area chips — scrollable row of square cards
                val displayAreas = buildList<Pair<String, String>> {

                    add("Nagpur" to "Entire City")

                    currentArea?.let {
                        add(it to "Current Location")
                    }

                    if (
                        userLatitude != null &&
                        userLongitude != null
                    ) {

                        val nearestAreas = nagpurAreas
                            .map { area ->

                                area.name to calculateDistanceKm(
                                    userLatitude!!,
                                    userLongitude!!,
                                    area.latitude,
                                    area.longitude
                                )
                            }
                            .sortedBy {
                                it.second
                                    .replace(" km", "")
                                    .toFloat()
                            }
                            .take(8)

                        addAll(nearestAreas)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayAreas.forEach { (area, dist) ->
                        val isSelected = selectedArea == area
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .height(52.dp)
                                .pressScale { selectedArea = area },
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) OrangeMain else BorderGray
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.LocationCity,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        OrangeMain
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                Column {

                                    Text(
                                        area,
                                        color =
                                            if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )

                                    Text(
                                        dist,
                                        color = TextGray,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── ANONYMOUS ACTIVE BANNER ───────────────────────────────


                // ── ERROR ─────────────────────────────────────────────────
                AnimatedVisibility(
                    uiState.error != null,
                    enter = fadeIn(tween(250)) + expandVertically(),
                    exit  = fadeOut(tween(200)) + shrinkVertically()
                ) {
                    if (uiState.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = uiState.error ?: "",
                                    color = Color(0xFFFF3B30),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── BOTTOM TOOLBAR — circle icon buttons ──────────────────
                // Photo | Location | Poll | Anonymous | More

            }
        }
    }
}


private val dynamicPrompts = listOf(
    "What's happening in Nagpur?",
    "Any traffic updates today?",
    "Share something useful...",
    "What's trending near you?",
    "Any hidden food gems?",
    "What's the weather like?",
    "Any events this evening?",
    "Power cut in your area?",
    "Any road closures today?",
    "Recommend a good cafe...",
    "What should Nagpur know?",
    "Any job openings nearby?",
    "Share a local update...",
    "What's new around VNIT?",
    "Anything happening in Sitabuldi?",
    "Best place to visit today?"
)

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

private fun calculateDistanceKm(
    userLat: Double,
    userLng: Double,
    areaLat: Double,
    areaLng: Double
): String {

    val results = FloatArray(1)

    android.location.Location.distanceBetween(
        userLat,
        userLng,
        areaLat,
        areaLng,
        results
    )

    val distanceKm = results[0] / 1000

    return String.format("%.1f km", distanceKm)
}



