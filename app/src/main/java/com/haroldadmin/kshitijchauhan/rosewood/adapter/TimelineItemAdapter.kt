package com.haroldadmin.kshitijchauhan.rosewood.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.haroldadmin.kshitijchauhan.rosewood.R
import com.haroldadmin.kshitijchauhan.rosewood.model.TimelineItem

class TimelineItemAdapter(var listOfTimeLineItems: List<TimelineItem>, val glide: RequestManager) : RecyclerView.Adapter<TimelineItemAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.timeline_item, parent, false))

	override fun getItemCount() = listOfTimeLineItems.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bindValues(listOfTimeLineItems[position])
	}

	fun updateList(newList : List<TimelineItem>) {
		this.listOfTimeLineItems = newList
	}

	fun clearAdapter() {
		updateList(emptyList())
	}

	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

		private val background: View = itemView.findViewById(R.id.timelineItemBackground)
		private val itemName: TextView = itemView.findViewById(R.id.itemNameTextview)
		private val itemIcon: ImageView = itemView.findViewById(R.id.itemIconImageview)
		private val itemStartTime: TextView = itemView.findViewById(R.id.startTimeValueTextview)
		private val itemEndTime: TextView = itemView.findViewById(R.id.endTimeValueTextview)
		private val itemType: TextView = itemView.findViewById(R.id.itemTypeTextview)
		private val scale = itemView.resources.displayMetrics.scaledDensity

		fun bindValues(item: TimelineItem) {
			itemName.text = item.name
			itemStartTime.text = item.startTime
			itemEndTime.text = item.endTime
			glide.load(item.icon).into(itemIcon)
			itemType.text = when(item.type) {
				TimelineItem.TYPE_APP_USAGE -> "App Usage"
				TimelineItem.TYPE_PHYSICAL_ACTIVITY -> "Physical Activity"
				else -> "Unknown"
			}
			if (item.type == TimelineItem.TYPE_PHYSICAL_ACTIVITY) {
				itemIcon.background = ContextCompat.getDrawable(itemView.context, R.drawable.item_icon_background)
				val padding: Int = (12 * scale + 0.5f).toInt()
				itemIcon.setPadding(padding, padding, padding, padding)
			} else {
				itemIcon.background = null
				itemIcon.setPadding(0, 0, 0, 0)
			}
		}
	}

	class TimelineItemsDiffUtil(val oldList: List<TimelineItem>, val newList: List<TimelineItem>) : DiffUtil.Callback() {

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return false
		}

		override fun getOldListSize(): Int = oldList.size

		override fun getNewListSize(): Int = newList.size

		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return false
		}

	}

}