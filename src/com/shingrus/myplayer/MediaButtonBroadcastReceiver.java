package com.shingrus.myplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.KeyEvent;

public class MediaButtonBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		if (event.getAction() != KeyEvent.ACTION_DOWN)
			return;

			Intent i = new Intent(context, MusicPlayerService.class);
            i.setAction(MusicPlayerService.CMD_SERVICEACTION);
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_STOP:
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_STOP);
				context.startService(i);
				break;
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			case 126: //KEYCODE_MEDIA_PLAY API lvl11 
			case 127: //KEYCODE_MEDIA_PAUSE API lvl11
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_PLAY);
				context.startService(i);
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_NEXT);
				context.startService(i);
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_PREV);
				context.startService(i);
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_VOLUME_UP);
				context.startService(i);
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_VOLUME_DOWN);
				context.startService(i);
				break;
				
				
			}
	}

}
