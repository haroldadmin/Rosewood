package com.haroldadmin.kshitijchauhan.rosewood.utils

import android.os.Handler
import android.os.Looper
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object AppExecutors {
	val workExecutor: Executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)

	val mainThreadExecutor : Executor = object : Executor {
		private val mainThreadHandler = Handler(Looper.getMainLooper())
		override fun execute(task: Runnable?) {
			mainThreadHandler.post(task)
		}
	}

	val workScheduler: Scheduler = Schedulers.from(workExecutor)
	val computationDispatcher = workExecutor.asCoroutineDispatcher()
}