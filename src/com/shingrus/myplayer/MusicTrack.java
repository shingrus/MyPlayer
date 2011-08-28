package com.shingrus.myplayer;


public final class MusicTrack {

	public enum MusicTrackStatus {
		NEW,
		DOWNLOADED,
		QUEUED,
		DOWNLOADING		
	};
	
	public String title, id, url, filename;
	private int Duration = 0;
//	private MusicTrackStatus status = MusicTrackStatus.NEW;
	
	public String getTitle() {
		return title;
	}

	public MusicTrack() {
		title = new String();
		filename = new String();
		id = new String();
		url = new String();
	}
	/**
	 * 
	 * @param id - String  Track id
	 * @param title - String,  Track title
	 * @param url - String Track URL
	 * @param filename - String
	 */
	public MusicTrack(String id, String title, String url, String filename) {
		this.title = title;
		this.id = id;
		this.url = url;
		this.filename = filename;
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
	}
	


	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public String toString() {
		return "MT: " + this.title + " : " + this.url;	
	}

	
	@Override
	public int hashCode() {
		return this.url.hashCode();
	}

	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MusicTrack) {
			
			return this.url.equals(((MusicTrack)o).url);
		}
		return super.equals(o);
	}

	public boolean isComplete() {
		boolean result = false;
		if (url != null && id != null && url.length() > 0 && id.length() > 0)
			result = true;

		return result;

	}

}
