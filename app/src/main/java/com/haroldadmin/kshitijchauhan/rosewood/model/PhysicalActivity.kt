package com.haroldadmin.kshitijchauhan.rosewood.model

import android.graphics.drawable.Drawable

class PhysicalActivity(activityName : String,
                       activityIcon : Drawable,
                       startTime : Long,
                       endTime : Long) : TimelineItem(activityName, activityIcon, startTime, endTime, TYPE_PHYSICAL_ACTIVITY)