package com.haroldadmin.kshitijchauhan.rosewood.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.request.DataReadRequest
import com.haroldadmin.kshitijchauhan.rosewood.model.AppUsage
import com.haroldadmin.kshitijchauhan.rosewood.model.PhysicalActivity
import com.haroldadmin.kshitijchauhan.rosewood.repository.google_fit.GoogleFitRepository
import com.haroldadmin.kshitijchauhan.rosewood.repository.usage_stats.UsageStatsRepository
import io.reactivex.Single

class Repository {

	private val googleFitRepository = GoogleFitRepository()
	private val usageStatsRepository = UsageStatsRepository()

	fun getAppUsagesList(usageStatsManager : UsageStatsManager, startTime: Long, endTime: Long, packageManager: PackageManager): Single<List<AppUsage>> {
		return usageStatsRepository.getAppUsages(usageStatsManager, startTime, endTime, packageManager)
	}

	fun getPhysicalActivities(dataReadRequest: DataReadRequest, historyClient: HistoryClient, context: Context): Single<List<PhysicalActivity>> {
		return googleFitRepository.getPhysicalActivities(dataReadRequest, historyClient, context)
	}
}