package com.dji.GSDemo.GoogleMap;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.MissionManager.DJICustomMission;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.MissionStep.DJIGoToStep;
import dji.sdk.MissionManager.MissionStep.DJIMissionStep;
import dji.sdk.MissionManager.MissionStep.DJITakeoffStep;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

/**
 * Created by ata1g11 on 15/06/2016.
 */
public class Listen extends AsyncTask<Double, Integer, Long> {

    private ConnectionFactory factory = new ConnectionFactory();

    private String uri;
    private String queueName;
    private String droneID;
    private String currentMissionID = null;
    //private TextView txtView;

    private String mRegisterAddress = "http://10.9.135.79:8000";
    private Activity mActivity;
    private DJICustomMission mCustomMission;
    private DJIFlightController mFlightController;

    protected DJIBaseProduct mproduct;

    public Listen(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Long doInBackground(Double... coordinates) {
        try {
            this.registerWithServer(coordinates);
            this.declareMissionConsumer();

        } catch (MalformedURLException e) {
            showErrorMessage("MalformedURLException");
            e.printStackTrace();
        } catch (ProtocolException e) {
            showErrorMessage("ProtocolException");
            e.printStackTrace();
        } catch (IOException e) {
            showErrorMessage("IOException");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            showErrorMessage("NoSuchAlgorithmException");
            e.printStackTrace();
        } catch (URISyntaxException e) {
            showErrorMessage("URISyntaxException");
            e.printStackTrace();
        } catch (TimeoutException e) {
            showErrorMessage("TimeoutException");
            e.printStackTrace();
        } catch (KeyManagementException e) {
            showErrorMessage("KeyManagementException");
            e.printStackTrace();
        }
        return null;
    }

    private void processInitialSettingsJSON(String sJSON) {
        JsonObject jsonObject = new JsonParser().parse(sJSON).getAsJsonObject();
        droneID = jsonObject.get("ID").getAsString().replace("\"", "");
        queueName = "UAV_TaskQueue_" + droneID;
        uri = jsonObject.get("URI").toString().replace("\"", "");
    }

    private void processMissionsJSON(final String mJSON) {
        JsonObject jsonObject = new JsonParser().parse(mJSON).getAsJsonObject();

        //if(currentMissionID == null) {
        //if(!currentMissionID.equals(jsonObject.get("current").getAsJsonObject().get("ID").getAsString())) {

        currentMissionID = jsonObject.get("current").getAsJsonObject().get("ID").getAsString();

        DJIMissionManager mMissionManager;
        mproduct = DJIDemoApplication.getProductInstance();
        mMissionManager = mproduct.getMissionManager();
        DJIAircraft aircraft = (DJIAircraft) mproduct;
        mFlightController = aircraft.getFlightController();


        double currentMissionLat = 0.0d;
        double currentMissionLon = 0.0d;

        // TODO Transform them in coordinates and use an JSONList


        try {
            String coordinates = jsonObject.get("current").getAsJsonObject().get("Coordinate").toString();
            currentMissionLat = Double.parseDouble(coordinates.split(",")[0].replace("\"", ""));
            currentMissionLon = Double.parseDouble(coordinates.split(",")[1].replace("\"", ""));
        } catch (Exception e) {
            showErrorMessage(e.getMessage());
        }

        List<DJIMissionStep> djiMissionSteps = new LinkedList<>();
        djiMissionSteps.add(new DJITakeoffStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        }));
        djiMissionSteps.add(new DJIGoToStep(currentMissionLat, currentMissionLon, 2f, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        }));

        mCustomMission = new DJICustomMission(djiMissionSteps);
        if (mMissionManager != null) {
            mMissionManager.prepareMission(mCustomMission, new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {

                }
            }, new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError djiError) {

                }
            });
            mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
        //}
        //}
    }

    private DJIFlightControllerDataType.DJILocationCoordinate3D getCurrentLocation(DJIFlightController flightController) {
        return flightController.getCurrentState().getAircraftLocation();
    }

    private void showErrorMessage(final String message) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerWithServer(Double... coordinates) throws IOException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {

        StringBuilder response = new StringBuilder();

        URL url = new URL(mRegisterAddress + "/register");

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnection.setRequestProperty("Accept", "application/json");

        JsonObject toBeSent = new JsonObject();

        toBeSent.addProperty("lat", coordinates[0]);
        toBeSent.addProperty("lon", coordinates[1]);

        Writer writer = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream()));

        writer.write(toBeSent.toString());
        writer.close();

        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader input = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String strLine = null;
            while ((strLine = input.readLine()) != null) {
                response.append(strLine);
            }
            input.close();
        }

        this.processInitialSettingsJSON(response.toString());
        factory.setUri(uri);
    }

    private void declareMissionConsumer() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                processMissionsJSON(new String(body, "UTF-8"));
            }
        };
        channel.basicConsume(queueName, true, consumer);
    }

    private void scheduleDataTransmission() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Connection connection = null;
                Channel channel = null;
                try {
                    JSONObject data = new JSONObject();
                    DJIFlightControllerDataType.DJILocationCoordinate3D coordinate3D = mFlightController.getCurrentState().getAircraftLocation();

                    data.put("ID", droneID);
                    data.put("MissionID", currentMissionID);
                    data.put("Coordinates", new Coordinates(coordinate3D.getLatitude(), coordinate3D.getLongitude(), coordinate3D.getAltitude()).toJSONObject());

                    connection = factory.newConnection();
                    channel = connection.createChannel();
                    channel.basicPublish("", "Meta_Drone_Data", null, data.toString().getBytes());

                    channel.close();
                    connection.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorMessage("IOException");
                } catch (TimeoutException e) {
                    e.printStackTrace();
                    showErrorMessage("TimeoutException");
                } catch (JSONException e) {
                    e.printStackTrace();
                    showErrorMessage("JSONException");
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorMessage("Exception");
                }
            }
        }, 0, 100);
    }
}