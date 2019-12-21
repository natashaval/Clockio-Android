package com.natasha.clockio.home.service

import com.natasha.clockio.activity.entity.ActivityCreateRequest
import com.natasha.clockio.base.model.DataResponse
import com.natasha.clockio.home.entity.Activity
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface ActivityApi {
    @GET("/api/activity/employee/{id}/today")
    suspend fun getActivityToday(@Path("id") id: String, @Query("date") date: String)
    : Response<List<Activity>>

    @POST("/api/activity/employee/{id}")
    suspend fun createActivity(@Path("id") id: String, @Body request: ActivityCreateRequest)
    : Response<DataResponse>
}