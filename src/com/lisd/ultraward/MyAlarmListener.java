package com.lisd.ultraward;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class MyAlarmListener implements WakefulIntentService.AlarmListener {
	
	private static final String TAG = MyAlarmListener.class.getSimpleName();

	@Override
	public long getMaxAge() {
		// 2.5 Hours
		return AlarmManager.INTERVAL_HOUR * 2 + AlarmManager.INTERVAL_HALF_HOUR;
	}

	@Override
	public void scheduleAlarms(AlarmManager mgr, PendingIntent pitt,
			Context ctx) {
		SharedPreferences sp = ctx.getSharedPreferences(UpdatedGradesCheckerService.WATCHER_PREFS, 0);
		if(sp.getBoolean(UpdatedGradesCheckerService.WATCHER_ON, false)) {
			mgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
	                System.currentTimeMillis() + 22 * 60 * 1000, // now + 22 minutes
	                AlarmManager.INTERVAL_HOUR, pitt);
			Log.i(TAG, "Scheduled the updated grades checker service!");
		} else
			Log.i(TAG, "No need to schedule the updated grades checker service.");
	}

	@Override
	public void sendWakefulWork(Context ctx) {
		WakefulIntentService.sendWakefulWork(ctx, UpdatedGradesCheckerService.class);
		Log.i(TAG, "Sent wakeful work!");
	}
}
