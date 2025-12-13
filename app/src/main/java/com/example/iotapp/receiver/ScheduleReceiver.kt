package com.example.iotapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Calendar

class ScheduleReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ScheduleReceiver"
        const val ACTION_SCHEDULE_ON = "com.example.iotapp.SCHEDULE_ON"
        const val ACTION_SCHEDULE_OFF = "com.example.iotapp.SCHEDULE_OFF"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCHEDULE_ON -> {
                Log.d(TAG, "Schedule ON triggered")
                setRelayState(true)

                val scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1)
                if (scheduleId != -1) {
                    scheduleOffAfter3Minutes(context, scheduleId)
                }
            }
            ACTION_SCHEDULE_OFF -> {
                Log.d(TAG, "Schedule OFF triggered")
                setRelayState(false)
            }
        }
    }

    private fun setRelayState(isOn: Boolean) {
        val sensorRef = FirebaseDatabase.getInstance().getReference("sensor")
        val update = mapOf(
            "value" to if (isOn) "ON" else "OFF",
            "timestamp" to ServerValue.TIMESTAMP
        )
        sensorRef.child("relay").updateChildren(update).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Relay state updated -> ${update["value"]}")
            } else {
                Log.e(TAG, "Failed to update relay", task.exception)
            }
        }
    }

    private fun scheduleOffAfter3Minutes(context: Context, scheduleId: Int) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 3)
        }
        
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_OFF
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        }
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            scheduleId + 1000,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    pendingIntent
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled OFF after 3 minutes at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule OFF task", e)
        }
    }
}

