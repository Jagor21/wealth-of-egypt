package com.argentum_petasum.wealthofegypt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerProperties
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val LOG_TAG = "WealthOfEgyptApp"

class WealthOfEgyptApp : Application() {

    private lateinit var sharedPreferences: SharedPreferences

    val conversionData = MutableStateFlow<Any?>("")

    override fun onCreate() {
        super.onCreate()
        createNotificationsChannels()
        createSilentNotificationChannel()
        FirebaseApp.initializeApp(this)
        sharedPreferences = getSharedPreferences("your_prefs", MODE_PRIVATE)
        val prefEditor = sharedPreferences.edit()
        val conversionDataListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                Log.d(LOG_TAG, data.toString())
                prefEditor.putString("conversion_data", data?.get("campaign").toString()).commit()
                conversionData.update { data?.get("campaign") }
            }

            override fun onConversionDataFail(error: String?) {
                Log.e(LOG_TAG, "error onAttributionFailure :  $error")
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                // Must be overriden to satisfy the AppsFlyerConversionListener interface.
                // Business logic goes here when UDL is not implemented.
                data?.map {
                    Log.d(LOG_TAG, "onAppOpen_attribute: ${it.key} = ${it.value}")
                }
            }

            override fun onAttributionFailure(error: String?) {
                // Must be overriden to satisfy the AppsFlyerConversionListener interface.
                // Business logic goes here when UDL is not implemented.
                Log.e(LOG_TAG, "error onAttributionFailure :  $error")
            }
        }
        AppsFlyerLib.getInstance().setDebugLog(true)
        AppsFlyerLib.getInstance()
            .init(getString(R.string.appsflyer_dev_key), conversionDataListener, this)
        AppsFlyerLib.getInstance().start(this)
        val id = AppsFlyerLib.getInstance().getAppsFlyerUID(this)
        id?.let { Log.d(LOG_TAG, "AppsFlyerUID: $it") }
        val cuid = AppsFlyerProperties.getInstance().getString(AppsFlyerProperties.APP_USER_ID)
        Log.d(LOG_TAG, "CUID: $cuid")
    }

    private fun createNotificationsChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.reminders_notification_channel_id),
                getString(R.string.reminders_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            ContextCompat.getSystemService(this, NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun createSilentNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(WealthOfEgyptFCMService.SILENT_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    WealthOfEgyptFCMService.SILENT_CHANNEL_ID,
                    "WheelOfFortune silent events",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setSound(null, null)
                manager.createNotificationChannel(channel)
            }
        }
    }
}