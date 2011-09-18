package com.shingrus.myplayer;

import android.content.SharedPreferences;

public interface MyPlayerAccountProfile {
	
	public  String  authorize (String login, String password);
	
	public enum TrackListFetchingStatus {
		SUCCESS, ERROR, NEEDREAUTH, UNKNOWN}

	public TrackListFetchingStatus lastFetchResult();
	public  TrackListFetchingStatus getTrackListFromInternet(TrackList tl, String cookie);
	public TrackListFetchingStatus getTrackList(TrackList tl);
	public String getOAuthURL();
	public void loadPreferences(SharedPreferences preferences); 
	public void storePreferences(SharedPreferences preferences);
	public void setAccessToken(String acccessToken);
	public void setRefreshToken(String refreshToken);
	public void setUID(String uid);
}
