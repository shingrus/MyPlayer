package com.shingrus.myplayer;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

public class TrackList {

	enum Direction {
		PREVIOUS, NEXT
	}

	public static final int LIMIT_TRACKS = 1024;
	public static final String DATABASE_NAME = "TrackList";
	public static final int DATABASE_VERSION = 5;
	public static final String TABLE_NAME = "track";
	private static final String TRACK_ARTIST = "Artist";
	private static final String TRACK_TITLE = "Title";
	private static final String TRACK_FILENAME = "Filename";
	private static final String TRACK_ID = "Id";
	private static final String TRACK_URL = "Url";
	private static final String TRACK_FLAGS = "Flags";
	private static final String TRACK_POSITION = "Position";
	private static final String TRACK_DURATION = "Duration";
	private static final String CREATE_DB = "CREATE TABLE " + TABLE_NAME + "(" + TRACK_ID + " CHAR PRIMARY KEY NOT NULL UNIQUE ," + TRACK_ARTIST
			+ " TEXT not null," + TRACK_TITLE + " TEXT not null, " + TRACK_FILENAME + " TEXT , " + TRACK_URL + " TEXT NOT NULL, " + TRACK_FLAGS
			+ " INT DEFAULT 0, " + TRACK_DURATION + " INT DEFAULT 0," + TRACK_POSITION + " INT DEFAULT 0)";
	private static final String TRACK_INSERT_STMNT = "INSERT INTO " + TABLE_NAME + " (" + TRACK_ID + "," + TRACK_ARTIST + "," + TRACK_TITLE + "," + TRACK_URL
			+ "," + TRACK_FILENAME + ") VALUES (?, ?, ?, ?, ?)";
	private static TrackList trackListInstance;
	private TrackListAdapter adapter;
	DBHelper dbHelper;
	private boolean isLoaded = false;
	List<MusicTrack> trackList;
	private int iteratePosition = 0;
	private boolean isPlaying = false;

	// private final Context context;

