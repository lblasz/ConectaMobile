package com.example.conectamobile;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttHandler {

    private MqttAndroidClient client;
    private static final String TAG = "MqttHandler";

    // broker público para pruebas académicas

    private static final String SERVER_URI = "tcp://broker.hivemq.com:1883";

    public void connect(Context context, String clientId) {
        client = new MqttAndroidClient(context, SERVER_URI, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false); // Mantiene la sesión si te desconectas brevemente

        try {
            client.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Conexión exitosa al broker MQTT");
                    // Aquí podrías suscribirte a un canal por defecto si quisieras
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error al conectar: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Configurar el callback para escuchar mensajes entrantes
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "Conexión perdida");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Mensaje recibido: " + new String(message.getPayload()));
                // Aquí deberás usar una interfaz o EventBus para avisar a la UI
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Confirmación de que el mensaje fue enviado
            }
        });
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 0); // QoS 0
                Log.d(TAG, "Suscrito al tema: " + topic);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                client.publish(topic, mqttMessage);
                Log.d(TAG, "Mensaje publicado en " + topic);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Método auxiliar para saber si estamos conectados
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}