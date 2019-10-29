# gcloudmqttheartrate

The code is based on heartrate app at https://github.com/phishman3579/android-heart-rate-monitor.  

This app catures the value provided by phishman's app and send it to google cloud IOT. This code is taken from samples provided by google.

You need to provide config file to the app in the following json format

`
{
"username": "Kamal",
"projectId": "myprojectid",
"registryId": "my-iot-registry",
"deviceId": "deviceid",
"cloudRegion": "asia-east1",
"mqttBridgeHostname": "mqtt.googleapis.com",
"mqttBridgePort": 8883,
"messageType": "event"
}
`
