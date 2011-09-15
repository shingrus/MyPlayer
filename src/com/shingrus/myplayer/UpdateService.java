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

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
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
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
//import android.util.Log;
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
			while (UpdateService.this.continueWorking) {
				Thread.yield();
				boolean isWiFiEnabled = true;
				if (UpdateService.this.currentDownload == null && isWiFiEnabled) { // we are
																	// waiting
																	// for
																	// downloading
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
							InputStream is = resp.getEntity().getContent();
							byte[] buf = new byte[1024];
							int readed = 0;
							int written = 0;
							String filename = "mailru" + prefs.getNextFilenameCounter() + ".mp3";

							filename = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + filename;
							file = new File(filename);
							OutputStream out = new FileOutputStream(file);
							while ((readed = is.read(buf)) != -1 && UpdateService.this.continueWorking) {
								out.write(buf);
								written += readed;
							}
							if (UpdateService.this.continueWorking && written > 0) {
								// chek gotten size and if i got less than
								// Content-Length remove file.
								int fileLength = Integer.getInteger(resp.getFirstHeader("Content-length").getValue());
								if (fileLength == written) {
									// add file to tracklist
									tl.setFileName(currentDownload, filename);
									//i got it
									currentDownload = null;
									
								} else {
									Log.d("shingrus", "Remove file in case of invalid size");
									file.delete();
								}
							}
						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							if (file != null) {
								file.delete();
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

		public void run2() {

			while (UpdateService.this.continueWorking) {
				Thread.yield();

				if (UpdateService.this.currentDownload == null) { // we are
																	// waiting
																	// for
																	// downloading
					if ((UpdateService.this.currentDownload = tl.getNextForDownLoad()) != null) {
						String urlString = "http://" + currentDownload.getUrl();
						DownloadManager.Request r = new Request(Uri.parse(urlString));
						// prevent downloading in roaming
						r.setAllowedOverRoaming(false);
						MyPlayerPreferences prefs = MyPlayerPreferences.getInstance(null);

						r.setDescription(DOWNLOAD_MANAGER_DESCRIPTION);
						r.setTitle(currentDownload.getTitle());
						// TODO remove mailru prefix to profile_name prefix
						r.setDestinationInExternalFilesDir(UpdateService.this, Environment.DIRECTORY_MUSIC, "mailru" + prefs.getNextFilenameCounter() + ".mp3");

						// TODO use wifi according to user settings
						// r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
						// | DownloadManager.Request.NETWORK_MOBILE);

						int flags = DownloadManager.Request.NETWORK_WIFI | (prefs.useOnlyWifi() ? 0 : DownloadManager.Request.NETWORK_MOBILE);
						r.setAllowedNetworkTypes(flags);

						r.setDescription(DOWNLOAD_MANAGER_DESCRIPTION);
						r.addRequestHeader("Cookie", MailRuProfile.MAILRU_COOKIE_NAME + "=" + prefs.getMpopCookie());
						UpdateService.this.downloadEnqueue = dm.enqueue(r);
					}
				}

				try {
					Thread.sleep(UpdateService.DOWNLOAD_SLEEP_MS);
				} catch (InterruptedException e) {
					UpdateService.this.continueWorking = false;
				}

			}
		}
	}

	// class UpdateThread extends Thread {
	//
	// private boolean reAuthorizationRequired;
	// String mpopCookie;
	//
	// public UpdateThread() {
	// super();
	// this.mpopCookie = null;
	// this.reAuthorizationRequired = false;
	// }
	//
	// protected void updateTrackList() {
	// if (this.mpopCookie != null && this.mpopCookie.length() > 0) {
	// AbstractHttpClient httpClient = new DefaultHttpClient();
	// HttpGet httpGet = new HttpGet(MailRuProfile.MUSIC_URL);
	//
	// BasicClientCookie cookie = new
	// BasicClientCookie(MailRuProfile.MAILRU_COOKIE_NAME, this.mpopCookie);
	// cookie.setDomain(".mail.ru");
	// cookie.setExpiryDate(new Date(2039, 1, 1, 0, 0));
	// cookie.setPath("/");
	// httpClient.getCookieStore().addCookie(cookie);
	//
	// try {
	// HttpResponse musicListResponse = httpClient.execute(httpGet);
	//
	// if (null != musicListResponse &&
	// musicListResponse.getStatusLine().getStatusCode() == 200) {
	// // Log.i("shingrus", "got music list");
	// SAXParserFactory sf = SAXParserFactory.newInstance();
	// try {
	// SAXParser parser = sf.newSAXParser();
	// XMLReader xr = parser.getXMLReader();
	// // boolean authorizationError = false;
	//
	// //XXX: !!!!it should create list and do not modify tracklist inside
	// global track list!!
	// xr.setContentHandler(new DefaultHandler() {
	//
	// MusicTrack mt = new MusicTrack();
	//
	// public final String TRACK_TAG = "TRACK", NAME_TAG = "NAME", URL_TAG =
	// "FURL", PARAM_ID = "id",
	// MUSICLIST_TAG = "MUSIC_LIST";
	// boolean isInsideTrackTag = false, isInsideFURL = false, isInsideName =
	// false, isInsideMusicList = false;
	// StringBuilder builder = new StringBuilder();
	//
	// @Override
	// public void characters(char[] ch, int start, int length) throws
	// SAXException {
	// if (isInsideFURL || isInsideName || isInsideMusicList) {
	// builder.append(ch, start, length);
	// }
	// }
	//
	// @Override
	// public void startElement(String uri, String localName, String qName,
	// Attributes attributes) throws SAXException {
	// // Log.i("shingrus",
	// // "XML: start element: " + localName);
	// super.startElement(uri, localName, qName, attributes);
	// if (localName.equalsIgnoreCase(TRACK_TAG)) {
	// isInsideTrackTag = true;
	// mt = new MusicTrack();
	// mt.setId(attributes.getValue(PARAM_ID));
	// } else if (localName.equalsIgnoreCase(URL_TAG) && isInsideTrackTag) {
	// isInsideFURL = true;
	// } else if (localName.equalsIgnoreCase(NAME_TAG) && isInsideTrackTag) {
	// isInsideName = true;
	// } else if (localName.equalsIgnoreCase(MUSICLIST_TAG)) {
	// isInsideMusicList = true;
	// }
	//
	// }
	//
	// @Override
	// public void endElement(String uri, String localName, String qName) throws
	// SAXException {
	//
	// // Log.i("shingrus",
	// // "XML: end element: " + localName);
	//
	// if (localName.equalsIgnoreCase(TRACK_TAG)) {
	// isInsideTrackTag = false;
	// isInsideName = isInsideFURL = false;
	// if (mt.isComplete()) {
	//
	// final TrackList tl = TrackList.getInstance();
	// Log.i("shingrus", mt.toString());
	//
	// // well, we have completed mt
	// // object with url and id
	// tl.addTrack(mt);
	// }
	// } else if (localName.equalsIgnoreCase(URL_TAG)) {
	// isInsideFURL = false;
	// mt.setUrl(builder.toString().replaceAll("[\\r\\n\\s]", ""));
	// } else if (localName.equalsIgnoreCase(NAME_TAG)) {
	// isInsideName = false;
	// mt.setTitle(builder.toString().replaceAll("[\\r\\n\\s]", ""));
	// } else if (localName.equalsIgnoreCase(MUSICLIST_TAG)) {
	// isInsideMusicList = false;
	// if (builder.toString().equals("Error!")) {
	// UpdateThread.this.reAuthorizationRequired = true;
	// }
	//
	// }
	// if (builder.length() > 0) {
	// builder.setLength(0);
	// }
	// }
	// });
	// InputSource is = new
	// InputSource(musicListResponse.getEntity().getContent());
	// xr.parse(is);
	// } catch (ParserConfigurationException e) {
	// e.printStackTrace();
	// } catch (SAXException e) {
	// e.printStackTrace();
	// }
	//
	// }
	// } catch (ClientProtocolException e) {
	// e.printStackTrace();
	// } catch (IllegalStateException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// } else
	// UpdateThread.this.reAuthorizationRequired = true;
	// }
	//
	// /**
	// * It Makes authorization on mail.ru currently, in future it may be
	// * the implementation of the interface
	// *
	// * @return true - in success, false in vice versa
	// */
	// protected boolean authorize() {
	//
	// boolean result = false;
	//
	// if (this.mpopCookie != null && reAuthorizationRequired == false) {
	// result = true;
	// } else if (reAuthorizationRequired == false && (this.mpopCookie == null
	// || this.mpopCookie.length() == 0)) {
	// MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
	// if (mpf != null) {
	// String mpopCookie = mpf.getMpopCookie();
	// if (mpopCookie != null && mpopCookie.length() > 0) {
	// this.mpopCookie = mpopCookie;
	// result = true;
	// } else
	// reAuthorizationRequired = true;
	// } else
	// reAuthorizationRequired = true;
	// } else {
	// MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
	// this.mpopCookie = mpf.getProfile().authorize(mpf.getEmail(),
	// mpf.getPassword());
	// if (this.mpopCookie != null) {
	// result = true;
	// reAuthorizationRequired = false;
	// // store it
	// mpf.setMpopCookie(this.mpopCookie);
	// }
	// }
	// return result;
	// }
	//
	// @Override
	// public void run() {
	// Thread.yield();
	// while (UpdateService.this.continueWorking) {
	// if (authorize()) {
	// updateTrackList();
	// }
	// try {
	// Thread.sleep(UPDATE_SLEEP_MS);
	// } catch (InterruptedException e) {
	//
	// continueWorking = false;
	// }
	// }
	//
	// }
	//
	// }

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
		dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		tracksHandler = new Handler();
		downloadsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					Query query = new Query();
					query.setFilterById(downloadId);
					Cursor c = dm.query(query);
					if (c.moveToFirst()) {
						int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

							String filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

							// File f = new File(URI.create(filename));

							// i don't know why, but sometimes i'm getting
							// broken files. i can't find when DM removes files.
							// i could use currentDownload link, but i
							tl.setFileName(currentDownload, filename);

							// try {
							// dm.openDownloadedFile(downloadId);
							// } catch (FileNotFoundException e) {
							// //TODO: show warning to user
							// }
						} else if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)) {
							String reason = c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON));
							Log.i("shingrus", "HTTP fail: " + reason);
						}
					}
					currentDownload = null;
					downloadEnqueue = 0;
				} else {
					Log.i("shingrus", "DM reciever: got unknown action:" + action);
				}
			}
		};

		registerReceiver(downloadsReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		// super.onCreate();
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
