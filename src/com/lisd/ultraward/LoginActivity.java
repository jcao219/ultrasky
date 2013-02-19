package com.lisd.ultraward;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
    /**
     * The default email to populate the email field with.
     */
    public static final String NO_AUTO_LOGIN = "com.lisd.ultraward.extra.noautologin";
    public static final String EXTRA_HTML_GRADES = "com.lisd.ultraward.extra.html.grades";
    
    public static final String LOGIN_PREFS = "UltraSky_LoginPrefs";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private static UserLoginTask mAuthTask = null;
    private boolean tried_autologging_in = false;
    
    private SkywardAuthenticator auth;

    // Values for email and password at the time of the login attempt.
    private String mUser;
    private String mPassword;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
	private static String lastMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        
        auth = SkywardAuthenticator.getInstance(this);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        
        SharedPreferences prefs = getSharedPreferences(LOGIN_PREFS, 0);
        mEmailView.setText(prefs.getString("login", ""));
        
        mPasswordView.setText(prefs.getString("pass", ""));        
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        
        if(!mEmailView.getText().toString().isEmpty() 
        		&& !getIntent().getBooleanExtra(NO_AUTO_LOGIN, false)
        		&& !tried_autologging_in) {
        	tried_autologging_in = true;
        	getIntent().putExtra(NO_AUTO_LOGIN, true);
        	attemptLogin();
        } else if (mAuthTask != null) {
        	mAuthTask.setContext(this);
            showProgress(true);
            mLoginStatusMessageView.setText(lastMessage);
        } else {
        	mLoginFormView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }
    
    @Override
    public void onResume() {
    	if(mAuthTask == null) {
	    	mLoginStatusMessageView.setText("Signing in...");
	    	mLoginStatusView.setVisibility(View.GONE);
	        mLoginFormView.setVisibility(View.VISIBLE);
	        mLoginFormView.setAlpha(1);
    	}
        super.onResume();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mUser = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } 
        // Check for a valid email address.
        if (TextUtils.isEmpty(mUser)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } 

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask(this);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
    	
    	if(show == true) {
    		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    	}
    	
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
    	private String temp;
    	private String gradesHtml;
    	
    	private LoginActivity act;
    	
    	public UserLoginTask(LoginActivity loginActivity) {
			act = loginActivity;
		}

		public void setContext(LoginActivity ctx) {
    		act = ctx;
    	}
    	
		@Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Try to logon to Skyward app
            	lastMessage = "Signing in...";
            	
                auth.login(mUser, mPassword);
                
                act.runOnUiThread(new Runnable () {
                	public void run() {
                		act.mLoginStatusMessageView.setText("Fetching grades...");
                		lastMessage = "Fetching grades...";
                	}
                });
                
                gradesHtml = auth.getGrades();
                return true;
            } catch (Exception e) {
            	temp = e.getMessage();
            	e.printStackTrace();
                return false;
            }
        }

		@Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
            	act.mLoginStatusMessageView.setText("Signed in!");
            	
            	SharedPreferences prefs = getSharedPreferences(LOGIN_PREFS, 0);
            	SharedPreferences.Editor editor = prefs.edit();
            	
            	editor.putString("login", mUser);
            	editor.putString("pass", mPassword);
            	editor.commit();
            	
            	auth.saveLogin(); // TODO: make this optional.
            	// without this login saved, Gradebook activity
            	// won't attempt to do anything with regards to the
            	// grade change watcher.
            	
                Intent intent = new Intent(act, Gradebook.class);
                intent.putExtra(EXTRA_HTML_GRADES, gradesHtml);
                intent.putExtra(Gradebook.EXTRA_REFRESH_GRADES, true);
                
                System.out.println("Starting the gradebook activity!");
                act.startActivity(intent);
            } else {
            	act.showProgress(false);
                act.mPasswordView.setError(temp);
                act.mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            act.showProgress(false);
        }
    }
}
