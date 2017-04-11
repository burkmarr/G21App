package uk.org.gilbert21.app;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private String mFileName = null;
    private MediaRecorder mRecorder = null;
    private ExtAudioRecorder mRecorderWav = null;
    private String mLatitude = null;
    private String mLongitude = null;
    private String mAccuracy = null;
    private String mBearing = null;
    private String mAltitude = null;
    private String mTime = null;
    private String mOSGR = null;

    private int mCount = 0;

    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private boolean mRecording = false;
    private boolean mPlaying = false;
    private boolean mWaitForGPS = true;
    private boolean mKeepGPSRunning = false;
    private boolean mUseShake = false;
    private boolean mStayAwake = false;
    private boolean mRecOnResume = false;
    private boolean mIgnoreRecOnResume = true;
    private boolean mDarken = false;
    private boolean mTracking = false;
    private boolean mReplay = false;
    private String mFileNameFormat = "G21";
    private String mFileType = "wav";

    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest = null;
    private static Location mCurrentLocation = null; //Static????
    private GPX mGPX = null;
    private MediaPlayer mPlayer = null;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        buildGoogleApiClient();

        setListeners();

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                microphoneClicked();
            }
        });

        //Set the media stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
                mIgnoreRecOnResume = true;
	            Intent prefs = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(prefs);
	            return true;

	        case R.id.action_record_manager:
                mIgnoreRecOnResume = true;
	        	Intent rm = new Intent(getApplicationContext(), RecordManagerActivity.class);
                startActivity(rm);
	        	return true;

	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
    public void onResume() {
        super.onResume();

        //Get preference values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //For testing
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.clear();
        //editor.commit();

        mKeepGPSRunning = prefs.getBoolean("pref_keep_gps_running", false);
        mStayAwake = prefs.getBoolean("pref_stay_awake", false);
        mUseShake = prefs.getBoolean("pref_use_shake", false);
        mRecOnResume = prefs.getBoolean("pref_rec_on_resume", false);
        mDarken = prefs.getBoolean("pref_darken", false);
        mFileNameFormat = prefs.getString("pref_output_format", "G21");
        mFileType = prefs.getString("pref_file_type_format", "wav");
        mTracking = prefs.getBoolean("pref_use_tracking", false);
        mReplay = prefs.getBoolean("pref_replay", false);

        int shakeSensitivity = Integer.parseInt(prefs.getString("pref_shake_sensitivity", "1"));
        //Set the sensitivity of the shake class
        switch (shakeSensitivity) {
            case 1:
                mShakeDetector.setShakeThresholdGravity(2.75F);
                break;
            case 2:
                mShakeDetector.setShakeThresholdGravity(2.3F);
                break;
            case 3:
                mShakeDetector.setShakeThresholdGravity(1.9F);
                break;
            case 4:
                mShakeDetector.setShakeThresholdGravity(1.5F);
                break;
            default:
                mShakeDetector.setShakeThresholdGravity(2.75F);
                break;
        }

        //Add the following line to register the Session Manager Listener onResume
        if (mUseShake) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        //Keep window awake?
        if (mStayAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        //Show error dialog if GooglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            problem();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.location_services_unavailable))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        //Set up location request object if it is null
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        //Set screen background
        LinearLayout ll = (LinearLayout) findViewById(R.id.main_layout);
        if (mDarken) {
            ll.setBackgroundColor(Color.BLACK);
        }else{
            ll.setBackgroundColor(Color.WHITE);
        }

        //Do UI updates
        updateUI();

        //Start recording if relevant flags are et
        if (mRecOnResume && !mIgnoreRecOnResume) {
            microphoneClicked();
        }
        mIgnoreRecOnResume = false;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        appNotActive();

        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
    }
    
    @Override
    public void onStop() {
        super.onStop();
       
        appNotActive();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove the listener you previously added
        //Toast.makeText( getApplicationContext(), "GPS keep: " + mKeepGPSRunning, Toast.LENGTH_LONG).show();
        if (mGoogleApiClient != null && !mKeepGPSRunning) {
            //Toast.makeText( getApplicationContext(), "Removing listener", Toast.LENGTH_LONG).show();
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        //Stop tracking if active
        if (mGPX != null) {
            mGPX.close();
        }

        appNotActive();
    }

    @Override
    public void onConnected(Bundle bundle) {

        //Belongs to GoogleApiClient.ConnectionCallbacks
        //Provided by Google API Client and called when the client is ready
        //Toast.makeText( getApplicationContext(), "onConnected", Toast.LENGTH_LONG).show();

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Belongs to GoogleApiClient.ConnectionCallbacks
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Belongs to GoogleApiClient.ConnectionCallbacks
    }

    @Override
    public void onLocationChanged(Location location) {

        //Belongs to LocationListener

        //Toast.makeText( getApplicationContext(), doubleToString(location.getLatitude(), "#0.00000"), Toast.LENGTH_LONG).show();

        //Gives a tone if this is first GPS fix
        if (mCurrentLocation == null){
            //makeTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE);
        }
        mCurrentLocation = location;
        mCount++;

        //Write to GPX file if necessary
        if (mGPX != null) {
            mGPX.addLoc(location);
        }

        updateUI();
    }

    public void updateUI() {

        mLatitude = "";
        mLongitude = "";
        mAccuracy = "";
        mBearing = "";
        mAltitude = "";
        mTime="";
        String osgr = "      ";
        String easting = "       ";
        String northing = "       ";

        if (mCurrentLocation != null) {
            mLatitude = doubleToString(mCurrentLocation.getLatitude(), "#0.00000");
            mLongitude = doubleToString(mCurrentLocation.getLongitude(), "#0.00000");
            mAccuracy = doubleToString(mCurrentLocation.getAccuracy(), "#0");
            mBearing = doubleToString(mCurrentLocation.getBearing(), "#0.00000");
            mAltitude = doubleToString(mCurrentLocation.getAltitude(), "#0");
            mTime = Long.toString(mCurrentLocation.getTime());

            LatLng latlng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            latlng.toOSGB36();
            OSRef osRef = latlng.toOSRef();
            osgr = osRef.toSixFigureString();
            easting = doubleToString(osRef.getEasting(),"#0000000");
            northing = doubleToString(osRef.getNorthing(),"#0000000");
        }
        TextView tv = null;

        tv = (TextView) findViewById(R.id.textLoc);
        if (mLatitude=="") {
            tv.setText("");
        }else {
            tv.setText(mLatitude + " / " + mLongitude + " (" + Integer.toString(mCount) + ")");
        }

        tv = (TextView) findViewById(R.id.textAlt);
        if (mAltitude=="") {
            tv.setText("");
        }else {
            tv.setText("Altitude: " + mAltitude + " m");
        }

        tv = (TextView) findViewById(R.id.textAcc);
        if (mAccuracy == "") {
            tv.setText("");
        }else{
            tv.setText("Accuracy: " + mAccuracy + " m");
        }

        tv = (TextView) findViewById(R.id.textOsgrPrefix);
        tv.setText(osgr.substring(0,2));
        tv = (TextView) findViewById(R.id.textOsgrEasting123);
        tv.setText(easting.substring(2,5));
        tv = (TextView) findViewById(R.id.textOsgrEasting4);
        tv.setText(easting.substring(5,6));
        tv = (TextView) findViewById(R.id.textOsgrEasting5);
        tv.setText(easting.substring(6,7));

        tv = (TextView) findViewById(R.id.textOsgrNorthing123);
        tv.setText(northing.substring(2,5));
        tv = (TextView) findViewById(R.id.textOsgrNorthing4);
        tv.setText(northing.substring(5,6));
        tv = (TextView) findViewById(R.id.textOsgrNorthing5);
        tv.setText(northing.substring(6,7));

        //Set mOSGR to 8 fig GR
        mOSGR = osgr.substring(0,2) + easting.substring(2,6)+ northing.substring(2,6);


        setMicrophoneImage();
        setGPXImages();
    }

    protected synchronized void buildGoogleApiClient(){

        //Toast.makeText( getApplicationContext(), "buildGoogleApiClient", Toast.LENGTH_LONG).show();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private boolean isGooglePlayServicesAvailable() {

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            problem();
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    private void appNotActive(){

        if (mRecording) {
            //Complete the recording
            microphoneClicked();
        }

        mFileName = null;

        if (mFileType.equals("wav")) {
            if (mRecorderWav != null) {
                mRecorderWav.release();
                mRecorderWav = null;
            }
        } else {
            if (mRecorder != null) {
                mRecorder.release();
                mRecorder = null;
            }
        }
    }
	
	private void setListeners() {
		
		ImageView microphone = (ImageView) findViewById(R.id.microphone);
		microphone.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				microphoneClicked();
			}
		});

        ImageView imageBin = (ImageView) findViewById(R.id.imageBin);
        imageBin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                cancelRecording();
            }
        });

        ImageView imageGPX = (ImageView) findViewById(R.id.imageGPX);
        imageGPX.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                gpxClicked();
            }
        });

        ImageView imageBinGPX = (ImageView) findViewById(R.id.imageBinGPX);
        imageBinGPX.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                cancelGPX();
            }
        });
	}

    private void gpxClicked(){
        //Tracking
        if (mGPX == null) {
            //Start tracking to file
            String gpxFile = getFileName(true);
            if (gpxFile != null) {
                makeTone(ToneGenerator.TONE_PROP_BEEP);
                mGPX = new GPX(gpxFile);
            }
        } else {
            //Stop tracking

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle(getString(R.string.end_track_dlg_title))
                    .setMessage(getString(R.string.end_track_dlg_question))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(getString(R.string.end_track_dlg_yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            makeTone(ToneGenerator.TONE_PROP_BEEP);
                            mGPX.close();
                            mGPX = null;
                            setGPXImages();
                        }
                    })
                    .setNegativeButton(getString(R.string.end_track_dlg_no), null)						//Do nothing on no
                    .show();
        }
        setGPXImages();
    }

    private void cancelGPX(){
        if (mGPX != null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle(getString(R.string.delete_track_dlg_title))
                    .setMessage(getString(R.string.delete_track_dlg_question))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(getString(R.string.delete_track_dlg_yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            makeTone(ToneGenerator.TONE_PROP_NACK);
                            mGPX.close();
                            mGPX.delete();
                            mGPX = null;
                            setGPXImages();
                        }
                    })
                    .setNegativeButton(getString(R.string.delete_track_dlg_no), null)						//Do nothing on no
                    .show();
        }
        setGPXImages();
    }

	private void microphoneClicked() {
		
		if (mRecording) {
            endRecording(false);
            makeTone(ToneGenerator.TONE_PROP_BEEP2);
        } else if (mPlaying){
            mPlayer.stop();
            mPlaying = false;
		} else {
            /*
            if (mWaitForGPS) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            }
            */

			if (mCurrentLocation != null || !mWaitForGPS) {
				
				startRecording();

                if (mFileType.equals("wav")) {
                    if (mRecorderWav != null) {
                        mRecording = true;
                        makeTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                } else {
                    if (mRecorder != null) {
                        mRecording = true;
                        makeTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
	    	}
		}
		setMicrophoneImage();
	}

    private void cancelRecording() {
        if (mRecording) {
            endRecording(true);
            //Delete the file
            File file = new File(mFileName);
            file.delete();

            makeTone(ToneGenerator.TONE_PROP_NACK);
        }
    }

    private void setGPXImages(){
        ImageView imageGPX = (ImageView) findViewById(R.id.imageGPX);
        Drawable newImageGPX;
        ImageView imageBinGPX = (ImageView) findViewById(R.id.imageBinGPX);
        Drawable newImageBinGPX;
        int visibility = 0;

        if (mTracking){
            visibility = View.VISIBLE;
            if (mGPX == null) {
                if (mCurrentLocation == null) {
                    newImageGPX = getResources().getDrawable(R.drawable.locationgrey);
                }else{
                    newImageGPX = getResources().getDrawable(R.drawable.locationgreen);
                }
                newImageBinGPX = getResources().getDrawable(R.drawable.greybin);
            } else {
                newImageGPX = getResources().getDrawable(R.drawable.locationred);
                newImageBinGPX = getResources().getDrawable(R.drawable.orangebin);
            }
        } else {
            visibility = View.INVISIBLE;
            newImageGPX = getResources().getDrawable(R.drawable.locationgrey);
            newImageBinGPX = getResources().getDrawable(R.drawable.greybin);
        }
        imageGPX.setImageDrawable(newImageGPX);
        imageBinGPX.setImageDrawable(newImageBinGPX);
        imageGPX.setVisibility(visibility);
        imageBinGPX.setVisibility(visibility);
    }

	private void setMicrophoneImage() {
		
		ImageView micImage = (ImageView) findViewById(R.id.microphone);
		Drawable newPhoneImage;
        ImageView binImage = (ImageView) findViewById(R.id.imageBin);
        Drawable newBinImage;
		
		if (mRecording) {
            newPhoneImage = getResources().getDrawable(R.drawable.redrecord);
            newBinImage = getResources().getDrawable(R.drawable.orangebin);
        } else if (mPlaying) {
            newPhoneImage = getResources().getDrawable(R.drawable.playbackred);
            newBinImage = getResources().getDrawable(R.drawable.greybin);
		} else {
            newBinImage = getResources().getDrawable(R.drawable.greybin);
			if (mCurrentLocation == null) {
				if (mWaitForGPS) {
					newPhoneImage = getResources().getDrawable(R.drawable.greyrecord);
				}else{
					newPhoneImage = getResources().getDrawable(R.drawable.yellowrecord);
				}
			} else {
				newPhoneImage = getResources().getDrawable(R.drawable.greenrecord);
			}
		}
        micImage.setImageDrawable(newPhoneImage);
        binImage.setImageDrawable(newBinImage);
	}
	
	private void startRecording() {
		
		checkMediaAvailability();
		if (!mExternalStorageAvailable) {
            problem();
			Toast.makeText( getApplicationContext(), R.string.cant_read_external_storage, Toast.LENGTH_LONG).show();
			return;
		}
		if (!mExternalStorageWriteable) {
            problem();
			Toast.makeText( getApplicationContext(), R.string.cant_write_external_storage, Toast.LENGTH_LONG).show();
			return;
		}

        mFileName = getFileName(false);
    	if (mFileName == null) {
            problem();
    		Toast.makeText( getApplicationContext(), "Can't create record sound file", Toast.LENGTH_LONG).show();
			return;
    	}

        if (mFileType.equals("wav")) {
            mRecorderWav = ExtAudioRecorder.getInstanse(false); // Uncompressed recording (WAV)
            mRecorderWav.setOutputFile(mFileName);
            mRecorderWav.prepare();
            mRecorderWav.start();
        }else{
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                problem();
                mRecorder = null;
                Toast.makeText( getApplicationContext(), "Can't prepare recorder", Toast.LENGTH_LONG).show();
                return;
            }
            mRecorder.start();
        }
    }

    private void endRecording(boolean isCanceled) {
        // Stop recording
        if (mFileType.equals("wav")) {
            mRecorderWav.stop();
            mRecorderWav.release();
            mRecorderWav = null;
        }else{
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        mRecording = false;

        //Replay if requested
        if (mReplay && !isCanceled) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    mPlaying = false;
                    setMicrophoneImage();
                }
            });

            try {
                mPlaying = true;
                mPlayer.setDataSource(getApplicationContext(), Uri.parse(mFileName));
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                mPlaying = false;
                Toast.makeText( getApplicationContext(), getString(R.string.cant_replay) + mFileName, Toast.LENGTH_LONG).show();
            }
        }
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

    private String getFileName(boolean isGPX) {

        String fileName = null;

   	 	fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        fileName += "/Android/data/";
        fileName += getApplicationContext().getPackageName();
        fileName += "/files";
        
        File folderData = new File(fileName);
        boolean success = true;
        if (!folderData.exists()) {
        	//Toast.makeText( getApplicationContext(), fileName + " doesnt exist", Toast.LENGTH_LONG).show();
            success = folderData.mkdirs();
        }
        
        if (!success) {
        	//Toast.makeText( getApplicationContext(), "failed to make folder", Toast.LENGTH_LONG).show();
            problem();
        	return null;
        }

        fileName += "/";
        if (mFileNameFormat.equals("G21") && !isGPX) {

            fileName += mTime;
            fileName += "_";
            fileName += Long.toString(System.currentTimeMillis());
            fileName += "_";
            fileName += mLatitude;
            fileName += "_";
            fileName += mLongitude;
        }else {
            fileName += DateFormat.format("yyyy-MM-dd_kk-mm-ss", new java.util.Date());
            if (!isGPX) {
                fileName += "_";
                if (mFileNameFormat.equals("OSGR")) {

                    fileName += mOSGR;
                } else {
                    fileName += mLatitude;
                    fileName += "_";
                    fileName += mLongitude;
                }
                fileName += "_";
                fileName += mAccuracy;
                fileName += "_";
                fileName += mAltitude;
            }
        }
        fileName += ".";

        if (isGPX) {
            fileName += "gpx";
        }else{
            fileName += mFileType;
        }

        return fileName;
   }

   private String doubleToString(double d, String pattern){

       DecimalFormat df =(DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
       df.applyLocalizedPattern(pattern);

       return df.format(d);
   }

   private void makeTone(int tone){
       ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
       toneG.startTone(tone);
   }

   private void problem(){
       makeTone(ToneGenerator.TONE_CDMA_HIGH_L);
   }
}


