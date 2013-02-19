package com.lisd.ultraward;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CourseAnalyzer {
	String tname;
	SQLiteDatabase db;
	boolean first_access;
	
	public CourseAnalyzer(String gbid, String csid, String sem, SQLiteDatabase db) {
		this("C" + gbid + csid + sem, db);
	}
	
	public boolean isFirstAccess() {
		return first_access;
	}
	
	public CourseAnalyzer(String table_name,
			SQLiteDatabase db2) {
		tname = table_name;
		
		db = db2;
		Cursor cursor = db.rawQuery("select DISTINCT tbl_name from "
				+"sqlite_master where tbl_name = '"+tname+"'", null);
		if(cursor != null && cursor.getCount() == 0) {		
			db.execSQL("CREATE TABLE " + tname
					+ " (name TEXT, grade INTEGER, type INTEGER);");
			first_access = true;
		}
		cursor.close();
	}

	public List<AssignmentGrade.GradeChangeInfo> analyze(List<AssignmentGrade> curgrades) {		
		if(curgrades.size() == 0) {  // forget about it, must be an error
			return new ArrayList<AssignmentGrade.GradeChangeInfo>();
		}
		List<AssignmentGrade> cachedgrades;

		Cursor c = db.query(tname, null, null, null, null, null, null);
	
		cachedgrades = new ArrayList<AssignmentGrade>();
		while(c.moveToNext()) {
			// get the list of previously saved assignments for detection of changes
			String nam = c.getString(c.getColumnIndex("name"));
			int grad = c.getInt(c.getColumnIndex("grade"));
			AssignmentGrade.AssignmentType typ = 
					AssignmentGrade.AssignmentType.valueOf(c.getString(c.getColumnIndex("type")));
			// we don't worry about percentage, so -1.00 is the arg here
			cachedgrades.add(new AssignmentGrade(nam, grad, -1, typ));
		}
		c.close();
		
		// if no cached grades, then let's just forget about it.
		boolean report_no_changes = cachedgrades.size() == 0;
		
		int c_index = 0;
		List<AssignmentGrade.GradeChangeInfo> gchanges = 
				new ArrayList<AssignmentGrade.GradeChangeInfo>();
		for(AssignmentGrade cur : curgrades) {
			if(first_access || report_no_changes)  // first time we see this course's grades, so
				// don't worry about highlighting every single thing
				// haha
				break;
			outer:
			switch(cur.GetType()) {
			case MINOR_CATEGORY:
				// who cares. do nothing.
				break outer;
			case ASSIGNMENT: case MAJOR_CATEGORY: case COURSE_GRADE:
				if(c_index >= cachedgrades.size()
						|| !cur.GetName().equals(cachedgrades.get(c_index).GetName())
						|| cur.GetType() != cachedgrades.get(c_index).GetType()) {
					for(AssignmentGrade ag : cachedgrades) {
						if(cur.GetName().equals(ag.GetName()) // found
								&& cur.GetGrade() == ag.GetGrade()
								&& cur.GetType() == ag.GetType()) {
							// grade unchanged, do nothing
							break outer;
						} else if(cur.GetName().equals(ag.GetName())
								&& cur.GetGrade() != ag.GetGrade()
								&& cur.GetType() == ag.GetType()) {
							// grade changed
							gchanges.add(cur.makeChangeInfo(ag.GetGrade()));
							break outer;
						}
					}
					// new grade
					gchanges.add(cur.makeChangeInfo(-1));
					c_index++;
					break outer;
				} else if(cur.GetName().equals(cachedgrades.get(c_index).GetName())
						&& cur.GetType() == cachedgrades.get(c_index).GetType()
						&& cur.GetGrade() != cachedgrades.get(c_index).GetGrade()) {
					// grade changed
					gchanges.add(cur.makeChangeInfo(cachedgrades.get(c_index).GetGrade()));
					break outer;
				}
				// grade unchanged.
				break outer;
			}
			
			c_index++;
		}
		db.execSQL("DROP TABLE " + tname + ";");
		db.execSQL("CREATE TABLE " + tname
				+ " (name TEXT, grade INTEGER, type TEXT);");
		for(AssignmentGrade ag : curgrades) {
			ContentValues cv = new ContentValues();
			cv.put("name", ag.GetName());
			cv.put("grade", ag.GetGrade());
			cv.put("type", ag.GetType().toString());
			db.insert(tname, null, cv);
		}
		return gchanges;
	}
}
