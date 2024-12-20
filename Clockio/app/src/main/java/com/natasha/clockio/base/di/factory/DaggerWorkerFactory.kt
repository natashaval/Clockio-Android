package com.natasha.clockio.base.di.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.natasha.clockio.location.worker.ChildWorkerFactory
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Provider

//https://medium.com/@neonankiti/how-to-use-dagger2-withworkmanager-bae3a5fb7dd3
class DaggerWorkerFactory @Inject constructor(
    private val workerFactories: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<ChildWorkerFactory>>
): WorkerFactory() {
  override fun createWorker(appContext: Context, workerClassName: String,
      workerParameters: WorkerParameters): ListenableWorker? {
    val foundEntry = workerFactories.entries.find {
      Class.forName(workerClassName).isAssignableFrom(it.key) }
    val factoryProvider = foundEntry?.value
                          ?: throw IllegalArgumentException("unknown worker class name: $workerClassName")
    return factoryProvider.get().create(appContext, workerParameters)
  }

}