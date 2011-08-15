package com.shingrus.myplayer;

import java.net.URL;

import android.util.Log;

public final class MusicTrack {

	private String title, id, url;
	private int Duration;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
//		Log.i("shingrus", "MT: Setted new title: " + this.title);
	}

	public int getDuration() {
		return Duration;
	}

	public void setDuration(int duration) {
		Duration = duration;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
//		Log.i("shingrus", "MT: Setted new id: " + this.id);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
//		Log.i("shingrus", "MT: Setted new url: " + this.url);
	}
	

	@Override
	public String toString() {
		return "MT: " + this.title + " : " + this.url;	
	}

	public boolean isComplete() {
		boolean result = false;
		if (url != null && id != null && url.length() > 0 && id.length() > 0)
			result = true;

		return result;

	}

}
