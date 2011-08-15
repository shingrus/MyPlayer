package com.shingrus.myplayer;

public class TrackList {

	private TrackList trackList;
	//Create Only static 
	private TrackList() {
		
	}
	public TrackList getInstance() {
		if (trackList == null) {
			trackList = new TrackList();
		}
		return trackList;
	}

}
