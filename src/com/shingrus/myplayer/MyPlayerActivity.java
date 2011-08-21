package com.shingrus.myplayer;

import com.shingrus.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

public class MyPlayerActivity extends Activity {
	
	TrackList trackList;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackList = TrackList.getInstance();
		setContentView(R.layout.playlist);
		ListView lv = (ListView)findViewById(R.id.playListView);
		lv.setAdapter(trackList.getAdapter(this));
		
		
	}

	@Override
	protected void onDestroy() {
		if (trackList != null) 
			trackList.dropAdapter();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.MenuPreferencesItem: {
			Intent i = new Intent(this, MyPlayerPreferencesActivity.class);
			startActivity(i);
			break;
		}
		}
		return true;
	}

}