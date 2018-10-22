package com.haroldadmin.kshitijchauhan.rosewood.view

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.haroldadmin.kshitijchauhan.rosewood.R
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {

	private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1234

	private val fitnessOptions: FitnessOptions = FitnessOptions
			.builder()
			.addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
			.build()

	private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestEmail()
			.requestScopes(Scope(Scopes.FITNESS_ACTIVITY_READ))
			.addExtension(fitnessOptions)
			.build()

	private var usageStatsPermissionGranted: Boolean = false
	private var googleFitPermissionGranted: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_sign_in)

		val googleSignInClient = GoogleSignIn.getClient(this, gso)

		checkGoogleFitPermission(this)
		checkUsageStatsPermission(this)

		materialButton.setOnClickListener {
			startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
		}

		googleSignInButton.apply {
			setSize(SignInButton.SIZE_STANDARD)
			setOnClickListener {
				val intent = googleSignInClient.signInIntent
				startActivityForResult(intent, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE)
			}
		}
	}

	override fun onResume() {
		super.onResume()
		checkGoogleFitPermission(this)
		checkUsageStatsPermission(this)
		if (googleFitPermissionGranted && usageStatsPermissionGranted) {
			finish()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
			val task = GoogleSignIn.getSignedInAccountFromIntent(data)
			googleFitPermissionGranted = true
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

