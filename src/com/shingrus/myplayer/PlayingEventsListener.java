package com.shingrus.myplayer;

public interface PlayingEventsListener {
	public void onChangePlayPosition(int position);
	public void onPause();
	public void onStop();
	public void onPlay();
}
