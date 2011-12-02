package com.shingrus.myplayer;

public interface PlayingEventsListener {
	public void onChangePlayingItem(int position);
	public void onPause();
	public void onStop();
	public void onPlay();
	/**
	 * 
	 * @param playedDuration
	 */
	public void onPlayedPositionProgress(int playedProgress);
}
