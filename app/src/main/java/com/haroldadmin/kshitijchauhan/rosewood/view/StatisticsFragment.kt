package com.haroldadmin.kshitijchauhan.rosewood.view


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.haroldadmin.kshitijchauhan.rosewood.R
import com.haroldadmin.kshitijchauhan.rosewood.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_statistics.*

class StatisticsFragment : Fragment() {

	private lateinit var mainViewModel: MainViewModel

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		activity?.let {
			mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
			with (mainViewModel) {
				appUsageTime.observe(this@StatisticsFragment, Observer {
					phoneUsageTimeValue.text = "${it / (1000 * 60)} minutes"
				})
				physicalActivityTime.observe(this@StatisticsFragment, Observer {
					physicalActivityValue.text = "${it / (1000 * 60)} minutes"
				})
				totalEngagementTime.observe(this@StatisticsFragment, Observer {
					totalEngagementTimeValue.text = "${it / (1000 * 60)} minutes"
				})
			}
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_statistics, container, false)
	}
}
