package com.example.minibooklibrary.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** A single onboarding card. Three of these compose the carousel. */
data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    @DrawableRes val illustrationRes: Int
)
