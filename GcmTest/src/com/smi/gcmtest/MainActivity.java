package com.smi.gcmtest;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class MainActivity extends ActionBarActivity {

	
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	public static final String TAG = "GCMDemo";
	
	private String SENDER_ID = "YOUR SENDER/PROJECT ID";
	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	
	private SharedPreferences prefs;
	private TextView mDisplay;
	private Context context;
	
	private String regId;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mDisplay = (TextView) findViewById(R.id.display);
        context = getApplicationContext();
        
        mDisplay.append("Project ID: " + SENDER_ID);
        if(checkPlayServices()){
        	gcm = GoogleCloudMessaging.getInstance(this);
        	regId = getRegistrationId(context);
        	
        	if(regId.isEmpty()){
        		registerInBackground();
        	}else{
        		mDisplay.append("\nRegId: " + regId);
        	}
        }else{
        	Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	checkPlayServices();
    }


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if(resultCode != ConnectionResult.SUCCESS){
			if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			}else{
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			
			return false;
		}
		
		return true;
	}
    
    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context){
    	final SharedPreferences prefs = getGCMPreferences(context);
    	String registrationId = prefs.getString(PROPERTY_REG_ID, "");
    	
    	if(registrationId.isEmpty()){
    		Log.i(TAG, "Registration not found.");
    		return "";
    	}
    	
    	// Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
    	int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
    	int currentVersion = getAppVersion(context);
    	if(registeredVersion != currentVersion){
    		Log.i(TAG, "App version changed");
    		return "";
    	}
    	
    	return registrationId;
    }
    
    private SharedPreferences getGCMPreferences(Context context){
    	return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }
    
    private static int getAppVersion(Context context){
    	try{
    		PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    		return packageInfo.versionCode;
    	}catch(NameNotFoundException e){
    		throw new RuntimeException("Could not get package name: " + e);
    	}
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground(){
    	new RegisterTask().execute();
    }
    
    private void sendRegistrationIdToBackend() {
		
	}
    
    private void storeRegistrationId(Context context, String regId){
    	final SharedPreferences prefs = getGCMPreferences(context);
    	int appVersion = getAppVersion(context);
    	Log.i(TAG, "Saving regId on app version " + appVersion);
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putString(PROPERTY_REG_ID, regId);
    	editor.putInt(PROPERTY_APP_VERSION, appVersion);
    	editor.commit();
    }
    
    private class RegisterTask extends AsyncTask<Void, String, String>{
    	@Override
        protected String doInBackground(Void... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regId = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regId;

                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                sendRegistrationIdToBackend();

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                // Persist the regID - no need to register again.
                storeRegistrationId(context, regId);
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            mDisplay.append(msg + "\n");
        }
    }
}
