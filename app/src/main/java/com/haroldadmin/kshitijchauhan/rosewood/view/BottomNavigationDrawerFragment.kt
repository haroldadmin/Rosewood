package com.haroldadmin.kshitijchauhan.rosewood.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.haroldadmin.kshitijchauhan.rosewood.R
import kotlinx.android.synthetic.main.fragment_bottom_sheet.*

class BottomNavigationDrawerFragment: BottomSheetDialogFragment() {

	private lateinit var fragmentListener: FragmentListener

	interface FragmentListener {
		fun switchFragment(type: Int)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		if (activity!! !is FragmentListener) {
			throw ClassCastException("This activity is not a fragment listener")
		} else {
			this.fragmentListener = activity!! as FragmentListener
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		navigation_view.setNavigationItemSelectedListener { menuItem ->
			when(menuItem.itemId) {
				R.id.homeNavigationMenu -> {
					fragmentListener.switchFragment(0)
					this.dismiss()
					true
				}
				R.id.timelineNavigationMenu -> {
					fragmentListener.switchFragment(1)
					this.dismiss()
					true
				}
				else -> false
			}
		}
	}

}