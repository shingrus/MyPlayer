package com.shingrus.myplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.shingrus.myplayer.MyPlayerAccountProfile.TrackListFetchingStatus;
import com.shingrus.myplayer.UpdateService.UpdatesHandler;

public class MyPlayerActivity extends Activity {

	TrackList trackList;
	ServiceConnection musicPlayerConnection, updateServiceConnection;
	MusicPlayerService playerService = null;
	UpdateService updateService = null;
	final UpdateHandler updateHandler = new UpdateHandler();
	MyPlayerPreferences mpf;
	TrackListFetchingStatus lastUpdateStatus;
	private ListView lv = null;
	private ProgressBar rCornerProgressBar = null;
	private ImageView alarmImage = null;
	SeekBar progressBar = null;
	ImageButton playButton = null;
	boolean isPlaying = false;
	boolean touchingProgress = false;
	int newPosition = 0;

	Handler handleProgressUpdate = null;

	final Runnable resultUpdate = new Runnable() {

		@Override
		public void run() {
			if (MyPlayerActivity.this.rCornerProgressBar != null) {
				MyPlayerActivity.this.rCornerProgressBar.setVisibility(View.INVISIBLE);
			}

			if (alarmImage != null) {
				int visibility = View.INVISIBLE;
				// for future
				switch (lastUpdateStatus) {
				case NEEDREAUTH:
					visibility = View.VISIBLE;
					break;
				}
				;
				alarmImage.setVisibility(visibility);
			}
		}
	};

	final Runnable progressUpdateJob = new Runnable() {
		@Override
		public void run() {
			if (playerService != null && progressBar != null) {
				if (!touchingProgress) {
					int playedProgress = playerService.getCurrentPosition();
					progressBar.setProgress(playedProgress);
					// Log.d("shingrus", "Got postion update: " +
					// playedProgress);
				}
				if (isPlaying)
					handleProgressUpdate.postDelayed(progressUpdateJob, MyPlayerPreferences.UPDATE_PLAYING_STATUS_MS);
			}
		}
	};

	class UpdateHandler implements UpdatesHandler {

		@Override
		public void onAfterUpdate(TrackListFetchingStatus updateStatus) {
			MyPlayerActivity.this.lastUpdateStatus = updateStatus;
			runOnUiThread(resultUpdate);
		}

		@Override
		public void onBeforeUpdate() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (rCornerProgressBar != null) {
						rCornerProgressBar.setVisibility(View.VISIBLE);
					}
				}
			});
		}
	}

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
						isPlaying = false;
