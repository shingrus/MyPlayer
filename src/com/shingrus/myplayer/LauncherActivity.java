package com.shingrus.myplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		MyPlayerPreferences mpp = MyPlayerPreferences.getInstance(null);
		Intent i;
		if (mpp.getEmail() != null && mpp.getEmail().length() > 0) {
			i = new Intent(this, MyPlayerActivity.class);
		} else {
			i = new Intent(this, MyAuthorizeActivity.class);
		}
		startActivity(i);
		finish();
		super.onCreate(savedInstanceState);
	}

}
