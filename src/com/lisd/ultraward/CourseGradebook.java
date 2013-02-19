package com.lisd.ultraward;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.lisd.ultraward.AssignmentGrade.GradeChangeInfo;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.renderscript.Sampler;
import android.support.v4.app.NavUtils;

public class CourseGradebook extends Activity {
	private ListView gradeslist;
	private ProgressBar progbar;
	
	private ParseCourseTask parseTask;
	
	private static List<GradeChangeInfo> cached_grade_changes;
	private static List<AssignmentGrade> cached_grades;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_course_gradebook);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		gradeslist = (ListView)findViewById(R.id.assignment_grades_listview);
		progbar = (ProgressBar)findViewById(R.id.progressBarForFetchingCourseGrades);
		parseTask = new ParseCourseTask();
		
		if(getIntent().getBooleanExtra(Gradebook.EXTRA_REFRESH_GRADES, false)) { // need refresh
			cached_grades = null;
			cached_grade_changes = null;
		}
		getIntent().putExtra(Gradebook.EXTRA_REFRESH_GRADES, false);
		
		if(cached_grades == null) {
			System.out.println("Fetching detailed grades for course");
			
			cached_grade_changes = null;
			parseTask.execute();
		} else {
			System.out.println("No need to fetch grades again.  Did you rotate the screen?");
			parseTask.execute(cached_grades);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onRestart() {
		parseTask = new ParseCourseTask();
		parseTask.execute(cached_grades);
			
		showProgress(true);
		super.onRestart();
	}
	
	@Override
	protected void onStop() {
		showProgress(false);
		System.out.println("Stopping CourseGradebook activity!");
		if(parseTask != null)
			parseTask.cancel(true);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_course_gradebook, menu);
		return true;
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            progbar.setVisibility(View.VISIBLE);
            progbar.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progbar.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            gradeslist.setVisibility(View.VISIBLE);
            gradeslist.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            gradeslist.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progbar.setVisibility(show ? View.VISIBLE : View.GONE);
            gradeslist.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
	
	private final class AssignmentGradesAdapter extends ArrayAdapter<AssignmentGrade> {
		private List<AssignmentGrade> grades;
		private Context context;
		private boolean[] grades_highlighted;
		public AssignmentGradesAdapter(Context contxt, int textViewResource, List<AssignmentGrade> stuff) {
			super(contxt, textViewResource, stuff);
			grades = stuff;
			context = contxt;	
			
			grades_highlighted = new boolean[grades.size()];
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if(rowView == null) {
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.assignment_grades_layout, null);						
			}
			TextView gradeText = (TextView)rowView.findViewById(R.id.course_grade);
			
			String grde = Integer.toString(grades.get(position).GetGrade());
			if(grde.equals("-1"))
				grde = "N/A";
			
			int pct = Math.round(grades.get(position).GetPercentageOfParent());
			String pctxt = pct > 0 ? "(" + Float.toString(pct) + "%)" : "";
			gradeText.setText(grades.get(position).GetName() + " " + pctxt + " - "
					+ grde);
			
			rowView.setTag(position);
			
			switch(grades.get(position).GetType()) {
			case MAJOR_CATEGORY: case COURSE_GRADE: 
				gradeText.setTextSize(25);
				gradeText.setTypeface(null, Typeface.BOLD);
				break;
			case MINOR_CATEGORY:
				gradeText.setTextSize(22);
				gradeText.setTypeface(null, Typeface.BOLD);
				break;
			case ASSIGNMENT:
				gradeText.setTypeface(null, Typeface.NORMAL);
				gradeText.setTextSize(18);
				break;
			}
			
			if(grades_highlighted[position])
				gradeText.setTextColor(Color.BLUE);
			else
				gradeText.setTextColor(Color.BLACK);
			
			return rowView;
		}

		public void highlightChanges(List<GradeChangeInfo> changes) {
			for(int i = 0; i < grades.size(); i++) {
				for(GradeChangeInfo ch : changes) {
					if(ch.getAssignment() == grades.get(i)) {
						grades_highlighted[i] = true;
						View child = gradeslist.getChildAt(i);
						if(child == null)
							break;
						TextView tv = ((TextView)child.findViewById(R.id.course_grade));
					    if(tv != null)
					    	tv.setTextColor(Color.BLUE);
						break;
					}
				}
			}			
		}
	}  // end of AssignmentGradesAdapter class
	
	private final class AnalyzeGradesTask extends AsyncTask<List<AssignmentGrade>, 
			Void, List<AssignmentGrade.GradeChangeInfo>> {
		private CourseAnalyzer analyzer;
		private SQLiteDatabase db;
		
		@Override
		protected List<AssignmentGrade.GradeChangeInfo> doInBackground(List<AssignmentGrade>... args) {
			Intent itt = getIntent();
			String dbname = SkywardAuthenticator.getInstance(CourseGradebook.this).getLoginIfSaved();
			if(dbname.isEmpty())
				return new ArrayList<AssignmentGrade.GradeChangeInfo>();
			db = openOrCreateDatabase(
					dbname, 
					MODE_PRIVATE,
					null);
			System.out.println("Opened database.");
			analyzer = new CourseAnalyzer(itt.getStringExtra(Gradebook.EXTRA_GBID),
					itt.getStringExtra(Gradebook.EXTRA_CSID),
					itt.getStringExtra(Gradebook.EXTRA_SEM), db);
			return analyzer.analyze(args[0]);
		}
		
		protected void onPostExecute(List<AssignmentGrade.GradeChangeInfo> blah) {	
			db.close();
			System.out.println("Closed database.");
			cached_grade_changes = blah;
			((AssignmentGradesAdapter)gradeslist.getAdapter()).highlightChanges(blah);
		}
	}
	
	private final class ParseCourseTask extends AsyncTask<List<AssignmentGrade>, Void, List<AssignmentGrade>> {
		private CourseParser parser;
		private AnalyzeGradesTask atask;
		
		@Override
		protected List<AssignmentGrade> doInBackground(List<AssignmentGrade>... args) {
			boolean successOnFirst = true;
			List<AssignmentGrade> cached_ret;
			if(args.length == 1)
				cached_ret = args[0];
			else
				cached_ret = null;
			List<AssignmentGrade> ret;
			SkywardAuthenticator sauth = SkywardAuthenticator.getInstance(CourseGradebook.this);
			Intent itt = getIntent();
			
			if(cached_ret == null || cached_ret.isEmpty()) {
				String gbhtml = "";
				try {
					gbhtml = sauth.getCourseGrades(itt.getStringExtra(Gradebook.EXTRA_GBID),
							itt.getStringExtra(Gradebook.EXTRA_CSID),
							itt.getStringExtra(Gradebook.EXTRA_SEM));
				} catch (IOException e) {
					e.printStackTrace();
					successOnFirst = false;
				}
				
				if(successOnFirst && !isCancelled()) {
					parser = new CourseParser(gbhtml);				
					ret = parser.getDumpOfGrades();
				} else {
					ret = new ArrayList<AssignmentGrade>();  // trigger relog in
				}
			} else {
				ret = cached_ret;
			}

			if(ret.size() == 0 && !isCancelled()) {
				// try logging in again
				
				try {
					sauth.reLogin();
					String gbhtml = sauth.getCourseGrades(itt.getStringExtra(Gradebook.EXTRA_GBID),
							itt.getStringExtra(Gradebook.EXTRA_CSID),
							itt.getStringExtra(Gradebook.EXTRA_SEM));
					parser = new CourseParser(gbhtml);
					ret = parser.getDumpOfGrades();
				} catch (Exception e) {
					System.out.println("Couldn't relogin.  Giving up."); // TODO: make this visible to user
					return ret;
				}
			}
			
			return ret;
		}
		
		@SuppressWarnings("unchecked")
		protected void onPostExecute(List<AssignmentGrade> blah) {
			parseTask = null;
			
			if(blah.size() == 0) // TODO: make this more informative that it is error
				NavUtils.navigateUpFromSameTask(CourseGradebook.this);
			
			cached_grades = blah;
			
			AssignmentGradesAdapter adptr = new AssignmentGradesAdapter(CourseGradebook.this, R.layout.course_grades_layout, blah);
			
			gradeslist.setAdapter(adptr);
			
			if(isCancelled()) {
				System.out.println("No analyze!  Cancelled!");
				return;
			}
			
			if(cached_grade_changes == null) { // no cached
				atask = new AnalyzeGradesTask();
				atask.execute(blah);
			} else {
				((AssignmentGradesAdapter)gradeslist.getAdapter()).highlightChanges(cached_grade_changes);
			}
			
			showProgress(false);
			return;
		}
		
		@Override
        protected void onCancelled() {
            parseTask = null;
            if(atask != null)
            	atask.cancel(true);
        }
	}

}
