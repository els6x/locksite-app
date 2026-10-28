package com.locksite.app.presentation.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD_DOMAIN = "add-domain"
    const val LOCKED_LIST = "locked-list"
    const val SETTINGS = "settings"

    val topLevel = setOf(HOME, LOCKED_LIST, SETTINGS)
}
