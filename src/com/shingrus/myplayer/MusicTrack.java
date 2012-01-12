package com.shingrus.myplayer;

import javax.xml.datatype.Duration;

import android.graphics.Shader.TileMode;


public final class MusicTrack {

	public enum MusicTrackStatus {
		NEW,
		DOWNLOADED,
		QUEUED,
		DOWNLOADING		
	};
	
	public String title, id, url, filename, artist;
	private int duration = 0;
	
	public String getTitle() {
		return title;
	}

	public MusicTrack() {
		title = new String();
		filename = new String();
		id = new String();
		url = new String();
		artist  = new String();
	}
	/**
	 * 
	 * @param id - String  Track id
	 * @param title - String,  Track title
	 * @param url - String Track URL
	 * @param filename - String
	 */
	public MusicTrack(String id, String artist, String title, String url, String filename, int duration) {
		if (artist!=null && title!=null) {
			if (title.startsWith(artist)) {
				title = title.substring(artist.length(), title.length());
				title = title.replaceFirst("\\s*\\-\\s*", "");
			}
		}
		this.artist = (artist==null || artist.equals("null")) ? "": artist;
		this.title = (title ==null) ? "": title;
		this.id = id;
		this.url = url;
		this.filename = filename;
		this.duration = duration;
	}

	public void setTitle(String title) {
		this.title = title;
//		Log.i("shingrus", "MT: Setted new title: " + this.title);
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	


	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
		//good place for loading duration
	}

	@Override
	public String toString() {
		return this.artist == null ?"":this.artist  + "-"+ this.title;	
	}

	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MusicTrack) {
			return this.id.equals(((MusicTrack)o).id);
//			return this.title.equals(((MusicTrack)o).title);
		}
		return super.equals(o);
	}

	public boolean isComplete() {
		boolean result = false;
		if (url != null && id != null && url.length() > 0 && id.length() > 0)
			result = true;

		return result;

	}

	public String getArtist() {
		return this.artist;
	}

}
