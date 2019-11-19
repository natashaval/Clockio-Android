package com.natasha.clockio.base.di.application;

import android.app.Application;
import android.util.Log;
import com.natasha.clockio.base.di.component.ApplicationComponent;
import com.natasha.clockio.base.di.component.DaggerApplicationComponent;
import com.natasha.clockio.base.di.constant.UrlConstantKt;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;

import javax.inject.Inject;

public class MyApplication extends Application implements HasAndroidInjector {

    private static final String TAG = MyApplication.class.getSimpleName();
    ApplicationComponent applicationComponent;

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationComponent = DaggerApplicationComponent.builder()
                .contextModule(this)
                .retrofitModule(UrlConstantKt.BASE_URL)
                .build();

        applicationComponent.injectApplication(this);
        Log.d(TAG, "application component init on create");
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }
}