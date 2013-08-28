package com.shingrus.myplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		MyPlayerPreferences mpp = MyPlayerPreferences.getInstance(null);
		Intent i;
		if (mpp.isHasProfile()) {
			Intent service = new Intent(this, UpdateService.class);
			service.putExtra(UpdateService.START_UPDATE_COMMAND, UpdateService.START_UPDATE_COMMAND_TIMER);
			startService(service);
			i = new Intent(this, MyPlayerActivity.class);
		} else {
//			i = new Intent(this, MyAuthorizeActivity.class);
			i = new Intent(this, OAuthWebAuthorization.class);
			i.putExtra(MyPlayerPreferences.OAUTH_URL, MailRuProfile.OAUTH_URL);
		}
		startActivity(i);
		finish();
		super.onCreate(savedInstanceState);
	}

}
