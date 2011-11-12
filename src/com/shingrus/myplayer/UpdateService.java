package com.shingrus.myplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;

import com.shingrus.myplayer.MusicPlayerService.LocalBinder;
import com.shingrus.myplayer.MyPlayerAccountProfile.TrackListFetchingStatus;

import android.app.AlarmManager;
import android.app.PendingIntent;
//import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class UpdateService extends Service {

	public static final String START_UPDATE_COMMAND = "START_UPDATE";
	public static final int DOWNLOAD_SLEEP_MS = 60 * 1000;
	public static final int UPDATE_PERIOD_MS = 30 * 1000;

	Thread downloadThread, updateThread;
	boolean continueWorking = true;
	private boolean updateThreadAlreadyRunning = false;

	private final IBinder binder = new LocalBinder();

	TrackList tl;
	Handler tracksHandler;

	public static final int DOWNLOAD_CONNECTION_TIMEOUT = 15 * 1000;

	private AlarmManager alarmManager;

	/**
	 * i use this list to store activities handlers, that will handle update
	 * results
	 */
	private CopyOnWriteArrayList<UpdatesHandler> updatesHandlers;

	public UpdateService() {
		super();
		this.downloadThread = new DownloadThread();
		this.updateThread = new UpdateThread();
		updatesHandlers = new CopyOnWriteArrayList<UpdateService.UpdatesHandler>();
		tl = TrackList.getInstance();
	}

	public interface UpdatesHandler {
		public void onBeforeUpdate();
		public void onAfterUpdate(TrackListFetchingStatus updateStatus);
	}

	public void addUpdateHandler(UpdatesHandler h) {
		updatesHandlers.add(h);
	}

	public void removeUpdateHandler(UpdatesHandler h) {
		updatesHandlers.remove(h);
	}

	public class LocalBinder extends Binder {
		UpdateService getService() {
			return UpdateService.this;
		}
	}

	class DownloadThread extends Thread {

		public static final String DOWNLOAD_MANAGER_DESCRIPTION = "MyPlayer: Downloading new music from social network.";

		public DownloadThread() {
			super();
		}

		@Override
		public void run() {
			boolean doNotSleepInNextIteration = false;
			MyPlayerPreferences prefs = MyPlayerPreferences.getInstance(null);
			ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo network;
			while (UpdateService.this.continueWorking) {
				Thread.yield();
				doNotSleepInNextIteration = false;
				MusicTrack currentDownload;
				// check for wifi status
				if ((currentDownload = tl.getNextForDownLoad()) != null) {
					boolean isNetworkReady = checkNetworkState(prefs, conMan);
					if (isNetworkReady) {
						String filename = "/mailru" + prefs.getNextFilenameCounter() + ".mp3";
						filename = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + filename;
						File file = new File(filename);
						boolean result = prefs.getProfile().downloadAudioFile(currentDownload.url, file);
						if (result) {
							tl.setFileName(currentDownload, filename);
							// i've got it
							doNotSleepInNextIteration = true;
						} else {
							file.delete();
						}
					}

				}
				if (!doNotSleepInNextIteration) {
					try {
						Thread.sleep(UpdateService.DOWNLOAD_SLEEP_MS);
					} catch (InterruptedException e) {
						UpdateService.this.continueWorking = false;
					}
				}
			}
		}

		private boolean checkNetworkState(MyPlayerPreferences prefs, ConnectivityManager conMan) {
			boolean isNetworkReady = false;
			NetworkInfo network = conMan.getActiveNetworkInfo();
			if (network != null) {
				switch (network.getType()) {
				case ConnectivityManager.TYPE_WIMAX:
					isNetworkReady = true;
					break;
				case ConnectivityManager.TYPE_MOBILE:
					if (!network.isRoaming() && !prefs.useOnlyWifi()) {// roaming
																		// transfer
																		// doesn't
																		// allowed
						isNetworkReady = true;
					}
					break;
				case ConnectivityManager.TYPE_WIFI:
					isNetworkReady = true;
					break;
				}

			}
			return isNetworkReady;
		}

	}

	class UpdateThread extends Thread {
		public void run() {

			try {
				MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
				TrackListFetchingStatus updateStatus = mpf.getProfile().getTrackListFromInternet();

				for (UpdatesHandler h : updatesHandlers) {
					h.onAfterUpdate(updateStatus);
				}
			} finally {
				UpdateService.this.updateThreadAlreadyRunning = false;
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i("shingrus", "OnconfigurationChanged in  updateService:" + newConfig);
		// super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onLowMemory() {
		Log.i("shingrus", "OnLowMemory in  updateService");
		super.onLowMemory();
	}

	@Override
	public void onCreate() {
		alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
		Intent service = new Intent(this, UpdateService.class);
		service.putExtra(START_UPDATE_COMMAND, 1);
		PendingIntent operation = PendingIntent.getService(this, 0, service, 0);
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+UPDATE_PERIOD_MS, UPDATE_PERIOD_MS, operation);
		// Start download thread
		// TODO: start only once
		downloadThread.start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("shingrus", "Strart updateService");

		if (intent != null) {
			int i = intent.getIntExtra(START_UPDATE_COMMAND, 0);
			if (i == 1) {
				startUpdate();
			}
		}

		return START_NOT_STICKY;
	}

	synchronized void startUpdate() {
		if (!updateThreadAlreadyRunning) {
			updateThreadAlreadyRunning = true;
			for(UpdatesHandler h: updatesHandlers){
				h.onBeforeUpdate();
			}
			updateThread = new UpdateThread();
			updateThread.start();
		}
	}

	@Override
	public void onDestroy() {
		downloadThread.interrupt();
		if (updateThread != null)
			updateThread.interrupt();
	}

	@Override
	public IBinder onBind(Intent i) {
		return binder;

	}

}
