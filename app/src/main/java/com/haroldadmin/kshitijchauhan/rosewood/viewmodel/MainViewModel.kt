package com.haroldadmin.kshitijchauhan.rosewood.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide.init
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.haroldadmin.kshitijchauhan.rosewood.model.AppUsage
import com.haroldadmin.kshitijchauhan.rosewood.model.CombinedLiveData
import com.haroldadmin.kshitijchauhan.rosewood.model.PhysicalActivity
import com.haroldadmin.kshitijchauhan.rosewood.model.TimelineItem
import com.haroldadmin.kshitijchauhan.rosewood.repository.Repository
import com.haroldadmin.kshitijchauhan.rosewood.utils.*
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

	private val TAG: String = this::class.java.simpleName
	private val compositeDisposable = CompositeDisposable()
	private val isPhysicalActivitiesListLoading = MutableLiveData<Boolean>()
	private val isAppUsageListLoading = MutableLiveData<Boolean>()
	private val _appUsageItems = MutableLiveData<List<AppUsage>>()
	private val _appUsageTime = MutableLiveData<Long>()
	private val _physicalActivityItems = MutableLiveData<List<PhysicalActivity>>()
	private val _physicalActivityTime = MutableLiveData<Long>()
	private val _totalEngagementTime: CombinedLiveData<Long, Long, Long>
	private var _isLoading: CombinedLiveData<Boolean, Boolean, Boolean>
	private var _combinedItemsList: CombinedLiveData<List<AppUsage>, List<PhysicalActivity>, List<TimelineItem>>
	private val _timelineItems = MediatorLiveData<List<TimelineItem>>()
	private val repository = Repository()

	val isLoading: LiveData<Boolean>
		get() = _isLoading

	val timelineItems: LiveData<List<TimelineItem>>
		get() = _timelineItems

	val physicalActivityTime: LiveData<Long>
		get() = _physicalActivityTime

	val appUsageTime: LiveData<Long>
		get() = _appUsageTime

	val totalEngagementTime: LiveData<Long>
		get() = _totalEngagementTime

	init {
		isPhysicalActivitiesListLoading.value = false
		isAppUsageListLoading.value = false
		_combinedItemsList = CombinedLiveData(_appUsageItems, _physicalActivityItems) { appUsageItems, physicalActivityItems ->
			println("List of physical activities: ${physicalActivityItems?.size}, List of appUsages: ${appUsageItems?.size}")
			val list = mutableListOf<TimelineItem>()
			list.addAll(appUsageItems ?: emptyList())
			list.addAll(physicalActivityItems ?: emptyList())
			list.sortByDescending { it.startTime }
			list
		}
		_isLoading = CombinedLiveData(isPhysicalActivitiesListLoading, isAppUsageListLoading) { physical, app ->
			val p = physical ?: true
			val a = app ?: true
			p || a
		}
		_timelineItems.addSource(_combinedItemsList) {
			_timelineItems.value = it.sortedByDescending { timelineItem ->
				timelineItem.startTime
			}
		}
		_totalEngagementTime = CombinedLiveData(_physicalActivityTime, _appUsageTime) { physicalTime, appTime ->
			val p = physicalTime ?: 0L
			val a = appTime ?: 0L
			p + a
		}
	}

	@SuppressLint("WrongConstant")
	private val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
		this.getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
	} else {
		this.getApplication<Application>().getSystemService("usagestats") as UsageStatsManager
	}

	fun loadData(context: Context, historyClient: HistoryClient) {
		val calendar = Calendar.getInstance()
		val endTime = calendar.timeInMillis
		calendar.add(Calendar.DAY_OF_YEAR, -1)
		val startTime = calendar.timeInMillis
		readFitnessData(context, historyClient, startTime, endTime)
		readAppUsageData(context.packageManager, startTime, endTime)
	}

	private fun readAppUsageData(packageManager: PackageManager, startTime: Long, endTime: Long) {
		repository.getAppUsagesList(usageStatsManager, startTime, endTime, packageManager)
				.subscribeOn(AppExecutors.workScheduler)
				.observeOn(AppExecutors.workScheduler)
				.doOnSubscribe {
					isAppUsageListLoading.postValue(true)
				}
				.doOnSuccess { list ->
					_appUsageItems.postValue(list)
					val time = list
							.asSequence()
							.map { appUsage -> appUsage._endTime - appUsage._startTime }
							.reduce { acc, l -> acc + l }
					_appUsageTime.postValue(time)
					isAppUsageListLoading.postValue(false)
				}
				.subscribe()
				.addToCompositeDisposable(compositeDisposable)
	}

	private fun readFitnessData(context: Context, historyClient: HistoryClient, startTime: Long, endTime: Long) {

		val dataReadRequest = DataReadRequest.Builder()
				.read(DataType.TYPE_ACTIVITY_SEGMENT)
				.setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
				.build()

		repository.getPhysicalActivities(dataReadRequest, historyClient, context)
				.subscribeOn(AppExecutors.workScheduler)
				.observeOn(AppExecutors.workScheduler)
				.doOnSubscribe {
					isPhysicalActivitiesListLoading.postValue(true)
				}
				.doOnSuccess { list ->
					_physicalActivityItems.postValue(list)
					val time = list
							.asSequence()
							.filter { physicalActivity -> physicalActivity.name.toLowerCase() != "still" }
							.map { physicalActivity ->
								physicalActivity._endTime - physicalActivity._startTime
							}
							.reduce { acc, l -> acc + l }
					_physicalActivityTime.postValue(time)
					isPhysicalActivitiesListLoading.postValue(false)
				}
				.subscribe { list ->
					_physicalActivityItems.postValue(list)
				}
				.addToCompositeDisposable(compositeDisposable)
	}

	override fun onCleared() {
		super.onCleared()
		compositeDisposable.dispose()
	}
}