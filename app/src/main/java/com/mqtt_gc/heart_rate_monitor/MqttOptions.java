package com.mqtt_gc.heart_rate_monitor;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/** Command line options for the MQTT example. */
public class MqttOptions {
    String projectId = "my-iot-project-2019";
    String registryId = "my-iot-registry";
    String command = "mqtt-demo";
    String deviceId = "testjavaclient";
    String gatewayId;
    String algorithm = "ES256";
    String cloudRegion = "asia-east1";
    int numMessages = 100;
    int tokenExpMins = 20;
    String telemetryData = "Specify with -telemetry_data";
    String mqttBridgeHostname = "mqtt.googleapis.com";
    short mqttBridgePort = 8883;
    String messageType = "event";
    int waitTime = 120;

    /**
     * Construct an MqttExampleOptions class from command line flags.
     */
    public static MqttOptions fromFlags(String[] args) {
        Options options = new Options();
        // Required arguments
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("project_id")
                        .hasArg()
                        .desc("GCP cloud project name.")
                        .required()
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("registry_id")
                        .hasArg()
                        .desc("Cloud IoT Core registry id.")
                        .required()
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("device_id")
                        .hasArg()
                        .desc("Cloud IoT Core device id.")
                        .required()
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("gateway_id")
                        .hasArg()
                        .desc("The identifier for the Gateway.")
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("private_key_file")
                        .hasArg()
                        .desc("Path to private key file.")
                        .required()
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("algorithm")
                        .hasArg()
                        .desc("Encryption algorithm to use to generate the JWT. Either 'RS256' or 'ES256'.")
                        .required()
                        .build());

        // Optional arguments.
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("command")
                        .hasArg()
                        .desc(
                                "Command to run:"
                                        + "\n\tlisten-for-config-messages"
                                        + "\n\tsend-data-from-bound-device")
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("telemetry_data")
                        .hasArg()
                        .desc("The telemetry data (string or JSON) to send on behalf of the delegated device.")
                        .build());

        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("cloud_region")
                        .hasArg()
                        .desc("GCP cloud region.")
                        .build());
        options.addOption(
                Option.builder()
                        .type(Number.class)
                        .longOpt("num_messages")
                        .hasArg()
                        .desc("Number of messages to publish.")
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("mqtt_bridge_hostname")
                        .hasArg()
                        .desc("MQTT bridge hostname.")
                        .build());
        options.addOption(
                Option.builder()
                        .type(Number.class)
                        .longOpt("token_exp_minutes")
                        .hasArg()
                        .desc("Minutes to JWT token refresh (token expiration time).")
                        .build());
        options.addOption(
                Option.builder()
                        .type(Number.class)
                        .longOpt("mqtt_bridge_port")
                        .hasArg()
                        .desc("MQTT bridge port.")
                        .build());
        options.addOption(
                Option.builder()
                        .type(String.class)
                        .longOpt("message_type")
                        .hasArg()
                        .desc("Indicates whether the message is a telemetry event or a device state message")
                        .build());
        options.addOption(
                Option.builder()
                        .type(Number.class)
                        .longOpt("wait_time")
                        .hasArg()
                        .desc("Wait time (in seconds) for commands.")
                        .build());
        MqttOptions res = new MqttOptions();
        return res;
    }
}