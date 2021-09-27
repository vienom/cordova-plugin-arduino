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

/**
* Get serial data from Arduino
*/
public class Arduino extends CordovaPlugin {

    public static final String TAG = "Arduino";

    public final String ACTION_USB_PERMISSION = "com.vienom.cordova.plugin.arduino.USB_PERMISSION";

    Context c;

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

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
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initSerialConnection")) {
            this.initSerialConnection(callbackContext);
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * intit serial connection
     *
     */
     private void initSerialConnection(CallbackContext callbackContext) {

      if(usbManager == null) usbManager = (UsbManager) this.cordova.getActivity().getSystemService(c.USB_SERVICE);

      HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
      if (!usbDevices.isEmpty()) {
          boolean keep = true;
          for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
              device = entry.getValue();
              int deviceVID = device.getVendorId();
              if (deviceVID == 0x2341)//Arduino Vendor ID
              {
                  PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(ACTION_USB_PERMISSION), 0);
                  usbManager.requestPermission(device, pi);
                  keep = false;
              } else {
                  connection = null;
                  device = null;
              }

              if (!keep){
                callbackContext.success("connection established");
                break;
              }

          }
      }else callbackContext.error("no usb devices found");

    }
}
