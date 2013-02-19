package com.lisd.ultraward;

import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CourseParser {
	final String assignmentsXPath = "//table[@class='w100']/tbody/tr";

	private String html;
	private HtmlCleaner cleaner;
	
	public CourseParser(String gbhtml) {
		html = gbhtml;
		cleaner = new HtmlCleaner();
		
		// Lighten up the load a little
		int stt = html.indexOf("<table border='0' cellpadding='1' cellspacing='0' class='w100'>");
		
		if(stt > 0) {
			int end = html.indexOf("</table><div id='GradeMarkDiv", stt);
			if(end > 0 && end > stt) {
				html = html.substring(stt, end + 8);
				System.out.println("<: " + stt + " " + end + " "
						+ html.substring(html.length() - 1));
			}
		}
	}
	
	public List<AssignmentGrade> getDumpOfGrades() {
		TagNode node = cleaner.clean(html);
		Object[] foundList;
		List<AssignmentGrade> results = new ArrayList<AssignmentGrade>();
		
		try {
			foundList = node.evaluateXPath(assignmentsXPath);
		} catch (XPatherException e) {
			e.printStackTrace();
			return results;
		}
		
		if(foundList.length == 0) {
			return results;
		}
		
		System.out.println("Found " + foundList.length + " assignment grades.");
		
		for(Object fnd : foundList) {
			TagNode found = (TagNode)fnd;
			String cls = found.getAttributeByName("class");
			if(cls == null)
				continue;
			
			int grade = -1;
			String name = "";
			float percentage = -1;
			
			TagNode[] teedees = found.getElementsByName("td", false);
			
			// all possible row types
			if(cls.equals("element") || cls.equals("themeMedium b") 
					|| cls.equals("bgw") || cls.equals("themeLight")
					|| cls.equals("themeDark b")) {
				for(TagNode tn : teedees) {
					Pattern p = Pattern.compile("(\\d+)&nbsp;");
					Matcher m = p.matcher(tn.getText().toString().trim());
					if(m.matches()) {
						grade = Integer.parseInt(m.group(1));
						break;
					} 
				}
			} else {
				continue;
			}
			if(cls.equals("bgw") || cls.equals("themeLight")) { // simple grade	
				if(teedees.length >= 4) {
					name = teedees[1].getText().toString().replace("&nbsp;", "");
					percentage = -1;
				} else {
					System.out.println("Not a real simple grade!");
					continue;
				}
			} else if(cls.equals("element") || cls.equals("themeMedium b")) { // grade category
				// element == major, themeMedium b == minor
				TagNode withNameAndPercent = null;
				
				for(TagNode t : teedees) {
					if(t.hasAttribute("colspan")) {
						withNameAndPercent = t;
						break;
					}
				} if(withNameAndPercent == null) {
					break;
				}
				
				Pattern p;
				if(cls.equals("themeMedium b")) // minor category
					p = Pattern.compile("^(.+) \\(([\\d\\.]+)%");
				else  // major category, remove some random stuff at the end
					p = Pattern.compile("^(.+) - [\\d\\w]+ \\(([\\d\\.]+)%");
				
				Matcher m = p.matcher(withNameAndPercent.getText().toString()
						.replace("&nbsp;", " ").trim());
				if(m.find()) {
					name = m.group(1);
					percentage = Float.parseFloat(m.group(2));
				} else {
					System.out.println("Not a real category!");
					continue;
				}
			}
			
			if(cls.equals("element")) {
				results.add(new AssignmentGrade(name, grade, percentage, 
						AssignmentGrade.AssignmentType.MAJOR_CATEGORY));
			} else if(cls.equals("themeMedium b")) {
				results.add(new AssignmentGrade(name, grade, percentage, 
						AssignmentGrade.AssignmentType.MINOR_CATEGORY));
			} else if(cls.equals("themeDark b")) {
				results.add(new AssignmentGrade("Current Course Grade", grade, percentage, 
						AssignmentGrade.AssignmentType.COURSE_GRADE));
			} else {
				results.add(new AssignmentGrade(name, grade, percentage,
						AssignmentGrade.AssignmentType.ASSIGNMENT));
			}
		}
			
		return results;
	}
}
