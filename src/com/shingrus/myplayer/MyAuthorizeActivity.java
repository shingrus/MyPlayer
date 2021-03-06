package com.shingrus.myplayer;

import com.shingrus.myplayer.MyPlayerAccountProfile.AuthorizeStatus;

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

	
	
	private MailRuAuthorization mailAuthorize;
	private boolean authorizationInProgress = false;

	class MailRuAuthorization extends AsyncTask<String, Void, AuthorizeStatus> {

		ProgressDialog progressDialog;
		String login, password, refreshToken, accessToken;
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
		protected AuthorizeStatus doInBackground(String... params) {
			AuthorizeStatus result = AuthorizeStatus.UNKNOWN;
			login = params[0];
			password = params[1];

			result = mpf.getProfile().authorize(login, password);
			
			if (result == AuthorizeStatus.SUCCESS) {
				mpf.setHasProfile(true);
			}
			return result;
		}

		@Override
		protected void onPostExecute(AuthorizeStatus result) {
			this.progressDialog.dismiss();
			if (result == AuthorizeStatus.SUCCESS) {
				Intent service = new Intent(MyAuthorizeActivity.this, UpdateService.class);
				service.putExtra(UpdateService.START_UPDATE_COMMAND, UpdateService.START_UPDATE_COMMAND_TIMER);
				startService(service);
				
				mpf.storePreferences(MyAuthorizeActivity.this);
				Intent i = new Intent(MyAuthorizeActivity.this, MyPlayerActivity.class);
				startActivity(i);
			
				finish();
			}
			else if (result == AuthorizeStatus.INVALID) {
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
		if(authorizationInProgress && mailAuthorize != null) {
			mailAuthorize.cancel(true);
		}
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
