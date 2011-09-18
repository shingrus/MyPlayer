package com.shingrus.myplayer;

import com.shingrus.myplayer.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyPlayerPreferences {

	private String login, password, mpopCookie;
	//yeah! it should be map or vector.
	private boolean isLoginChanged = false, isPasswordChanged = false;
	
	
	private static final String MPOPCOOKIE_KEY = "mpop_cookie";
	public static final String OAUTH_URL = "openauthurl";
	private final String FILENAMECOUNTER_KEY = "filename_counter";
	private boolean useOnlyWifi, pauseOnLoud, pauseOnCall;
	private int nextFilenameCounter;
	private SharedPreferences preferences;
	private static MyPlayerPreferences playerPreferences ;
	
	
	
	
	//TODO in next version it should be an array
	private MyPlayerAccountProfile profile; 
	

	public String getMpopCookie() {
		return mpopCookie;
	}

	/**
	 * This method stores mpopCookie inside preferences
	 * it makes it asynchronously
	 * 
	 * @param mpopCookie
	 */
	synchronized public void setMpopCookie(String mpopCookie) {
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(MPOPCOOKIE_KEY, mpopCookie);
		editor.apply();
		this.mpopCookie = mpopCookie;
	}

	public void setLogin(String login) {
		this.login = login;
		isLoginChanged = true;
	}

	public void setPassword(String password) {
		this.password = password;
		isPasswordChanged = true;
	}
	
	public void storePreferences(Context ctx) {
		if (ctx != null) {
			SharedPreferences.Editor editor = this.preferences.edit();
			if (isLoginChanged ) {
				editor.putString(ctx.getString(R.string.mailru_login_preference_key), this.login);
			}
			if (isPasswordChanged) {
				editor.putString(ctx.getString(R.string.mailru_password_preference_key), this.password);
			}
			editor.apply();
			isLoginChanged = isPasswordChanged = true;
			profile.storePreferences(preferences);
		}
	}
	
	/**
	 * 
	 * @return Next incremented value for
	 * filenames
	 */
	synchronized public int getNextFilenameCounter() {
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putInt(FILENAMECOUNTER_KEY, ++this.nextFilenameCounter);
		editor.apply();
		return this.nextFilenameCounter;
	}
	/**
	 * 
	 * @return {@link String} user's email
	 */
	public synchronized String getEmail() {
		return login;
	}

	/**
	 * 
	 * @return {@link String} user's password
	 */
	public synchronized String getPassword() {
		return password;
	}

	public MyPlayerAccountProfile getProfile() {
		return profile;
	}

	/**
	 * 
	 * @return {@link Boolean} user's setting for wifi or other connections
	 *         usage
	 */
	public boolean useOnlyWifi() {
		return useOnlyWifi;
	}

	public final boolean isPauseOnLoud() {
		return pauseOnLoud;
	}

	public final boolean isPauseOnCall() {
		return pauseOnCall;
	}

	private MyPlayerPreferences() {
		login = new String();
		password = new String();
		useOnlyWifi = true;
		pauseOnCall = true;
		pauseOnLoud = true;
		preferences = null;
		mpopCookie = new String();
		profile = new MailRuProfile();
	}

	public synchronized void loadPreferences(Context context) {
			PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
			this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
			this.login = preferences.getString(context.getString(R.string.mailru_login_preference_key), "");
			this.password = preferences.getString(context.getString(R.string.mailru_password_preference_key), "");
			this.mpopCookie = preferences.getString(MPOPCOOKIE_KEY, "");
			this.useOnlyWifi = preferences.getBoolean(context.getString(R.string.useWifiOnly_key), true);
			this.pauseOnLoud = preferences.getBoolean(context.getString(R.string.pauseOnLoud_key), true);
			this.pauseOnCall = preferences.getBoolean(context.getString(R.string.pauseOnCall_key), true);			
			this.nextFilenameCounter = preferences.getInt(FILENAMECOUNTER_KEY, 0);
			
			profile.loadPreferences(preferences);
			
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
