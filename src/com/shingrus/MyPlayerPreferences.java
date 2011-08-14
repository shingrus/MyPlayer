package com.shingrus;

public class MyPlayerPreferences {

	private static MyPlayerPreferences playerSettings;
	
	private MyPlayerPreferences() {
		//load info here
	}
	
	public static final MyPlayerPreferences getInstance() {
		if (playerSettings == null)
			playerSettings = new MyPlayerPreferences();
		return playerSettings;
	}
}
