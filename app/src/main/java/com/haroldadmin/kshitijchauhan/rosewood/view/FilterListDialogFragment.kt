package com.haroldadmin.kshitijchauhan.rosewood.view


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.app.Fragment
import android.content.Context
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

import com.haroldadmin.kshitijchauhan.rosewood.R

class FilterListDialogFragment : DialogFragment() {

	private lateinit var filterDialogListener: FilterDialogListener

	interface FilterDialogListener {
		fun filterActivities()
		fun filterAppUsage()
		fun filterBoth()
	}

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		if (context !is FilterDialogListener) {
			throw ClassCastException("This activity is not a FilterDialogListener")
		} else {
			filterDialogListener = context
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val inflater = activity?.layoutInflater
		val dialogView = inflater?.inflate(R.layout.filter_list_dialog, null)
		val filterApp = dialogView?.findViewById<TextView>(R.id.appUsageFilter)
		val filterPhysicalActivity = dialogView?.findViewById<TextView>(R.id.physicalActivityFilter)
		val filterBoth = dialogView?.findViewById<TextView>(R.id.bothFilter)

		filterApp?.setOnClickListener {
			filterDialogListener.filterAppUsage()
			this.dismiss()
		}

		filterPhysicalActivity?.setOnClickListener {
			filterDialogListener.filterActivities()
			this.dismiss()
		}

		filterBoth?.setOnClickListener {
			filterDialogListener.filterBoth()
			this.dismiss()
		}

		return AlertDialog.Builder(activity)
				.setView(dialogView)
				.create()
	}


}
