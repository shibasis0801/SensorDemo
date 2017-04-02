package com.thinkternet.myapplication;

import android.util.Pair;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//Singleton
public class DataStash {

    //************************************************************  EVENT LISTENER
    private Map<
            UUID,
            Pair<DatabaseReference, ValueEventListener>
            > valueEventListeners;

    public UUID attachValueEventListener(DatabaseReference ref, ValueEventListener listener){
        UUID uuid = UUID.randomUUID();
        valueEventListeners.put(
                uuid,
                new Pair<>(
                        ref,
                        listener
                )
        );
        //ref.
        valueEventListeners.get(uuid).first
                .addValueEventListener(
                        //listener)
                        valueEventListeners.get(uuid).second);
        return uuid;
    }
    public void detachValueEventListener(UUID uuid){
        valueEventListeners.get(uuid).first
                .removeEventListener(valueEventListeners.get(uuid).second);
        valueEventListeners.remove(uuid);
    }

    //
    private Map<
            UUID,
            Pair<DatabaseReference, ChildEventListener>
            > childEventListeners;

    public UUID attachChildEventListener(DatabaseReference ref, ChildEventListener listener){
        UUID uuid = UUID.randomUUID();
        childEventListeners.put(
                uuid,
                new Pair<>(
                        ref,
                        listener
                )
        );
        //ref
        childEventListeners.get(uuid).first
                .addChildEventListener(
                        //listener
                        childEventListeners.get(uuid).second);
        return uuid;
    }
    public void detachChildEventListener(UUID uuid){
        childEventListeners.get(uuid).first
                .removeEventListener(childEventListeners.get(uuid).second);
        childEventListeners.remove(uuid);
    }

    //
    public void detachEventListeners(){
        for(UUID uuid : valueEventListeners.keySet())
            detachValueEventListener(uuid);

        for(UUID uuid : childEventListeners.keySet())
            detachChildEventListener(uuid);
    }
    //************************************************************



    //************************************************************  SINGLETON, INITIALIZATION
    private DataStash(){
        fireBase            = FirebaseDatabase.getInstance().getReference();
        valueEventListeners = new ConcurrentHashMap<>();
        childEventListeners = new ConcurrentHashMap<>();
    }
    private static DataStash sDataStash;
    public DatabaseReference fireBase;
    public  static DataStash getDataStash() {
        if(sDataStash == null)
            sDataStash = new DataStash();
        return sDataStash;
    }
    //************************************************************

}

