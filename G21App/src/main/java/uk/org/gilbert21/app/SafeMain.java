package uk.org.gilbert21.app;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import uk.org.gilbert21.app.FusedLocationService;
import uk.org.gilbert21.app.R;
import uk.org.gilbert21.app.RecordManagerActivity;
import uk.org.gilbert21.app.SettingsActivity;

public class SafeMain extends Activity {

    //implements ConnectionCallbacks, OnConnectionFailedListener {

    private static String mFileName = null;
    private static String mLatitude = null;
    private static String mLongitude = null;
    private static String mTime = null;
    private static String mAcuracy = null;
    private static String mBearing = null;
    private static String mAltitude = null;
    private static MediaRecorder mRecorder = null;
    /* rtmp */
    private static LocationManager locationManager = null;
    private static LocationListener locationListener = null;

    private static boolean mExternalStorageAvailable = false;
    private static boolean mExternalStorageWriteable = false;
    private static boolean mRecording = false;
    private static boolean mWaitForGPS = true;
    private static boolean mKeepGPSRunning = false;

    private static FusedLocationService fusedLocationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        fusedLocationService = new FusedLocationService(this);

        setContentView(R.layout.activity_main);

        setListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:

                //AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                //alertDialog.setTitle("Placeholder");
                //alertDialog.setMessage("Replace with preferences thingy.");
                //alertDialog.show();

                Intent prefs = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(prefs);

                return true;

