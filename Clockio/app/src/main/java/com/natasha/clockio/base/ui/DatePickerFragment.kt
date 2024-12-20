package com.natasha.clockio.base.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import java.util.*

class DatePickerFragment constructor(editText: EditText): DialogFragment(), DatePickerDialog.OnDateSetListener {
    private val TAG: String = DatePickerFragment::class.java.simpleName
    private val myCalendar: Calendar = Calendar.getInstance()
    private val editText = editText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year = myCalendar.get(Calendar.YEAR)
        val month = myCalendar.get(Calendar.MONTH)
        val day = myCalendar.get(Calendar.DAY_OF_MONTH)
        Log.d(TAG, "calendar on focus change $day-$month-$year")
        return DatePickerDialog(activity!!, this, year, month, day)
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val yyyy = year.toString()
        val mm = String.format("%02d", month+1)
        val dd = String.format("%02d", dayOfMonth)
        this.editText.setText("$dd-$mm-$yyyy")
    }
}