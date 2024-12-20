package com.natasha.clockio.activity.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.natasha.clockio.location.ui.MapsFragment

import com.natasha.clockio.R
import com.natasha.clockio.activity.entity.ActivityCreateRequest
import com.natasha.clockio.base.constant.ParcelableConst
import com.natasha.clockio.base.constant.PreferenceConst
import com.natasha.clockio.base.ui.TimePickerFragment
import com.natasha.clockio.base.util.observeOnce
import com.natasha.clockio.home.ui.HomeActivity
import com.natasha.clockio.home.ui.fragment.OnViewOpenedInterface
import com.natasha.clockio.activity.viewmodel.ActivityViewModel
import com.natasha.clockio.base.util.ResponseUtils
import com.natasha.clockio.location.entity.LocationModel
import com.natasha.clockio.location.LocationViewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_activity_add.*
import java.util.*
import javax.inject.Inject

class ActivityAddFragment : Fragment() {

  companion object {
    fun newInstance() = ActivityAddFragment()
    val TAG: String = ActivityAddFragment::class.java.simpleName
  }

  @Inject lateinit var sharedPref: SharedPreferences
  @Inject lateinit var factory: ViewModelProvider.Factory
  private lateinit var activityViewModel: ActivityViewModel
  private lateinit var locationViewModel: LocationViewModel
  private var tp: TimePickerDialog? = null
  var location: LocationModel =
      LocationModel(0.0, 0.0)

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    val act = activity as HomeActivity
    act.supportActionBar?.setTitle(R.string.activity_create_title)
    return inflater.inflate(R.layout.fragment_activity_add, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    activityViewModel = ViewModelProvider(this, factory).get(ActivityViewModel::class.java)
    locationViewModel = ViewModelProvider(this, factory).get(LocationViewModel::class.java)
    setHasOptionsMenu(true)

    setTime()
    observeCreateActivityResult()
    observeLocation()
  }

  override fun onAttach(context: Context) {
    AndroidSupportInjection.inject(this)
    super.onAttach(context)
  }

  override fun onStart() {
    super.onStart()
    val i: OnViewOpenedInterface = activity as OnViewOpenedInterface
    i.onOpen()
  }

  override fun onStop() {
    super.onStop()
    val i: OnViewOpenedInterface = activity as OnViewOpenedInterface
    i.onClose()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.save, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when(item.itemId) {
      R.id.action_save -> {
        Log.d(TAG, "activity saved button clicked!")
        createActivity()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setTime() {
    activityStartInput.setOnClickListener {
      Log.d(TAG, "activity start time clicked!")
      if (tp == null) {
        val ft = fragmentManager!!.beginTransaction()
        val startTime: DialogFragment = TimePickerFragment(
            activityStartInput
        )
        startTime.show(ft, "TimePicker")
      }
    }

    activityEndInput.setOnClickListener {
      Log.d(TAG, "activity end time clicked!")
      if (tp == null) {
        val ft = fragmentManager!!.beginTransaction()
        val endTime: DialogFragment = TimePickerFragment(
            activityEndInput
        )
        endTime.show(ft, "TimePicker")
      }
    }
  }

  private fun createActivity() {
    var employeeId = sharedPref.getString(PreferenceConst.EMPLOYEE_ID_KEY, "")
    val title = activityTitleInput.text.toString()
    val content = activityContentInput.text.toString()
    val startTime = activityStartInput.text.toString()
    val endTime = activityEndInput.text.toString()
    val request = ActivityCreateRequest(title, content, Date(), startTime, endTime, location.latitude, location.longitude)
    Log.d(TAG, "create Activity $request")
    activityViewModel.createActivity(employeeId!!, request)
  }

  private fun observeCreateActivityResult() {
    activityViewModel.activityResult.observe(this, androidx.lifecycle.Observer {
      ResponseUtils.showResponseAlert(activity!!, it)
    })
  }

  private fun setMap(loc: LocationModel) {
    Log.d(TAG, "location setMap $loc")
    val frag = MapsFragment.newInstance()
    val bundle = Bundle().apply {
      putParcelable(ParcelableConst.LOCATION, loc)
      putBoolean(ParcelableConst.LOCATION_SAVE, true)
    }
    frag.arguments = bundle
    Log.d(TAG, "addFragment setMap")
    fragmentManager?.beginTransaction()
        ?.replace(R.id.activityMapInput, frag, TAG)
        ?.commit()
  }

  private fun observeLocation() {
    locationViewModel.getLocationData().observeOnce(this, androidx.lifecycle.Observer {
      location = it
      Log.d(TAG, "location observe $location")
      setMap(location)
    })
  }
}
