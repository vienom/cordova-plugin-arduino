/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.vienom.cordova.plugin.arduino;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

import java.nio.charset.StandardCharsets;

import android.util.Log;

/**
* Get serial data from Arduino
*/
public class Arduino extends CordovaPlugin {

    public static final String TAG = "Arduino";

    private final String ACTION_USB_PERMISSION = "com.vienom.cordova.plugin.arduino.USB_PERMISSION";

    private Context c;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;

    CallbackContext measureContext;

    private StringBuilder sb = new StringBuilder();

    /**
    * Constructor.
    */
    public Arduino(){}

    /**
    * Sets the context of the Command.
    *
    * @param cordova The context of the main Activity.
    * @param webView The CordovaWebView Cordova is running in.
    */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);
      c = cordova.getActivity().getApplicationContext();

      IntentFilter filter = new IntentFilter();
      filter.addAction(ACTION_USB_PERMISSION);
      filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
      filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
      cordova.getActivity().registerReceiver(broadcastReceiver, filter);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initSerialConnection")) {
            Log.d("arduino", "initSerialConnection");
            measureContext = callbackContext;
            requestPermission();
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
      super.onPause(multitasking);
      if(serialPort != null) serialPort.close();
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * intit serial connection
     *
     */
     private void requestPermission() {

     Log.d("arduino", "initSerialConnection");

      if(usbManager == null) usbManager = (UsbManager) this.cordova.getActivity().getSystemService(c.USB_SERVICE);

      HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
      if (!usbDevices.isEmpty()) {
          boolean keep = true;
          for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
              device = entry.getValue();

              Log.d("arduino", device.toString());

              int deviceVID = device.getVendorId();
              if (deviceVID == 0x2341)//Arduino Vendor ID
              {
                  Log.d("arduino", "permission");
                  PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(ACTION_USB_PERMISSION), 0);
                  usbManager.requestPermission(device, pi);

                  keep = false;
              } else {
                  connection = null;
                  device = null;
              }

              if (!keep){
                if(measureContext != null) {
                  // Preserve callback
                  PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT);
                  pluginResult.setKeepCallback(true);
                  measureContext.sendPluginResult(pluginResult);

                  //callbackContext.success("connection established");

                }
                break;
              }

          }
      }else if(measureContext != null) measureContext.error("no usb devices found");

    }

    private void initConnection() {
      connection = usbManager.openDevice(device);
      serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
      if (serialPort != null) {
          if (serialPort.open()) { //Set Serial Connection Parameters.
              serialPort.setBaudRate(9600);
              serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
              serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
              serialPort.setParity(UsbSerialInterface.PARITY_NONE);
              serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
              serialPort.read(mCallback);

          } else {
              Log.d("SERIAL", "PORT NOT OPEN");
          }
      } else {
          Log.d("SERIAL", "PORT IS NULL");
      }
    }

    private boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = new String(arg0, StandardCharsets.UTF_8);
            sb.append(data);
            if (data.endsWith("#") || data.endsWith("#\n")) {
                sb.deleteCharAt(sb.indexOf("#"));
                final String distance = sb.toString();

                if(distance.length() > 0) {
                    if(isInteger(distance)){
                        int m = Integer.parseInt(distance);
                        if(m > 0){
                            //Log.d("SERIAL", "distance: " + m);
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, m);
                            pluginResult.setKeepCallback(true); // keep callback
                            measureContext.sendPluginResult(pluginResult);
                        }
                    }
                }

                sb.setLength(0);
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    initConnection();
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
              boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
              if (granted) {
                  initConnection();
              } else {
                  Log.d("SERIAL", "PERM NOT GRANTED");
              }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close();
            }
        }

        ;
    };

}
