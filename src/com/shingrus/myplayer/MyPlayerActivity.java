package com.shingrus.myplayer;

import com.shingrus.myplayer.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

public class MyPlayerActivity extends Activity {
	
	TrackList trackList;
	ServiceConnection musicPlayerConnection;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackList = TrackList.getInstance();
		musicPlayerConnection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {

			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				
			}
		};

		this.bindService(new Intent(this, MusicPlayerService.class),
				musicPlayerConnection, Context.BIND_AUTO_CREATE);
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