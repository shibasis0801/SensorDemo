package com.thinkternet.myapplication.sensors;

import com.thinkternet.myapplication.DataStash;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by overlord on 1/4/17.
 */

public abstract class SensorUtils {

    public static String getTime(){
        return (new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss"))
                .format(Calendar.getInstance().getTime());
    }

    public static Long getTimeStamp(){
        return System.currentTimeMillis();
    }

    public static ArrayList<Float> getArrayList(float array[]){
        ArrayList<Float> arrayList = new ArrayList<>();
        for(float item : array)
            arrayList.add(item);
        return  arrayList;
    }
}
