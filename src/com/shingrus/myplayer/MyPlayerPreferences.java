package com.shingrus.myplayer;

import com.shingrus.myplayer.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyPlayerPreferences {

	private String email, password, mpopCookie;

	private final String MPOPCOOKIE_KEY = "mpop_cookie";
	private final String FILENAMECOUNTER_KEY = "filename_counter";
	private boolean useOnlyWifi;
	private int nextFilenameCounter;
	private SharedPreferences preferences;
	private static MyPlayerPreferences playerPreferences ;
	

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

	public void setEmail(String login) {
		
	}

	public void setPassword(String password2) {
		// TODO Auto-generated method stub
		
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
		return email;
	}

	/**
	 * 
	 * @return {@link String} user's password
	 */
	public synchronized String getPassword() {
		return password;
	}

	/**
	 * 
	 * @return {@link Boolean} user's setting for wifi or other connections
	 *         usage
	 */
	public boolean useOnlyWifi() {
		return useOnlyWifi;
	}

	private MyPlayerPreferences() {
		email = new String();
		password = new String();
		useOnlyWifi = true;
		preferences = null;
		mpopCookie = new String();
		// load info here
	}

	public synchronized void loadPreferences(Context context) {
			PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
			this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
			this.email = preferences.getString(context.getString(R.string.mailru_email_preference_key), "");
			this.password = preferences.getString(context.getString(R.string.mailru_password_preference_key), "");
			this.mpopCookie = preferences.getString(MPOPCOOKIE_KEY, "");
			this.useOnlyWifi = preferences.getBoolean(context.getString(R.string.useWifiOnly_key), true);
			this.nextFilenameCounter = preferences.getInt(FILENAMECOUNTER_KEY, 0);
			
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
