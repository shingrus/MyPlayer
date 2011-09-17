package com.shingrus.myplayer;

import com.shingrus.myplayer.MyPlayerAccountProfile.TrackListFetchingStatus;
import com.shingrus.myplayer.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MyPlayerActivity extends Activity {

	TrackList trackList;
	ServiceConnection musicPlayerConnection;
	MusicPlayerService playerService;
	final Handler handleUpdate = new Handler();
	MyPlayerPreferences mpf;
	TrackListFetchingStatus updateStatus;
	
	boolean updateInProgress = false;
	Thread updateThread;
	
	final Runnable resultUpdate = new Runnable() {
		
		@Override
		public void run() {
			updateInProgress = false;
			updateThread = null;
		}
	};
	
	public MyPlayerActivity() {
		trackList = TrackList.getInstance();
		musicPlayerConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				playerService = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder binder) {
				playerService = ((MusicPlayerService.LocalBinder) binder).getService();
			}
		};
		mpf = MyPlayerPreferences.getInstance(null);

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("shingrus", "Creating Player Activity");
		this.bindService(new Intent(this, MusicPlayerService.class), musicPlayerConnection, Context.BIND_AUTO_CREATE);
		setContentView(R.layout.playlist);
		ListView lv = (ListView) findViewById(R.id.playListView);
		lv.setAdapter(trackList.getAdapter(this));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				if (playerService != null) {
					playerService.startPlayFrom(position);
				}

			}

		});

	}

	@Override
	protected void onDestroy() {
		Log.i("shingrus", "Destroying Player activity");
		if (trackList != null)
			trackList.dropAdapter();
		if (musicPlayerConnection != null) {
			unbindService(musicPlayerConnection);
			musicPlayerConnection = null;
		}
		if (updateThread != null) {
			updateThread.interrupt();
			updateThread=null;
		}
		updateInProgress = false;
		
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
		case R.id.MenuUpdateItem: {
			// TODO start AsyncTask for update
			// i decided to use handler for learning purpose
			if (!updateInProgress) {
				updateInProgress = true;
				updateThread = new Thread() {
					public void run() {
						MyPlayerActivity.this.updateStatus = mpf.getProfile().getTrackListFromInternet(TrackList.getInstance(), mpf.getMpopCookie());
						handleUpdate.post(resultUpdate);
					}
				};
				updateThread.start();
			}
			break;
		}
		}
		return true;
	}

	// Click Listeners
	public void onClickPlayPause(View v) {
		playerService.playPause();
	}

	public void onClickForward(View v) {
		playerService.playNext();

	}

	public void onClickRewind(View v) {
		playerService.playPrevious();
	}

	public void onClickStop(View v) {
		playerService.stopMusic();
	}

}