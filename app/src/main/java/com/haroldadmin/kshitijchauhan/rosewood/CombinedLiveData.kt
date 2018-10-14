package com.haroldadmin.kshitijchauhan.rosewood

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class CombinedLiveData<T : S, K : S, S>(source1: LiveData<T>, source2: LiveData<K>, private val combine: (data1: T?, data2: K?) -> S) : MediatorLiveData<S>() {

	private var data1: T? = null
	private var data2: K? = null

	init {
		super.addSource(source1) {
			data1 = it
			value = combine(data1, data2)
		}
		super.addSource(source2) {
			data2 = it
			value = combine(data1, data2)
		}
	}

	override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
		throw UnsupportedOperationException()
	}

	override fun <T : Any?> removeSource(toRemote: LiveData<T>) {
		throw UnsupportedOperationException()
	}
}