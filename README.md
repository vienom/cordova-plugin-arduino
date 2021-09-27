# cordova-plugin-arduino
Plugin to get sensor data from Arduino
only for Android

## Usage
```
cordova plugin add https://github.com/vienom/cordova-plugin-arduino
```

## Usage
```javascript
window.initSerialConnection(function(distance){
  console.log(distance);
});
```

## Arduino
The arduino is connected to an IR distance sensor over the analog pin A1. In my case an Arduino Uno3 and a Grove 80cm Infrared Proximity Sensor was used.
Here is the Arduino Sketch:
```
#define IR_PROXIMITY_SENSOR A1 // Analog input pin that  is attached to the sensor

void setup()
{
    // initialise serial communications at 9600 bps:
    Serial.begin(9600);
}

void loop()
{
    int sensor_value;
    int sum=0;  
    for (int i = 0;i < 20;i ++)//Continuous sampling 20 times
    {
        sensor_value = analogRead(IR_PROXIMITY_SENSOR);
        sum += sensor_value;
    }

    Serial.print(sum/20);
    Serial.write("#");

    // wait 100 milliseconds before the next loop
    delay(100);
}
```

## Useful Links
* [Arduino](https://www.arduino.cc/)
* [IR Sensor](https://wiki.seeedstudio.com/Grove-80cm_Infrared_Proximity_Sensor/)
