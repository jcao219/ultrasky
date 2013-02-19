/**
 * 
 */
package com.lisd.ultraward;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

/**
 * @author jimmy
 *
 */
public class GradebookHtmlParser {
	// XPath to find node, then regex to get the numbers we need
	
	private String htmlGradebook;
	private HtmlCleaner cleaner;
	
	public int Semester;
	
	final String neededXPath = "//table[@class='myBox']//td[@style='border-style:none;']"
			+ "//span[@style='color:gray;']";
	final String unneededXPath = "//table[@class='myBox']//tr[@id='nonCurrentClass']";
		//	+ "//td[@style='border-style:none;']//span[@style='color:gray;']";
	
	final String neededXPathForSemesterInfo = "//table[@class='myBox']/tbody/tr/td[@style='height:25px; "
			+ "line-height:25px; border-top:1px solid black;']";
	
	public GradebookHtmlParser(String html) {
		htmlGradebook = html;
		cleaner = new HtmlCleaner();
	}
	
	public void setHtml(String html) {
		htmlGradebook = html;
	}
	
	public List<CourseGrades> getCurrentGrades() {
		System.out.println("Start parsing gradebook html...");
		TagNode node = cleaner.clean(htmlGradebook);
		Object[] foundBadList;
		try {
			foundBadList = node.evaluateXPath(unneededXPath);
		} catch (XPatherException e) {
			e.printStackTrace();
			return new ArrayList<CourseGrades>();
		}
		for(Object bad : foundBadList) {
			TagNode nd = (TagNode)bad;
			if(nd != null)
				nd.removeFromTree();
		}
		Object[] foundList;
		try {
			foundList = node.evaluateXPath(neededXPath);
		} catch (XPatherException e) {
			e.printStackTrace();
			return new ArrayList<CourseGrades>();
		}
		if(foundList == null || foundList.length < 1) {
		    System.out.println("Found nothing, buddy..");
			return new ArrayList<CourseGrades>();
		}
		
		System.out.printf("Found %d classes\n", foundList.length);

		List<CourseGrades> res = new ArrayList<CourseGrades>();
		Pattern pat1 = Pattern.compile("Grade: +(<B>)?(\\d+)", Pattern.CASE_INSENSITIVE);
		Pattern pat2 = Pattern.compile("javascript:gradebook"
				+ "\\.showClassDetails\\(\"(\\d+)\",\"(\\d+)\"\\)");

		for(Object found : foundList) {
			TagNode nodnod = (TagNode)found;
			Matcher mat = pat1.matcher(nodnod.getText());
			String grade = "N/A";
			if(mat.find()) {
				grade = mat.group(2);
			}
			
			StringBuffer partext = nodnod.getParent().getText();
			String period = Character.toString(partext.charAt(0));
			
			int perd;
			try {
				perd = Integer.parseInt(period);
			} catch (NumberFormatException e) {
				perd = 0;
			}
			
			int endcindex = partext.indexOf("Teacher:");
			
			String cours = partext.substring(2, endcindex > 0 ? endcindex : 5);
			
			int endtindex = partext.indexOf("Term:");
			
			String teacher = "";
			
			if(endcindex > 0 && (endtindex - endcindex) > 8) {
				teacher = partext.substring(endcindex + 8, endtindex);
			}
			
			TagNode dtld = nodnod.getParent().getParent().getParent().getParent().getParent();
			
			String gbid = "", csid = "";
			mat = pat2.matcher(dtld.getAttributeByName("onclick"));
			if(mat.find()) {
				gbid = mat.group(1);
				csid = mat.group(2);
			}
							
			CourseGrades cgrd = new CourseGrades(cours.trim(), teacher.trim(), 
											     perd, grade.trim(), gbid, csid);
			res.add(cgrd);
		}
		
		// time to find out which semester we be in!
		System.out.println("Start parsing semester info...");
		
		Object[] foundSemInfo;
		try {
			foundSemInfo = node.evaluateXPath(neededXPathForSemesterInfo);
		} catch (XPatherException e) {
			e.printStackTrace();
			return res;
		}		
		if(foundSemInfo == null || foundSemInfo.length < 1) {
		    System.out.println("Found no sem info, buddy..");
			return res;
		}
		TagNode[] boxes = ((TagNode)foundSemInfo[0]).getParent().getElementsByName("td", false);
		
		int howFarIntoYear = 0;
		
		for(int i = 1; i < boxes.length; i++) { // skip the first rectangle box
			TagNode[] bolded = boxes[i].getElementsByName("b", false);
			TagNode realThing;
			if(bolded.length > 0) {
				realThing = bolded[0];
			} else {
				realThing = boxes[i];
			}
			String text = realThing.getText().toString().replaceAll("&nbsp;", "");
			if(text.trim().isEmpty()) {
				continue;
			}
			
			howFarIntoYear = i;  // This box has been filled with a grade!
		}

		Semester = 1;
		if(howFarIntoYear >= 10)
			Semester = 2;
		if(howFarIntoYear >= 15)
			Semester = 3;
		if(howFarIntoYear >= 20)
			Semester = 4;
		
		System.out.println("Finished parsing html...");
		
		return res;
	}
}
