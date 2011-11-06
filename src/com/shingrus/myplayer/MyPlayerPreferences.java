package com.shingrus.myplayer;

import com.shingrus.myplayer.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyPlayerPreferences {

	private static final String MPOPCOOKIE_KEY = "mpop_cookie";
	public static final String OAUTH_URL = "openauthurl";
	private final String FILENAMECOUNTER_KEY = "filename_counter";
	private boolean useOnlyWifi, pauseOnLoud, pauseOnCall;
	private int nextFilenameCounter;
	private SharedPreferences preferences;
	private static MyPlayerPreferences playerPreferences ;
	
	private static final String PLAYER_PROFILES_LIST_KEY = "profiles_list";
	private boolean hasProfile = false;
	
	//TODO in next version it should be an array
	private MyPlayerAccountProfile profile;
	private boolean isProfileChanged = false; 
	
	
	public static final int CONNECTION_TIMEOUT = 15*1000;

	
	public void storePreferences(Context ctx) {
		if (ctx != null) {
			SharedPreferences.Editor editor = this.preferences.edit();
			if (isProfileChanged) {
				editor.putBoolean(PLAYER_PROFILES_LIST_KEY, hasProfile);
			}
			editor.commit();
			isProfileChanged = false;
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
		editor.commit();
		return this.nextFilenameCounter;
	}
//	/**
//	 * 
//	 * @return {@link String} user's email
//	 */
//	public synchronized String getEmail() {
//		return login;
//	}
//
//	/**
//	 * 
//	 * @return {@link String} user's password
//	 */
//	public synchronized String getPassword() {
//		return password;
//	}

	/**
	 * @return the hasProfile
	 */
	public boolean isHasProfile() {
		return hasProfile;
	}

	/**
	 * @param hasProfile the hasProfile to set
	 */
	public void setHasProfile(boolean hasProfile) {
		this.hasProfile = hasProfile;
		this.isProfileChanged = true;
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

	public final boolean doPauseOnCall() {
		return pauseOnCall;
	}

	private MyPlayerPreferences() {
		useOnlyWifi = true;
		pauseOnCall = true;
		pauseOnLoud = true;
		preferences = null;
		profile = new MailRuProfile();
		
	}

	public synchronized void loadPreferences(Context context) {
			PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
			this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
			this.useOnlyWifi = preferences.getBoolean(context.getString(R.string.useWifiOnly_key), true);
			this.pauseOnLoud = preferences.getBoolean(context.getString(R.string.pauseOnLoud_key), true);
			this.pauseOnCall = preferences.getBoolean(context.getString(R.string.pauseOnCall_key), true);			
			this.hasProfile = preferences.getBoolean(PLAYER_PROFILES_LIST_KEY, false);
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
