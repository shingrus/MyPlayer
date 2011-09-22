package com.shingrus.myplayer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.SharedPreferences;
import android.net.SSLCertificateSocketFactory;
import android.util.Log;

public class MailRuProfile implements MyPlayerAccountProfile {
	public static final String SWA_URL = "http://swa.mail.ru/?";
	public static final String MUSIC_URL = "http://my.mail.ru/musxml";
	public static final String SUCCESS_OATH_PREFIX = "http://connect.mail.ru/oauth/success.html#";
	public static final String APPID = "640583";
	public static final String OAUTH_URL = "https://connect.mail.ru/oauth/authorize?client_id=" + APPID
			+ "&response_type=token&redirect_uri=http%3A%2F%2Fconnect.mail.ru%2Foauth%2Fsuccess.html&display=mobile";
	public static final String POST_OAUTH_TOKEN_URL = "https://appsmail.ru/oauth/token";

	// 6b7c3ce87724ac6e72de6968cc5046b5
	public static final String REFRESH_TOKEN_NAME = "refresh_token";
	public static final String ACCESS_TOKEN_NAME = "access_token";
	public static final String UID_NAME = "x_mailru_vid";

	private static final String BASE_API_URL = "http://www.appsmail.ru/platform/api?";
	private static final String GET_TRACKS_LIST_METHOD = "audio.get";
	private static final String PRIVATE_KEY = "8bd7022c723f4cea429a70437d72ad07";
	private static final String SECRET_KEY = "dce3709dff6f799ab55705295f11ec09";

	public static final String MAILRU_COOKIE_NAME = "Mpop";

	public static final int CONNECTION_TIMEOUT = 15 * 1000;

	private String accessToken;
	boolean isAccessTokenChanged = false;
	private String refreshToken;
	boolean isRefreshTokenChanged = false;
	private String uid;
	boolean isUidChanged = false;

	TrackListFetchingStatus lastFetchResult;

	public MailRuProfile() {
		accessToken = "";
		refreshToken = "";
	}

	public final String getRefreshToken(String login, String password) {
		String refreshToken = null;
		BasicHttpParams httpParams = new BasicHttpParams();
		httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
		httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, CONNECTION_TIMEOUT);

		try {
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);

			schemeRegistry.register(new Scheme("https", new MySSLSocketFactory(trustStore)

			, 443));

			SingleClientConnManager mgr = new SingleClientConnManager(httpParams, schemeRegistry);

			HttpClient swaClient = new DefaultHttpClient(mgr, httpParams);
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
			// grant_type=password&client_id=123&client_secret=234&
			// username=test@mail.ru&password=qwerty&scope=widget

			HttpPost httpPost = new HttpPost(POST_OAUTH_TOKEN_URL);
			List<NameValuePair> postParams = new ArrayList<NameValuePair>(6);
			postParams.add(new BasicNameValuePair("grant_type", "password"));
			postParams.add(new BasicNameValuePair("client_id", APPID));
			postParams.add(new BasicNameValuePair("client_secret", PRIVATE_KEY));
			postParams.add(new BasicNameValuePair("username", login));
			postParams.add(new BasicNameValuePair("password", password));
			postParams.add(new BasicNameValuePair("scope", APPID));

			httpPost.setEntity(new UrlEncodedFormEntity(postParams));
			HttpResponse response = swaClient.execute(httpPost);
			if (response != null) {
				StatusLine sl = response.getStatusLine();
			}
		} catch (UnsupportedEncodingException e) {
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
			Log.d("shingrus", e.toString());
		} catch (KeyStoreException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (CertificateException e) {
		} catch (KeyManagementException e) {
		} catch (UnrecoverableKeyException e) {
		}

		return refreshToken;
	}

	public final String authorize(String login, String password) {
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

	private final String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public final TrackListFetchingStatus getTrackList(TrackList tl) {
		lastFetchResult = TrackListFetchingStatus.ERROR;
		String params = "app_id=" + APPID + "method=" + GET_TRACKS_LIST_METHOD + "session_key=" + accessToken;

		String md5 = md5("1324730981306483817app_id=423004method=friends.getsession_key=be6ef89965d58e56dec21acb9b62bdaa7815696ecbf1c96e6894b779456d330e");
		md5 = md5(uid + params + PRIVATE_KEY);

		String url = BASE_API_URL + "method=" + GET_TRACKS_LIST_METHOD + "&" + "app_id=" + APPID + "&session_key=" + accessToken + "&sig=" + md5;

		return lastFetchResult;
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
					lastFetchResult = TrackListFetchingStatus.SUCCESS;
					SAXParserFactory sf = SAXParserFactory.newInstance();
					try {
						SAXParser parser = sf.newSAXParser();
						XMLReader xr = parser.getXMLReader();
						xr.setContentHandler(new DefaultHandler() {

							MusicTrack mt = new MusicTrack();

							public final String TRACK_TAG = "TRACK", NAME_TAG = "NAME", URL_TAG = "FURL", PARAM_ID = "id", MUSICLIST_TAG = "MUSIC_LIST";
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

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
		this.isAccessTokenChanged = true;
	}

	public final String getAccessToken() {
		return accessToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
		this.isRefreshTokenChanged = true;
	}

	public final String getRefreshToken() {
		return refreshToken;
	}

	@Override
	public void setUID(String uid) {
		this.uid = uid;
		isUidChanged = true;

	}

	@Override
	public void loadPreferences(SharedPreferences preferences) {
		this.refreshToken = preferences.getString(REFRESH_TOKEN_NAME, "");
		this.accessToken = preferences.getString(ACCESS_TOKEN_NAME, "");
		this.uid = preferences.getString(UID_NAME, "");
	}

	@Override
	public void storePreferences(SharedPreferences preferences) {
		SharedPreferences.Editor editor = preferences.edit();
		if (isAccessTokenChanged) {
			editor.putString(ACCESS_TOKEN_NAME, accessToken);
		}
		if (isRefreshTokenChanged) {
			editor.putString(REFRESH_TOKEN_NAME, refreshToken);
		}
		if (isUidChanged) {
			editor.putString(UID_NAME, uid);
		}

		editor.apply();
		isAccessTokenChanged = isRefreshTokenChanged = isUidChanged = true;
	}

}
