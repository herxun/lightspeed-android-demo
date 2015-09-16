package com.demo.lightspeeddemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.arrownock.exception.ArrownockException;
import com.arrownock.push.AnPush;
import com.arrownock.push.AnPushCallbackAdapter;
import com.arrownock.push.AnPushStatus;
import com.arrownock.push.IAnPushRegistrationCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	static final String LOG_TAG = "LightSpeedDemo";

	// sCurrentAct will reference to current Activity context as Activity's onResume() is invoked.
	static Context sCurrentAct = null;

	static HttpClient httpClient;
	static Typeface gFont;

	private EditText gEditName;
	private EditText gEditPass;
	private ImageButton gButtonLogin;
	private Handler gMainhandler;
	private TextView gTextResult;

	private SharedPreferences gSharePref;
	private SharedPreferences.Editor gEditor;
	private final String LIGHT_SPEED_PREF = "Lightspeed";
	private final String SAVED_LOGIN_NAME = "saved_login_name";
	private final String SAVED_LOGIN_PASS = "saved_login_pass";
	private final String REGISTER_TIME_STAMP = "register_time_stamp";
	private final int PushActResult = 0x01;
	private final long RegisterDelay = 604800000l;

	private AnPush gAnPush;
	private boolean gPushServiceEnalbed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		checkBundle();
		
		// Retrieve view from XML
		gEditName = (EditText) findViewById(R.id.editName);
		gEditPass = (EditText) findViewById(R.id.editPass);
		gTextResult = (TextView) findViewById(R.id.showResult);

		// Set font type. The font file is under assests directory.
		gFont = Typeface.createFromAsset(this.getAssets(), "AvenirNextLTPro-Regular.ttf");
		gTextResult.setTypeface(gFont);
		gEditName.setTypeface(gFont);
		gEditPass.setTypeface(gFont);

		// Generate Sharepreference which can save & retrieve persistent key-value pairs of data.
		// Scenario: As login is success, we'll save login email & password.
		// And we'll display saved data on EditText directly at next launch.
		gSharePref = this.getSharedPreferences(LIGHT_SPEED_PREF, MODE_PRIVATE);
		gEditor = gSharePref.edit();
		gEditName.setText(gSharePref.getString(SAVED_LOGIN_NAME, ""));
		gEditPass.setText(gSharePref.getString(SAVED_LOGIN_PASS, ""));

		// Instantiate HTTP Client.
		// Later, we'll use httpClient to do url post.
		httpClient = new DefaultHttpClient();

		// Handler binding with main thread. Handler allow you to send task to thread.
		// Later, we need this handler to post UI task to main thread.
		gMainhandler = new Handler();

		// The channel names we'll register at Lightspeed.
		List<String> channels = new ArrayList<String>();
		channels.add("channel1");
		channels.add("channel2");
		channels.add("channel3");

		try {

			// Instantiate Lightspeed AnPush instance which is an entry point of Lightspeed service.
			gAnPush = AnPush.getInstance(getBaseContext());
			gAnPush.setCallback(new AnPushCallbackAdapter() {

				@Override
				public void statusChanged(AnPushStatus currentStatus, ArrownockException exception) {
					
					if (currentStatus == AnPushStatus.ENABLE) {
						Log.i(LOG_TAG,"Push status enalbed");
						switchPushBtn(true);
						
					} else if (currentStatus == AnPushStatus.DISABLE) {
						Log.e(LOG_TAG,"Push status disabled");
						switchPushBtn(false);
					}
					
					if (exception != null) {
						Log.e(LOG_TAG,"Push status changed with error occuring = "+ exception.toString() );
						switchPushBtn(false);
					}
				}
			});

			// Register channels to Lightspeed service.
			// If register is success, the callback function onSuccess() in IAnPushRegistrationCallback would be invoked.
			Log.i(LOG_TAG, "Register push channels");
			
			if(needRegister()){
				gAnPush.register(channels, new IAnPushRegistrationCallback() {
					@Override
					public void onSuccess(String s) {
						gEditor.putLong(REGISTER_TIME_STAMP, Calendar.getInstance().getTimeInMillis()).commit();
						switchPushBtn(true);
						try {
							gAnPush.enable();
						} catch (ArrownockException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public void onError(ArrownockException e) {
						Log.e(LOG_TAG, "Register with error = " + e.getMessage());
					}
				});
			}else{
				gAnPush.enable();
				switchPushBtn(true);
			}

		} catch (ArrownockException ex) {
			// If there's any error occur during register procedure, we'll print error message on Logcat.
			ex.printStackTrace();
		}

		// Set click event for login button.
		// Scenario: We'll post user's email & password to Ligtspeed LoginUrl and retrieve the response see if the login is success.
		gButtonLogin = (ImageButton) findViewById(R.id.buttonLogin);
		gButtonLogin.setOnClickListener(new OnClickListener() {

			@Override
			// As click event invoked, system would automatically execute onClick method.
			public void onClick(View arg0) {

				// This is a custom runnable calss handling login procedure.
				// Send email, password and loginUrl as arguments to create this class.
				// This is the login URL of Lightspeed.
				String api_server_address = getString(R.string.api_server_address);
				String loginUrl = "http://"+api_server_address+"/v1/admins/login.json";				
				loginRunnHttp logHttp = new loginRunnHttp(gEditName.getText().toString(), gEditPass.getText().toString(), loginUrl);

				// We can't perform a network operation on main thread so that we need to create a new thread to handle the network operation.
				Thread th = new Thread(logHttp);
				th.start();
			}

		});
	}
	
	private void checkBundle(){
		Bundle b = getIntent().getExtras();
		if( b != null ){
			Iterator<String> it = b.keySet().iterator();
			if( it.hasNext() ){
				Log.i(LOG_TAG,"key = " + it.next());
			}
		}
	}
	
	public void switchPushBtn(Boolean bool){
		if( bool == true ){
			
			if (PushActivity.sPushHandler == null) {
				// sPushHandler null means PushActivity is not running yet.
				// This boolean value would be carried with intent as starting PushActivity.
				gPushServiceEnalbed = true;
			} else {
				
				// PushActivity is running.
				// Send message to Handler of PushActivity to enable push button.
				PushActivity.sPushHandler.obtainMessage(PushActivity.PUSH_ENABLE).sendToTarget();
			}
		}
		else{
			
			if (PushActivity.sPushHandler == null) {
				gPushServiceEnalbed = false;
			} else {
				PushActivity.sPushHandler.obtainMessage(PushActivity.PUSH_DISABLE).sendToTarget();
			}
		}
		
		
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Allocate current activity to null as life cycle changes to pause..
		sCurrentAct = null;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Allocate current activity context to sCurrentAct
		sCurrentAct = this;

		// Fill blank into result TextView so that it won't display former text such as Login Success.
		gTextResult.setText("");
	}

	public class loginRunnHttp implements Runnable {
		private String mName;
		private String mPass;
		private String mUrlAddr;

		public loginRunnHttp(String name, String pass, String url) {
			mName = name;
			mPass = pass;
			mUrlAddr = url;
		}

		@Override
		public void run() {

			// Instantiate the entity of httpPost with loginUrl
			HttpPost httpPost = new HttpPost(mUrlAddr);

			// Put the email and password in key-value pair into a List.
			// This is the data we're going to post to loginUrl.
			List<NameValuePair> pair = new ArrayList<NameValuePair>(2);
			pair.add(new BasicNameValuePair("email", mName));
			pair.add(new BasicNameValuePair("password", mPass));

			try {

				// UrlEncodedFromEntity transform the pair list into HTTP request entity.
				// setEntity will send request to loginUrl.
				httpPost.setEntity(new UrlEncodedFormEntity(pair));

				// httpClient execute() would receive the response from server.
				HttpResponse response = httpClient.execute(httpPost);

				// The response is a InputStream type. Here convert InputStream into String.
				String str = inputStreamToString(response.getEntity().getContent());

				// Check whether the login is success or fail.
				CheckLoginResultRunn checkRun = new CheckLoginResultRunn(str);
				gMainhandler.post(checkRun);

			} catch (ClientProtocolException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}

	public class CheckLoginResultRunn implements Runnable {
		private String resultString;
		private JSONObject json;

		public CheckLoginResultRunn(String str) {
			resultString = str;
		}

		public void run() {
			String status = "null";

			try {
				// The response is a json format String
				json = new JSONObject(resultString);
				// Get the value of the key "status".
				status = (String) json.getJSONObject("meta").get("status");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (status.equals("ok")) {
				// status ok, refer to login success. Display success text on resultTextView.
				gTextResult.setText(R.string.login_success);

				// Save email and password into SharePreference
				gEditor.putString(SAVED_LOGIN_NAME, gEditName.getText().toString());
				gEditor.putString(SAVED_LOGIN_PASS, gEditPass.getText().toString());
				gEditor.commit();

				// Start PushActivity by intent.
				// Intent could be think as the glue between activities. It could be used to launch activity.
				Intent intent = new Intent();
				intent.putExtra("pushService", gPushServiceEnalbed);
				intent.setClass(getApplicationContext(), PushActivity.class);
				startActivityForResult(intent, PushActResult);
			} else {
				try {
					// If status is not ok, get the error message in response and display on resultTextView.
					String error = (String) json.getJSONObject("meta").get("message");
					gTextResult.setText(error);
					gTextResult.setTextColor(0xFF00676F);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}
	}

	// Method to create AlertDialog.
	static AlertDialog CreateMsgDialog(Context ctx, String title, String msg) {

		Builder build = new AlertDialog.Builder(ctx);

		build.setTitle(title);
		build.setMessage(msg);
		return build.create();
	}

	static String inputStreamToString(InputStream is) {
		String line = "";

		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		try {
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Return full string
		return total.toString();
	}

	private Boolean needRegister(){
		Long now = Calendar.getInstance().getTimeInMillis();
		Long last = gSharePref.getLong(REGISTER_TIME_STAMP, 0);
		if(now - last >= RegisterDelay){
			return true;
		}else{
			return false;
		}
	}
}
