package com.joon.chalkak.data.settings

import android.content.Context

class DrivingRegionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getPrimaryRegions(): List<DrivingRegion> =
        preferences.getStringSet(KEY_PRIMARY_PROVINCES, null)
            ?.map { province -> DrivingRegion(provinceName = province) }
            ?.sortedBy { it.provinceName }
            ?.takeIf { it.isNotEmpty() }
            ?: getLegacyPrimaryRegion()?.let { listOf(it) }
            ?: emptyList()

    fun savePrimaryRegions(regions: List<DrivingRegion>) {
        preferences.edit()
            .putStringSet(KEY_PRIMARY_PROVINCES, regions.map { it.provinceName }.toSet())
            .remove(KEY_PRIMARY_PROVINCE)
            .remove(KEY_PRIMARY_CITY)
            .apply()
    }

    private fun getLegacyPrimaryRegion(): DrivingRegion? {
        val province = preferences.getString(KEY_PRIMARY_PROVINCE, null)?.takeIf { it.isNotBlank() }
        return province?.let { DrivingRegion(provinceName = it) }
    }

    private companion object {
        const val PREFERENCES_NAME = "driving_region_preferences"
        const val KEY_PRIMARY_PROVINCES = "primary_provinces"
        const val KEY_PRIMARY_PROVINCE = "primary_province"
        const val KEY_PRIMARY_CITY = "primary_city"
    }
}

data class DrivingRegion(
    val provinceName: String,
    val cityName: String? = null
)
