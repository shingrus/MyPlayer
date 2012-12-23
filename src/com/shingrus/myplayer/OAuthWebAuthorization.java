package com.shingrus.myplayer;

import java.util.Scanner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthWebAuthorization extends Activity {

	WebView mWebView;

	private class HelloWebViewClient extends WebViewClient {
		@Override
		
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d("shingrus", "Loading url: " + url);
			if (url.startsWith(MailRuProfile.SUCCESS_OATH_PREFIX)) {
				// seems that we've got auth
				String fragment, accessToken = null, refreshToken = null, uid = null;
				int refresh_in =0;
				Uri uri = Uri.parse(url);
				if (uri != null && (fragment = uri.getEncodedFragment()) != null) {
					Scanner sc = new Scanner(fragment);
					sc.useDelimiter("&");
					while (sc.hasNext()) {
						final String[] nameValue = sc.next().split("=");
						if (nameValue.length == 0 || nameValue.length > 2)
							continue;
						if (nameValue.length == 2) {
							String name = nameValue[0];
							if (name.equals(MailRuProfile.ACCESS_TOKEN_NAME) && nameValue[1].length() == 32)
								accessToken = nameValue[1];
							else if (name.equals(MailRuProfile.REFRESH_TOKEN_NAME) && nameValue[1].length() == 32)
								refreshToken = nameValue[1];
							else if (name.equals(MailRuProfile.UID_RESPONSE_NAME) && nameValue[1].length() > 2)
								uid = nameValue[1];
							else if (name.equals(MailRuProfile.EXPIRES_IN_RESPONSE_NAME) && nameValue[1].length()>0)
								refresh_in = Integer.getInteger(nameValue[1], 0);
						}
					}
				}
				if (accessToken != null && refreshToken != null && uid != null) {
					// TODO: store data in preferences, launch player activity,
					// finish this activity
					MyPlayerPreferences mpf = MyPlayerPreferences.getInstance(null);

					MyPlayerAccountProfile mpp = mpf.getProfile();
					mpp.setAccessToken(accessToken,refresh_in);
					mpp.setRefreshToken(refreshToken);
					mpp.setUID(uid);
					mpf.storePreferences(OAuthWebAuthorization.this);

					Intent i = new Intent(OAuthWebAuthorization.this, MyPlayerActivity.class);
					startActivity(i);
					OAuthWebAuthorization.this.finish();
					return true;
				}
			}
			view.loadUrl(url);
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.oauthweb);
		Bundle b = getIntent().getExtras();
		String url = "";
		if (b != null) {
			url = b.getString(MyPlayerPreferences.OAUTH_URL);
		}
		mWebView = (WebView) findViewById(R.id.oauthWebView);
//		CookieSyncManager.createInstance(this);
//		CookieManager cm = CookieManager.getInstance();
//		cm.removeAllCookie();

		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new HelloWebViewClient());
		mWebView.loadUrl(url);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
