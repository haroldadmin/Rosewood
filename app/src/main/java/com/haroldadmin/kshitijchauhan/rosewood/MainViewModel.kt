package com.haroldadmin.kshitijchauhan.rosewood

import android.annotation.SuppressLint
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide.init
import com.google.android.gms.fitness.HistoryClient
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

	private val TAG: String = this::class.java.simpleName
	private val compositeDisposable = CompositeDisposable()
	private val isPhysicalActivitiesListLoading = MutableLiveData<Boolean>()
	private val isAppUsageListLoading = MutableLiveData<Boolean>()
	private val _appUsageItems = MutableLiveData<List<AppUsageItem>>()
	private val _physicalActivityItems = MutableLiveData<List<PhysicalActivityItem>>()
	private var _isLoading : CombinedLiveData<Boolean, Boolean, Boolean>
	private var _combinedItemsList : CombinedLiveData<List<AppUsageItem>, List<PhysicalActivityItem>, List<TimelineItem>>
	private val _timelineItems = MediatorLiveData<List<TimelineItem>>()

	val isLoading: LiveData<Boolean>
		get() = _isLoading

	val timelineItems: LiveData<List<TimelineItem>>
		get() = _timelineItems

	init {
		isPhysicalActivitiesListLoading.value = false
		isAppUsageListLoading.value = false
		_combinedItemsList = CombinedLiveData(_appUsageItems, _physicalActivityItems) { appUsageItems, physicalActivityItems ->
			val list = mutableListOf<TimelineItem>()
			list.clear()
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
		Observable.fromCallable { usageStatsManager.queryEvents(startTime, endTime) }
				.flatMap { usageEvents -> Observable.fromIterable(usageEvents.toEventsList()) }
				.filter { usageEvent -> packageManager.getLaunchIntentForPackage(usageEvent.packageName) != null && (usageEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND || usageEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) }
				.toList()
				.map { listOfEvents -> listOfEvents.toAppUsageItemsList(packageManager) }
				.doOnSubscribe {
					isAppUsageListLoading.postValue(true)
				}
				.doOnSuccess { listOfTimelineItems ->
					isAppUsageListLoading.postValue(false)
					_appUsageItems.postValue(listOfTimelineItems)
				}
				.doOnError {
					Log.d(TAG, "Failed to read app usage items list: ${it.localizedMessage}")
				}
				.subscribeOn(AppExecutors.workScheduler)
				.subscribe()
				.addToCompositeDisposable(compositeDisposable)
	}

	private fun readFitnessData(context: Context, historyClient: HistoryClient, startTime: Long, endTime: Long) {

		val dataReadRequest = DataReadRequest.Builder()
				.read(DataType.TYPE_ACTIVITY_SEGMENT)
				.setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
				.build()

		historyClient.readData(dataReadRequest)
				.addOnSuccessListener { dataReadResponse ->
					Observable.fromIterable(dataReadResponse.dataSets)
							.filter { dataSet -> !dataSet.isEmpty }
							.flatMap { dataSet: DataSet -> Observable.fromIterable(dataSet.dataPoints) }
							.map { dataPoint: DataPoint -> dataPoint.toPhysicalActivityItem(context) }
							.toList()
							.subscribeOn(Schedulers.computation())
							.observeOn(Schedulers.io())
							.doOnSubscribe {
								isPhysicalActivitiesListLoading.postValue(true)
							}
							.doOnSuccess {
								isPhysicalActivitiesListLoading.postValue(false)
								_physicalActivityItems.postValue(it)
							}
							.doOnError {
								Log.e(TAG, "Failed to read fitness data: ${it.localizedMessage}")
							}
							.subscribe()
							.addToCompositeDisposable(compositeDisposable)
				}
				.addOnFailureListener {
					Log.e(TAG, "Failed to read fitness data: ${it.localizedMessage}")
				}
	}

	override fun onCleared() {
		super.onCleared()
		compositeDisposable.dispose()
	}
}