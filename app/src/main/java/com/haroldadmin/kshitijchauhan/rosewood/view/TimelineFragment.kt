package com.haroldadmin.kshitijchauhan.rosewood.view


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.haroldadmin.kshitijchauhan.rosewood.R
import com.haroldadmin.kshitijchauhan.rosewood.adapter.TimelineItemAdapter
import com.haroldadmin.kshitijchauhan.rosewood.model.TimelineItem
import com.haroldadmin.kshitijchauhan.rosewood.utils.AppExecutors
import com.haroldadmin.kshitijchauhan.rosewood.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_statistics.*
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

class TimelineFragment : Fragment(), CoroutineScope {

	interface RefreshTimelineListener {
		fun refreshTimeline()
	}

	private val TAG = this::class.java.simpleName
	private lateinit var job: Job

	private lateinit var adapter: TimelineItemAdapter
	private lateinit var timeLineRecyclerView: RecyclerView
	private lateinit var mainViewModel: MainViewModel
	private lateinit var layoutManager: LinearLayoutManager
	private lateinit var dividerItemDecoration: DividerItemDecoration
	private lateinit var swipeToRefreshLayout: SwipeRefreshLayout
	private lateinit var refreshTimelineListener: RefreshTimelineListener

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
		if (activity!! !is RefreshTimelineListener) {
			throw ClassCastException("This activity is not a RefreshTimelineListener")
		} else {
			this.refreshTimelineListener = activity!! as RefreshTimelineListener
		}

		with(mainViewModel) {
			timelineItems
					.observe(this@TimelineFragment, Observer { newList ->
						launch(AppExecutors.computationDispatcher) {
							val result = calculateDiff(newList, adapter)
							withContext(Dispatchers.Main) {
								adapter.updateList(newList)
								result.dispatchUpdatesTo(adapter)
								layoutManager.scrollToPositionWithOffset(0, 0)
							}
						}
					})

			isLoading
					.observe(this@TimelineFragment, Observer { isRefreshing ->
						swipeToRefreshLayout.isRefreshing = isRefreshing
					})
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		job = Job()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_timeline, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupRecyclerView()
		setupSwipeRefreshLayout()
	}

	private fun setupRecyclerView() {
		timeLineRecyclerView = timelineRecyclerView

		adapter = TimelineItemAdapter(emptyList(), Glide.with(this))
		layoutManager = LinearLayoutManager(context)
		timeLineRecyclerView.layoutManager = layoutManager
		timeLineRecyclerView.adapter = adapter
	}

	private fun setupSwipeRefreshLayout() {
		swipeToRefreshLayout = timelineSwipeRefreshLayout
		swipeToRefreshLayout.setOnRefreshListener {
			this.refreshTimelineListener.refreshTimeline()
			AppExecutors.workExecutor.execute {
				val diffUtil = TimelineItemAdapter.TimelineItemsDiffUtil(adapter.listOfTimeLineItems, emptyList())
				val result = DiffUtil.calculateDiff(diffUtil)
				AppExecutors.mainThreadExecutor.execute {
					adapter.clearAdapter()
					result.dispatchUpdatesTo(adapter)
				}
			}
		}
	}

	private fun calculateDiff(newList: List<TimelineItem>, adapter: TimelineItemAdapter): DiffUtil.DiffResult {
		val diffCallback = TimelineItemAdapter.TimelineItemsDiffUtil(adapter.listOfTimeLineItems, newList)
		val diffResult = DiffUtil.calculateDiff(diffCallback)
		return diffResult
	}
}
