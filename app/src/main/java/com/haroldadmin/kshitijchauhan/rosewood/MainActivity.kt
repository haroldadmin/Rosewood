package com.haroldadmin.kshitijchauhan.rosewood

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

	private val TAG = this::class.java.simpleName
	private val FILE_AUTHORITY = "com.haroldadmin.kshitijchauhan.rosewood.fileprovider"
	private lateinit var adapter: TimelineItemAdapter
	private lateinit var timeLineRecyclerView: RecyclerView
	private lateinit var mainViewModel: MainViewModel
	private lateinit var layoutManager: LinearLayoutManager
	private lateinit var dividerItemDecoration: DividerItemDecoration
	private lateinit var swipeToRefreshLayout: SwipeRefreshLayout
	private var googleFitPermissionGranted: Boolean = false
	private var usageStatsPermissionGranted: Boolean = false
	private var lastSignedInAccount: GoogleSignInAccount? = null
	private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1234

	private val fitnessOptions: FitnessOptions = FitnessOptions
			.builder()
			.addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
			.build()

	private val usageStatsPermissionDialog: AlertDialog by lazy {
		AlertDialog.Builder(this)
				.setTitle("Usage Access permission required")
				.setMessage("Please grant us access to this permission in the settings app")
				.setPositiveButton("Settings") { _, _ ->
					startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
				}
				.setNegativeButton("Cancel") { dialogInterface, _ ->
					dialogInterface.dismiss()
				}
				.setCancelable(true)
				.create()
	}

	@SuppressLint("WrongConstant")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
		lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

		setupRecyclerView()
		setupSwipeRefreshLayout()
		checkUsageStatsPermission(this)
		checkGoogleFitPermission(this)

		lastSignedInAccount?.let { account ->
			if (usageStatsPermissionGranted && googleFitPermissionGranted) {
				mainViewModel.loadData(this, Fitness.getHistoryClient(this, account))
			}
		} ?: run {
			if (!usageStatsPermissionGranted) askForUsageStatsPermission()
			else askForGoogleFitPermission()
		}

		mainViewModel.timelineItems
				.observe(this, Observer { newList ->
					AppExecutors.workExecutor.execute {
						val diffUtil = TimelineItemAdapter.TimelineItemsDiffUtil(adapter.listOfTimeLineItems, newList)
						val result = DiffUtil.calculateDiff(diffUtil)
						AppExecutors.mainThreadExecutor.execute {
							adapter.updateList(newList)
							result.dispatchUpdatesTo(adapter)
						}
					}
				})
		mainViewModel.isLoading
				.observe(this, Observer {
					swipeToRefreshLayout.isRefreshing = it
				})
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (resultCode == Activity.RESULT_OK) {
			this.googleFitPermissionGranted = requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE
		}
	}

	override fun onResume() {
		super.onResume()
		checkGoogleFitPermission(this)
		checkUsageStatsPermission(this)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.main_activity_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when (item?.itemId) {
			R.id.writeToFile -> {
				val gson = GsonBuilder().create()
				val json = gson.toJson(mainViewModel.timelineItems.value)
				var file = File(this.filesDir, "logs")
				if (!file.exists()) {
					file.mkdir()
				}
				file = File(this.filesDir, "logs/log.json")
				FileOutputStream(file)
						.use {
							it.write(json.toByteArray())
						}
				val uri = FileProvider.getUriForFile(this, FILE_AUTHORITY, file)
				Snackbar.make(constraint_layout, "File written successfully", Snackbar.LENGTH_SHORT)
						.setAction("Share") {
							val shareIntent = ShareCompat.IntentBuilder
									.from(this)
									.setStream(uri)
									.intent
							shareIntent.data = uri
							shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
							if (shareIntent.resolveActivity(packageManager) != null) {
								startActivity(shareIntent)
							}
						}
						.show()
			}
			else -> super.onOptionsItemSelected(item)
		}
		return true
	}

	private fun askForUsageStatsPermission() {
		if (!usageStatsPermissionDialog.isShowing) usageStatsPermissionDialog.show()
	}

	private fun askForGoogleFitPermission() {
		GoogleSignIn.requestPermissions(
				this,
				GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
				GoogleSignIn.getLastSignedInAccount(this),
				this.fitnessOptions)
	}

	private fun setupRecyclerView() {
		timeLineRecyclerView = recyclerView

		adapter = TimelineItemAdapter(emptyList(), Glide.with(this))
		layoutManager = LinearLayoutManager(this)
		dividerItemDecoration = DividerItemDecoration(this, layoutManager.orientation)
		dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.list_divider)!!)

		timeLineRecyclerView.layoutManager = layoutManager
		timeLineRecyclerView.adapter = adapter
		timeLineRecyclerView.addItemDecoration(dividerItemDecoration)
	}

	private fun setupSwipeRefreshLayout() {
		swipeToRefreshLayout = swipeRefreshLayout
		swipeToRefreshLayout.setOnRefreshListener {
			mainViewModel.loadData(this, Fitness.getHistoryClient(this, lastSignedInAccount!!))
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

	private fun checkUsageStatsPermission(context: Context) {
		val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
		val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
		this.usageStatsPermissionGranted = mode == AppOpsManager.MODE_ALLOWED
	}

	private fun checkGoogleFitPermission(context: Context) {
		this.googleFitPermissionGranted = GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), this.fitnessOptions)
	}
}
