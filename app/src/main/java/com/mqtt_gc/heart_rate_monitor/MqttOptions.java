package com.mqtt_gc.heart_rate_monitor;



/** Command line options for the MQTT example. */
public class MqttOptions {
    String username = null;
    String projectId = null;
    String registryId = null;
    String deviceId = null;
    String cloudRegion = null;
    String mqttBridgeHostname = "mqtt.googleapis.com";
    short mqttBridgePort = 8883;
    String messageType = null;
}