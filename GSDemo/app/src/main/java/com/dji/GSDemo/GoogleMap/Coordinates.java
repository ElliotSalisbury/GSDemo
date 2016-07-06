package com.dji.GSDemo.GoogleMap;

import com.google.gson.JsonObject;

import dji.sdk.MissionManager.MissionStep.DJIGoToStep;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;

/**
 * Created by uav on 04/07/2016.
 */
public class Coordinates {
    private double latitude;
    private double longitude;
    private float altitude;

    public Coordinates (double latitude, double longitude, float altitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public String toString(){
        return latitude + " " + longitude + " " + altitude;
    }

    public JsonObject toJSONObject(){
        JsonObject tbr = new JsonObject();
        tbr.addProperty("Latitude", latitude);
        tbr.addProperty("Longitude", longitude);
        tbr.addProperty("Altitude", altitude);
        return tbr;
    }

    public DJIGoToStep getMissionStep(){
        return new DJIGoToStep(latitude,longitude,altitude,new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                // TODO Implement on result return statement to server
            }
        });
    }
}
