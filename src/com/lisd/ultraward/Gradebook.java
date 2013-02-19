package com.lisd.ultraward;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Gradebook extends Activity {
	private ListView lview;
	private ProgressBar progbar;
	private Button saveButton;
	
	private boolean noSavedLogin = false;
	
	private static ParseGradebookTask parseTask;
	private static String gradesHtml = "";
	
	private static List<CourseGrades> watchedCourses; 
	
	// "already" = cached from last time	
	private static List<CourseGrades> grades_already;
	private static int semester_already;
	
	public static final String EXTRA_GBID = "com.lisd.ultraward.extra.gbid";
	public static final String EXTRA_CSID = "com.lisd.ultraward.extra.csid";
	public static final String EXTRA_SEM = "com.lisd.ultraward.extra.semester";
	public static final String EXTRA_REFRESH_GRADES = "com.lisd.ultraward.extra.refresh";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gradebook);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		lview = (ListView)findViewById(R.id.listview1);
		progbar = (ProgressBar)findViewById(R.id.progress_bar_gradebook);
		saveButton = (Button)findViewById(R.id.save_watched_btn);
		
		System.out.println("Trying get intent from the Login Activity");
		Intent i = getIntent();
		boolean refresh = i.getBooleanExtra(EXTRA_REFRESH_GRADES, false);
		
		if(refresh) {
			grades_already = null;
		}		
		i.putExtra(EXTRA_REFRESH_GRADES, false);  // next rotate, DON'T REFRESH
		
		if(grades_already == null || refresh) {
			if(i.hasExtra(LoginActivity.EXTRA_HTML_GRADES))
				gradesHtml = i.getStringExtra(LoginActivity.EXTRA_HTML_GRADES);
			else {
				Intent intt = new Intent(this, LoginActivity.class);
				// it should hopefully autologin
				startActivity(intt);
				finish();
			}
			if(!gradesHtml.isEmpty()) {
				parseTask = new ParseGradebookTask();
				parseTask.execute();
			}
		} else {  // Neither conditions up there of the if block are satisfied.
			// So we have everything we need.
			// Went back to here.
			System.out.println("Back here!");
			progbar.setVisibility(View.GONE);
			progbar.setAlpha(0);
			lview.setVisibility(View.VISIBLE);
			lview.setAlpha(1);
			setUpGradebook();
		}
	}
	
    @Override
    public void onBackPressed() {  // i have to do this?
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.NO_AUTO_LOGIN, true);
        System.out.println("Starting the login activity, no autologin!");
        startActivity(intent);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_gradebook, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		menu.findItem(R.id.menu_watched_settings).setEnabled(
				lview.getAdapter() != null);
		return true;
	}
	
	// defined in xml file for this menu
	// called when the "Watched" button is clicked
	public void onWatchedMenuItemClicked(MenuItem item) {
		if(noSavedLogin) {
			Toast.makeText(this, "Please login again with " +
					"a saved password to use this functionality.", Toast.LENGTH_LONG);
			return;
		}
			
		if(saveButton.getVisibility() == View.GONE) {
			// Start editing watch list
			if(grades_already != null && lview.getChildCount() > 0) {
				saveButton.setVisibility(View.VISIBLE);
				Toast.makeText(this, "Press the courses to toggle.", 
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Please wait for courses to load...", 
						Toast.LENGTH_LONG).show();
			}
		} else {
			// Stop editing watch list, commit changes
			saveButton.setVisibility(View.GONE);
			saveWatchedCourses();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void saveWatchedCourses() {
		saveButton.setVisibility(View.GONE);
		WatchStuffTask task = new WatchStuffTask();
		task.execute(watchedCourses);
		
		if(watchedCourses.size() > 0)
			WakefulIntentService.scheduleAlarms(new MyAlarmListener(), this, false);
		else
			WakefulIntentService.cancelAlarms(this);
	}
	
	public void onSaveButtonClicked(View view) {
		saveButton.setVisibility(View.GONE);
		saveWatchedCourses();
		
		Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
	}
	
	// defined in xml file for this menu
	// called when the "Check Now" button is clicked
	public void onCheckNowMenuItemClicked(MenuItem item) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	    // from http://stackoverflow.com/a/5921190, wonderful elegant solution!
	        if (UpdatedGradesCheckerService.class.getName().
	        		equals(service.service.getClassName())) {
	        	Toast toast = Toast.makeText(this, 
	        			"Currently already checking for updated grades.", Toast.LENGTH_SHORT);
	        	toast.show();
	            return;
	        }
	    }
	    Toast toast = Toast.makeText(this, 
    			"Now checking for updated grades...", Toast.LENGTH_SHORT);
    	toast.show();
		WakefulIntentService.sendWakefulWork(this, UpdatedGradesCheckerService.class);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.NO_AUTO_LOGIN, true);
             
            System.out.println("Starting the login activity, no autologin!");
            startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void hideProgressBar() {
		int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
		
		progbar.animate()
			.setDuration(shortAnimTime)
			.alpha(0)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					progbar.setVisibility(View.GONE);
				}
			});
		
		lview.setVisibility(View.VISIBLE);
	}
	
	public void setUpGradebook() {
		hideProgressBar();
		
		CoursesAdapter aadapt = new CoursesAdapter(Gradebook.this, 
				R.layout.course_grades_layout, grades_already);
		lview.setAdapter(aadapt);
		
		invalidateOptionsMenu();
	}
	
	/**
	 * Only a listener for the rowviews that make up the lview Listview
	 * either opens up the class grades detail page
	 * or toggles whether to watch this course for updated grades
	 * if we are in edit mode
	 */	
	private final class ClickCourseListener implements View.OnClickListener {
		public void onClick(View v) {
			if(saveButton.getVisibility() == View.GONE) {
				// not in the process of editing the watchlist
				Intent intent = new Intent(Gradebook.this, CourseGradebook.class);
				CourseGrades cgr = grades_already.get((Integer)v.getTag());
				intent.putExtra(EXTRA_GBID, cgr.getGBID());
				intent.putExtra(EXTRA_CSID, cgr.getCSID());
				intent.putExtra(EXTRA_SEM, Integer.toString(semester_already));
				intent.putExtra(EXTRA_REFRESH_GRADES, true);
				startActivity(intent);
			} else {
				CheckBox cBox = (CheckBox)v.findViewById(R.id.watched_checkbox_indicator);
				cBox.toggle();
			}
		}
	}
	
	
	private final class CoursesAdapter extends ArrayAdapter<CourseGrades> {
		private List<CourseGrades> courses;
		private Context context;
		private ClickCourseListener clklistener;
		public CoursesAdapter(Context contxt, int textViewResource, List<CourseGrades> stuff) {
			super(contxt, textViewResource, stuff);
			courses = stuff;
			context = contxt;
			clklistener = new ClickCourseListener();			
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if(rowView == null) {
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.course_grades_layout, null);						
			}
			TextView gradeText = (TextView)rowView.findViewById(R.id.course_grade);
			TextView courseText = (TextView)rowView.findViewById(R.id.course_name);
			gradeText.setText(courses.get(position).getGrade());
			courseText.setText("\n" + courses.get(position).getCourse());
			
			CheckBox checkBox = (CheckBox)rowView.findViewById(R.id.watched_checkbox_indicator);
			if(watchedCourses.contains(courses.get(position)))
				checkBox.setButtonDrawable(R.drawable.eye);
			else
				checkBox.setButtonDrawable(R.drawable.eye_gray);
			
			// this condition is TRUE when EDITING WATCHLIST, so make it clickable then.
		    checkBox.setClickable(saveButton.getVisibility() == View.VISIBLE);
		    checkBox.setTag(position);
		    
		    checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton chkBox, boolean isWhatIWantToWatch) {
					CourseGrades correspondingCourse = courses.get((Integer)chkBox.getTag());
					if(isWhatIWantToWatch) {  // isChecked == isWhatIWantToWatch
						if(watchedCourses.contains(correspondingCourse))
							return; // already watched
						watchedCourses.add(correspondingCourse);
						chkBox.setButtonDrawable(R.drawable.eye);
					} else {
						if(watchedCourses.contains(correspondingCourse))
							watchedCourses.remove(correspondingCourse);
						chkBox.setButtonDrawable(R.drawable.eye_gray);
					}
				}
			});
			
			rowView.setTag(position);
			rowView.setOnClickListener(clklistener);
			return rowView;
		}
	}
	
	private final class ParseGradebookTask extends AsyncTask<Void, Void, List<CourseGrades>> {
		private GradebookHtmlParser parser;
		@Override
		protected List<CourseGrades> doInBackground(Void... arg0) {
			parser = new GradebookHtmlParser(gradesHtml);
			List<CourseGrades> cglist = parser.getCurrentGrades();
			
			String dbname = SkywardAuthenticator.getInstance(Gradebook.this).getLoginIfSaved();
			
			if(dbname.isEmpty()) {
				return cglist;
			}
			
			SQLiteDatabase db = openOrCreateDatabase(dbname, MODE_PRIVATE, null);
			System.out.println("Opened database.");
			
			String[] csids = new String[cglist.size()];
			StringBuilder selection = new StringBuilder();
			
			boolean first = true;
			for(int i = 0; i < cglist.size(); i++) {
				csids[i] = cglist.get(i).getCSID();
				if(!first)
					selection.append(" OR ");
				first = false;
				selection.append("csid = ?");
			}
			
			db.execSQL("CREATE TABLE IF NOT EXISTS watched_courses " +
					"(oldgrade INTEGER, course TEXT, gbid TEXT, csid TEXT, sem TEXT);");
			Cursor c = db.query("watched_courses", null, selection.toString(), 
					csids, null, null, null);
			System.out.println(c.getCount() + " watched courses found.");
			
			watchedCourses = new ArrayList<CourseGrades>();
			
			while(c.moveToNext()) {
				for(CourseGrades courseGrades : cglist){
					if(courseGrades.getCSID().equals(c.getString(c.getColumnIndex("csid")))) {
						watchedCourses.add(courseGrades);
						break;
					}
				}
			}
			c.close();
			db.close();
			System.out.println("Closed database.");
			
			saveWatchedCourses();
			return cglist;
		}
		@Override
		protected void onPostExecute(List<CourseGrades> res) {
			grades_already = res;
			semester_already = parser.Semester;
			
			if(SkywardAuthenticator.getInstance(Gradebook.this).getLoginIfSaved().isEmpty())
				noSavedLogin = true;
			
			setUpGradebook();
		}
	}
	
	/**
	 *  a background task to access the database to update watched_courses
	 *  .execute accepts 1 argument of a List<CourseGrades> which will
	 *  be the courses that the app is supposed to watch for changes
	 *  and update the database accordingly.  Called when settings are changed.
	 */
	private final class WatchStuffTask extends AsyncTask<List<CourseGrades>, Void, Void> {
		@Override
		protected Void doInBackground(List<CourseGrades>... arg0) {
			// turn on the watcher if we have arguments, which are the classes to watch
			String dbname = SkywardAuthenticator.getInstance(Gradebook.this).getLoginIfSaved();
			
			if(dbname.isEmpty())
				return (Void)null;
			
			SharedPreferences sPreferences = getSharedPreferences(UpdatedGradesCheckerService.WATCHER_PREFS, 0);
			SharedPreferences.Editor editor = sPreferences.edit();
			editor.putBoolean(UpdatedGradesCheckerService.WATCHER_ON, arg0[0].size() > 0);
			editor.putString(UpdatedGradesCheckerService.WATCHED_USER, dbname);
			editor.commit();
			
			SQLiteDatabase db = openOrCreateDatabase(dbname, MODE_PRIVATE, null);
			System.out.println("Opened database.");
			
			db.execSQL("DROP TABLE IF EXISTS watched_courses;");
			db.execSQL("CREATE TABLE watched_courses "+
					"(oldgrade INTEGER, course TEXT, gbid TEXT, csid TEXT, sem TEXT);");
			for(CourseGrades cg : arg0[0]) {
				ContentValues cv = new ContentValues();
				cv.put("oldgrade", cg.getGrade());
				cv.put("course", cg.getCourse());
				cv.put("gbid", cg.getGBID());
				cv.put("csid", cg.getCSID());
				cv.put("sem", Integer.toString(semester_already));
				db.insert("watched_courses", null, cv);
			}
			db.close();
			System.out.println("Closed database.");
			
			return (Void)null;
		}
	}
}
