package com.natasha.clockio.base.di.module.viewmodel

import androidx.lifecycle.ViewModel
import com.natasha.clockio.base.di.qualifier.ViewModelKey
import com.natasha.clockio.home.viewmodel.EmployeeViewModel
import com.natasha.clockio.home.viewmodel.ProfileViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ProfileViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(ProfileViewModel::class)
  abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(EmployeeViewModel::class)
  abstract fun bindEmployeeViewModel(viewModel: EmployeeViewModel): ViewModel
}