package com.joon.chalkak.data.settings

import android.content.Context

class TermsAgreementPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun hasAgreedToRequiredTerms(): Boolean =
        preferences.getBoolean(KEY_REQUIRED_TERMS_AGREED, false)

    fun saveRequiredTermsAgreement() {
        preferences.edit()
            .putBoolean(KEY_REQUIRED_TERMS_AGREED, true)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "terms_agreement_preferences"
        const val KEY_REQUIRED_TERMS_AGREED = "required_terms_agreed"
    }
}
