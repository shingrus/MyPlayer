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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MyPlayerActivity extends Activity {

	TrackList trackList;
	ServiceConnection musicPlayerConnection;
	MusicPlayerService playerService;
	final Handler handleUpdate = new Handler();
	MyPlayerPreferences mpf;
	TrackListFetchingStatus updateStatus;
	private ListView lv = null;
	private ProgressBar pb = null;

	boolean updateInProgress = false;
	Thread updateThread;

	final Runnable resultUpdate = new Runnable() {

		@Override
		public void run() {
			updateInProgress = false;
			updateThread = null;
			if (pb!=null) {
				pb.setVisibility(View.INVISIBLE);
			}
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
				playerService.setEventsListener(new PlayingEventsListener() {

					@Override
					public void onStop() {
					}

					@Override
					public void onPlay() {
					}

					@Override
					public void onPause() {
					}

					@Override
					public void onChangePlayingItem(int position) {
						// int pos = position - lv.getFirstVisiblePosition();
						// pos /=2;
						// lv.setSelection(pos);
					}

					@Override
					public void onPlayedPosition(int playedDurationSecs) {
						MyPlayerActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
							}
						});
					}
				});
			}
		};
		mpf = MyPlayerPreferences.getInstance(null);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//		setProgressBarIndeterminateVisibility(true);
		boolean isCustomTitileSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		Log.d("shingrus", "Creating Player Activity");
		Intent i = new Intent(this, MusicPlayerService.class);
		this.bindService(i, musicPlayerConnection, Context.BIND_AUTO_CREATE);
		setContentView(R.layout.playlist);
		
		if (isCustomTitileSupported) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.playertitle);
		}
		
		pb = (ProgressBar) findViewById(R.id.playerTitleProgressId);
		lv = (ListView) findViewById(R.id.playListView);
		lv.setAdapter(trackList.getAdapter(this));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				if (playerService != null) {
					playerService.startPlayFrom(position);
				}
			}
		});
		
		// SeekBar sb = (SeekBar) findViewById(R.id.playingSeek);
		// sb.setClickable(false);
		// sb.setOnTouchListener(new View.OnTouchListener() {
		// @Override
		// public boolean onTouch(View v, MotionEvent event) {
		// return true;
		// }
		// });
	}

	@Override
	protected void onResume() {
		if (trackList.isEmpty()) {
			startUpdate();
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.i("shingrus", "Destroying Player activity");
		if (trackList != null)
			trackList.dropAdapter();
		if (musicPlayerConnection != null) {
			if (playerService != null)
				playerService.unsetEventsListener();
			unbindService(musicPlayerConnection);
			musicPlayerConnection = null;
		}
		if (updateThread != null) {
			updateThread.interrupt();
			updateThread = null;
		}
		updateInProgress = false;
		lv = null;
		pb = null;
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
		Intent i;
		switch (item.getItemId()) {
		case R.id.MenuPreferencesItem: 
			i = new Intent(this, MyPlayerPreferencesActivity.class);
			startActivity(i);
			break;
		case R.id.MenuUpdateItem: 
			startUpdate();
			break;
		case R.id.MenuHandshakeItem: 
			// TODO start authorize activity again
			i = new Intent(this, MyAuthorizeActivity.class);
			startActivity(i);
			break;
		}
		return true;
	}

	private void startUpdate() {
		// TODO start AsyncTask for update
		// i decided to use handler for learning purpose
		if (!updateInProgress) {
			updateInProgress = true;
			if (pb!=null) {
				pb.setVisibility(View.VISIBLE);
			}
			updateThread = new Thread() {
				public void run() {
					MyPlayerActivity.this.updateStatus = mpf.getProfile().getTrackListFromInternet();
					handleUpdate.post(resultUpdate);
				}
			};
			updateThread.start();
		}
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