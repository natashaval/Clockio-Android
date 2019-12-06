package com.natasha.clockio.presence.ui.fragment

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.google.gson.Gson

import com.natasha.clockio.R
import com.natasha.clockio.base.model.BaseResponse
import com.natasha.clockio.base.model.DataResponse
import com.natasha.clockio.location.LocationModel
import com.natasha.clockio.location.LocationViewModel
import com.natasha.clockio.presence.service.request.CheckinRequest
import com.natasha.clockio.presence.viewModel.ImageViewModel
import com.natasha.clockio.presence.viewModel.PresenceViewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_image.*
import okhttp3.ResponseBody
import java.io.File
import java.util.*
import javax.inject.Inject

class ImageFragment : Fragment() {

  companion object {
    fun newInstance() = ImageFragment()
    private val TAG = ImageFragment::class.java.simpleName
  }

  @Inject lateinit var sharedPref: SharedPreferences
//  @Inject lateinit var gson: Gson
  @Inject lateinit var factory: ViewModelProvider.Factory
  private lateinit var imageViewModel: ImageViewModel
  private lateinit var locationViewModel: LocationViewModel
  private lateinit var presenceViewModel: PresenceViewModel
  private lateinit var imagePath: String
  private var employeeId: String? = null

  override fun onAttach(context: Context) {
    AndroidSupportInjection.inject(this)
    super.onAttach(context)
  }

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_image, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    imageViewModel = ViewModelProvider(this, factory).get(ImageViewModel::class.java)
    locationViewModel = ViewModelProvider(this, factory).get(LocationViewModel::class.java)
    presenceViewModel = ViewModelProvider(this, factory).get(PresenceViewModel::class.java)
    observeProgress()
    observeEvents()
    observeCheckInResult()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    employeeId = sharedPref.getString("id", null)
    imagePath = arguments?.getString("imagePath", "").toString()
    Log.d(TAG, imagePath)
    Glide.with(this)
        .load(Uri.fromFile(File(imagePath)))
        .into(imageResult)

    closeImage()
    sendImage(imagePath)
  }

  //    https://stackoverflow.com/questions/10863572/programmatically-go-back-to-the-previous-fragment-in-the-backstack
  private fun closeImage() {
    imageClose.setOnClickListener {
      activity!!.finish()
    }
  }

  //    https://cloudinary.com/documentation/android_image_and_video_upload
  //    https://cloudinary.com/documentation/image_upload_api_reference
  private fun sendImage(imagePath: String) {
    imageCheck.setOnClickListener {
      imageViewModel.uploadImage(employeeId!!, imagePath)
    }
  }

  private fun observeProgress(){
    imageViewModel.progressValue.observe(this, androidx.lifecycle.Observer {
      var prog = (it * 100).toInt()
//      Log.d(TAG, "progressImage $prog")
      progressImage.progress = prog
    })
  }

  private fun observeEvents(){
    imageViewModel.events.observe(this, androidx.lifecycle.Observer {
      when(it) {
        ImageViewModel.STATUS_CODE_STARTING -> {
          imageCheck.visibility = View.INVISIBLE
          imageClose.visibility = View.INVISIBLE
          Toast.makeText(activity, descriptionFromCode(it), Toast.LENGTH_SHORT).show()
        }
        ImageViewModel.STATUS_CODE_PROGRESS -> {
          progressImage.visibility = View.VISIBLE
        }
        ImageViewModel.STATUS_CODE_UPLOAD_ERROR-> {
          imageCheck.visibility = View.VISIBLE
          imageClose.visibility = View.VISIBLE
          progressImage.visibility = View.INVISIBLE
          Toast.makeText(activity, descriptionFromCode(it), Toast.LENGTH_SHORT).show()
        }
        ImageViewModel.STATUS_CODE_FINISHED -> {
          imageCheck.visibility = View.VISIBLE
          imageClose.visibility = View.VISIBLE
          progressImage.visibility = View.INVISIBLE
          Toast.makeText(activity, descriptionFromCode(it), Toast.LENGTH_SHORT).show()

          doCheckIn()
        }
      }
    })
  }

  private fun descriptionFromCode(status: Int): String {
    var resId: Int = R.string.cloudinary_default
    when(status) {
      ImageViewModel.STATUS_CODE_STARTING ->
        resId = R.string.cloudinary_start
      ImageViewModel.STATUS_CODE_UPLOAD_ERROR ->
        resId = R.string.cloudinary_error
      ImageViewModel.STATUS_CODE_FINISHED ->
        resId = R.string.cloudinary_success
    }
    return getString(resId)
  }

  private fun doCheckIn() {
    var location: LocationModel = LocationModel(0.0, 0.0)
    locationViewModel.getLocationData().observe(this, androidx.lifecycle.Observer {
      location.latitude = it.latitude
      location.longitude = it.longitude
      Log.d(TAG, "presence $location")
    })
    imageViewModel.resultUrl.observe(this, androidx.lifecycle.Observer { url ->
      url?.let {
        Log.d(TAG, "url is exists $url")
        var request = CheckinRequest(employeeId!!, url, Date(), location.latitude, location.longitude)
        presenceViewModel.sendCheckIn(employeeId!!, request)
      }
    })
  }

  private fun observeCheckInResult() {
    presenceViewModel.presenceResult.observe(this, androidx.lifecycle.Observer {
      Log.d(TAG, "checkin in ImageFragment ${it.data}")
      when(it.status) {
        BaseResponse.Status.LOADING -> {
          SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE)
            .setTitleText("Loading ...");
        }
        BaseResponse.Status.SUCCESS -> {
          it.data?.let { result ->
            var presenceSucces = result as DataResponse
            Log.d(TAG, "checkin success $presenceSucces")
            SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
              .setTitleText("Check In!")
              .setContentText(presenceSucces.message)
              .show()
          }
        }
        BaseResponse.Status.FAILED -> {
          it.data?.let { result ->
            var presenceFailed = result as DataResponse
            Log.d(TAG, "checkin failed $presenceFailed")
            SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
              .setTitleText("Failed!")
              .setContentText(presenceFailed.message)
              .show()
          }
        }
        BaseResponse.Status.ERROR -> {
          SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Error!")
            .setContentText(it.data.toString())
            .show()
        }
      }
    })
  }
}
