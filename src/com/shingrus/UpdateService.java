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
			((AbstractHttpClient) (swaClient))
					.setRedirectHandler(new RedirectHandler() {
						@Override
						public boolean isRedirectRequested(
								HttpResponse response, HttpContext context) {
							return false;
						}

						@Override
						public URI getLocationURI(HttpResponse response,
								HttpContext context) throws ProtocolException {
							return null;
						}
					});

			try {

				// TODO: here we need timeout
				HttpResponse swaResponse = swaClient.execute(new HttpGet(SWA_URL + "Login=" + email
						+ "&Password=" + password));
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
						
						HttpResponse musicListResponse = swaClient.execute(new HttpGet(UpdateThread.MUSIC_URL));
						if (null != musicListResponse) {
							SAXParserFactory sf = SAXParserFactory.newInstance();
							try {
								SAXParser parser = sf.newSAXParser();
								XMLReader xr = parser.getXMLReader();
								MusicTrack mt = new MusicTrack();
								xr.setContentHandler(new DefaultHandler() {

									boolean insideTrackTag = false;

									@Override
									public void startElement(String uri,
											String localName, String qName,
											Attributes attributes)
											throws SAXException {
										if (localName.equals("track")) {
											insideTrackTag = false;
										}
									}

									@Override
									public void endElement(String uri,
											String localName, String qName)
											throws SAXException {
										if (localName.equals("track")) {
											insideTrackTag = false;
										}
									}

								});
								InputSource is = new InputSource(musicListResponse
										.getEntity().getContent());
								xr.parse(is);
							} catch (Exception e) {
								// TODO Something wrong and i need to return error back
								e.printStackTrace();
							} 
								//catch (ParserConfigurationException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							} catch (SAXException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
								
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

		UpdateThread updateThread = new UpdateThread("yermakov@mail.ru",
				"hryn177");
		new Thread(updateThread).start();

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
