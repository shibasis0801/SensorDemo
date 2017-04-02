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

/**
 * Created by overlord on 1/4/17.
 */

public class JerkService extends JobService implements SensorEventListener{
    private DataStash dataStash = DataStash.getDataStash();

    public class Sense{
        Long                timeStamp;
        ArrayList<Float>    values;

        public Sense() {
            values = new ArrayList<>();
        }

        public Sense(float[] valuesArray){
            timeStamp = SensorUtils.getTimeStamp();
            values = SensorUtils.getArrayList(valuesArray);
        }

        public Long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(Long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public ArrayList<Float> getValues() {
            return values;
        }

        public void setValues(ArrayList<Float> values) {
            this.values = values;
        }
    }

    public Float differentiate(Float xh, Float x, Long h){
        return (1 * (xh - x)) / h;
    }

    public Sense differentiate(Sense one, Sense two){
        Sense three = new Sense();

        three.timeStamp = (one.timeStamp + two.timeStamp) / 2;

        for(int i = 0; i < one.values.size(); ++i)
            three.values.add(i,
                    differentiate(
                            two.values.get(i),
                            one.values.get(i),
                            (two.timeStamp - one.timeStamp)
                    )
            );
        return three;
    }

    public ArrayList<Sense> differentiate(ArrayList<Sense> data){
        ArrayList<Sense> differentiatedData = new ArrayList<>();

        for(int i = 1; i < data.size(); ++i){
            Sense one = data.get(i-1);
            Sense two = data.get(i);
            if((two.timeStamp - one.timeStamp) != 0)
                differentiatedData.add(differentiate(one, two));
        }

        return differentiatedData;
    }

    public ArrayList<Sense> getLinearJerk(){
        return differentiate(linearAcceleration);
    }

    public ArrayList<Sense> getAngularJerk(){
        return differentiate(differentiate(differentiate(gyroScopeAngles)));
    }


    public ArrayList<Sense> linearAcceleration = new ArrayList<>();
    public ArrayList<Sense> gyroScopeAngles = new ArrayList<>();

    private Thread sensorThread;
    private SensorManager sensorManager;

    private final String
    accelerometer           = "accelerometer",
    magnetic                = "magnetic",
    linear_acceleration     = "linear_acceleration",
    gyroscope               = "gyroscope",
    orientation             = "orientation",
    linearJerk              = "linearJerk",
    angularJerk             = "angularJerk";


    /**
     *              Sensor Initiation
     */
    private Map<String, Sensor>  sensorStore     = new HashMap<>();
    private Map<String, Integer> sensorIDStore   = new HashMap<>();
    private Map<Integer, String> reverseIDLookup = new HashMap<>();

    private Map<String, ArrayList<Sense>> sensorData = new HashMap<>();

    public void inputSensors(){
        sensorIDStore.put(linear_acceleration,  Sensor.TYPE_LINEAR_ACCELERATION);
        sensorIDStore.put(accelerometer,        Sensor.TYPE_ACCELEROMETER);
        sensorIDStore.put(gyroscope,            Sensor.TYPE_GYROSCOPE);
        sensorIDStore.put(magnetic,             Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void initiateSensors() {
        for(Map.Entry<String, Integer> sensorDetails : sensorIDStore.entrySet()){

            String sensorName = sensorDetails.getKey();
            int sensorID      = sensorDetails.getValue();
            Sensor sensor = sensorManager.getDefaultSensor(sensorID);

            if(sensor != null) {
                sensorStore.put(sensorName, sensor);
                reverseIDLookup.put(sensorID, sensorName);
                sensorManager.registerListener(
                        this,
                        sensorStore.get(sensorName),
                        SensorManager.SENSOR_DELAY_NORMAL * 50
                );
            }

            else {
                sensorIDStore.remove(sensorName);
            }
        }
    }

    public float rms(float readings[]){
        float x = 0;
        for(float i : readings)
            x += i*i;
        x /= (float) readings.length;
        return (float) Math.sqrt((double)x);
    }

    float a[];
    float m[];
    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorName = reverseIDLookup.get(event.sensor.getType());

        switch (sensorName){
            case accelerometer:
                a = event.values;
                break;
            case magnetic:
                m = event.values;
                break;
            case linear_acceleration:
                linearAcceleration.add(new Sense(event.values));
                break;
            case gyroscope:
                gyroScopeAngles.add(new Sense(event.values));
                break;
        }

        if (a != null && m != null) {
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

                if(sensorData.get(orientation) == null)
                    sensorData.put(orientation, new ArrayList<Sense>());

                sensorData.get(orientation).add(new Sense(ypr));
                Log.d(orientation, "" + ypr[0]);
            }
        }
        if(sensorData.get(orientation) != null)
            if(sensorData.get(orientation).size() > 10)
                sendBatch();

    }

    public void sendBatch(){
        sensorData.put(linearJerk, getLinearJerk());
//        sensorData.put(angularJerk, getAngularJerk());

        for(Map.Entry<String, ArrayList<Sense>> entry : sensorData.entrySet())
            for(Sense sense : entry.getValue())
                Log.d(entry.getKey(), sense.getValues().get(0) + "");

        dataStash.fireBase.child("sensor")
                .child("android")
                .child(SensorUtils.getTime())
                .setValue(sensorData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        sensorData.clear();
                    }
                });
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
        return true;
    }

    public void receiveAndroidSensorData(){

    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        inputSensors();
        initiateSensors();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

}
