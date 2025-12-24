package com.example.iotapp.ui.dashboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.PreferenceHelper
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentPumperBinding
import com.example.iotapp.model.PlantInformation
import com.example.iotapp.receiver.ScheduleReceiver
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PumperFragment : BaseFragment<FragmentPumperBinding>(FragmentPumperBinding::inflate) {

    private val sensorRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("sensor")
    }
    private var sensorListener: ValueEventListener? = null
    private var isPumpOn = false
    private var cachedTempC: Double? = null
    private var tempUnit: String = "C"

    override fun FragmentPumperBinding.initView() {
        tempUnit = PreferenceHelper.getTempUnit(requireContext())
        startSensorListener()
    }

    override fun FragmentPumperBinding.initListener() {
        bgButton.setSingleClick { togglePump() }
        tvPumpNow.setSingleClick {
            Log.d(TAG, "Pump now clicked on Pumper tab")
            setRelayState(true)
        }
        ivAdjustAlarm.setSingleClick { toggleDialog(true) }
        icCloseDialog.setOnClickListener { toggleDialog(false) }
        tvSave.setSingleClick { handleSaveSchedule() }
        icAdjustDialog.setSingleClick {
            Log.d(TAG, "icAdjustDialog clicked, focus on etDate")
            binding.etDate.requestFocus()
            showKeyboard(binding.etDate)
        }
        icAdjustDialog2.setSingleClick {
            Log.d(TAG, "icAdjustDialog2 clicked, focus on etTime")
            binding.etTime.requestFocus()
            showKeyboard(binding.etTime)
        }
    }

    override fun initObserver() = Unit

    override fun onResume() {
        super.onResume()
        val newTempUnit = PreferenceHelper.getTempUnit(requireContext())
        if (newTempUnit != tempUnit) {
            tempUnit = newTempUnit
            binding.tvTemperatureReal.text = formatTemperature(cachedTempC)
        }
    }

    override fun onDestroyView() {
        sensorListener?.let { sensorRef.removeEventListener(it) }
        super.onDestroyView()
    }

    private fun startSensorListener() {
        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val humidity = snapshot.child("humidity/value").getValue(Double::class.java)
                val rainStatus = snapshot.child("rain_status/value").getValue(String::class.java)
                val tempValue = snapshot.child("temperature/value").getValue(Double::class.java)
                val relay = snapshot.child("relay/value").getValue(String::class.java)
                val scheduleDate = snapshot.child("relay/schedule/date").getValue(String::class.java)
                val scheduleTime = snapshot.child("relay/schedule/time").getValue(String::class.java)

                cachedTempC = tempValue
                isPumpOn = relay.equals("ON", true)
                updateUi(
                    humidity.toString(),
                    rainStatus,
                    isPumpOn,
                    scheduleDate,
                    scheduleTime
                )
                val info = PlantInformation(
                    temperature = (tempValue ?: "--".toString()).toString(),
                    humidity = (humidity ?: "--").toString(),
                    rainStatus = rainStatus ?: "--",
                    connectStatus = if (isPumpOn) getString(R.string.online) else getString(R.string.offline_label),
                    schedule = "$scheduleDate $scheduleTime"
                )
                mainViewModel.fireBaseInformation.value = info
                Log.d(TAG, "Pumper data -> humidity=$humidity rain=$rainStatus temp=$tempValue relay=$relay date=$scheduleDate time=$scheduleTime")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Pumper listener cancelled: ${error.message}")
                Toast.makeText(requireContext(), "Load data failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        sensorListener?.let { sensorRef.addValueEventListener(it) }
    }

    private fun updateUi(
        humidity: String?,
        rainStatus: String?,
        pumpOn: Boolean,
        scheduleDate: String?,
        scheduleTime: String?
    ) {
        val onColor = R.color.success_green
        val offColor = R.color.light_gray

        binding.bgButton.backgroundTintList =
            ContextCompat.getColorStateList(
                requireContext(),
                if (pumpOn) onColor else offColor
            )

        togglePumpUI(pumpOn)

        binding.tvPumperSwitch.text = createSwitchText(pumpOn)
        binding.tvOutsideMoistureReal.text = humidity?.let { "${it}%" } ?: "--"
        binding.tvRainStatusReal.text = rainStatus ?: "--"
        binding.tvTemperatureReal.text = formatTemperature(cachedTempC)
        binding.tvOnlineReal.text = if (pumpOn) getString(R.string.online) else getString(R.string.offline_label)

        binding.tvAlarmDate.text = if (!scheduleDate.isNullOrBlank()) "${getString(R.string.date)} $scheduleDate" else getString(R.string.no_schedule)
        binding.tvHour.text = if (!scheduleTime.isNullOrBlank()) "${getString(R.string.time)} $scheduleTime" else ""
        if (!scheduleDate.isNullOrBlank()) binding.etDate.setText(scheduleDate)
        if (!scheduleTime.isNullOrBlank()) binding.etTime.setText(scheduleTime)
    }

    private fun togglePumpUI(pumpOn: Boolean) {
        val constraintLayout = binding.root as ConstraintLayout
        val set = ConstraintSet()
        set.clone(constraintLayout)

        val marginStart = 16
        val marginEnd = 16

        if (pumpOn) {
            set.clear(R.id.knob, ConstraintSet.START)
            set.connect(R.id.knob, ConstraintSet.END, R.id.bgButton, ConstraintSet.END, marginEnd)
        } else {
            set.clear(R.id.knob, ConstraintSet.END)
            set.connect(R.id.knob, ConstraintSet.START, R.id.bgButton, ConstraintSet.START, marginStart)
        }

        val transition = AutoTransition().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
        }

        TransitionManager.beginDelayedTransition(constraintLayout, transition)
        set.applyTo(constraintLayout)
    }



    private fun togglePump() {
        val nextState = !isPumpOn
        Log.d(TAG, "Toggle pump -> $nextState")
        setRelayState(nextState)
    }

    private fun setRelayState(isOn: Boolean) {
        val update = mapOf(
            "value" to if (isOn) "ON" else "OFF",
            "timestamp" to ServerValue.TIMESTAMP
        )
        sensorRef.child("relay").updateChildren(update).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Relay state updated -> ${update["value"]}")
            } else {
                Log.e(TAG, "Failed to update relay", task.exception)
                Toast.makeText(requireContext(), "Update relay failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleDialog(show: Boolean) {
        binding.dialogAdjust.visibility = if (show) View.VISIBLE else View.GONE
        hideKeyboard()
    }

    private fun handleSaveSchedule() {
        hideKeyboard()
        
        val date = binding.etDate.text?.toString()?.trim().orEmpty()
        val time = binding.etTime.text?.toString()?.trim().orEmpty()
        val isDateValid = validateDate(date)
        val isTimeValid = validateTime(time)

        markValidation(binding.etDate, isDateValid)
        markValidation(binding.etTime, isTimeValid)

        if (!isDateValid || !isTimeValid) {
            Toast.makeText(requireContext(), getString(R.string.invalid_time_message), Toast.LENGTH_SHORT).show()
            return
        }

        val isPastTime = isTimeInPast(date, time)
        if (isPastTime) {
            Log.d(TAG, "Schedule time is in the past: date=$date time=$time")
            markValidation(binding.etDate, false)
            markValidation(binding.etTime, false)
            binding.etDate.error = getString(R.string.past_time_error)
            binding.etTime.error = getString(R.string.past_time_error)
            Toast.makeText(requireContext(), getString(R.string.past_time_error), Toast.LENGTH_SHORT).show()
            return
        }

        val scheduledTimeMillis = getScheduledTimeMillis(date, time)
        if (scheduledTimeMillis == null) {
            Log.e(TAG, "Failed to parse scheduled time: date=$date time=$time")
            Toast.makeText(requireContext(), "Invalid time format", Toast.LENGTH_SHORT).show()
            return
        }

        val scheduleId = scheduledTimeMillis.toInt()
        scheduleAlarm(scheduledTimeMillis, scheduleId)

        val updates = mapOf(
            "schedule/date" to date,
            "schedule/time" to time,
            "schedule/timestamp" to ServerValue.TIMESTAMP
        )
        sensorRef.child("relay").updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Schedule saved date=$date time=$time, scheduled for ${java.util.Date(scheduledTimeMillis)}")
                binding.tvAlarmDate.text = "${getString(R.string.date)} $date"
                binding.tvHour.text = "${getString(R.string.time)} $time"
                toggleDialog(false)
            } else {
                Log.e(TAG, "Save schedule failed", task.exception)
                Toast.makeText(requireContext(), "Save schedule failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = view?.findFocus()
        if (currentFocus != null) {
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } else {
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    private fun showKeyboard(editText: EditText) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun validateDate(date: String): Boolean {
        if (date.isBlank()) return false
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun validateTime(time: String): Boolean {
        if (time.isBlank()) return false
        val regex = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")
        return regex.matches(time)
    }

    private fun isTimeInPast(date: String, time: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateFormat.isLenient = false
            val scheduledDateTime = dateFormat.parse("$date $time")
            val currentDateTime = Calendar.getInstance().time
            
            scheduledDateTime?.before(currentDateTime) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing scheduled time: date=$date time=$time", e)
            false
        }
    }

    private fun getScheduledTimeMillis(date: String, time: String): Long? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateFormat.isLenient = false
            val scheduledDateTime = dateFormat.parse("$date $time")
            scheduledDateTime?.time
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing scheduled time: date=$date time=$time", e)
            null
        }
    }

    private fun scheduleAlarm(timeInMillis: Long, scheduleId: Int) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), ScheduleReceiver::class.java).apply {
                action = ScheduleReceiver.ACTION_SCHEDULE_ON
                putExtra(ScheduleReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                scheduleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Alarm scheduled for ${Date(timeInMillis)} with ID=$scheduleId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    private fun markValidation(editText: EditText, isValid: Boolean) {
        val color = ContextCompat.getColor(requireContext(), if (isValid) R.color.color_447661 else R.color.error_red)
        editText.setTextColor(color)
        editText.error = if (isValid) null else getString(R.string.invalid_time_hint)
    }

    private fun formatTemperature(tempC: Double?): String {
        tempC ?: return "--"
        val df = DecimalFormat("#.##")
        return if (tempUnit.equals("F", true)) {
            val f = tempC * 9 / 5 + 32
            "${df.format(f)} °F"
        } else {
            "${df.format(tempC)} °C"
        }
    }

    private fun createSwitchText(isOn: Boolean): SpannableString {
        val onText = getString(R.string.on)
        val offText = getString(R.string.off)
        val separator = " / "
        val fullText = "$onText$separator$offText"
        
        val spannable = SpannableString(fullText)
        val greenColor = ContextCompat.getColor(requireContext(), R.color.color_447661)
        val grayColor = ContextCompat.getColor(requireContext(), R.color.gray)
        
        val onStart = 0
        val onEnd = onText.length
        val offStart = onEnd + separator.length
        val offEnd = fullText.length
        
        if (isOn) {
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                onStart,
                onEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(grayColor),
                offStart,
                offEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            spannable.setSpan(
                ForegroundColorSpan(grayColor),
                onStart,
                onEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                offStart,
                offEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannable
    }
}
