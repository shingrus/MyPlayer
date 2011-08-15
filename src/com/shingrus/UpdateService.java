package com.shingrus;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
//import android.util.Log;
import android.util.Log;

public class UpdateService extends Service {

	UpdateThread updateThread;

	public UpdateService() {
		this.updateThread = new UpdateThread();
	}

	class UpdateThread extends Thread {

		public static final String SWA_URL = "http://swa.mail.ru/?";
		public static final String MUSIC_URL = "http://my.mail.ru/musxml";
		public static final int SLEEP_MS = 600*1000;
		public static final String COOKIE_NAME = "Mpop";

		private boolean reAuthorizationRequired, continueWorking;
		String mpopCookie;

		public UpdateThread() {
			super();
			this.mpopCookie = null;
			this.reAuthorizationRequired = false;
			this.continueWorking = true;
		}

		protected void updateTrackList() {
			if (this.mpopCookie != null && this.mpopCookie.length() > 0) {
				AbstractHttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(MUSIC_URL);

				BasicClientCookie cookie = new BasicClientCookie(COOKIE_NAME, this.mpopCookie);
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
							boolean authorizationError = false;
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

											Log.i("shingrus", mt.toString());

											// well, we have completed mt
											// object with url and id
											// TODO place mt object and

										}
									} else if (localName.equalsIgnoreCase(URL_TAG)) {
										isInsideFURL = false;
										mt.setUrl(builder.toString());
									} else if (localName.equalsIgnoreCase(NAME_TAG)) {
										isInsideName = false;
										mt.setTitle(builder.toString());
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
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
			} else if (reAuthorizationRequired == false && (this.mpopCookie == null || this.mpopCookie.length()==0)) {
				MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);
				if (mpf != null) {
					String mpopCookie = mpf.getMpopCookie(); 
					if (mpopCookie != null && mpopCookie.length() > 0) {
						this.mpopCookie = mpopCookie; 
						result = true;
					}
					else
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
								if (cookie.getName().equalsIgnoreCase(COOKIE_NAME)) {
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return result;
		}

		@Override
		public void run() {
			Thread.yield();
			while (continueWorking) {
				if (authorize()) {
					updateTrackList();
				}
				try {
					Thread.sleep(SLEEP_MS);
				} catch (InterruptedException e) {

					continueWorking = false;
				}
			}

		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Start our thread
		updateThread.start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		updateThread.interrupt();
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
