package com.haroldadmin.kshitijchauhan.rosewood.repository.usage_stats

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import com.haroldadmin.kshitijchauhan.rosewood.model.AppUsage
import com.haroldadmin.kshitijchauhan.rosewood.utils.toAppUsageItemsList
import com.haroldadmin.kshitijchauhan.rosewood.utils.toEventsList
import io.reactivex.Observable
import io.reactivex.Single

class UsageStatsRepository() {

	private val TAG = this::class.java.simpleName

	fun getAppUsages(usageStatsManager: UsageStatsManager, startTime: Long, endTime: Long, packageManager : PackageManager): Single<List<AppUsage>> {
		return Observable.fromCallable { usageStatsManager.queryEvents(startTime, endTime) }
				.flatMap { usageEvents -> Observable.fromIterable(usageEvents.toEventsList()) }
				.filter { usageEvent -> packageManager.getLaunchIntentForPackage(usageEvent.packageName) != null && (usageEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND || usageEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) }
				.toList()
				.map { listOfEvents -> listOfEvents.toAppUsageItemsList(packageManager) }
	}

}