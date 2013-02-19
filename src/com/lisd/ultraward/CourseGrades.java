/**
 * 
 */
package com.lisd.ultraward;

/**
 * @author jimmy
 *
 */
public class CourseGrades {
	private String course, teacher, GBID, CSID, grade;
	private int period;
	
	public CourseGrades(String cou, String tea, int per, String grd, String gbid, String csid) {
		course = cou;
		period = per;
		teacher = tea;
		grade = grd;
		GBID = gbid;
		CSID = csid;
	}
	
	public String getGrade() {
		return grade;
	}

	public int getPeriod() {
		// TODO Auto-generated method stub
		return period;
	}

	public String getCourse() {
		// TODO Auto-generated method stub
		return course;
	}

	public String getGBID() {
		// TODO Auto-generated method stub
		return GBID;
	}
	
	public String getCSID() {
		return CSID;
	}

	public String getTeacher() {
		// TODO Auto-generated method stub
		return teacher;
	}
}
