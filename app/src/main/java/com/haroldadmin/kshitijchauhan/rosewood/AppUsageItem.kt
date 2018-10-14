package com.haroldadmin.kshitijchauhan.rosewood

import android.graphics.drawable.Drawable

class AppUsageItem(appName : String,
                   appIcon : Drawable,
                   startTime : Long,
                   endTime : Long) : TimelineItem(appName, appIcon, startTime, endTime, TimelineItem.TYPE_APP_USAGE)