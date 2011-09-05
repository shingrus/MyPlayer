package com.shingrus.myplayer;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

public class MailRuSpecific {
	public static final String SWA_URL = "http://swa.mail.ru/?";
	public static final String MUSIC_URL = "http://my.mail.ru/musxml";
	public static final String MAILRU_COOKIE_NAME = "Mpop";
	
	public static final String authorizeOnMailRu(String login, String password) {
		String mpopCookie = null;
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
}
