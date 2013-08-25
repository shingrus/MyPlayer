package com.shingrus.myplayer;

import java.io.File;

import android.content.SharedPreferences;

public interface MyPlayerAccountProfile {
	
	public enum TrackListFetchingStatus {
		SUCCESS, ERROR, NEEDREAUTH, UPDATEACCESSTOKEN, UNKNOWN
	}
		
	public enum AuthorizeStatus {
		UNKNOWN, INVALID, SUCCESS 
	}
	
	public TrackListFetchingStatus lastFetchResult();
	public  TrackListFetchingStatus getTrackListFromInternet();
	public String getOAuthURL();
//	public String getTokens(String...strings);
	public AuthorizeStatus authorize(String login, String password);
	public boolean downloadAudioFile(String url, File whereToDownload);
	
	public void loadPreferences(SharedPreferences preferences); 
	public void storePreferences(SharedPreferences preferences);
	public void setAccessToken(String acccessToken, int validUntil);
	public void setRefreshToken(String refreshToken);
	public void setUID(String uid);
}
