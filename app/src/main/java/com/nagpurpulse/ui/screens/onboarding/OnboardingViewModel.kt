package com.nagpurpulse.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ViewModel for the splash/onboarding flow.
// Auto-navigation to Home (if session exists) or Onboarding is handled in NavGraph.
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel()
