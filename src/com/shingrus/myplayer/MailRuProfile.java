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
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.SharedPreferences;
import android.util.Log;

public class MailRuProfile  implements MyPlayerAccountProfile {
	public static final String SWA_URL = "http://swa.mail.ru/?";
	public static final String MUSIC_URL = "http://my.mail.ru/musxml";
	public static final String SUCCESS_OATH_PREFIX = "http://connect.mail.ru/oauth/success.html#";
	public static final String APPID = "640583";
	public static final String OAUTH_URL = "https://connect.mail.ru/oauth/authorize?client_id=" + APPID 
	+ "&response_type=token&redirect_uri=http%3A%2F%2Fconnect.mail.ru%2Foauth%2Fsuccess.html&display=mobile";
	
	public static final String REFRESH_TOKEN_NAME = "refresh_token";
	public static final String ACCESS_TOKEN_NAME = "access_token";
	
	public static final String MAILRU_COOKIE_NAME = "Mpop";

	public static final int CONNECTION_TIMEOUT = 15*1000;
	
	private String accessToken;
	boolean isAccessTokenChanged = false;
	private String refreshToken;
	boolean isRefreshTokenChanged = false;
	
	TrackListFetchingStatus lastFetchResult;
	
	public MailRuProfile () {
		accessToken = "";
		refreshToken= "";
	}
	public final String  authorize (String login, String password) {
		String mpopCookie = null;
		BasicHttpParams httpParams = new BasicHttpParams();
		httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
		httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT);
		
		HttpClient swaClient = new DefaultHttpClient(httpParams);
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
			HttpGet httpGet = new HttpGet(SWA_URL + "Login=" + login + "&Password=" + password);
			HttpResponse swaResponse = swaClient.execute(httpGet);
			if (null != swaResponse) {
				for (Cookie cookie : ((AbstractHttpClient) swaClient).getCookieStore().getCookies()) {
					if (cookie.getName().equalsIgnoreCase(MAILRU_COOKIE_NAME)) {
						mpopCookie = cookie.getValue();
						break;
					}
				}
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		return mpopCookie;
	}
	
	
	

	public final TrackListFetchingStatus getTrackListFromInternet(final TrackList tl, String mpopCookie) {
		lastFetchResult = TrackListFetchingStatus.ERROR;
		
		if (mpopCookie != null && mpopCookie.length() > 0) {
			BasicHttpParams httpParams = new BasicHttpParams();
			httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
			httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT);
			
			AbstractHttpClient httpClient = new DefaultHttpClient(httpParams);
			HttpGet httpGet = new HttpGet(MailRuProfile.MUSIC_URL);

			BasicClientCookie cookie = new BasicClientCookie(MailRuProfile.MAILRU_COOKIE_NAME, mpopCookie);
			cookie.setDomain(".mail.ru");
			cookie.setExpiryDate(new Date(2039, 1, 1, 0, 0));
			cookie.setPath("/");
			httpClient.getCookieStore().addCookie(cookie);

			try {
				HttpResponse musicListResponse = httpClient.execute(httpGet);

				if (null != musicListResponse && musicListResponse.getStatusLine().getStatusCode() == 200) {
					lastFetchResult =TrackListFetchingStatus.SUCCESS;
					SAXParserFactory sf = SAXParserFactory.newInstance();
					try {
						SAXParser parser = sf.newSAXParser();
						XMLReader xr = parser.getXMLReader();
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
							public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
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

								if (localName.equalsIgnoreCase(TRACK_TAG)) {
									isInsideTrackTag = false;
									isInsideName = isInsideFURL = false;
									if (mt.isComplete()) {

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
									mt.setTitle(builder.toString().replaceAll("^\\s+", ""));
								} else if (localName.equalsIgnoreCase(MUSICLIST_TAG)) {
									isInsideMusicList = false;
									if (builder.toString().equals("Error!")) {
										lastFetchResult = TrackListFetchingStatus.NEEDREAUTH;
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
			lastFetchResult = TrackListFetchingStatus.NEEDREAUTH;
		
	return lastFetchResult;
	}
	@Override
	public final TrackListFetchingStatus lastFetchResult() {
		return lastFetchResult;
	}
	@Override
	public String getOAuthURL() {
		return OAUTH_URL;
	}
	public void setAccessToken(String access_token) {
		this.accessToken = access_token;
		this.isAccessTokenChanged = true;
	}
	public final String getAccessToken() {
		return accessToken;
	}
	public void setRefreshToken(String refresh_token) {
		this.refreshToken = refresh_token;
		this.isRefreshTokenChanged= true;
	}
	public final String getRefreshToken() {
		return refreshToken;
	}
	
	@Override
	public void loadPreferences(SharedPreferences preferences) {
		this.refreshToken = preferences.getString(REFRESH_TOKEN_NAME, "");
		this.accessToken = preferences.getString(ACCESS_TOKEN_NAME, "");
	}
	@Override
	public void storePreferences(SharedPreferences preferences) {
		SharedPreferences.Editor editor = preferences.edit();
		if (isAccessTokenChanged ) {
			editor.putString(ACCESS_TOKEN_NAME, accessToken);
		}
		if (isRefreshTokenChanged) {
			editor.putString(REFRESH_TOKEN_NAME, refreshToken);
		}
		editor.apply();
		isAccessTokenChanged = isRefreshTokenChanged = true;
	}
	
}
