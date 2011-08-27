package com.shingrus.myplayer;

import com.shingrus.myplayer.R;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TrackList {

	public static final int LIMIT_TRACKS = 1024;
	public static final String DATABASE_NAME = "TrackList";
	public static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME = "track";
	private static final String TRACK_TITLE = "Title";
	private static final String TRACK_FILENAME = "Filename";
	private static final String TRACK_ID = "Id";
	private static final String TRACK_URL = "Url";
	private static final String CREATE_DB = "CREATE TABLE " + TABLE_NAME + "(" + TRACK_ID + " INTEGER PRIMARY KEY autoincrement default 0," + TRACK_TITLE
			+ " TEXT not null, " + TRACK_FILENAME + " TEXT , " + TRACK_URL + " TEXT NOT NULL)";
	private static final String TRACK_INSERT_STMNT = "INSERT INTO " + TABLE_NAME + " (" + TRACK_TITLE + "," + TRACK_URL + "," + TRACK_FILENAME
			+ ") VALUES (?, ?, ?)";
	private static TrackList trackListInstance;
	private TrackListAdapter adapter;
	DBHelper dbHelper;
	private SQLiteStatement insertStmt;

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
			db.execSQL(CREATE_DB);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}

	public class TrackListHandler extends Handler {

	}

	/**
	 * Loads track list from internal storage
	 */
	public synchronized void loadTracks(Context ctx) {

		dbHelper = new DBHelper(ctx);
		// Because of ctx we have some warranty it's main thread
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db != null) {
			Cursor c = db.query(TABLE_NAME, new String[] { TRACK_ID, TRACK_TITLE, TRACK_FILENAME, TRACK_URL }, null, new String[] {}, null, null,
					null);
			if (c != null && c.moveToFirst()) {
				do {
					MusicTrack mt = new MusicTrack(c.getString(0), c.getString(1), c.getString(2), c.getString(3));
					trackList.add(mt);
				} while (c.moveToNext());
				c.close();
			}
			db.close();
		}
		dataChanged();
	}

	/**
	 * 
	 */

	public synchronized void addTrack(final MusicTrack mt) {
		if (!trackList.contains(mt) && mt.getTitle().length()>0 && mt.getUrl().length()>0 && trackList.size()<LIMIT_TRACKS) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					trackList.add(mt);
					if (dbHelper != null) {
						SQLiteDatabase db = dbHelper.getWritableDatabase();
						if (db != null) {
							TrackList.this.insertStmt = db.compileStatement(TRACK_INSERT_STMNT);
							insertStmt.bindString(1, mt.getTitle());
							insertStmt.bindString(2, mt.getUrl());
							insertStmt.bindString(3, mt.getFilename());
							long rowid = insertStmt.executeInsert();
							if (rowid == -1) {
								Log.i("shingrus", "Can't insert new value to db");
							}
							insertStmt.clearBindings();
						}
					}
					dataChanged();

				}
			};
			if (this.adapter != null) {
				this.adapter.activity.runOnUiThread(r);
			} else
				r.run();
		}
	}

	public synchronized void serFileName(MusicTrack mt, String filename) {
		for (MusicTrack track : trackList) {
			if (track.equals(mt)) {
				track.setFilename(filename);
			}
		}
		// TODO: store into storage
		Runnable r = new Runnable() {

			@Override
			public void run() {
				dataChanged();
			}
		};
		if (this.adapter != null) {
			this.adapter.activity.runOnUiThread(r);
		} else
			r.run();
	}

	public synchronized boolean contains(MusicTrack mt) {
		return trackList.contains(mt);
	}

	public synchronized void removeAll() {

		Runnable r = new Runnable() {

			@Override
			public void run() {
				trackList.clear();
				// TODO store into storage
				dataChanged();
			}
		};
		if (this.adapter != null) {
			this.adapter.activity.runOnUiThread(r);
		} else
			r.run();

	}

	static public synchronized TrackList getInstance() {
		if (trackListInstance == null) {
			trackListInstance = new TrackList();
		}
		return trackListInstance;
	}

	public synchronized MusicTrack getNextForDownLoad() {
		for (MusicTrack mt : trackList) {
			if (mt.getFilename() == null || mt.getFilename().length() < 1) {
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
			adapter.notifyDataSetChanged();
		}
	}

}
