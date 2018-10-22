package com.haroldadmin.kshitijchauhan.rosewood.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.haroldadmin.kshitijchauhan.rosewood.R
import com.haroldadmin.kshitijchauhan.rosewood.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(),
		BottomNavigationDrawerFragment.FragmentListener,
		TimelineFragment.RefreshTimelineListener {

	private val FILE_AUTHORITY = "com.haroldadmin.kshitijchauhan.rosewood.fileprovider"

	private lateinit var mainViewModel: MainViewModel

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

		setSupportActionBar(bottomAppBar)
		fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_outline_refresh_24px))

		mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
		lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

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

		val statsFragment = StatisticsFragment()
		val fragmentManager = supportFragmentManager
		fragmentManager.beginTransaction()
				.replace(R.id.fragmentContainer, statsFragment, "StatisticsFragment")
				.commit()
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
		menuInflater.inflate(R.menu.bottom_app_bar_menu, menu)
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
				Snackbar.make(coordinatorLayout, "File written successfully", Snackbar.LENGTH_LONG)
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
			android.R.id.home -> {
				val bottomNavDrawerFragment = BottomNavigationDrawerFragment()
				bottomNavDrawerFragment.show(supportFragmentManager, bottomNavDrawerFragment.tag)
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

	private fun checkUsageStatsPermission(context: Context) {
		val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
		val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
		this.usageStatsPermissionGranted = mode == AppOpsManager.MODE_ALLOWED
	}

	private fun checkGoogleFitPermission(context: Context) {
		this.googleFitPermissionGranted = GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), this.fitnessOptions)
	}

	override fun switchFragment(type: Int) {
		when (type) {
			0 -> {
				supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, StatisticsFragment(), "StatisticsFragment").commit()
				fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_outline_refresh_24px))
				fab.setOnClickListener {
					mainViewModel.loadData(this, Fitness.getHistoryClient(this, lastSignedInAccount!!))
				}
			}
			1 -> {
				supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, TimelineFragment(), "StatisticsFragment").commit()
				fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_outline_filter_list_24px))
			}
		}
	}

	override fun refreshTimeline() {
		mainViewModel.loadData(this, Fitness.getHistoryClient(this, lastSignedInAccount!!))
	}
}
