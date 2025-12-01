package com.example.conectamobile.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.conectamobile.Adapters.ContactsAdapter;
import com.example.conectamobile.MqttHandler;
import com.example.conectamobile.R;
import com.example.conectamobile.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<User> contactList;
    private DatabaseReference mDatabase;
    private String currentUid;
    private Button btnAdd;

    private MqttHandler mqttHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recyclerView = findViewById(R.id.recyclerContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactList = new ArrayList<>();

        // Al hacer clic en un contacto, abriremos el Chat
        adapter = new ContactsAdapter(contactList, user -> {
            Toast.makeText(this, "Abriendo chat con " + user.username, Toast.LENGTH_SHORT).show();
             Intent intent = new Intent(MainActivity.this, ChatActivity.class);
             intent.putExtra("friendUid", user.uid);
             startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        btnAdd = findViewById(R.id.btnAddContact);
        btnAdd.setOnClickListener(v -> showAddContactDialog());

        loadContacts();

        // Conexión MQTT
        mqttHandler = new MqttHandler();
        mqttHandler.connect(this, "Client_" + currentUid);
    }

    private void loadContacts() {
        // 1. Ir al nodo de mis contactos
        mDatabase.child("contacts").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                // 2. Por cada ID de amigo encontrado...
                for (DataSnapshot data : snapshot.getChildren()) {
                    String friendUid = data.getKey();

                    // 3. Buscar los detalles de ese amigo en el nodo "users"
                    mDatabase.child("users").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            User friend = userSnapshot.getValue(User.class);
                            if (friend != null) {
                                contactList.add(friend);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Contacto");

        final EditText input = new EditText(this);
        input.setHint("Ingresa el correo del usuario");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                searchAndAddContact(email);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void searchAndAddContact(String email) {
        // Consultar en "users" ordenando por email
        mDatabase.child("users").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Usuario encontrado (solo debería haber uno)
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String foundUid = userSnapshot.getKey();

                                // Evitar agregarse a uno mismo
                                if (foundUid.equals(currentUid)) {
                                    Toast.makeText(MainActivity.this, "No te puedes agregar a ti mismo", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Agregar a mi lista de contactos: contacts/mi_id/id_amigo = true
                                mDatabase.child("contacts").child(currentUid).child(foundUid).setValue(true);
                                Toast.makeText(MainActivity.this, "Contacto Agregado", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}