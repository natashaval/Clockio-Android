package com.natasha.clockio.login.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.annotation.StringRes
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.natasha.clockio.R
import com.natasha.clockio.base.model.BaseResponse
import com.natasha.clockio.base.model.LoggedInUser
import com.natasha.clockio.base.util.RetrofitInterceptor
import com.natasha.clockio.home.ui.HomeActivity
import dagger.android.AndroidInjection
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject

class LoginActivity : DaggerAppCompatActivity() {
  private val TAG: String = LoginActivity::class.java.simpleName

  @Inject lateinit var factory: ViewModelProvider.Factory
  @Inject lateinit var interceptor: RetrofitInterceptor
  @Inject lateinit var sharedPref: SharedPreferences
  private lateinit var loginViewModel: LoginViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_login)
    AndroidInjection.inject(this)

    interceptor.setBasic(getString(R.string.client_id), getString(R.string.client_secret))
    loginViewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)


    loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
      val loginState = it ?: return@Observer

      // disable login button unless both username / password is valid
      login.isEnabled = loginState.isDataValid

      if (loginState.usernameError != null) {
        username.error = getString(loginState.usernameError)
      }
      if (loginState.passwordError != null) {
        password.error = getString(loginState.passwordError)
      }
    })


    loginViewModel.loginResult.observe(this@LoginActivity, Observer {
      val loginResult = it ?: return@Observer

      loading.visibility = View.GONE

      Log.d(TAG, "login is called from login result $loginResult")
      /*when(loginResult.status) {
          BaseResponse.Status.SUCCESS -> {
              Log.d(TAG, "msg: ${loginResult.message} data: ${loginResult.data}")
          }
          BaseResponse.Status.LOADING -> showLoading()
          BaseResponse.Status.ERROR -> {
              showError(loginResult.message!!)
          }
      }*/
      Log.d(TAG, "msg: ${loginResult.message} data: ${loginResult.data}")
      interceptor.setToken(loginResult.data!!.accessToken)
      setResult(Activity.RESULT_OK)

      loginViewModel.loadProfile()

      //Complete and destroy login activity once successful
      finish()
      val intent = Intent(this, HomeActivity::class.java)
      startActivity(intent)
    })

    loginViewModel.loginFailed.observe(this@LoginActivity, Observer {
      loading.visibility = View.GONE
      Log.e(TAG, "Login Failed: $it")
      showLoginFailed(R.string.login_failed)
    })

    loginViewModel.profile.observe(this, Observer { result ->
      when(result.status) {
        BaseResponse.Status.LOADING -> loading.visibility = View.VISIBLE
        BaseResponse.Status.SUCCESS -> {
          loading.visibility = View.GONE
          if (result.success) {
            Log.d(TAG, "profile Success ${result.data}")
            val loggedInUser: LoggedInUser = result.data as LoggedInUser
            val editor = sharedPref.edit()
            editor.putString("id", loggedInUser.employeeId)
            Log.d(TAG, "employee-id ${loggedInUser.employeeId}")
            editor.apply()
          }
          else {
            val errorBody = result.data as ResponseBody
//            Log.d(TAG, "profile Failed ${errorBody.string()}")
            Toast.makeText(this, errorBody.string(), Toast.LENGTH_SHORT).show()
          }
        }
        BaseResponse.Status.ERROR -> {
          Log.d(TAG, "profile Error ${result.message}")
          Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        }
      }
    })

    username.afterTextChanged {
      loginViewModel.loginDataChanged(
          username.text.toString(),
          password.text.toString()
      )
    }

    password.apply {
      afterTextChanged {
        loginViewModel.loginDataChanged(
            username.text.toString(),
            password.text.toString()
        )
      }
    }

    login.setOnClickListener {
      loading.visibility = View.VISIBLE
      Log.d(TAG, "login is called from activity")
      loginViewModel.login(username.text.toString(), password.text.toString())
    }
  }

  private fun updateUiWithUser(model: LoggedInUserView) {
    val welcome = getString(R.string.welcome)
    val displayName = model.displayName
    // TODO : initiate successful logged in experience
    Toast.makeText(
        applicationContext,
        "$welcome $displayName",
        Toast.LENGTH_LONG
    ).show()
  }

  private fun showLoginFailed(@StringRes errorString: Int) {
    Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
  }

  private fun showLoading() {
    loading.visibility = View.VISIBLE
    Log.d(TAG, "login return response loading")
  }
  private fun showError(message: String?) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    Log.d(TAG, "login return response error")
  }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
  this.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(editable: Editable?) {
      afterTextChanged.invoke(editable.toString())
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
  })
}
