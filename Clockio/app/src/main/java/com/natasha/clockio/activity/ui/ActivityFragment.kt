package com.natasha.clockio.activity.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.AdapterView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.natasha.clockio.R

import com.natasha.clockio.base.model.BaseResponse
import com.natasha.clockio.home.entity.Activity
import com.natasha.clockio.home.entity.Employee
import com.natasha.clockio.home.ui.HomeActivity
import com.natasha.clockio.home.ui.adapter.ActivityAdapter
import com.natasha.clockio.activity.viewmodel.ActivityViewModel
import com.natasha.clockio.base.constant.ParcelableConst
import com.natasha.clockio.base.constant.PreferenceConst
import com.natasha.clockio.base.constant.UserConst
import com.natasha.clockio.base.ui.setStatusIcon
import com.natasha.clockio.home.ui.adapter.StatusSpinnerAdapter
import com.natasha.clockio.home.viewmodel.EmployeeViewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_activity.*
import kotlinx.android.synthetic.main.item_activity_recyler_view.*
import kotlinx.android.synthetic.main.item_profile.*
import kotlinx.android.synthetic.main.item_status.*
import org.apache.commons.lang3.StringUtils
import java.text.SimpleDateFormat
import javax.inject.Inject

class ActivityFragment : Fragment() {

  companion object {
    @JvmStatic fun newInstance(employeeId: String) = ActivityFragment().apply {
      arguments = Bundle().apply {
        putString(PreferenceConst.EMPLOYEE_ID_KEY, employeeId)
      }
    }
    private val TAG: String = ActivityFragment::class.java.simpleName
  }

  @Inject lateinit var sharedPref: SharedPreferences
  @Inject lateinit var factory: ViewModelProvider.Factory
  private lateinit var employeeViewModel: EmployeeViewModel
  private lateinit var activityViewModel: ActivityViewModel
  private lateinit var statusArray: Array<String>
  private lateinit var statusIconArray: Array<Int>
  private var employeeId: String? = ""
  private var employeeStatus: String? = null
  private lateinit var adapter: StatusSpinnerAdapter

  private lateinit var activityAdapter: ActivityAdapter
  private lateinit var linearLayoutManager: LinearLayoutManager
  private var activityList = mutableListOf<Activity>()

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    val act = activity as HomeActivity
    statusArray = act!!.resources.getStringArray(R.array.status_array)
    statusIconArray = arrayOf(
        R.drawable.ic_status_online_24dp, R.drawable.ic_status_meeting_24dp,
        R.drawable.ic_status_away_24dp, R.drawable.ic_status_offline_24dp)
    act.supportActionBar?.setTitle(R.string.navigation_activity)

    return inflater.inflate(R.layout.fragment_activity, container, false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
//    https://stackoverflow.com/questions/55087234/error-lateinit-property-adapter-has-not-been-initialized-after-come-back-from-ac
    activityAdapter = ActivityAdapter(mutableListOf())
    arguments?.let {
      employeeId = it.getString(PreferenceConst.EMPLOYEE_ID_KEY)
    }
  }

  override fun onAttach(context: Context) {
    AndroidSupportInjection.inject(this)
    super.onAttach(context)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setHasOptionsMenu(true)
    employeeViewModel = ViewModelProvider(this, factory).get(EmployeeViewModel::class.java)
    activityViewModel = ViewModelProvider(this, factory).get(
        ActivityViewModel::class.java)

    getEmployee()
    getStatus()
    getActivityToday()

    setUpActivityAdapter(mutableListOf())

    observeEmployee()
    observeActivity()

    addActivityClick()
  }

  override fun onResume() {
    Log.d(TAG, "activity Fragment onResume")
    super.onResume()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.history, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when(item.itemId) {
      R.id.action_history -> {
        Log.d(TAG, "History icon clicked!")
        fragmentManager?.
            beginTransaction()?.
            replace(R.id.content, ActivityHistoryFragment.newInstance(employeeId!!))?.
            addToBackStack(null)?.
            commit()
        return true
      }
    }
    return false
  }

