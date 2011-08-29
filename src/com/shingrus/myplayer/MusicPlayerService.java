package com.shingrus.myplayer;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.MediaController.MediaPlayerControl;
import android.media.*;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener {

	MediaPlayer mp;
	TrackList trackList;
	MusicTrack currentTrack;
	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		MusicPlayerService getService() {
			return MusicPlayerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		trackList = TrackList.getInstance();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d("shingrus", "Destroy: MusicPlayerService");
		if (mp != null) {
			if (mp.isPlaying())
				mp.stop();
			mp.release();
		}
//		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Method from OnPreparedListener
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

	/**
	 * Method from OnErrorListener
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.i("shingrus", "MP error: " + what);
		return false;
	}

	private void playMusic(MusicTrack mt) {
		if (mt != null && mt.getFilename().length() > 0) {
			mp.reset();
			try {
				mp.setDataSource(mt.getFilename());
				mp.setOnPreparedListener(this);
				mp.setOnCompletionListener(this);
				mp.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method from OnCompletionListener
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		MusicTrack mt = trackList.getNextTrack();
		playMusic(mt);
	}

	// my methods

	public void startPlayFrom(int position) {
		playMusic(trackList.startIterateFrom(position));

	}

	public void stopMusic() {
		mp.stop();
	}

}
