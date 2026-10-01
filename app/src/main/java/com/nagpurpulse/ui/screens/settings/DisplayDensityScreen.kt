//java/com/nagpurpulse/ui/screens/settings/DisplayDensityScreen.kt

package com.nagpurpulse.ui.screens.settings


import com.nagpurpulse.ui.preferences.FeedLayoutManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nagpurpulse.ui.preferences.DensityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayDensityScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {

    val options = listOf(
        "compact",
        "comfortable",
        "spacious"
    )

    val feedOptions = listOf(
        "compact",
        "expanded"
    )

    var selected by remember {
        mutableStateOf(
            DensityManager.density
        )
    }

    var selectedFeedStyle by remember {
        mutableStateOf(
            FeedLayoutManager.feedStyle
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Display Density")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Density options
            items(options) { density ->

                ListItem(
                    headlineContent = {
                        Text(
                            density.replaceFirstChar {
                                it.uppercase()
                            }
                        )
                    },
                    trailingContent = {

                        RadioButton(
                            selected = selected == density,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {

                        selected = density

                        viewModel.updateDisplayDensity(
                            density
                        )
                    }
                )
            }

            item {

                HorizontalDivider()

                ListItem(
                    headlineContent = {
                        Text("Feed Style")
                    }
                )
            }

            // Feed style options
            items(feedOptions) { style ->

                ListItem(
                    headlineContent = {
                        Text(
                            style.replaceFirstChar {
                                it.uppercase()
                            }
                        )
                    },
                    trailingContent = {

                        RadioButton(
                            selected = selectedFeedStyle == style,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {

                        selectedFeedStyle = style

                        FeedLayoutManager.feedStyle = style

                        android.util.Log.d(
                            "FEED_STYLE_CLICK",
                            "Selected = $style"
                        )

                        viewModel.updateFeedStyle(style)
                    }
                )
            }
        }
    }
}