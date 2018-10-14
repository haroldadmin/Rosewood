package com.haroldadmin.kshitijchauhan.rosewood

import android.graphics.drawable.Drawable

class PhysicalActivityItem(activityName : String,
                           activityIcon : Drawable,
                           startTime : Long,
                           endTime : Long) : TimelineItem(activityName, activityIcon, startTime, endTime, TimelineItem.TYPE_PHYSICAL_ACTIVITY)