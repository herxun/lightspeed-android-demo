package com.demo.lightspeeddemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class PushActivity extends Activity {
	private Handler gMainHandler;
	private ImageButton gButtonPush;
	private TextView gTextWelcome;
	private TextView gShowResult;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_push);
		
		// Retrieve view entity from XML
		gTextWelcome = (TextView) findViewById(R.id.textWelcome);
		gTextWelcome.setTypeface(MainActivity.gFont);
		gShowResult = (TextView)findViewById(R.id.showResult);
		
		
		// Handler binding with main thread. Handler allow you to send task to thread.
		// Later, we need this handler to post UI task to main thread.
		gMainHandler = new Handler();
		
		// Push Button click event
		gButtonPush = (ImageButton) findViewById(R.id.buttonPush);
		gButtonPush.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				PushRunn pushRunn = new PushRunn();
				Thread th = new Thread(pushRunn);
				th.start();
			}
		});
		
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		//Allocate current activity to null as life cycle changes to pause.
		MainActivity.sCurrentAct = null;
	}
	
	public class PushRunn implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			// Initiate httpPost with loginUrl
			HttpPost httpPost = new HttpPost("http://api.lightspeedmbs.com/v1/push_notification/send.json?key="+MainActivity.appKey);
			
			// The format of notification message should be a json type. 
			List<NameValuePair> pair = new ArrayList<NameValuePair>(1);
			pair.add(new BasicNameValuePair("payload","{android: {alert:This is a Lightspeed Push Notification ,sound:default, vibrate:true, title:Lightspeed}}"));
			
			try {
				// Implement a http request on httpPost 
				httpPost.setEntity(new UrlEncodedFormEntity(pair));
				// Execute the request implemented by httpPost and receive the http response.
				HttpResponse response = MainActivity.httpClient.execute(httpPost);
				
				// Convert InputStream to String
				// The method inputStreamToString is a static method of MainActivity
				String str = MainActivity.inputStreamToString(response.getEntity().getContent());
				Log.i(MainActivity.LOG_TAG,"Response string = "+ str);
				
				// Check whether the post is success
				gMainHandler.post(new CheckPushResult(str));
				
			} catch (ClientProtocolException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	
	public class CheckPushResult implements Runnable{
		private String mResult;
		private JSONObject json;
		public CheckPushResult(String result){
			mResult = result;
		}
		
		public void run(){
			String status = "null";
			
			try{
				// The response is a json format String
				json = new JSONObject(mResult);
				// Get the "status" key of response
				status = json.getJSONObject("meta").getString("status");
				
			}catch(JSONException e){
				e.printStackTrace();
			}
			
			if(status.equals("ok")){
				// status ok. Display success text on resultTextView.
				gShowResult.setText(R.string.push_success);
			}
			
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//Allocate current activity context to sCurrentAct
		MainActivity.sCurrentAct = this;
	}
	
	
}
