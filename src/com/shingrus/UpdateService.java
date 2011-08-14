package com.shingrus;

import java.io.IOException;
import java.net.URI;

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
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
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

	class UpdateThread implements Runnable {

		public static final String SWA_URL = "http://swa.mail.ru/?";
		public static final String MUSIC_URL = "http://my.mail.ru/musxml";

		final String email, password;
		String mpopCookie;

		public UpdateThread(String email, String password) {
			super();
			this.email = email;
			this.password = password;
			this.mpopCookie = null;
		}

		/**
		 * It Makes authorization on mail.ru currently in future it may be
		 * implementation of the interface
		 * 
		 * @return true - in success, false in vise versa
		 */
		protected boolean authorize() {

			boolean result = false;
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

				HttpGet httpGet = new HttpGet(SWA_URL + "Login=" + email + "&Password=" + password);
				HttpResponse swaResponse = swaClient.execute(httpGet);
				if (null != swaResponse) {
					for (Cookie cookie : ((AbstractHttpClient) swaClient).getCookieStore().getCookies()) {
						if (cookie.getName().equalsIgnoreCase("Mpop")) {
							System.err.println("Set-cookie: " + cookie.getValue());
							this.mpopCookie = cookie.getValue();
							break;
						}
					}
					// if we got mpopcookie - we need to get token in future,
					// but now it's ok to get playlist directly
					if (this.mpopCookie != null) {
						httpGet.setURI(URI.create(MUSIC_URL));
						HttpResponse musicListResponse = swaClient.execute(httpGet);
						if (null != musicListResponse && musicListResponse.getStatusLine().getStatusCode() == 200) {
							// Log.i("shingrus", "got music list");
							SAXParserFactory sf = SAXParserFactory.newInstance();
							try {
								SAXParser parser = sf.newSAXParser();
								XMLReader xr = parser.getXMLReader();
								xr.setContentHandler(new DefaultHandler() {

									MusicTrack mt = new MusicTrack();

									public final String TRACK_TAG = "TRACK", NAME_TAG = "NAME", URL_TAG = "FURL", PARAM_ID = "id";
									boolean isInsideTrackTag = false, isInsideFURL = false, isInsideName = false;
									StringBuilder builder = new StringBuilder();

									@Override
									public void characters(char[] ch, int start, int length) throws SAXException {
										if (isInsideFURL || isInsideName) {
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
											mt.setId(attributes.getValue(PARAM_ID));
										} else if (localName.equalsIgnoreCase(URL_TAG) && isInsideTrackTag) {
											isInsideFURL = true;
										} else if (localName.equalsIgnoreCase(NAME_TAG) && isInsideTrackTag) {
											isInsideName = true;
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
												// clear it
												// create new
												mt = new MusicTrack();
											}
										} else if (localName.equalsIgnoreCase(URL_TAG)) {
											isInsideFURL = false;
											mt.setUrl(builder.toString());
										} else if (localName.equalsIgnoreCase(NAME_TAG)) {
											isInsideName = false;
											mt.setTitle(builder.toString());
										}

										if (builder.length() > 0) {
											builder.setLength(0);
										}
									}
								});
								InputSource is = new InputSource(musicListResponse.getEntity().getContent());
								xr.parse(is);
							}
							// catch (Exception e) {
							// // TODO Something wrong and i need to return
							// // error back
							// e.printStackTrace();
							// }
							catch (ParserConfigurationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SAXException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

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
			return result;
		}

		@Override
		public void run() {

			authorize();
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Start our thread
		MyPlayerPreferences mpp = MyPlayerPreferences.getInstance(null);
		if (mpp != null) {
			UpdateThread updateThread = new UpdateThread(mpp.getEmail(), mpp.getPassword());
			new Thread(updateThread).start();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
