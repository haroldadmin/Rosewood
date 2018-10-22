package com.haroldadmin.kshitijchauhan.rosewood.utils

import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.haroldadmin.kshitijchauhan.rosewood.R
import com.haroldadmin.kshitijchauhan.rosewood.model.AppUsage
import com.haroldadmin.kshitijchauhan.rosewood.model.PhysicalActivity
import com.haroldadmin.kshitijchauhan.rosewood.model.TimelineItem
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun UsageEvents.toEventsList(): List<UsageEvents.Event> {
	val list = mutableListOf<UsageEvents.Event>()
	while (this.hasNextEvent()) {
		val event = UsageEvents.Event()
		this.getNextEvent(event)
		list.add(event)
	}
	return list
}

fun List<UsageEvents.Event>.toAppUsageItemsList(packageManager: PackageManager): List<AppUsage> {
	val eventItemsList = mutableListOf<AppUsage>()
	val map = this.groupBy { it.packageName }
	for ((packageName, listOfEvents) in map) {
		listOfEvents
				.asSequence()
				.windowed(2, 2, false) {
					Pair(it[0], it[1])
				}
				.mapTo(eventItemsList) {
					AppUsage(packageName.toAppLabel(packageManager), packageManager.getApplicationIcon(packageName), it.first.timeStamp, it.second.timeStamp)
				}
	}
	return eventItemsList
}

fun List<AppUsage>.toTimelineItemsList(): List<TimelineItem> {
	return this.map { it as TimelineItem }
}


fun Long.toTime(): String {
	val formatter = SimpleDateFormat("HH:mm:ss a")
	return formatter.format(Date(this))
}

fun String.toAppLabel(packageManager: PackageManager): String {
	// The string we're working on here is assume to be the package name
	val appInfo: ApplicationInfo? = try {
		packageManager.getApplicationInfo(this, PackageManager.GET_META_DATA)
	} catch (e: PackageManager.NameNotFoundException) {
		null
	}
	return appInfo?.let {
		packageManager.getApplicationLabel(it) as String
	} ?: "Unknown"
}

fun HistoryClient.getDataSetsObservable(dataReadRequest: DataReadRequest): Observable<DataSet> {
	var list = mutableListOf<DataSet>()
	this.readData(dataReadRequest)
			.addOnSuccessListener { result ->
				list = result?.dataSets ?: emptyList<DataSet>() as MutableList<DataSet>
			}
	return Observable.fromIterable(list)

}

fun DataPoint.toPhysicalActivityItem(context: Context): PhysicalActivity {
	val activityName = this.getValue(Field.FIELD_ACTIVITY).asActivity().capitalize()
	val icon = when (activityName.toLowerCase()) {
		"running" -> ContextCompat.getDrawable(context, R.drawable.ic_baseline_directions_run_24px)!!
		"walking" -> ContextCompat.getDrawable(context, R.drawable.ic_baseline_directions_walk_24px)!!
		"in_vehicle" -> ContextCompat.getDrawable(context, R.drawable.ic_baseline_directions_car_24px)!!
		else -> ContextCompat.getDrawable(context, R.drawable.ic_baseline_fitness_center_24px)!!
	}
	val startTime = this.getStartTime(TimeUnit.MILLISECONDS)
	val endTime = this.getEndTime(TimeUnit.MILLISECONDS)
	return PhysicalActivity(activityName, icon, startTime, endTime)
}

fun Disposable.addToCompositeDisposable(compositeDisposable: CompositeDisposable) {
	compositeDisposable.add(this)
}