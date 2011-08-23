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
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TrackList {



	public static final String DATABASE_NAME = "TrackList";
	public static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME = "track";
	private static TrackList trackListInstance;
	private TrackListAdapter adapter;
	private boolean alreadyLoaded = false;

	// private final Context context;

	class TrackListAdapter extends BaseAdapter {
		private final Activity activity;

		public TrackListAdapter(Activity activity) {
			super();
			this.activity = activity;
		}

		@Override
		public synchronized int getCount() {
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
		public synchronized View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = this.activity.getLayoutInflater();
			TextView rowView = (TextView) inflater.inflate(R.layout.tracklist_item, null, true);
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

		// bind to UpdateService

	}

	class DBHelper extends SQLiteOpenHelper {

		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY, name TEXT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
		
	/**
	 * Loads track list from internal storage
	 */
	public synchronized void loadTracks(Context ctx) {
		
		DBHelper dbHelper = new DBHelper(ctx); 
			//SQL database
			dataChanged();
	}

	/**
	 * 
	 */

	public synchronized void addTrack(MusicTrack mt) {
		if (!trackList.contains(mt)) {
			trackList.add(mt);

			// TODO store into storage
			dataChanged();
		}
	}

	public synchronized void serFileName(MusicTrack mt, String filename) {
		for (MusicTrack track : trackList) {
			if (track.equals(mt)) {
				track.setFilename(filename);
			}
		}

		// TODO: store into storage

		dataChanged();
	}

	public synchronized boolean contains(MusicTrack mt) {
		return trackList.contains(mt);
	}

	public synchronized void removeAll() {
		trackList.clear();
		// TODO: drop table inside storage
		dataChanged();
	}

	static public synchronized TrackList getInstance() {
		if (trackListInstance == null) {
			trackListInstance = new TrackList();
		}
		return trackListInstance;
	}

	public synchronized MusicTrack getNextForDownLoad() {
		for (MusicTrack mt : trackList) {
			if (mt.getFilename() == null || mt.getFilename().length()<1) {
				return mt;
			}
		}
		return null;
	}
	
	public TrackListAdapter getAdapter(Activity actvty) {
		adapter = new TrackListAdapter(actvty);
		return adapter;
	}

	public void dropAdapter() {
		adapter = null;
	}

	private synchronized void dataChanged() {
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
