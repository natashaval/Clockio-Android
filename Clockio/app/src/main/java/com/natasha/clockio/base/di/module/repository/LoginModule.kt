package com.natasha.clockio.base.di.module.repository

import com.natasha.clockio.base.di.scope.ActivityScope
import com.natasha.clockio.login.data.LoginRepository
import com.natasha.clockio.login.service.AuthApi
import dagger.Module
import dagger.Provides

@Module
class LoginModule {

    @Provides
    @ActivityScope
    fun providesLoginRepository(authApi: AuthApi): LoginRepository {
        return LoginRepository(authApi)
    }
}