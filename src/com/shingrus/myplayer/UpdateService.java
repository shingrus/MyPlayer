package com.shingrus.myplayer;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
//import android.util.Log;
import android.util.Log;

public class UpdateService extends Service {

	public static final int DOWNLOAD_SLEEP_MS = 3000;
	public static final int UPDATE_SLEEP_MS = 600 * 1000;
	public static final String SWA_URL = "http://swa.mail.ru/?";
	public static final String MUSIC_URL = "http://my.mail.ru/musxml";
	public static final String MAILRU_COOKIE_NAME = "Mpop";

	UpdateThread updateThread;
	Thread downloadThread;
	boolean continueWorking = true;

	long downloadEnqueue;
	DownloadManager dm;
	MusicTrack currentDownload;

	TrackList tl;
	Handler tracksHandler;

	public static final int MAXIMUM_SIM_DOWNLOAD = 1;
	private BroadcastReceiver downloadsReceiver;

	// private final IBinder mBinder = new LocalBinder();

	public UpdateService() {
		super();
		this.updateThread = new UpdateThread();
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

			while (UpdateService.this.continueWorking) {
				Thread.yield();

				if (UpdateService.this.currentDownload == null) { // we are
																	// waiting
																	// for
																	// downloading
					if ((UpdateService.this.currentDownload = tl.getNextForDownLoad()) != null) {
						String urlString = "http://" + currentDownload.getUrl();
						DownloadManager.Request r = new Request(Uri.parse(urlString));
						r.setAllowedOverRoaming(false);
						// set proper directory
						// r.setDestinationInExternalFilesDir(getApplicationContext(),
						// arg1, arg2)
						
						// TODO comment below and uncomment next line
						r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
						
						// r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
						//TODO set download directory
						
						// |
						// (MyPlayerPreferences.getInstance(null).useOnlyWifi()?
						// 0:DownloadManager.Request.NETWORK_MOBILE));
						r.setDescription(DOWNLOAD_MANAGER_DESCRIPTION);
						r.addRequestHeader("Cookie", MAILRU_COOKIE_NAME + "=" + MyPlayerPreferences.getInstance(null).getMpopCookie());
						UpdateService.this.downloadEnqueue = dm.enqueue(r);
					}
				}

				try {
					Thread.sleep(UpdateService.DOWNLOAD_SLEEP_MS);
				} catch (InterruptedException e) {

				}

			}
		}
	}

	class UpdateThread extends Thread {

		private boolean reAuthorizationRequired;
		String mpopCookie;

		public UpdateThread() {
			super();
			this.mpopCookie = null;
			this.reAuthorizationRequired = false;
		}

		protected void updateTrackList() {
			if (this.mpopCookie != null && this.mpopCookie.length() > 0) {
				AbstractHttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(MUSIC_URL);

				BasicClientCookie cookie = new BasicClientCookie(MAILRU_COOKIE_NAME, this.mpopCookie);
				cookie.setDomain(".mail.ru");
				cookie.setExpiryDate(new Date(2039, 1, 1, 0, 0));
				cookie.setPath("/");
				httpClient.getCookieStore().addCookie(cookie);

				try {
					HttpResponse musicListResponse = httpClient.execute(httpGet);

					if (null != musicListResponse && musicListResponse.getStatusLine().getStatusCode() == 200) {
						// Log.i("shingrus", "got music list");
						SAXParserFactory sf = SAXParserFactory.newInstance();
						try {
							SAXParser parser = sf.newSAXParser();
							XMLReader xr = parser.getXMLReader();
							// boolean authorizationError = false;
							xr.setContentHandler(new DefaultHandler() {

								MusicTrack mt = new MusicTrack();

								public final String TRACK_TAG = "TRACK", NAME_TAG = "NAME", URL_TAG = "FURL", PARAM_ID = "id",
										MUSICLIST_TAG = "MUSIC_LIST";
								boolean isInsideTrackTag = false, isInsideFURL = false, isInsideName = false, isInsideMusicList = false;
								StringBuilder builder = new StringBuilder();

								@Override
								public void characters(char[] ch, int start, int length) throws SAXException {
									if (isInsideFURL || isInsideName || isInsideMusicList) {
										builder.append(ch, start, length);
									}
								}

								@Override
								public void startElement(String uri, String localName, String qName, Attributes attributes)
										throws SAXException {
									// Log.i("shingrus",
									// "XML: start element: " + localName);
									super.startElement(uri, localName, qName, attributes);
									if (localName.equalsIgnoreCase(TRACK_TAG)) {
										isInsideTrackTag = true;
										mt = new MusicTrack();
										mt.setId(attributes.getValue(PARAM_ID));
									} else if (localName.equalsIgnoreCase(URL_TAG) && isInsideTrackTag) {
										isInsideFURL = true;
									} else if (localName.equalsIgnoreCase(NAME_TAG) && isInsideTrackTag) {
										isInsideName = true;
									} else if (localName.equalsIgnoreCase(MUSICLIST_TAG)) {
										isInsideMusicList = true;
									}

								}

								@Override
								public void endElement(String uri, String localName, String qName) throws SAXException {

									// Log.i("shingrus",
									// "XML: end element: " + localName);

									if (localName.equalsIgnoreCase(TRACK_TAG)) {
										isInsideTrackTag = false;
										isInsideName = isInsideFURL = false;
										if (mt.isComplete()) {

											final TrackList tl = TrackList.getInstance();
											Log.i("shingrus", mt.toString());

											// well, we have completed mt
											// object with url and id
											tl.addTrack(mt);
										}
									} else if (localName.equalsIgnoreCase(URL_TAG)) {
										isInsideFURL = false;
										mt.setUrl(builder.toString().replaceAll("[\\r\\n\\s]", ""));
									} else if (localName.equalsIgnoreCase(NAME_TAG)) {
										isInsideName = false;
										mt.setTitle(builder.toString().replaceAll("[\\r\\n\\s]", ""));
									} else if (localName.equalsIgnoreCase(MUSICLIST_TAG)) {
										isInsideMusicList = false;
										if (builder.toString().equals("Error!")) {
											UpdateThread.this.reAuthorizationRequired = true;
										}

									}
									if (builder.length() > 0) {
										builder.setLength(0);
									}
								}
							});
							InputSource is = new InputSource(musicListResponse.getEntity().getContent());
							xr.parse(is);
						} catch (ParserConfigurationException e) {
							e.printStackTrace();
						} catch (SAXException e) {
							e.printStackTrace();
						}

					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else
				reAuthorizationRequired = true;
		}

		/**
		 * It Makes authorization on mail.ru currently in future it may be
		 * implementation of the interface
		 * 
		 * @return true - in success, false in vice versa
		 */
		protected boolean authorize() {

			boolean result = false;

			if (this.mpopCookie != null && reAuthorizationRequired == false) {
				result = true;
			} else if (reAuthorizationRequired == false && (this.mpopCookie == null || this.mpopCookie.length() == 0)) {
				MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
				if (mpf != null) {
					String mpopCookie = mpf.getMpopCookie();
					if (mpopCookie != null && mpopCookie.length() > 0) {
						this.mpopCookie = mpopCookie;
						result = true;
					} else
						reAuthorizationRequired = true;
				} else
					reAuthorizationRequired = true;
			} else {
				HttpClient swaClient = new DefaultHttpClient();
				((AbstractHttpClient) (swaClient)).setRedirectHandler(new RedirectHandler() {
					@Override
					public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
						return false;
					}

					@Override
					public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
						return null;
					}
				});

				try {

					// TODO: here we need timeout tuning on http requests

					MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
					if (mpf != null) {
						HttpGet httpGet = new HttpGet(SWA_URL + "Login=" + mpf.getEmail() + "&Password=" + mpf.getPassword());
						HttpResponse swaResponse = swaClient.execute(httpGet);
						if (null != swaResponse) {
							for (Cookie cookie : ((AbstractHttpClient) swaClient).getCookieStore().getCookies()) {
								if (cookie.getName().equalsIgnoreCase(MAILRU_COOKIE_NAME)) {
									this.mpopCookie = cookie.getValue();
									break;
								}
							}
							// if we got mpopcookie - we need to get token in
							// future,
							// but now it's ok to get playlist directly
							if (this.mpopCookie != null) {
								result = true;
								reAuthorizationRequired = false;
								// store it
								mpf.setMpopCookie(this.mpopCookie);
							}

						}
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return result;
		}

		@Override
		public void run() {
			Thread.yield();
			while (UpdateService.this.continueWorking) {
				if (authorize()) {
					updateTrackList();
				}
				try {
					Thread.sleep(UPDATE_SLEEP_MS);
				} catch (InterruptedException e) {

					continueWorking = false;
				}
			}

		}

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

							//currentDownload.setFilename(filename);
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
				}
			}
		};

		registerReceiver(downloadsReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Start update thread
		updateThread.start();
		// Start download thread
		downloadThread.start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		this.dm.remove(downloadEnqueue);
		updateThread.interrupt();
		downloadThread.interrupt();
		unregisterReceiver(downloadsReceiver);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent i) {
		return null;

	}

}
