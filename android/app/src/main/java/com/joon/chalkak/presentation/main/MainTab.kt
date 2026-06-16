package com.joon.chalkak.presentation.main

enum class MainTab(val route: String) {
    HOME("home"),
    HISTORY("history"),
    SETTINGS("settings");

    companion object {
        fun fromRoute(route: String?): MainTab =
            entries.firstOrNull { it.route == route } ?: HOME

        fun isTabRoute(route: String?): Boolean =
            entries.any { it.route == route }
    }
}
