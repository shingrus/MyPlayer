package com.shingrus.myplayer;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.MediaController.MediaPlayerControl;
import android.media.*;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

	enum NotificationStatus {
		Stopped, Playing, Paused
	}

	public enum PlayingStatus {
		Playing, Paused, Stopped
	}

	private static final int NOTIFICATION_ID = 11;
	MediaPlayer mPlayer =null;
	TrackList trackList;
	private final IBinder mBinder = new LocalBinder();
	// boolean isPaused = false;
	boolean isPausedDurinngCall = false;
	NotificationManager nm;
	TelephonyManager tm;
	PhoneStateListener mPhoneListener;
	Notification notification;
	String currentStatusDesc;
	BroadcastReceiver audioReceiver;
	MyPlayerPreferences mpf;
	PlayingEventsListener eventsListener = null;
	// String currentTitle;
	CurrentState state = new CurrentState();

	public class LocalBinder extends Binder {
		MusicPlayerService getService() {
			return MusicPlayerService.this;
		}
	}

	private class CurrentState {
		public MusicTrack currentTrack = null;
		public int playedProgress = 0;
		public int duration;
		public PlayingStatus currentStatus = PlayingStatus.Stopped;

		public final void setNewTrack(MusicTrack mt) {
			playedProgress =0;
			currentTrack = mt;
			currentStatus = PlayingStatus.Stopped;
			duration = 0;
			
		}

	}

	/**
	 * Receiver for AudioManager.ACTION_AUDIO_BECOMING_NOISY it pauses if
	 * headphones disable
	 * 
	 * @author shingrus
	 */
	public class AudioBroacastReciever extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mpf.doPauseOnLoud() && intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
				stopMusic();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);

		trackList = TrackList.getInstance();
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		notification = new Notification(R.drawable.status, "", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		updateNotification(NotificationStatus.Stopped);
		audioReceiver = new AudioBroacastReciever();
		mpf = MyPlayerPreferences.getInstance(null);
		mPhoneListener = new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (mpf.doPauseOnCall()) {
					switch (state) {
					case TelephonyManager.CALL_STATE_IDLE:
						if (isPausedDurinngCall)
							playMusic();
						break;
					case TelephonyManager.CALL_STATE_OFFHOOK:
						if (MusicPlayerService.this.state.currentStatus == PlayingStatus.Playing) {
							stopMusic();
							isPausedDurinngCall = true;
						}
						break;
					case TelephonyManager.CALL_STATE_RINGING:
						if (MusicPlayerService.this.state.currentStatus == PlayingStatus.Playing) {
							stopMusic();
							isPausedDurinngCall = true;
						}
						break;
					}
				}
			}

		};
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		registerReceiver(audioReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		super.onCreate();
	}

	private final void updateNotification(NotificationStatus nStatus) {
		CharSequence nTitle = "";
		switch (nStatus) {
		case Paused:
			nTitle = getText(R.string.NotificationTitle_Paused);
			if (eventsListener != null)
				eventsListener.onPause();
			break;
		case Playing:
			nTitle = getText(R.string.NotificationTitle_Playing);
			trackList.notifyPlayStarted();
			if (eventsListener != null)
				eventsListener.onPlay();
			break;
		case Stopped:
			nTitle = getText(R.string.NotificationTitle_Stopped);
			trackList.notifyPlayStopped();
			if (eventsListener != null)
				eventsListener.onStop();
			break;
		}

		// Intent i = new Intent(this, MyPlayerActivity.class);
		Intent i = new Intent(this, LauncherActivity.class);
		// i.setFlags(Intent.FLAG_ACTIVITY_SIN
		// GLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
		String trackTitle = state.currentTrack == null ? "" : state.currentTrack.toString();
		notification.setLatestEventInfo(this, "MyPlayer - " + nTitle, trackTitle, pi);
		nm.notify(NOTIFICATION_ID, notification);
		switch (nStatus) {
		case Paused:
		case Playing:
			// TODO remove this strange notify throw tracklist scheme
			trackList.notifyPlayStarted();
			if (this.eventsListener != null) {
				eventsListener.onChangePlayingItem(trackList.getIteratePosition());
			}
			break;
		case Stopped:
			trackList.notifyPlayStopped();
			break;
		}
	}

	@Override
	public void onDestroy() {
		Log.d("shingrus", "Destroy: MusicPlayerService");
		stopForeground(true);
		nm.cancelAll();
		if (mPlayer != null) {
			if (mPlayer.isPlaying())
				mPlayer.stop();
			mPlayer.release();
		}
		if (tm != null)
			tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		unregisterReceiver(audioReceiver);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		nm.cancel(NOTIFICATION_ID);
		return Service.START_NOT_STICKY;
	}

	/**
	 * Method from OnPreparedListener
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		if (state.playedProgress != 0)
			mp.seekTo(state.playedProgress);
		if (state.currentTrack.getDuration() <= 0 ) {
			state.duration = mp.getDuration();
			trackList.setTrackDuration(state.currentTrack, state.duration);
		}
		else { 
			state.duration = state.currentTrack.getDuration();
		}
		mp.start();
		state.currentStatus = PlayingStatus.Playing;
		isPausedDurinngCall = false;
		updateNotification(NotificationStatus.Playing);

	}

	/**
	 * Method from OnErrorListener
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.i("shingrus", "MP error: " + what);
		updateNotification(NotificationStatus.Stopped);
		return false;
	}

	private void playMusic() {
		MusicTrack mt = state.currentTrack;
		if (mt != null && mt.filename.length() > 0) {
			mPlayer.reset();
			updateNotification(NotificationStatus.Stopped);
			isPausedDurinngCall = false;
			try {
				mPlayer.setDataSource(mt.filename);
				mPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (mt == null)
			Log.d("shingrus", "MusicPlayerService: mt is null");
	}

	/**
	 * Method from OnCompletionListener
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		playNext();
	}

	// my methods

	/**
	 * Used by activity to start/stop playing
	 */
	public void playPause() {
		switch (state.currentStatus) {
		case Stopped: // here we start to play music
			if (state.currentTrack == null) { // first attempt - we are starting
				state.setNewTrack(trackList.startIterateFrom(0)); 
				playMusic();
			} else { // start from previous state
			}
			playMusic();
			break;
		case Playing:// just stop
			stopMusic();
			break;
		}

	}

	public void startPlayFrom(int position) {
		state.setNewTrack(trackList.startIterateFrom(position));
		playMusic();

	}

	public void playNext() {
		MusicTrack mt = trackList.getNextTrack();
		state.setNewTrack(mt);
		playMusic();

	}

	public void playPrevious() {
		MusicTrack mt = trackList.getPreviuosTrack();
		state.setNewTrack(mt);
		playMusic();

	}

	// private void pause() {
	// if (mPlayer.isPlaying()) {
	// mPlayer.pause();
	// updateNotification(NotificationStatus.Paused);
	// state.currentStatus = PlayingStatus.Paused;
	// }
	// }

	// private void play() {
	// if (state.currentStatus == PlayingStatus.Paused) {
	// mPlayer.start();
	// updateNotification(NotificationStatus.Playing);
	// state.currentStatus = PlayingStatus.Playing;
	// isPausedDurinngCall = false;
	//
	// }
	// }

	public void stopMusic() {
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
			state.playedProgress = mPlayer.getCurrentPosition();;
		}
		mPlayer.stop();
		updateNotification(NotificationStatus.Stopped);
		state.currentStatus = PlayingStatus.Stopped;
		isPausedDurinngCall = false;

	}

	public void setEventsListener(PlayingEventsListener listener) {
		this.eventsListener = listener;
	}

	public void unsetEventsListener() {
		this.eventsListener = null;
	}

	public void setPosition(int percent) {
		if (mPlayer != null && mPlayer.isPlaying()) {
			double mult = (double) percent / 100;
			int msec = (int) (mPlayer.getDuration() * mult);
			Log.d("shingrus", "New position: " + msec);
			mPlayer.seekTo(msec);
			state.playedProgress = msec;
		}
	}

	public int getCurrentPosition() {
		int result = 0;
		switch (state.currentStatus) {
		case Playing:
			if (mPlayer != null ) {
				state.playedProgress = mPlayer.getCurrentPosition();
			}
			break;
		case Stopped:
		case Paused:
			//do nothing and use state progress
			break;
		}
		if (state.duration != 0) {
			double duration = state.duration;
			result = (int) (state.playedProgress / duration * 100) + 1;
		}
		return result;

	}

	public final CurrentState getState() {
		return state;
	}

	public boolean isPlaying() {
		if (mPlayer != null)
			return mPlayer.isPlaying();
		return false;
	}
}
