package com.natasha.clockio.home.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.natasha.clockio.base.di.executor.AppExecutors
import com.natasha.clockio.base.model.ApiResponse
import com.natasha.clockio.base.model.BaseResponse
import com.natasha.clockio.base.util.NetworkBoundResource
import com.natasha.clockio.home.dao.EmployeeDao
import com.natasha.clockio.home.entity.Employee
import com.natasha.clockio.home.service.EmployeeApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val employeeDao: EmployeeDao,
    private val employeeApi: EmployeeApi
) {
  //class ProfileRepository @Inject constructor(private val database: AppDatabase){
  private val TAG: String = ProfileRepository::class.java.simpleName
  //  private var employeeDao: EmployeeDao = database.employeeDao()

  fun getEmployeeById(id: String) : LiveData<BaseResponse<Employee>> {
    return object : NetworkBoundResource<Employee, Employee>(appExecutors) {
      override fun loadFromDb(): LiveData<Employee> {
        val result = employeeDao.getById(id)
        Log.d(TAG, "load From DB ${result.value}")
        //        return employeeDao.getById(id)
        return result
      }

      override fun shouldFetch(data: Employee?) = data == null

      override fun createCall(): LiveData<ApiResponse<Employee>> {
        Log.d(TAG, "create Call")
        return employeeApi.getEmployeeById(id)
      }

      override fun saveCallResult(item: Employee) {
        Log.d(TAG, "Employee save Call Result")
        employeeDao.insert(item)
      }
    }.asLiveData()
  }

  fun updateEmployee(id: String) {
    employeeApi.getEmployee(id).enqueue(object: Callback<Employee> {
      override fun onFailure(call: Call<Employee>, t: Throwable) {
        Log.e(TAG, "Employee update Error ${t.message}")
        t.printStackTrace()
      }

      override fun onResponse(call: Call<Employee>, response: Response<Employee>) {
        if (response.isSuccessful) {
          Log.d(TAG, "update Employee SUCCESS")
          response.body()?.let {
            appExecutors.diskIO().execute {
              employeeDao.update(it)
            }
          }
        } else {
          Log.d(TAG, "update Employee Failed ${response.errorBody()}")
        }
      }

    })
  }

}