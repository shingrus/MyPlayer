package com.shingrus.myplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
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
import android.app.DownloadManager;
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
import android.util.Log;

public class UpdateService extends Service {

	public static final int DOWNLOAD_SLEEP_MS = 60 * 1000;
	public static final int UPDATE_SLEEP_MS = 600 * 1000;

	// UpdateThread updateThread;
	Thread downloadThread;
	boolean continueWorking = true;

	long downloadEnqueue;
	DownloadManager dm;
	// MusicTrack currentDownload;

	TrackList tl;
	Handler tracksHandler;

	// public static final int MAXIMUM_SIM_DOWNLOAD = 1;
	public static final int DOWNLOAD_CONNECTION_TIMEOUT = 15 * 1000;
	private BroadcastReceiver downloadsReceiver;

	// private final IBinder mBinder = new LocalBinder();

	public UpdateService() {
		super();
		// this.updateThread = new UpdateThread();
		this.downloadThread = new DownloadThread();

		downloadEnqueue = 0;
		tl = TrackList.getInstance();
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i("shingrus", "OnconfigurationChanged in  updateService:" + newConfig);
		// super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i("shingrus", "OnUnbind in  updateService:" + intent);
		return super.onUnbind(intent);
	}

	@Override
	public void onLowMemory() {
		Log.i("shingrus", "OnLowMemory in  updateService");
		super.onLowMemory();
	}

	@Override
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("shingrus", "Strart updateService");
		// Start update thread
		// updateThread.start();
		// Start download thread
		// TODO: start only once
		downloadThread.start();
		// return super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		this.dm.remove(downloadEnqueue);
		// no more async updates in threads
		// updateThread.interrupt();
		downloadThread.interrupt();
		unregisterReceiver(downloadsReceiver);
		// super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent i) {
		return null;

	}

}
