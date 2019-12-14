package com.natasha.clockio.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.natasha.clockio.base.model.BaseResponse
import com.natasha.clockio.home.repository.EmployeeRepository
import com.natasha.clockio.home.repository.ProfileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ActivityViewModel @Inject constructor(private val employeeRepository: EmployeeRepository) : ViewModel() {
  private val TAG: String = ActivityViewModel::class.java.simpleName

  private val _employee = MutableLiveData<BaseResponse<Any>>()
  val employee: LiveData<BaseResponse<Any>>
  get() = _employee

  fun getEmployee(id: String) {
    viewModelScope.launch {
      _employee.value = BaseResponse.loading(null)
      val response = employeeRepository.getEmployee(id)
      Log.d(TAG, "employee get $response")
      _employee.value = response
    }
  }

}
