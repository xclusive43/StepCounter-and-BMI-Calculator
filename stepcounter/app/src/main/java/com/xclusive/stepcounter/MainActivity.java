package com.xclusive.stepcounter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static java.lang.String.*;

public class MainActivity extends AppCompatActivity {

    private double prev_magnitude = 0;
    private int steps = 0;
    private ProgressBar circular;
    private TextView s, remain, localaddress,bmires;
    private Button bmibtn;
    private EditText height,weight;
    private FusedLocationProviderClient fused;
    private String sharedbmi;
    // private LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        s = findViewById(R.id.textview);
        remain = findViewById(R.id.textView3);
        localaddress = findViewById(R.id.localaddress);
        circular = findViewById(R.id.progress_circular);
        bmibtn = findViewById(R.id.bmibtn);
        bmires = findViewById(R.id.bmires);
        height = findViewById(R.id.height);
        weight = findViewById(R.id.weight);
        circular.setMax(1000);
        circular.setProgress(0);
         //todo: BMI

        bmires.setText(sharedbmi);

        bmibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bmi();
            }
        });


        ////////////////todo:location
        fused = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loc();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
        //////////////

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event != null) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double magnitude = Math.sqrt(x * x + y * y + z * z);
                    double magnitude_delta = magnitude - prev_magnitude;
                    prev_magnitude = magnitude;

                    if (magnitude_delta > 6) {
                        steps++;

                    }
                    //Toast.makeText(MainActivity.this, "steps"+steps, Toast.LENGTH_SHORT).show();
                    if (magnitude_delta > 10) {
                        // steps++;
                    }

                    if (steps > 1000) {
                        steps = 1;
                        circular.setProgress(0);
                        s.setText(valueOf(steps));
                        remain.setText(valueOf("Remaing = " + valueOf(1000 - steps - 1)));
                    } else {

                        s.setText(valueOf(steps - 1));
                        circular.setProgress(steps - 1);
                        remain.setText("Remaing = " + valueOf(1000 - steps + 1));
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @SuppressLint("DefaultLocale")
    private void bmi() {


        if(height.getText().toString().isEmpty() && weight.getText().toString().isEmpty()){
            Toast.makeText(MainActivity.this, "Both Height and Weight are Mandatory.", Toast.LENGTH_SHORT).show();
        }
        else{

            float height1 = Float.parseFloat(height.getText().toString());
            float weight1  =  Float.parseFloat(weight.getText().toString());
            float heightinm = (float) (height1 * 0.3048);// converting in meter from feet
            float res = weight1/(heightinm*heightinm);


            if(res<18.5){
                bmires.setText("Underweight "+ format("%.2f",res));
                bmires.setBackgroundColor(Color.parseColor("#00658F"));
                sharedbmi = "Underweight";
            }
            if(res>18.5 && res<24.9){
                bmires.setText("Healthy "+ format("%.2f",res));
                bmires.setBackgroundColor(Color.parseColor("#63C1C6"));
                sharedbmi = "Healthy";
            }
            if(res>24.9 && res<29.9){
                bmires.setText("Overweight "+ format("%.2f",res));
                bmires.setBackgroundColor(Color.parseColor("#F5AD84"));
                sharedbmi = "Overweight";
            }
            if(res>30 && res<34.9){
                bmires.setText("obesity "+ format("%.2f",res));
                bmires.setBackgroundColor(Color.parseColor("#ED7632"));
                sharedbmi = "obesity";
            }
            if(res>35){
                bmires.setText("severe obesity "+ format("%.2f",res));
                bmires.setBackgroundColor(Color.parseColor("#CB414C"));
                sharedbmi = "severe obesity";

            }
        }

    }

    @SuppressLint("SetTextI18n")
    private void loc() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fused.getLastLocation().addOnCompleteListener(task -> {
            Location location = task.getResult();

            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    localaddress.setText(addresses.get(0).getFeatureName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fused.getLastLocation().addOnCompleteListener(task -> {
            Location location = task.getResult();

            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    localaddress.setText(addresses.get(0).getFeatureName());



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPreferences  = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("steps",steps);
        editor.putString("bmi", sharedbmi);
        editor.apply();

    }
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences  = getPreferences(MODE_PRIVATE);
        steps = sharedPreferences.getInt("steps",0);
        sharedbmi = sharedPreferences.getString("bmi","");
    }
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences  = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("steps",steps);
        editor.putString("bmi", sharedbmi);
        editor.apply();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences  = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("steps",steps);
        editor.putString("bmi", sharedbmi);
        editor.apply();
    }
    public void reset(View view) {
        steps = 1;
        sharedbmi = "";
        circular.setProgress(0);
        SharedPreferences sharedPreferences  = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

    }


}