//						playButton.setBackgroundResource(R.drawable.playbutton_stopped_states);
						changePlayButton();
						progressUpdate();
					}

					@Override
					public void onPlay() {
						isPlaying = true;
//						playButton.setBackgroundResource(R.drawable.playbutton_playing_states);
						changePlayButton();
						progressUpdate();
					}

					@Override
					public void onPause() {
						//playButton.setBackgroundResource(R.drawable.playbutton_stopped_states);
						isPlaying = false;
						changePlayButton();
					}

					@Override
					public void onChangePlayingItem(int position) {
						int toppos = lv.getFirstVisiblePosition();
						int lastpos = lv.getLastVisiblePosition();
						
						if (position > lastpos || position < toppos) {// to scroll only if position is invisible
							
							int visibleCount = lastpos-toppos;
							int halfVisible = visibleCount /2;
							int newpos;
							if (position <= halfVisible) 
								newpos = 0;
							else if (position > lv.getCount() -1 - halfVisible )
								newpos = lv.getCount()-1 - visibleCount;
							else 
								newpos = position - halfVisible;
							
							Log.d("shingrus", "Set new position: " + newpos);
							lv.setSelection(newpos);
						}
					}

				});
				if (playerService.isPlaying()) {
					isPlaying = true;
//					playButton.setBackgroundResource(R.drawable.playbutton_playing_states);
					changePlayButton();
				}
				progressUpdate();
			}
		};
		updateServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
				updateService.removeUpdateHandler(MyPlayerActivity.this.updateHandler);
				updateService = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				updateService = ((UpdateService.LocalBinder) service).getService();
				updateService.addUpdateHandler(MyPlayerActivity.this.updateHandler);
				if (trackList.isEmpty()) {
					startUpdate();
				}
			}
		};
		mpf = MyPlayerPreferences.getInstance(null);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handleProgressUpdate = new Handler();
		isPlaying = false;
		touchingProgress = false;
		boolean isCustomTitileSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		Log.d("shingrus", "Creating Player Activity");
		Intent i = new Intent(getApplicationContext(), MusicPlayerService.class);
		this.bindService(i, musicPlayerConnection, Context.BIND_AUTO_CREATE);
		setContentView(R.layout.playlist);

		if (isCustomTitileSupported) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.activetitle);
		}

		rCornerProgressBar = (ProgressBar) findViewById(R.id.playerTitleProgressId);
		alarmImage = (ImageView) findViewById(R.id.playerTitleImageId);
		alarmImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("shigrus", "click on alarm image");
			}
		});

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
		registerForContextMenu(lv);

		Intent service = new Intent(this, UpdateService.class);
		bindService(service, updateServiceConnection, Context.BIND_AUTO_CREATE);
		progressBar = (SeekBar) findViewById(R.id.playingSeek);
		progressBar.setClickable(false);
		progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				touchingProgress = false;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				touchingProgress = true;
				newPosition = 0;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && playerService != null) {
					// Log.d("shingrus", "Gonna set position: " + progress);
					newPosition = progress;
				}
			}
		});
		progressBar.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent motion) {
				boolean result = true;
				if (isPlaying) {
					result = false;
					switch (motion.getAction()) {
					// case MotionEvent.ACTION_DOWN:
					// break;
					// case MotionEvent.ACTION_MOVE:
					// break;
					case MotionEvent.ACTION_UP:
						// Log.d("shingrus", "ACTION_UP");
						if (playerService!=null)
							playerService.setPosition(newPosition);
						break;
					}
				}
				return result;
			}
		});
		playButton = (ImageButton) findViewById(R.id.playButton);
		
	}

	@Override
	protected void onResume() {
		if (trackList.isEmpty() && updateService != null) {
			startUpdate();
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.i("shingrus", "Destroying Player activity");
		isPlaying = false;
		handleProgressUpdate.removeCallbacks(progressUpdateJob);
		if (trackList != null)
			trackList.dropAdapter();
		if (updateService != null) {
			unbindService(updateServiceConnection);
			updateService = null;
			updateServiceConnection = null;
		}
		if (musicPlayerConnection != null) {
			if (playerService != null)
				playerService.unsetEventsListener();
			playerService = null;
			unbindService(musicPlayerConnection);
			musicPlayerConnection = null;
		}
		if (lv!=null)
			unregisterForContextMenu(lv);
		
		lv = null;
		rCornerProgressBar = null;
		playButton = null;
		progressBar = null;
		
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionmenu, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.playlistcontextmenu,menu);		
	}
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	        case R.id.PlaylistContextMenuSetAsRingtone:
	        	//TODO: to add set as ringtone method
				trackList.setAsRingtone(this, info.position);
	            return true;
	        default:
	            return super.onContextItemSelected(item);
	    }
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
		Intent service = new Intent(this, UpdateService.class);
		service.putExtra(UpdateService.START_UPDATE_COMMAND, UpdateService.START_UPDATE_COMMAND_UPDATE);
		startService(service);
	}

	private void changePlayButton() {
		//fuckin shit. i do need to change it
		int top = playButton.getPaddingTop();
		int left = playButton.getPaddingLeft();
		int right = playButton.getPaddingRight();
		int bottom = playButton.getPaddingBottom();
		if (isPlaying) 
			playButton.setBackgroundResource(R.drawable.playbutton_playing_states);
		else
			playButton.setBackgroundResource(R.drawable.playbutton_stopped_states);
		playButton.setPadding(left, top, right, bottom);
		
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

	private void progressUpdate() {
		handleProgressUpdate.postDelayed(progressUpdateJob, MyPlayerPreferences.UPDATE_PLAYING_STATUS_MS);
	}
	
	

}