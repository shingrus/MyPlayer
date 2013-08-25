package com.shingrus.myplayer;

import android.app.Application;
import android.content.Intent;


public class MyPlayerApplication extends Application {

	@Override
	public void onCreate() {
		
		
		MyPlayerPreferences.getInstance(getApplicationContext());
		TrackList.getInstance().loadTracks(getApplicationContext());
		startService(new Intent(this, MusicPlayerService.class));
		
		startService(new Intent(this, UpdateService.class));
		
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		stopService(new Intent(this, UpdateService.class));
		super.onTerminate();
		
	}
	

}
