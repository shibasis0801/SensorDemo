package com.thinkternet.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.thinkternet.myapplication.service.JobFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(
                new GooglePlayDriver(MainActivity.this)
        );

        dispatcher.mustSchedule(JobFactory.createSensorJob(dispatcher));
    }
}
