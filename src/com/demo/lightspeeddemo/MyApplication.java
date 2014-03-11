package com.demo.lightspeeddemo;

import com.testflightapp.lib.TestFlight;

import android.app.Application;

public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		TestFlight.takeOff(this, "7ca79c85-561d-44fa-b47b-314c9508ebe9");
	}
}
