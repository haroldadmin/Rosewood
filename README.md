# Rosewood
This application aims to create a timeline of user interactions with their device.

The user is either using the device, or not using it. When they are using the device, their activity timeline can be created using the **UsageStatsManager** API. 
However, when the user is not using the device, the device can either be stationary (for example, on a table), or it can be in their pockets (for example, when they are walking). **Google Fit** can be used to track their physical activity when the device is not stationary, and is not being actively used.

The project makes use of RxJava, LiveData, and ViewModels.
