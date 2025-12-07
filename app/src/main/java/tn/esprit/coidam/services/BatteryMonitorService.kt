package tn.esprit.coidam.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.repository.BatteryRepository

class BatteryMonitorService : Service() {

    private val TAG = "BatteryMonitorService"
    private val batteryRepository by lazy { BatteryRepository(this) }
    private val tokenManager by lazy { TokenManager(this) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastReportedLevel = -1
    private var lastReportedCharging = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL

                    // Only report if there's a significant change (5% difference or charging status changed)
                    val shouldReport = Math.abs(batteryPct - lastReportedLevel) >= 5 ||
                                     isCharging != lastReportedCharging ||
                                     lastReportedLevel == -1

                    if (shouldReport) {
                        scope.launch {
                            reportBatteryLevel(batteryPct, isCharging)
                        }
                        lastReportedLevel = batteryPct
                        lastReportedCharging = isCharging
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BatteryMonitorService created")

        // Register battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        // Report initial battery level
        scope.launch {
            delay(2000) // Wait 2 seconds for service to initialize
            reportCurrentBatteryLevel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BatteryMonitorService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BatteryMonitorService destroyed")
        unregisterReceiver(batteryReceiver)
        scope.cancel()
    }

    private suspend fun reportCurrentBatteryLevel() {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

            val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                           batteryStatus == BatteryManager.BATTERY_STATUS_FULL

            if (batteryLevel >= 0) {
                reportBatteryLevel(batteryLevel, isCharging)
                lastReportedLevel = batteryLevel
                lastReportedCharging = isCharging
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current battery level", e)
        }
    }

    private suspend fun reportBatteryLevel(level: Int, isCharging: Boolean) {
        try {
            // Check if user is logged in and is a blind user
            val token = tokenManager.getTokenSync()
            val userType = tokenManager.getUserTypeSync()

            if (token.isNullOrEmpty() || userType != "blind") {
                Log.d(TAG, "Skipping battery report - user not logged in as blind user")
                return
            }

            val result = batteryRepository.reportBattery(level, isCharging)
            result.onSuccess { response ->
                Log.d(TAG, "Battery level reported successfully: ${response.saved.level}% (charging: ${response.saved.isCharging})")
            }.onFailure { error ->
                Log.e(TAG, "Failed to report battery level: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting battery level", e)
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            context.stopService(intent)
        }
    }
}