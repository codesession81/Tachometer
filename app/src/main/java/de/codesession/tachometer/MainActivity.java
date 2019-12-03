package de.codesession.tachometer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity {

    /*
    Views
     */
    private TextView tv_speedField,showCoordinates,showHight,showWith;
    private RadioButton rb_setMeter, rb_setKmh, rb_setMph;

    /*
    Instanzvariablen
     */
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private long UPDATE_INTERVAL = 100;  /* 10/10 secs */
    private long FASTEST_INTERVAL = 10; /* 100 ms */
    private double hoehe, breite;
    private float speed;
    private int speedCast;
    private String speedString;
    private String city,stadt;
    private int selectedSpeedUnit;


    /*
    Systemkomponenten
     */
    private SharedPreferences saveSpeedUnit;
    private SharedPreferences.Editor savedUnitEditor;

    private LocationRequest locationRequest;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Views
         */
        tv_speedField = (TextView) findViewById(R.id.tv_speedField);
        showCoordinates = (TextView) findViewById(R.id.showCoordinates);
        showHight = (TextView) findViewById(R.id.showHight);
        showWith = (TextView) findViewById(R.id.showWith) ;

        rb_setMeter = (RadioButton) findViewById(R.id.rb_setMeter);
        rb_setKmh = (RadioButton) findViewById(R.id.rb_setKmh);
        rb_setMph = (RadioButton) findViewById(R.id.rb_setMph);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        tv_speedField.setText("0");

        /*
        No gps status broadcastreceiver
         */
        getApplicationContext().registerReceiver(gpsReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        isSpeedUnitSet();
        checkRuntimePermissions();
    }

    /*
    Prüfe welches SDK installiert ist und frage nach Laufzeitberechtigungen
    */
    private void checkRuntimePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                startLocationUpdates();
                getLastLocation();

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_PERMISSIONS_CODE);
            }
        }else{
            startLocationUpdates();
            getLastLocation();
        }
    }


    /*
    Prüfe ob Berechtigungen erteilt wurden
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_PERMISSIONS_CODE: {
                if(grantResults.length>0){
                    for(int i=0; i<grantResults.length;i++){
                        if(grantResults[i]==PackageManager.PERMISSION_GRANTED){
                            startLocationUpdates();
                            getLastLocation();
                        }
                    }

                }
            }
        }
    }

    /*
    Read the speedunit which was setted by the user
     */
    private void isSpeedUnitSet() {
           /*
        Sharedpreferences für die Speicherung der Geschwindigkeitseinheit
        initialisieren
         */

        saveSpeedUnit = this.getSharedPreferences("SpeedUnitDatei", MODE_PRIVATE);
        savedUnitEditor = saveSpeedUnit.edit();

        /*
        Prüfe, ob bereits eine SpeedUnit gewählt wurde
        */
        selectedSpeedUnit = saveSpeedUnit.getInt("SavedUnit", 0);
        if (selectedSpeedUnit == 0) {
            rb_setMeter.setChecked(true);
            rb_setMeter.setTextColor(Color.GREEN);
            rb_setKmh.setTextColor(Color.RED);
            rb_setMph.setTextColor(Color.RED);
        } else if (selectedSpeedUnit == 1) {
            rb_setKmh.setChecked(true);
            rb_setKmh.setTextColor(Color.GREEN);
            rb_setMph.setTextColor(Color.RED);
            rb_setMph.setTextColor(Color.RED);
        } else if (selectedSpeedUnit == 2) {
            rb_setMph.setChecked(true);
            rb_setMph.setTextColor(Color.GREEN);
            rb_setMph.setTextColor(Color.RED);
            rb_setKmh.setTextColor(Color.RED);
        }
    }


    /*
    Speicher die gesetzte Geschwindigkeitseinheit
     */
    public void setMeter(View v){
        rb_setMeter.setTextColor(Color.GREEN);
        rb_setKmh.setTextColor(Color.RED);
        rb_setMph.setTextColor(Color.RED);
        savedUnitEditor.putInt("SavedUnit",0);
        savedUnitEditor.apply();
    }


    public void setKmh(View v){
        rb_setKmh.setTextColor(Color.GREEN);
        rb_setMeter.setTextColor(Color.RED);
        rb_setMph.setTextColor(Color.RED);
        savedUnitEditor.putInt("SavedUnit",1);
        savedUnitEditor.apply();
    }


    public void setMph(View v){
        rb_setMph.setTextColor(Color.GREEN);
        rb_setMeter.setTextColor(Color.RED);
        rb_setKmh.setTextColor(Color.RED);
        savedUnitEditor.putInt("SavedUnit",2);
        savedUnitEditor.apply();
    }


    /*
    If GPS Signal is strong enough, request GPS positions
     */
    private void startLocationUpdates() {
        // Create the location request to start receiving updates
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);


        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();


        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());

    }


    private void getLastLocation() {
        // Get last known recent location using new Google Play Services SDK onDestroy(); (v11+)

        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }

    private void onLocationChanged(Location location) {
                if(location != null){
                    speed = location.getSpeed();
                    speedCast = (int) speed;
                    hoehe = location.getAltitude();
                    breite = location.getLongitude();
                    if (rb_setMeter.isChecked()) {
                        speedCast = (int) speed;
                        tv_speedField.setText("" + speedCast);
                        speedString = speedCast + " m/s";
                    } else if (rb_setKmh.isChecked()) {
                        speed = (float) (speed * 3.6);
                        speedCast = (int) speed;
                        tv_speedField.setText("" + speedCast);
                        speedString = speedCast + " km/h";
                    } else if (rb_setMph.isChecked()) {
                        speed = (float) (speed * 2.23694);
                        speedCast = (int) speed;
                        tv_speedField.setText("" + speedCast);
                        speedString = speedCast + " mph";
                    }

                   showWith.setText(""+breite);
                   showHight.setText(""+hoehe);
                }
    }


    /*
    Aktiviere GPS
     */
    private void enableLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener(this, (LocationSettingsResponse response) -> {
                    // startUpdatingLocation(...);
                })
                .addOnFailureListener(this, ex -> {
                    if (ex instanceof ResolvableApiException) {
                        // Location settings are NOT satisfied,  but this can be fixed  by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),  and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_PERMISSIONS_CODE);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_PERMISSIONS_CODE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                //user clicked OK, you can startUpdatingLocation(...);

            } else {
                //user clicked cancel: informUserImportanceOfLocationAndPresentRequestAgain();
            }
        }
    }

    BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent!=null){
                if(LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())){
                    boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if(isGpsEnabled || isNetworkEnabled){
                        startLocationUpdates();
                        getLastLocation();
                    }else{
                        showWith.setText("Kein Wert verfügbar");
                        showHight.setText("Kein Wert verfügbar");
                        enableLocationSettings();
                    }
                }
            }
        }
    };

    /*
    Show toast dialog
     */
    private void showToastDialog(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
