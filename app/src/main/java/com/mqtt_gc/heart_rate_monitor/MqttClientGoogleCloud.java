package com.mqtt_gc.heart_rate_monitor;

import android.content.res.Resources;
import android.view.View;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.Timer;

public class MqttClientGoogleCloud {

    private static MqttCallback mCallback;
    private static MqttClient mqttclient;
    private static Boolean  tokenrefreshed;
    private static Resources resources;
    private static MqttOptions options;
    private static int keyfile;

    public MqttClientGoogleCloud(Resources resources, int keyfile) {
        System.out.println("Creating MqttClientGoogleCloud instance");
        MqttClientGoogleCloud.resources = resources;
        options = MqttOptions.fromFlags(null);
        this.createTokenRefreshTimer();
        try {
            mqttclient = this.getMqttClient();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        MqttClientGoogleCloud.keyfile = keyfile;
    }
    private void createTokenRefreshTimer() {
        Timer timer = new Timer();
        long tokenexpiry = 55 * 60 * 1000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Expiring token at: " + DateTime.now());
                tokenrefreshed = Boolean.FALSE;
            }}, tokenexpiry);
    }

    /** Create a Cloud IoT Core JWT for the given project id, signed with the given ES key. */
    private String createJwtEs(String projectId)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        DateTime now = new DateTime();
        System.out.println("Creating JWT at: " + now);
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(60).toDate())
                        .setAudience(projectId);
        InputStream inputStream = resources.openRawResource(keyfile);
        byte[] keyBytes = IOUtils.toByteArray(inputStream);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        tokenrefreshed = Boolean.TRUE;
        return jwtBuilder.signWith(SignatureAlgorithm.ES256, kf.generatePrivate(spec)).compact();
    }

    public MqttClient getMqttClient() throws MqttException {
        if (mqttclient == null) {
            System.out.println("Creating new mqtt client");
            final String mqttServerAddress =
                    String.format("ssl://%s:%s", options.mqttBridgeHostname, options.mqttBridgePort);

            // Create our MQTT client. The mqttClientId is a unique string that identifies this device. For
            // Google Cloud IoT Core, it must be in the format below.
            final String mqttClientId =
                    String.format(
                            "projects/%s/locations/%s/registries/%s/devices/%s",
                            options.projectId, options.cloudRegion, options.registryId, options.deviceId);
            mqttclient = new MqttClient(mqttServerAddress, mqttClientId, new MemoryPersistence());
        }
        return mqttclient;
    }

    public void mqttconnect()
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, MqttException, InterruptedException {
        System.out.print("In mqttconnect()");
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        connectOptions.setUserName("unused");
        connectOptions.setPassword(
                this.createJwtEs(options.projectId).toCharArray());

        long initialConnectIntervalMillis = 500L;
        long maxConnectIntervalMillis = 6000L;
        long maxConnectRetryTimeElapsedMillis = 900000L;
        float intervalMultiplier = 1.5f;

        long retryIntervalMs = initialConnectIntervalMillis;
        long totalRetryTimeMs = 0;

        while (!mqttclient.isConnected() && totalRetryTimeMs < maxConnectRetryTimeElapsedMillis) {
            try {
                mqttclient.connect(connectOptions);
            } catch (MqttException e) {
                int reason = e.getReasonCode();

                // If the connection is lost or if the server cannot be connected, allow retries, but with
                // exponential backoff.
                System.out.println("An error occurred: " + e.getMessage());
                if (reason == MqttException.REASON_CODE_CONNECTION_LOST
                        || reason == MqttException.REASON_CODE_SERVER_CONNECT_ERROR) {
                    System.out.println("Retrying in " + retryIntervalMs / 1000.0 + " seconds.");
                    Thread.sleep(retryIntervalMs);
                    totalRetryTimeMs += retryIntervalMs;
                    retryIntervalMs *= intervalMultiplier;
                    if (retryIntervalMs > maxConnectIntervalMillis) {
                        retryIntervalMs = maxConnectIntervalMillis;
                    }
                } else {
                    throw e;
                }
            }
        }

        attachCallback(this, options.deviceId);
    }

    public void mqttpublish(String payload)
            throws MqttException, NoSuchAlgorithmException,InvalidKeySpecException, IOException, InterruptedException {
        String subTopic = options.messageType.equals("event") ? "events" : options.messageType;
        String mqttTopic = String.format("/devices/%s/%s", options.deviceId, subTopic);
        if (tokenrefreshed) {
            System.out.print("Token is still current...");
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttclient.publish(mqttTopic, message);
        } else {
            System.out.print("Calling connect from publish...");
            this.mqttconnect();
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttclient.publish(mqttTopic, message);
        }
    }

    /** Attaches the callback used when configuration changes occur. */
    public static void attachCallback(final MqttClientGoogleCloud client, String deviceId) throws MqttException {
        mCallback =
                new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        try {
                            System.out.print("Reconnecting...");
                            client.mqttconnect();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InvalidKeySpecException e) {
                            e.printStackTrace();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload());
                        System.out.println("Payload : " + payload);
                        // TODO: Insert your parsing / handling of the configuration message here.
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        System.out.println("Message delivered " + token.getMessageId());
                    }
                };

        String commandTopic = String.format("/devices/%s/commands/#", deviceId);
        System.out.println(String.format("Listening on %s", commandTopic));

        String configTopic = String.format("/devices/%s/config", deviceId);
        System.out.println(String.format("Listening on %s", configTopic));

        client.getMqttClient().subscribe(configTopic, 1);
        client.getMqttClient().subscribe(commandTopic, 1);
        client.getMqttClient().setCallback(mCallback);
    }
    // [END iot_mqtt_configcallback]
}

