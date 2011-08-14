package com.shingrus;

public class MyPlayerSettings {

	private static MyPlayerSettings playerSettings;
	
	private MyPlayerSettings() {
		//load info here
	}
	
	public static final MyPlayerSettings getInstance() {
		if (playerSettings == null)
			playerSettings = new MyPlayerSettings();
		return playerSettings;
	}
}
