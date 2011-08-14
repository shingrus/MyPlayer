package com.shingrus;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class MyPlayerPreferences {

	private String email, password, mpopCookie;

	private boolean useOnlyWifi;
	private SharedPreferences preferences;
	private static MyPlayerPreferences playerPreferences;
	private Context context;

	public String getMpopCookie() {
		return mpopCookie;
	}

	synchronized public void setMpopCookie(String mpopCookie) {
		// here we need to store it inside
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(this.context.getString(R.string.mpop_cookie_key), mpopCookie);
		this.mpopCookie = mpopCookie;
	}

	/**
	 * 
	 * @return {@link String} user's email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * 
	 * @return {@link String} user's password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 
	 * @return {@link Boolean} user's setting for wifi or other connections
	 *         usage
	 */
	public boolean isUseOnlyWifi() {
		return useOnlyWifi;
	}

	private MyPlayerPreferences() {
		email = new String();
		password = new String();
		useOnlyWifi = true;
		preferences = null;
		mpopCookie = new String();
		context = null;
		// load info here
	}

	public void loadPreferences(Context context) {
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.email = preferences.getString(context.getString(R.string.mailru_email_preference_key), "");
		this.password = preferences.getString(context.getString(R.string.mailru_password_preference_key), "");
		this.mpopCookie = preferences.getString(context.getString(R.string.mpop_cookie_key), "");
		this.useOnlyWifi = preferences.getBoolean(context.getString(R.string.useWifiOnly_key), true);
		this.context = context;
	}

	// TODO: i don't know why i made it like singleton
	/**
	 * @param Context
	 *            context - context of the application Creates new instance
	 *            fills it and return or just returns already created and filled
	 *            instance
	 */
	public synchronized static final MyPlayerPreferences getInstance(Context context) {

		if (playerPreferences == null && context != null) {
			playerPreferences = new MyPlayerPreferences();
			playerPreferences.loadPreferences(context);
		} else if (context == null && null == playerPreferences) {
			return null;
		}
		return playerPreferences;
	}
}
