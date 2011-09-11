package com.shingrus.myplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MyAuthorizeActivity extends Activity {

	public static final int AUTHORIZE_RESULT_SUCCESS = 103;
	public static final int AUTHORIZE_RESULT_INVALID_PASS = 105;
	public static final int AUTHORIZE_RESULT_NETWORK_ERROR = 106;
	public static final int AUTHORIZE_RESULT_NETWORK_CANCELED = 107;
	
	private MailRuAuthorization mailAuthorize;
	private boolean authorizationInProgress = false;

	class MailRuAuthorization extends AsyncTask<String, Void, Integer> {

		ProgressDialog progressDialog;
		String login, password, mpopCookie;
		MyPlayerPreferences mpf;

		public MailRuAuthorization() {
			super();
			mpf= MyPlayerPreferences.getInstance(null);
		}

		@Override
		protected void onPreExecute() {
			this.progressDialog = new ProgressDialog(MyAuthorizeActivity.this);
			this.progressDialog.setMessage(getResources().getString(R.string.LoginPassword_SpinnerDialogText)); 
			this.progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					MyAuthorizeActivity.this.mailAuthorize.cancel(true);
				}
			});
			this.progressDialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
			int result = AUTHORIZE_RESULT_NETWORK_ERROR;
			login = params[0];
			password = params[1];

			 mpopCookie = mpf.getProfile().authorize(login, password);
			if (mpopCookie != null && mpopCookie.length() > 0) {
				result = AUTHORIZE_RESULT_SUCCESS;
			}
			else {
				result = AUTHORIZE_RESULT_INVALID_PASS;
			}
			return result;
		}

		@Override
		protected void onPostExecute(Integer result) {
			this.progressDialog.dismiss();
			if (result == AUTHORIZE_RESULT_SUCCESS && mpopCookie!= null) {
				mpf.setMpopCookie(mpopCookie);
				mpf.setLogin(login);
				mpf.setPassword(password);
				mpf.store(MyAuthorizeActivity.this);
				Intent i = new Intent(MyAuthorizeActivity.this, MyPlayerActivity.class);
				startActivity(i);
				finish();
			}
			else if (AUTHORIZE_RESULT_INVALID_PASS == result) {
				TextView t = (TextView) findViewById(R.id.LoginPassword_ErrorMsg);
				t.setVisibility(View.VISIBLE);
			}

			authorizationInProgress = false;
		}

		@Override
		protected void onCancelled() {
			authorizationInProgress = false;
			super.onCancelled();
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.loginpassword);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	// Click Listeners
	public void onClickLogin(View v) {
		if (!authorizationInProgress) {
			authorizationInProgress = true;
			mailAuthorize = new MailRuAuthorization();
			String login = ((EditText) findViewById(R.id.LoginPassword_LoginId)).getText().toString();
			String password = ((EditText) findViewById(R.id.LoginPassword_PasswordId)).getText().toString();
			TextView t = (TextView) findViewById(R.id.LoginPassword_ErrorMsg);
			t.setVisibility(View.INVISIBLE);
			mailAuthorize.execute(login, password);
		}
	}
}
