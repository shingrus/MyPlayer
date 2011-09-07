package com.shingrus.myplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

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

		public MailRuAuthorization() {
			super();
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

			 mpopCookie = MailRuSpecific.authorizeOnMailRu(login, password);
			if (mpopCookie != null && mpopCookie.length() > 0) {
				result = AUTHORIZE_RESULT_SUCCESS;
			}
			return result;
		}

		@Override
		protected void onPostExecute(Integer result) {
			this.progressDialog.dismiss();
			if (result == AUTHORIZE_RESULT_SUCCESS && mpopCookie!= null) {
				//TODO set login and password
				MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(MyAuthorizeActivity.this);
				mpf.setMpopCookie(mpopCookie);
				mpf.setEmail(login);
				mpf.setPassword(password);
				//Start service
				Intent i = new Intent(MyAuthorizeActivity.this, MyPlayerActivity.class);
				startActivity(i);
				finish();
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
			mailAuthorize.execute(login, password);
		}
	}
}