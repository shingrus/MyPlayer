package com.shingrus.myplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
//import android.util.Log;
import android.test.IsolatedContext;
import android.util.Log;

public class UpdateService extends Service {

	public static final int DOWNLOAD_SLEEP_MS = 60 * 1000;
	public static final int UPDATE_SLEEP_MS = 600 * 1000;

	// UpdateThread updateThread;
	Thread downloadThread;
	boolean continueWorking = true;

	long downloadEnqueue;
	DownloadManager dm;
	MusicTrack currentDownload;

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
			MyPlayerPreferences prefs = MyPlayerPreferences.getInstance(null);
			ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo network;
			while (UpdateService.this.continueWorking) {
				Thread.yield();
				boolean isNetworkReady = false;
				network  = conMan.getActiveNetworkInfo();
				if (network != null) {
					switch (network.getType()) {
					case ConnectivityManager.TYPE_WIMAX:
						isNetworkReady = true;
						break;
					case ConnectivityManager.TYPE_MOBILE:
						if (!network.isRoaming() && !prefs.useOnlyWifi()) {//roaming transfer doesn't allowed
							isNetworkReady = true;
						}
						break;
					case ConnectivityManager.TYPE_WIFI:
							isNetworkReady = true;
						break;
					}
					
				}					
				// we are waiting for downloading
				if (UpdateService.this.currentDownload == null && isNetworkReady) {

					// check for wifi status
					if ((UpdateService.this.currentDownload = tl.getNextForDownLoad()) != null) {
						String urlString = "http://" + currentDownload.getUrl();
						BasicHttpParams httpParams = new BasicHttpParams();
						httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DOWNLOAD_CONNECTION_TIMEOUT);
						httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, DOWNLOAD_CONNECTION_TIMEOUT);

						AbstractHttpClient httpClient = new DefaultHttpClient(httpParams);
						HttpGet httpGet = new HttpGet(urlString);

						BasicClientCookie cookie = new BasicClientCookie(MailRuProfile.MAILRU_COOKIE_NAME, prefs.getMpopCookie());
						cookie.setDomain(".mail.ru");
						cookie.setExpiryDate(new Date(2039, 1, 1, 0, 0));
						cookie.setPath("/");
						httpClient.getCookieStore().addCookie(cookie);
						File file = null;

						try {
							HttpResponse resp = httpClient.execute(httpGet);
							StatusLine status = resp.getStatusLine();
							if (status != null && status.getStatusCode() == 200) {
								Header contentLengthH = resp.getFirstHeader("Content-Length");
								Header contentTypeH = resp.getFirstHeader("Content-Type");

								if (contentTypeH != null && contentTypeH.getValue().contains("audio")) {
									InputStream is = resp.getEntity().getContent();
									byte[] buf = new byte[4096];
									int readed = 0;
									int written = 0;
									String filename = "/mailru" + prefs.getNextFilenameCounter() + ".mp3";

									filename = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + filename;
									file = new File(filename);
									OutputStream out = new FileOutputStream(file);
									while ((readed = is.read(buf)) != -1 && UpdateService.this.continueWorking) {
										out.write(buf, 0, readed);
										written += readed;
									}
									if (UpdateService.this.continueWorking && written > 0) {
										// chek gotten size and if i got less
										// than
										// Content-Length remove file.
										if (contentLengthH != null) {
											int fileLength = Integer.parseInt(contentLengthH.getValue());
											if (fileLength == written) {
												// add file to tracklist
												tl.setFileName(currentDownload, filename);
												// i got it
												currentDownload = null;
												file = null;
											} else {
												Log.d("shingrus", "Remove file in case of invalid size");
											}
										}
									}

								}
							}// seems like we are not authorized
							else if (status.getStatusCode() == 500){
								// TODO here needs to notify about problem with
								// authorization
								
							}
							if (file != null) {
								file.delete();
								file = null;
							}
						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							if (file != null) {
								file.delete();
								file = null;
							}
						}

					}
				}
				try {
					Thread.sleep(UpdateService.DOWNLOAD_SLEEP_MS);
				} catch (InterruptedException e) {
					UpdateService.this.continueWorking = false;
				}
			} // while
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
