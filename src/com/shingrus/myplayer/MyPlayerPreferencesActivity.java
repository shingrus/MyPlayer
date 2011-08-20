package com.shingrus.myplayer;

import com.shingrus.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MyPlayerPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lv = (ListView)findViewById(R.id.playListView);
		
		addPreferencesFromResource(R.xml.preferences);
	}


	@Override
	protected void onDestroy() {
		
		//TODO: 
		// update MyPlayerPreferences
		// update our service to get new authorize info
		//
		MyPlayerPreferences mpp = MyPlayerPreferences.getInstance(this);
		if (mpp != null) 
			mpp.loadPreferences(this);
		super.onDestroy();
	}

}
