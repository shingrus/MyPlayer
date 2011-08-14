package com.shingrus;

import android.app.Application;
import android.content.Intent;

import java.lang.*;
import java.util.*;

public class MyPlayerApplication extends Application {

	@Override
	public void onCreate() {
		
		
		MyPlayerPreferences.getInstance(getApplicationContext());
		
		this.startService(new Intent(this, UpdateService.class));
		super.onCreate();
	}

}
