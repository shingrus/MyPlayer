package com.shingrus.myplayer;

public interface MyPlayerAccountProfile {
	
	public  String  authorize (String login, String password);
	
	public enum TrackListFetchingStatus {
		SUCCESS, ERROR, NEEDREAUTH, UNKNOWN}

	public TrackListFetchingStatus lastFetchResult();
	public  TrackListFetchingStatus getTrackListFromInternet(TrackList tl, String cookie);
	
}
