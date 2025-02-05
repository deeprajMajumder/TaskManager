package com.deepraj.taskmanager.di

import android.app.Application
import com.deepraj.taskmanager.utils.FirebaseAnalyticsUtil
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseAnalyticsUtil.initFirebaseAnalytics()
    }
}