package com.thinkternet.myapplication.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.thinkternet.myapplication.DataStash;
import com.thinkternet.myapplication.sensors.SensorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

/**
 * Created by overlord on 1/4/17.
 */

public class JerkService extends JobService implements SensorEventListener{
    private DataStash dataStash = DataStash.getDataStash();

    public class Sense{
        long                timeStamp;
        float               rmsJerk;
        ArrayList<Float>    orientation;

        public Sense() {
            orientation = new ArrayList<>();
        }

        public Sense(float jerk, float[] valuesArray){
            timeStamp = SensorUtils.getTimeStamp();
            rmsJerk = jerk;
            orientation = SensorUtils.getArrayList(valuesArray);
        }

        public Sense(float[] valuesArray){
            timeStamp = SensorUtils.getTimeStamp();
            orientation = SensorUtils.getArrayList(valuesArray);
            rmsJerk = 0.0f;
        }

        public Long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(Long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public Float getRmsJerk() {
            return rmsJerk;
        }

        public void setRmsJerk(Float rmsJerk) {
            this.rmsJerk = rmsJerk;
        }

        public ArrayList<Float> getOrientation() {
            return orientation;
        }

        public void setOrientation(ArrayList<Float> orientation) {
            this.orientation = orientation;
        }
    }

    private SensorManager sensorManager;

    public Sensor linearAccSensor;

    private Thread sensorThread;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccSensor = sensorManager.getDefaultSensor(TYPE_LINEAR_ACCELERATION);
        if(linearAccSensor != null)
            sensorManager.registerListener(
                    this,
                    linearAccSensor,
                    SensorManager.SENSOR_DELAY_NORMAL * 50
            );
    }


    public float d_dx(float xh, float x, long h){
        return (1 * (xh - x)) / h;
    }

    public float rms(Sense sense){
        float x = 0;
        for(float i : sense.orientation)
            x += i*i;
        x /= (float) sense.orientation.size();
        return (float) Math.sqrt((double)x);
    }

    public float getJerk(Sense previousReading, Sense currentReading){
        return d_dx(
                rms(currentReading),
                rms(previousReading),
                (currentReading.timeStamp - previousReading.timeStamp)
        );
    }

    Sense previousReading, currentReading;

    float a[];
    float m[];
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case TYPE_ACCELEROMETER:
                a = event.values;
                break;
            case TYPE_MAGNETIC_FIELD:
                m = event.values;
                break;
            case TYPE_LINEAR_ACCELERATION:
                previousReading = currentReading;
                currentReading  = new Sense(event.values);
                break;
        }

        if (previousReading != null &&
                a != null && m != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(
                    R,
                    I,
                    a,
                    m
            );

            if (success) {
                float ypr[] = new float[3];
                SensorManager.getOrientation(R, ypr);
                Sense sense = new Sense(getJerk(previousReading, currentReading), ypr);

                dataStash.fireBase
                        .child("sensor")
                        .child("android")
                        .child(SensorUtils.getTime())
                        .setValue(sense);

            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters parameters) {
        sensorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveAndroidSensorData();
            }
        });
        sensorThread.start();
        return false;
    }

    public void receiveAndroidSensorData(){

    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return true;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

}
