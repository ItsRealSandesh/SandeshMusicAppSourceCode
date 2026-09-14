package com.sandeshmusic.app.inappmessaging

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.installations.FirebaseInstallations

object InAppMessagingHelper {

    private const val TAG = "InAppMessagingHelper"

    private var cachedInstallationId: String = ""

    fun getInstallationId(onResult: (String) -> Unit) {
        if (cachedInstallationId.isNotEmpty()) {
            onResult(cachedInstallationId)
            return
        }
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cachedInstallationId = task.result ?: ""
                onResult(cachedInstallationId)
            } else {
                onResult("")
            }
        }
    }

    fun initialize(context: Context) {
        try {
            // Enable In-App Messaging
            val inAppMessaging = FirebaseInAppMessaging.getInstance()
            inAppMessaging.isAutomaticDataCollectionEnabled = true
            inAppMessaging.setMessagesSuppressed(false)

            // Log Firebase Installation ID (useful for sending test in-app campaign messages in console)
            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val installationId = task.result
                    cachedInstallationId = installationId ?: ""
                    Log.d(TAG, "==================================================")
                    Log.d(TAG, "Firebase Installation ID for In-App Messaging Testing:")
                    Log.d(TAG, installationId ?: "unknown")
                    Log.d(TAG, "==================================================")
                } else {
                    Log.w(TAG, "Failed to retrieve Firebase Installation ID", task.exception)
                }
            }

            Log.d(TAG, "Firebase In-App Messaging initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase In-App Messaging", e)
        }
    }

    /**
     * Trigger a custom Analytics event that can trigger Firebase In-App Campaign messages.
     */
    fun triggerEvent(context: Context, eventName: String, params: Bundle? = null) {
        try {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
            Log.d(TAG, "Logged analytics event for In-App Messaging trigger: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event $eventName", e)
        }
    }

    /**
     * Helper to trigger a predefined trigger event (e.g. user played song, liked song, opened settings, etc.)
     */
    fun triggerCustomCampaignTrigger(context: Context, triggerName: String) {
        val bundle = Bundle().apply {
            putString("campaign_trigger", triggerName)
            putLong("timestamp", System.currentTimeMillis())
        }
        triggerEvent(context, triggerName, bundle)
    }
}
