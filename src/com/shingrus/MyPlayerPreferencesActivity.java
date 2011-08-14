package com.shingrus;

import com.shingrus.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MyPlayerPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
