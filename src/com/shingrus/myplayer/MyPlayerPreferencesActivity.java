package com.shingrus.myplayer;

import com.shingrus.myplayer.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MyPlayerPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onDestroy() {

		// TODO:
		// update MyPlayerPreferences
		// update our service to get new authorize info
		//
		MyPlayerPreferences mpp = MyPlayerPreferences.getInstance(this);
		if (mpp != null)
			mpp.updatePreferences(this);
		super.onDestroy();
	}

}
