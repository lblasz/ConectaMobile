package com.example.conectamobile.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.conectamobile.Adapters.ChatAdapter;
import com.example.conectamobile.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import com.example.conectamobile.R;
import org.eclipse.paho.client.mqttv3.MqttClient;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    private String chatTopic;
    private String friendUid, myUid;
    private String chatId; // ID único de la conversación
    private TextView tvFriendName;
    private ImageView btnBack;

    // UI
    private RecyclerView recyclerView;
    private EditText etMessage;
    private Button btnSend;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    // Firebase
    private DatabaseReference mDbRef;

    // MQTT
    private MqttAndroidClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Obtener datos del Intent
        friendUid = getIntent().getStringExtra("friendUid");
        String friendName = getIntent().getStringExtra("friendName");
        myUid = FirebaseAuth.getInstance().getUid();

        // Generar Chat ID único: (UID_Menor + "_" + UID_Mayor)
        if (myUid.compareTo(friendUid) < 0) {
            chatId = myUid + "_" + friendUid;
        } else {
            chatId = friendUid + "_" + myUid;
        }

        chatTopic = "conecta/chat/" + chatId;

        // 2. Setup Firebase
        mDbRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        // 3. Setup UI
        recyclerView = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSendMsg);
        tvFriendName = findViewById(R.id.tvChatFriendName);
        btnBack = findViewById(R.id.btnBackChat);

        if (friendName != null) {
            tvFriendName.setText(friendName);
        } else {
            tvFriendName.setText("Chat");
        }

        btnBack.setOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList, myUid);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 4. Cargar historial antiguo de Firebase (SOLO UNA VEZ)
        loadFirebaseHistory();

        // 5. Conectar MQTT para tiempo real
        connectMQTT();

        // 6. Botón Enviar
        btnSend.setOnClickListener(v -> {
            String txt = etMessage.getText().toString();
            if (!txt.isEmpty()) {
                sendMessage(txt);
                etMessage.setText("");
            }
        });
    }

    private void loadFirebaseHistory() {
        mDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    ChatMessage msg = d.getValue(ChatMessage.class);
                    messageList.add(msg);
                }
                adapter.notifyDataSetChanged();
                scrollToBottom();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void connectMQTT() {
        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId);

        try {
            mqttClient.connect(null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
                @Override
                public void onSuccess(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken) {
                    subscribeToTopic();
                }

                @Override
                public void onFailure(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(ChatActivity.this, "Error MQTT", Toast.LENGTH_SHORT).show();
                }
            });

            // Callback para recibir mensajes
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // llegó un mensaje por MQTT
                    String payload = new String(message.getPayload());

                    String[] parts = payload.split(":", 2);
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String text = parts[1];

                        // Creamos objeto visual (No lo guardamos en DB aquí, ya se guardó al enviar)
                        ChatMessage newMsg = new ChatMessage("", sender, text, System.currentTimeMillis());

                        runOnUiThread(() -> {
                            messageList.add(newMsg);
                            adapter.notifyDataSetChanged();
                            scrollToBottom();
                        });
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(chatTopic, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String text) {
        String msgId = mDbRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        ChatMessage msg = new ChatMessage(msgId, myUid, text, timestamp);

        // A. Guardar en Firebase (Persistencia)
        mDbRef.child(msgId).setValue(msg);

        // B. Enviar por MQTT (Tiempo Real)
        // Formato simple: "UID:TEXTO"
        String payload = myUid + ":" + text;
        try {
            if (mqttClient.isConnected()) {
                mqttClient.publish(chatTopic, new MqttMessage(payload.getBytes()));
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }
}