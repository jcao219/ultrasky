package com.lisd.ultraward;

import java.util.List;

public class AssignmentGrade {
	
	int grade;
	String name;
	List<AssignmentGrade> children;
	float percentageOfParent;
	AssignmentType type;
	
	public AssignmentGrade(String name, int grade, float percent, AssignmentType type) {
		this.grade = grade;
		this.name = name;
		this.percentageOfParent = percent;
		
		this.type = type;
	}
	
	String GetName() {
		return name;
	}
	
	int GetGrade() {
		return grade;
	}
	
	void AddChild(AssignmentGrade agr) {
		children.add(agr);
	}
	
	public enum AssignmentType {
		MAJOR_CATEGORY, MINOR_CATEGORY, ASSIGNMENT, COURSE_GRADE
	}

	public AssignmentType GetType() {
		return this.type;
	}
	
	class GradeChangeInfo {
		int oldgrade;
		GradeChangeType type;
		
		// if olfgrade == -1, then it's a new one
		private GradeChangeInfo(GradeChangeType type, int oldgrade) {
			this.type = type;
			this.oldgrade = oldgrade;
		}
		
		public int getOldGrade() {
			return oldgrade;
		}
		
		public GradeChangeType getType() {
			return type;
		}
		
		public AssignmentGrade getAssignment() {
			return AssignmentGrade.this;
		}
	}
	
	// -1 if it's a new grade
	public GradeChangeInfo makeChangeInfo(int oldgrade) {
		if(oldgrade == -1)
			return new GradeChangeInfo(GradeChangeType.NEW_GRADE, oldgrade);
		else
			return new GradeChangeInfo(GradeChangeType.CHANGED_GRADE, oldgrade);
	}
	
	public enum GradeChangeType {
		NEW_GRADE, CHANGED_GRADE;
	}

	public float GetPercentageOfParent() {
		return percentageOfParent;
	}
}
