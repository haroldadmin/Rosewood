package com.haroldadmin.kshitijchauhan.rosewood.repository.google_fit

import android.content.Context
import android.util.Log
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import com.haroldadmin.kshitijchauhan.rosewood.model.PhysicalActivity
import com.haroldadmin.kshitijchauhan.rosewood.utils.toPhysicalActivityItem
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.ExecutionException

class GoogleFitRepository {

	private val TAG = this::class.java.simpleName

	fun getPhysicalActivities(dataReadRequest: DataReadRequest, historyClient: HistoryClient, context: Context): Single<List<PhysicalActivity>> {
		val task = historyClient.readData(dataReadRequest)

		return Observable.fromCallable { Tasks.await(task) }
				.switchMap { Observable.fromIterable(it.dataSets) }
				.filter { dataset -> !dataset.isEmpty }
				.flatMap { dataset -> Observable.fromIterable(dataset.dataPoints) }
				.map { datapoint -> datapoint.toPhysicalActivityItem(context) }
				.toList()
	}
}