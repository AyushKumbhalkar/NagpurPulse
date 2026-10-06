
        composable(Screen.NotifSettings.route) {
            NotifSettingsScreen(navController = navController)
        }

        composable(Screen.TextSize.route) {
            TextSizeScreen(navController = navController)
        }

        composable(Screen.DisplayDensity.route) {
            DisplayDensityScreen(navController = navController)
        }

        // ── Admin Panel (NEW) ──────────────────────────────────────────────────
        composable(
            route = Screen.AdminPanel.route,
            enterTransition = { sheetEnter(this) },
            exitTransition = { sheetExit(this) },
            popExitTransition = { sheetExit(this) }
        ) {
            var adminAccess by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                adminAccess = authRepository.isCurrentUserAdmin()
            }

            when (adminAccess) {
                true -> AdminPanelScreen(navController = navController)
                false -> {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
                null -> {
                    // Fail closed while the server-backed role check is running.
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        // ──────────────────────────────────────────────────────────────────────
    }
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showLoginDialog = false
            },
            title = {
                Text("Sign in Required")
            },
            text = {
                Text("To continue, please sign in or create an account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginDialog = false
                        navController.navigate(Screen.Login.route)
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {