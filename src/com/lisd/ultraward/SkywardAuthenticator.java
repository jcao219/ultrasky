/**
 * A thing to do HTTP POST onto the Skyward login thing
 */
package com.lisd.ultraward;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

/**
 * @author Jimmy Cao
 *
 */
public class SkywardAuthenticator {
	private static SkywardAuthenticator instance = null;
	
	private Context context;
	private SSLContext ctx;
	private String user = "";
	private String dwd = ""; // security stuff for Skyward, values determined upon successful login
	private String wfaacl = "";
	private String nameid = ""; // skyward internal id of student
	private String pass = ""; // stored in case if need re login

	private boolean loginSaved = false;
	
	protected SkywardAuthenticator(Context c) {
		this.context = c;
		
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] {
					new X509TrustManager() {
						public void checkClientTrusted(X509Certificate[] chain, String authType) {}
					    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
					    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
					}
			}, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
		
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			  public boolean verify(String hostname, SSLSession session) {
				  return true;
			  }
		});
		
		CookieManager cm = new CookieManager();
		CookieHandler.setDefault(cm);
	}
	
	protected void setContext(Context c) {
		context = c;
	}
	
	public static SkywardAuthenticator getInstance(Context c) {
		// as a singleton
		if(instance == null) {
			instance = new SkywardAuthenticator(c);
			return instance;
		} else {
			instance.setContext(c);
			return instance;
		}
	}
	
	public void saveLogin() {
		loginSaved  = true;
	}
	
	public void login(String login, String passw) throws Exception {
		user = login;
		pass = passw;
		
		Resources ress = context.getResources();
		
		String loginurl = ress.getString(R.string.login_url);
		

		String param = ress.getString(R.string.login_post_params);
		param = param.replaceAll("%LOGIN%", URLEncoder.encode(login, "UTF-8"));  // let's put in the user's credentials
		param = param.replaceAll("%PASSWORD%", URLEncoder.encode(passw, "UTF-8"));
		
		System.out.println("Accessing login URL...");
		
		String response = accessUrl(loginurl, param);
		
		if(response.startsWith("<li>Invalid login"))
			throw new Exception("Invalid login or password.");
		
		String[] splitted = response.substring(4).split("\\^");
		
		if(splitted.length < 4)
			throw new Exception("Malformed response from Skyward");
		
		dwd = URLEncoder.encode(splitted[0], "UTF-8");
		wfaacl = URLEncoder.encode(splitted[3], "UTF-8");
		nameid = URLEncoder.encode(splitted[4], "UTF-8");
	}

	public String getGrades() throws IOException {
		// access Skyward gradebook using the security from login
		Resources ress = context.getResources();
		String params = ress.getString(R.string.gradebook_post_params);
		params = params.replaceAll("%DWD%", dwd).replaceAll("%WFAACL%", wfaacl);
		params = params.replaceAll("%NAMEID%", nameid).replaceAll("%LOGIN%", user);
		
		System.out.println("Getting grades...");
		
		String result = accessUrl(ress.getString(R.string.grades_url), params);  // big http gradebook
		
		System.out.println("Fetched gradebook html...");
		return result;
	}
	
	public String getCourseGrades(String gbid, String csid, String sem) throws IOException {
		Resources ress = context.getResources();
		
		String params = ress.getString(R.string.course_grades_post_params);
		params = params.replaceAll("%DWD%", dwd).replaceAll("%WFAACL%", wfaacl);
		params = params.replaceAll("%NAMEID%", nameid).replaceAll("%LOGIN%", user);
		params = params.replaceAll("%GBID%", gbid).replaceAll("%CSID%", csid);
		params = params.replaceAll("%SEM%", sem);
		
		return accessUrl(ress.getString(R.string.course_grades_url), params);
	}
	
	public String accessUrl(String url, String params) throws IOException {
		// do POST with a url and return the results
		URL grades_url = new URL(url);
		
		HttpsURLConnection conn = (HttpsURLConnection)grades_url.openConnection();
		
		conn.setDoOutput(true);
	    conn.setDoInput(true);
		
		conn.setRequestMethod("POST");
		
		conn.setFixedLengthStreamingMode(params.getBytes().length);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		OutputStream os = conn.getOutputStream();
		PrintWriter out = new PrintWriter(os);
		out.print(params);
		out.flush();
		out.close();
		
		conn.setSSLSocketFactory(ctx.getSocketFactory());
		
		InputStreamReader in = new InputStreamReader(conn.getInputStream(), "UTF-8");
		StringBuilder response = new StringBuilder();
		
		int BUF_SIZE = 16384;
		char[] buffer = new char[BUF_SIZE];
		
		int ret;
		while((ret = in.read(buffer)) > 0) {
			response.append(buffer, 0, ret);
		}
		in.close();
		
		conn.disconnect();
		
		return response.toString();
	}

	public String getLoginIfSaved() {
		if(loginSaved)
			return user;
		else
			return "";
	}

	public void reLogin() throws Exception {
		if(user.isEmpty()) {
			SharedPreferences sp = context.getSharedPreferences(LoginActivity.LOGIN_PREFS, 0);
			user = sp.getString("login", "");
			pass = sp.getString("pass", "");
		}
		if(user.isEmpty()) { // .isStillEmpty()
			throw new Exception("No saved login or password available.");
		}
		login(user, pass);
	}
}
