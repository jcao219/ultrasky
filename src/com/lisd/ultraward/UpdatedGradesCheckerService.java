package com.lisd.ultraward;

import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.lisd.ultraward.AssignmentGrade.AssignmentType;

@SuppressWarnings("unused")
public class UpdatedGradesCheckerService extends WakefulIntentService {
	
	private static final String TAG = UpdatedGradesCheckerService.class.getSimpleName();
	public static final String WATCHER_PREFS = "watcher";
	public static final String WATCHER_ON = "watcherOn";
	public static final String WATCHED_USER = "watchedUser";
	public static final String NOTIFICATION_ID_START = "watcherNotificationIDStart";

	public UpdatedGradesCheckerService() {
		super(TAG);
	}

	@Override
	protected void doWakefulWork(Intent arg0) {
		Log.i(TAG, "I'm awake!  Hurray!  Let's see those grades.");
		
		SkywardAuthenticator authenticator = SkywardAuthenticator.getInstance(this);
		try {
			authenticator.reLogin(); // try to use pass and username from shared preferences to log in
		} catch (Exception e) {
			Log.w(TAG, e);
			try {
				authenticator.reLogin();
			} catch (Exception e2) {
				Log.i(TAG, "Unable to log in.  Giving up on grade checking work.");
				return;
			}
		}
		
		SharedPreferences sPreferences = getSharedPreferences(WATCHER_PREFS, 0);
		if(!sPreferences.getBoolean(WATCHER_ON, false)) {
			Log.w(TAG, "So, I'm supposed to be disabled.  What?");
			return;  // don't do it if i'm disabled.
		}
		String dbname = sPreferences.getString(WATCHED_USER, "");
		if(dbname.isEmpty()) {
			Log.w(TAG, "Updated grades checker service called with "
					+ "no user to check for.  Giving up.");
			return; // don't even try
		}
		int startNotificationID = sPreferences.getInt(NOTIFICATION_ID_START, 1570);
		if(startNotificationID > 1666) {
			startNotificationID = 1570;
		}

		SQLiteDatabase db = openOrCreateDatabase(dbname, MODE_PRIVATE, null);
		Log.i(TAG, "Opened database.");
		
		Cursor cursor = db.query("watched_courses", new String[] {"course", "gbid", "csid", "sem"}, null, 
				null, null, null, null);
		while(cursor.moveToNext()) {  // for each course
			String gbid = cursor.getString(cursor.getColumnIndex("gbid"));
			String csid = cursor.getString(cursor.getColumnIndex("csid"));
			String course = cursor.getString(cursor.getColumnIndex("course"));
			String sem = cursor.getString(cursor.getColumnIndex("sem"));
			
			CourseAnalyzer analyzer = new CourseAnalyzer(gbid, csid, sem, db);
			String html;
			
			if(analyzer.isFirstAccess())
				continue; // don't worry about this one.
			try {
				html = authenticator.getCourseGrades(gbid, csid, sem);
			} catch (IOException e) {
				Log.w(TAG, e);  // will retry once
				try {
					html = authenticator.getCourseGrades(gbid, csid, sem);
				} catch (IOException e2) {
					Log.i(TAG, "Too many IOExceptions, giving up!");
					continue;
				}
			}
			
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  // just pause...
			
			CourseParser parser = new CourseParser(html);
			List<AssignmentGrade> results = parser.getDumpOfGrades();
			if(results.size() == 0) {
				Log.w(TAG, "Somehow got no assignment grade results.  Giving up.");
				continue;
			}
			List<AssignmentGrade.GradeChangeInfo> changes = analyzer.analyze(results);
			
//			if(course.equals("PHYSICSC AP SEM"))
//			 	changes.add((new AssignmentGrade("Dummy", 99, 9, AssignmentType.ASSIGNMENT)).makeChangeInfo(-1));
			
			for(AssignmentGrade.GradeChangeInfo changeInfo : changes) {
				AssignmentGrade assignment = changeInfo.getAssignment();
				NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
				builder.setSmallIcon(R.drawable.eye_gray);
				builder.setDefaults(Notification.DEFAULT_VIBRATE);
				switch(changeInfo.getType()) {
				case CHANGED_GRADE:
					builder.setContentTitle("Changed grade in " + course);
					builder.setContentText(changeInfo.getOldGrade() + " to "
							+ assignment.GetGrade() + " on " 
							+ assignment.GetName());
					break;
				case NEW_GRADE:
					builder.setContentTitle("New grade in " + course);
					builder.setContentText(assignment.GetGrade()
							+ " on " + assignment.GetName());
					break;
				}
				Intent resultIntent = new Intent(this, CourseGradebook.class);
				resultIntent.putExtra(Gradebook.EXTRA_GBID, gbid);
				resultIntent.putExtra(Gradebook.EXTRA_CSID, csid);
				resultIntent.putExtra(Gradebook.EXTRA_SEM, sem);
				resultIntent.putExtra(Gradebook.EXTRA_REFRESH_GRADES, true);

				TaskStackBuilder tsb = TaskStackBuilder.create(this);
				tsb.addParentStack(CourseGradebook.class);
				tsb.addNextIntent(resultIntent);
				PendingIntent ptt = tsb.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setContentIntent(ptt);
				
				NotificationManager man = (NotificationManager) 
						getSystemService(Context.NOTIFICATION_SERVICE);
				man.notify(startNotificationID, builder.build());
				startNotificationID++;
			}
		}
		cursor.close();
		db.close();
		Log.i(TAG, "Closed database.");
		
		SharedPreferences.Editor editor = sPreferences.edit();
		editor.putInt(NOTIFICATION_ID_START, startNotificationID);
		editor.commit();
	}
}