  private fun getStatus() {
    adapter = StatusSpinnerAdapter(context!!,
        R.layout.item_status, statusArray, statusIconArray)
    statusSpinner.adapter = adapter
    selectStatus(UserConst.STATUS_ONLINE)
    statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(p0: AdapterView<*>?) {
      }

      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val status: String = statusSpinner.selectedItem.toString()
//        Toast.makeText(activity!!, "Status selected: $status", Toast.LENGTH_SHORT).show()
//        Log.d(TAG, "onItemSelected tapi var employeeStatus $employeeStatus dibanding ${status.toLowerCase()}")
        if (employeeStatus!= null && !employeeStatus.equals(status.toLowerCase())) {
          Log.d(TAG, "status changed! $status")
          employeeViewModel.updateStatus(employeeId!!, status)
        }
      }
    }
  }

  fun getEmployee() {
    employeeViewModel.getEmployee(employeeId!!)
  }

  private fun getActivityToday() {
    activityViewModel.getActivityToday(employeeId!!)
  }

  private fun observeEmployee() {
    employeeViewModel.employee.observe(this, Observer {
      when(it.status) {
        BaseResponse.Status.LOADING -> {
          statusProgressBar.visibility = View.VISIBLE
        }
        BaseResponse.Status.SUCCESS -> {
          statusProgressBar.visibility = View.INVISIBLE
          val response = it.data as Employee
          employeeName.text = response.firstName + " " + response.lastName
          employeeDept.text = response.department.name
          val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")
          response.checkIn?.let {
            employeeLastCheckin.text = getString(R.string.employee_last_checkin, dateFormat.format(it))
          }
          response.status?.let { status ->
            employeeStatus = status.toLowerCase()
            // https://stackoverflow.com/questions/2390102/how-to-set-selected-item-of-spinner-by-value-not-by-position:
            Log.d(TAG, "BaseResponse status $employeeStatus")
            selectStatus(status)
          }
          Glide.with(this).load(response.profileUrl)
              .apply(RequestOptions.circleCropTransform()).into(employeeProfile)
        }
        BaseResponse.Status.ERROR, BaseResponse.Status.FAILED -> {
          statusProgressBar.visibility = View.INVISIBLE
        }
      }
    })
  }

  private fun observeActivity() {
    activityViewModel.activityToday.observe(this, Observer {
      when(it.status) {
        BaseResponse.Status.SUCCESS -> {
          var data = it.data as List<Activity>
          Log.d(TAG, "activity $data")
          activityList = data.toMutableList()
          activityAdapter.addAll(activityList)
          showActivity()
        }
      }
    })
  }

  private fun selectStatus(status: String) {
    var statusText = status.toLowerCase().capitalize()
    val statusPosition: Int = adapter.getPosition(statusText)
    statusSpinner.setSelection(statusPosition)
    statusTextView.text = statusText
    var iconPosition: Int = setStatusIcon(statusText)
    statusIcon.setImageResource(iconPosition)
  }

  private fun setUpActivityAdapter(activities: MutableList<Activity>) {
    activityAdapter = ActivityAdapter(activities)
    activityAdapter.setListener(object: ActivityAdapter.OnActivityClickListener {
      override fun onActivityClick(actvy: Activity) {
        val mFragment = ActivityDetailsFragment.newInstance()
        val mBundle = Bundle()
        mBundle.putParcelable(ParcelableConst.ITEM_ACTIVITY, actvy)
        mFragment.arguments = mBundle
        val act = activity as HomeActivity
        act.addFragmentBackstack(mFragment)
      }
    })
    linearLayoutManager = LinearLayoutManager(activity)
    activityRecyclerView.apply {
      layoutManager = linearLayoutManager
      adapter = activityAdapter
      addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    }
  }

  private fun showActivity() {
    if(activityList.isEmpty()) {
      activityNotAvailableLabel.visibility = View.VISIBLE
      activityRecyclerViewLayout.visibility = View.INVISIBLE
    } else {
      Log.d(TAG, "show Activity ${activityList.size}")
      activityNotAvailableLabel.visibility = View.INVISIBLE
      activityRecyclerViewLayout.visibility = View.VISIBLE
    }
  }

  private fun addActivityClick() {
    val userEmployeeId = sharedPref.getString(PreferenceConst.EMPLOYEE_ID_KEY, "") // to check if admin
    if (userEmployeeId != employeeId) {
      activityAddButton.visibility = View.INVISIBLE
      statusSpinner.visibility = View.INVISIBLE
      employeeStatusLayout.visibility = View.VISIBLE
    }
    activityAddButton.setOnClickListener {
      Log.d(TAG, "FAB activity clicked!")
      fragmentManager?.beginTransaction()?.
        replace(R.id.content, ActivityAddFragment.newInstance())?.
        addToBackStack(null)?.
        commit()
    }
  }

  interface OnEmployeeRefreshListener {
    fun onEmployeeRefresh()
  }

  internal var callback: OnEmployeeRefreshListener? = null
  fun setOnEmployeeRefreshListener(callback: OnEmployeeRefreshListener) {
    //    https://developer.android.com/training/basics/fragments/communicating.html
    this.callback = callback
  }
}