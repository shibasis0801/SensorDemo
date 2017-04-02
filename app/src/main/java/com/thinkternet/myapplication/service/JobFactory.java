package com.thinkternet.myapplication.service;

import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.thinkternet.myapplication.service.JerkService;

/**
 * Created by overlord on 1/4/17.
 */

public class JobFactory {
    public static final String SENSOR_JOB = "SENSOR_JOB";

    public static Job createSensorJob(FirebaseJobDispatcher dispatcher){
        return dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setService(JerkService.class)
                .setTag(SENSOR_JOB)
                .setTrigger(Trigger.executionWindow(0,30))
                .build();
    }

    public static Job updateJob(FirebaseJobDispatcher dispatcher) {
        Job newJob = dispatcher.newJobBuilder()
                .setReplaceCurrent(true)
                .setService(JerkService.class)
                .setTag(SENSOR_JOB)
                .setTrigger(Trigger.executionWindow(60, 120))
                .build();
        return newJob;
    }

}
