package com.nagpurpulse.ui.screens.settings

import androidx.compose.material.icons.automirrored.filled.ArrowBack


import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nagpurpulse.ui.preferences.PreferenceManager
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.settingsMessage) {
        uiState.settingsMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissSettingsMessage()
        }
    }

    val options = listOf(
        "small",
        "medium",
        "large",
        "extra_large"
    )

    var selected by remember {
        mutableStateOf(
            PreferenceManager.textSize
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Text Size", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            items(options) { size ->

                ListItem(
                    headlineContent = {
                        Text(
                            text = size.replace("_", " ")
                                .replaceFirstChar {
                                    it.uppercase()
                                },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {

                        RadioButton(
                            selected = selected == size,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {

                        selected = size

                        viewModel.updateTextSize(size)

                        android.util.Log.d(
                            "TEXT_SIZE",
                            "Selected = $size"
                        )
                    }
                )
            }
        }
    }
}