            /*
            case R.id.action_record_manager:

                Intent rm = new Intent(getApplicationContext(), RecordManagerActivity.class);
                startActivity(rm);

                return true;
            */

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (locationManager == null){
            // Acquire a reference to the system Location Manager
            locationManager =  (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    makeUseOfNewLocation(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };
            // Register the listener with the Location Manager to receive location updates
            // Second and third parameters are minimum time in milliseconds between updates and
            // minimum distance between updates in metres
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, locationListener);
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 10, locationListener);
        }

        setMicrophoneImage();
    }

    @Override
    public void onPause() {
        super.onPause();

        appNotActive();
    }

    @Override
    public void onStop() {
        super.onStop();

        appNotActive();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mKeepGPSRunning = false;
        appNotActive();
    }

    private void appNotActive(){

        // Remove the listener you previously added
        if (locationManager != null && !mKeepGPSRunning) {
            locationManager.removeUpdates(locationListener);
            locationManager = null;
        }

        if (mRecording) {
            //Complete the recording
            microphoneClicked();
        }

        mLatitude = null;
        mLongitude = null;
        mTime = null;
        mFileName = null;

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void setListeners() {

        ImageView microphone = (ImageView) findViewById(R.id.microphone);
        microphone.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                microphoneClicked();
            }
        });
    }

    public void chkKeepGPSOnClicked(View view) {
        // Is the view now checked?
        boolean isChecked = ((CheckBox) view).isChecked();
        if (isChecked) {
            mKeepGPSRunning = true;
        }else{
            mKeepGPSRunning = false;
        }
    }

    public void chkWaitForGPSClicked(View view) {
        // Is the view now checked?
        boolean isChecked = ((CheckBox) view).isChecked();
        if (isChecked) {
            mWaitForGPS = true;
        }else{
            mWaitForGPS = false;
        }
        setMicrophoneImage();
    }

    private void microphoneClicked() {

        if (mRecording) {
            mRecording = false;
            endRecording();
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 300);
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 300);
        } else {
            /* rtmp
            if (mWaitForGPS) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            }
            */

            if (mLatitude != null || !mWaitForGPS) {

                startRecording();
                if (mRecorder != null) {
                    mRecording = true;

                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 300);
                    toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 300);
                }

                /*
                String locationResult = "Latitude: " + mLatitude + "\n" +
                        "Longitude: " + mLongitude + "\n" +
                        "Altitude: " + mAltitude + "\n" +
                        "Accuracy: " + mAcuracy + "\n" +
                        "Bearing: " + mBearing;
                Toast.makeText( getApplicationContext(), locationResult, Toast.LENGTH_LONG).show();
                */
            }
        }
        setMicrophoneImage();

        rtmp();
    }


    private void rtmp() {

        Location location = fusedLocationService.getLocation();
        String locationResult = "";
        if (null != location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy();

            //double elapsedTimeSecs = (double) location.getElapsedRealtimeNanos()
            //        / 1000000000.0;
            String elapsedTimeSecs = "not avail";

            String provider = location.getProvider();
            double altitude = location.getAltitude();
            locationResult = "Latitude: " + latitude + "\n" +
                    "Longitude: " + longitude + "\n" +
                    "Altitude: " + altitude + "\n" +
                    "Accuracy: " + accuracy + "\n" +
                    "Elapsed Time: " + elapsedTimeSecs + " secs" + "\n" +
                    "Provider: " + provider + "\n";
        } else {
            locationResult = "Location Not Available!";
        }
        Toast.makeText( getApplicationContext(), locationResult, Toast.LENGTH_LONG).show();
    }

    private void setMicrophoneImage() {

        ImageView imageView = (ImageView) findViewById(R.id.microphone);
        Drawable newPhoneImage;

        if (mRecording){
            newPhoneImage = getResources().getDrawable(R.drawable.redrecord);
        } else {
            if (mLatitude == null) {
                if (mWaitForGPS) {
                    newPhoneImage = getResources().getDrawable(R.drawable.greyrecord);
                }else{
                    newPhoneImage = getResources().getDrawable(R.drawable.yellowrecord);
                }
            } else {
                newPhoneImage = getResources().getDrawable(R.drawable.greenrecord);
            }
        }
        imageView.setImageDrawable(newPhoneImage);
    }

    private void startRecording() {

        checkMediaAvailability();
        if (!mExternalStorageAvailable) {
            Toast.makeText( getApplicationContext(), R.string.cant_read_external_storage, Toast.LENGTH_LONG).show();
            return;
        }
        if (!mExternalStorageWriteable) {
            Toast.makeText( getApplicationContext(), R.string.cant_write_external_storage, Toast.LENGTH_LONG).show();
            return;
        }


        getFileName();
        if (mFileName == null) {
            Toast.makeText( getApplicationContext(), "Can't create record sound file", Toast.LENGTH_LONG).show();
            return;
        } else {
            //Toast.makeText( getApplicationContext(), mFileName, Toast.LENGTH_LONG).show();
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);


        try {
            mRecorder.prepare();
        } catch (IOException e) {
            mRecorder = null;
            Toast.makeText( getApplicationContext(), "Can't prepare recorder", Toast.LENGTH_LONG).show();
            return;
        }

        mRecorder.start();
    }

    private void endRecording() {

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        setMicrophoneImage();
    }

    private void checkMediaAvailability() {

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            //Toast.makeText( getApplicationContext(), "we can mount", Toast.LENGTH_LONG).show();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
    }

    private void getFileName() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/Android/data/";
        mFileName += getApplicationContext().getPackageName();
        mFileName += "/files";

        File folderData = new File(mFileName);
        boolean success = true;
        if (!folderData.exists()) {
            //Toast.makeText( getApplicationContext(), mFileName + " doesnt exist", Toast.LENGTH_LONG).show();
            success = folderData.mkdirs();
        }


        if (!success) {
            //Toast.makeText( getApplicationContext(), "failed to make folder", Toast.LENGTH_LONG).show();
            mFileName = null;
            return;
        }


        mFileName += "/";
        mFileName += mTime;
        mFileName += "_";
        mFileName += Long.toString(System.currentTimeMillis());
        mFileName += "_";
        mFileName += mLatitude;
        mFileName += "_";
        mFileName += mLongitude;
        mFileName += ".3gp";


        /*
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date curDateTime = new Date(System.currentTimeMillis());
        String currentDateandTime = sdf.format(curDateTime);

        mFileName += "/";
        mFileName += currentDateandTime;
        mFileName += "_";
        mFileName += mLatitude;
        mFileName += "_";
        mFileName += mLongitude;
        mFileName += ".3gp";
        */

        //Toast.makeText( getApplicationContext(), mFileName, Toast.LENGTH_LONG).show();
    }

    protected void makeUseOfNewLocation(Location location) {

        //Toast.makeText( getApplicationContext(), doubleToString(location.getLatitude(), "#0.00000"), Toast.LENGTH_LONG).show();

        if (mLatitude == null){
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 300);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 300);
        }

        mLatitude = doubleToString(location.getLatitude(), "#0.00000");
        mLongitude = doubleToString(location.getLongitude(), "#0.00000");
        mAcuracy = doubleToString(location.getAccuracy(), "#0.00000");
        mBearing = doubleToString(location.getBearing(), "#0.00000");
        mAltitude = doubleToString(location.getAltitude(), "#0.00000");

        mTime = Long.toString(location.getTime());

        setMicrophoneImage();
    }

    private String doubleToString(double d, String pattern){

        DecimalFormat df =(DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        df.applyLocalizedPattern(pattern);

        return df.format(d);
    }

    private boolean isGooglePlayServicesAvailable() {

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }
}