	// TODO - move basedapter to activity
	// TODO - give handler from activity
	// TODO - and don't use runonuithread!

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
			MusicTrack mt = trackList.get(position);
			LayoutInflater inflater = this.activity.getLayoutInflater();
			TableLayout rowView = (TableLayout) inflater.inflate(R.layout.tracklist_item, null, true);
			int resourceId = (mt.getFilename().length() > 0) ? R.drawable.list_item_active_bg : R.drawable.list_item_inactive_bg;
			rowView.setBackgroundDrawable(parent.getResources().getDrawable(resourceId));
			TextView text = (TextView) rowView.findViewById(R.id.trackrow_titleid);
			// (TextView) inflater.inflate(R.layout.tracklist_item, null, true);
			text.setText(mt.getTitle());
			if (isPlaying && position == iteratePosition) {
				View v = rowView.findViewById(R.id.trackrow_playinSignId);
				v.setVisibility(View.VISIBLE);
			}
			text = (TextView) rowView.findViewById(R.id.trackrow_artistid);
			text.setText(mt.getArtist());
			return rowView;
		}

	}

	// Create Only static
	private TrackList() {
		// trackList = new LinkedHashSet<MusicTrack>();
		trackList = new CopyOnWriteArrayList<MusicTrack>();
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

	/**
	 * Loads track list from internal storage
	 */
	public synchronized void loadTracks(Context ctx) {

		if (!isLoaded) {
			dbHelper = new DBHelper(ctx);
			// Because of ctx we have some warranty it's main thread
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			if (db != null) {
				Cursor c = db.query(TABLE_NAME, new String[] { TRACK_ID, TRACK_ARTIST, TRACK_TITLE, TRACK_URL, TRACK_FILENAME, TRACK_FLAGS, TRACK_DURATION, },
						null, new String[] {}, null, null, null);
				if (c != null) {
					if (c.moveToFirst()) {
						do {
							String filename = c.getString(4);
							File f = new File(Uri.parse(filename).getPath());
							if (!f.exists())
								filename = "";
							MusicTrack mt = new MusicTrack(c.getString(0), c.getString(1), c.getString(2), c.getString(3), filename, c.getInt(6));
							trackList.add(mt);
							Log.d("shingrus", "Add music track: " + mt.getFilename() + " was:" + filename);
						} while (c.moveToNext());
					}
					c.close();
				}
				db.close();
			}
			dataChanged();
			isLoaded = true;
		}
	}

	/**
	 * 
	 */

	public synchronized void addTrack(final MusicTrack mt) {
		if (!trackList.contains(mt) && mt.getTitle().length() > 0 && mt.getUrl().length() > 0 && trackList.size() < LIMIT_TRACKS) {
			Log.d("shingrus", "Adding new track: " + mt + ", track id: " + trackList.size());
			Runnable r = new Runnable() {
				@Override
				public void run() {
					trackList.add(0, mt);
					if (dbHelper != null) {
						SQLiteDatabase db = dbHelper.getWritableDatabase();
						if (db != null) {
							SQLiteStatement insertStmt = db.compileStatement(TRACK_INSERT_STMNT);
							insertStmt.bindString(1, mt.getId());
							insertStmt.bindString(2, mt.getArtist());
							insertStmt.bindString(3, mt.getTitle());
							insertStmt.bindString(4, mt.getUrl());
							insertStmt.bindString(5, mt.getFilename());
							insertStmt.executeInsert();
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

	private final void storeTrackAttributeToStorage(MusicTrack mt, ContentValues values) {
		if (dbHelper != null) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			if (db != null) {
				db.update(TABLE_NAME, values, TRACK_ID + "=?", new String[] { mt.getId() });
			}
			db.close();
		}

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

	public synchronized void setFileName(MusicTrack mt, String filename) {
		for (MusicTrack track : trackList) {
			if (track.equals(mt)) {
				track.setFilename(filename);
				ContentValues values = new ContentValues();
				values.put(TRACK_FILENAME, filename);
				storeTrackAttributeToStorage(mt, values);
				break;
			}
		}
	}

	public synchronized void setTrackDuration(MusicTrack mt, int duration) {
		for (MusicTrack track : trackList) {
			if (track.equals(mt)) {
				track.setDuration(duration);
				ContentValues values = new ContentValues();
				values.put(TRACK_DURATION, duration);
				storeTrackAttributeToStorage(mt, values);
				break;
			}
		}
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
		synchronized (this.trackList) {
			for (MusicTrack mt : trackList) {
				if (mt.getFilename() == null || mt.getFilename().length() < 1) {
					return mt;
				}
			}
		}
		return null;
	}

	public final synchronized MusicTrack getTrackAt(int position) {
		return trackList.get(position);
	}

	private final MusicTrack getTrack(Direction direction) {
		int counter = trackList.size();
		MusicTrack mt = null;
		for (; counter > 0; counter--) {

			if (direction == Direction.NEXT) {
				iteratePosition++;
				if (iteratePosition >= trackList.size())
					iteratePosition = 0;
			} else if (direction == Direction.PREVIOUS) {
				--iteratePosition;
				if (iteratePosition < 0)
					iteratePosition = trackList.size() - 1;
			}
			if (trackList.get(iteratePosition).filename.length() > 0) {
				mt = trackList.get(iteratePosition);
				break;
			}
		}
		return mt;
	}

	public final synchronized MusicTrack getPreviuosTrack() {
		return getTrack(Direction.PREVIOUS);
	}

	public final synchronized MusicTrack getNextTrack() {
		return getTrack(Direction.NEXT);
	}

	public final synchronized MusicTrack startIterateFrom(int position) {
		// TODO check size and check that we got mt with Bfilename
		iteratePosition = (position > trackList.size() - 1) ? 0 : position;
		return trackList.get(iteratePosition);
	}

	public int getIteratePosition() {
		return iteratePosition;
	}

	public final void notifyPlayStarted() {
		isPlaying = true;
		dataChanged();
	}

	public final boolean isEmpty() {
		return trackList.isEmpty();
	}

	public final void notifyPlayStopped() {
		isPlaying = false;
		dataChanged();
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

	public final void setAsRingtone(Context ctx, int location) {
		MusicTrack mt = trackList.get(location);
		if (mt != null && mt.isComplete()) {
			File k = new File(trackList.get(location).getFilename());

			Log.d("shingrus", "Gonna set '" + mt + "' as ringtone");
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
			values.put(MediaStore.MediaColumns.TITLE, mt.getTitle());
			//values.put(MediaStore.MediaColumns.SIZE,);
			values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
			values.put(MediaStore.Audio.Media.ARTIST, mt.getArtist());
			values.put(MediaStore.Audio.Media.DURATION, mt.getDuration());
			values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
			values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
			values.put(MediaStore.Audio.Media.IS_ALARM, false);
			values.put(MediaStore.Audio.Media.IS_MUSIC, false);

			// Insert it into the database
			
			Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.getAbsolutePath());
			ContentResolver crl = ctx.getContentResolver(); 
			Uri newUri = crl.insert(uri, values);
		//	crl.delete(newUri, MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath() + "\"", null);
			RingtoneManager.setActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE, newUri);
		}
	}

}
