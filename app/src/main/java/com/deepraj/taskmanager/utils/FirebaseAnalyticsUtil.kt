package com.deepraj.taskmanager.utils

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

object FirebaseAnalyticsUtil {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initFirebaseAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    fun logAnalyticsEvent(event: String, vararg params: Any) {
        val bundle = Bundle().apply {
            params.forEachIndexed { index, value ->
                putString("param_$index", value.toString())
            }
        }
        firebaseAnalytics.logEvent(event, bundle)
    }

}