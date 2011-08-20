package com.shingrus.myplayer;

import com.shingrus.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentSkipListSet;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

//TODO extends ArrayAdapter
public class TrackList {

	private static TrackList trackListInstance;
	private TrackListAdapter adapter;

	// private final Context context;

	class TrackListAdapter extends BaseAdapter {
		private final Activity activity;

		public TrackListAdapter(Activity activity) {
			super();
			this.activity = activity;
		}

		@Override
		public int getCount() {
			return trackList.size();
		}

		@Override
		public synchronized Object getItem(int position) {
			return trackList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = this.activity.getLayoutInflater();
			TextView rowView = (TextView)inflater.inflate(R.layout.tracklist_item, null, true);
			rowView.setText(trackList.get(position).getTitle());
			return rowView;
		}

	}

	// LinkedHashSet<MusicTrack> trackList;
	ArrayList<MusicTrack> trackList;

	// Create Only static
	private TrackList() {
		// trackList = new LinkedHashSet<MusicTrack>();
		trackList = new ArrayList<MusicTrack>();
	}

	/**
	 * Loads track list from internal storage
	 */
	public synchronized void LoadTracks() {
		dataChanged();
	}

	/**
	 * 
	 */

	public synchronized void AddTrack(MusicTrack mt) {
		if (!trackList.contains(mt)) {
			trackList.add(mt);
		}
		dataChanged();
	}

	static public synchronized TrackList getInstance() {
		if (trackListInstance == null) {
			trackListInstance = new TrackList();
			// Load track list from sql DataBase
			trackListInstance.LoadTracks();
		}
		return trackListInstance;
	}

	public TrackListAdapter getAdapter(Activity actvty) {
		adapter = new TrackListAdapter(actvty);
		return adapter;
	}

	public void DropAdapter () {
		if (adapter != null) 
			adapter = null;
	}
	
	private void dataChanged() {
		if (adapter != null) {
			this.adapter.activity.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
					
				}
			});
		}
	}

}
