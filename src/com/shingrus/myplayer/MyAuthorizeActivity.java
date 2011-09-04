package com.shingrus.myplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public class MyAuthorizeActivity extends Activity {

	public static final int AUTHORIZE_RESULT_SUCCESS = 103;
	public static final int AUTHORIZE_RESULT_INVALID_PASS = 105;
	public static final int AUTHORIZE_RESULT_NETWORK_ERROR = 106;
	public static final int AUTHORIZE_RESULT_NETWORK_CANCELED = 107;
	
	private MailRuAuthorization ma = new MailRuAuthorization();
   
	class MailRuAuthorization extends AsyncTask<String, Void, Integer> {

		ProgressDialog progressDialog; 
		
		public MailRuAuthorization() {
			super();
		}
		
		@Override
		protected void onPreExecute() {
			this.progressDialog = new ProgressDialog(MyAuthorizeActivity.this);
			this.progressDialog.setMessage("Lalala");
			this.progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					MyAuthorizeActivity.this.ma.cancel(true);
				}
			});
			this.progressDialog.show();
		}


		@Override
		protected Integer doInBackground(String... params ) {
			int result = AUTHORIZE_RESULT_NETWORK_ERROR ;
			String login = params[0]; 
			String password = params[1];
			try {
				Thread.sleep(10*1024);
			} catch (InterruptedException e) {
				result = AUTHORIZE_RESULT_NETWORK_CANCELED;	
			}
			return result;
		}

		@Override
		protected void onPostExecute(Integer result) {
			this.progressDialog.dismiss();
			finish();
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
		ma.execute("", "");
	}
}
