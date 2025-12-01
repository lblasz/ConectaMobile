package com.example.conectamobile.Activitys;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.conectamobile.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etUsername;
    private TextView tvEmail;
    private Button btnSave;
    private ImageView ivProfile;

    private DatabaseReference mUserRef;
    private String myUid;
    private Uri imageUri;

    // Launcher para abrir la galería
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivProfile.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etUsername = findViewById(R.id.etProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        btnSave = findViewById(R.id.btnSaveProfile);
        ivProfile = findViewById(R.id.ivProfileImage);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
            mUserRef = FirebaseDatabase.getInstance().getReference("users").child(myUid);
            loadUserData();
        }

        ivProfile.setOnClickListener(v -> selectImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String base64Image = snapshot.child("profileImageUrl").getValue(String.class);

                    if(name != null) etUsername.setText(name);
                    if(email != null) tvEmail.setText(email);

                    // Decodificar Base64 a Imagen para mostrarla
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivProfile.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            ivProfile.setImageResource(R.mipmap.ic_launcher_round);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfile() {
        String newName = etUsername.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Procesando...");

        String base64Image = null;

        if (imageUri != null) {
            // Convertir Imagen a Base64 (Texto)
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                // IMPORTANTE: Reducir tamaño para no saturar la base de datos (máx 300x300 aprox)
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 50, stream); // Calidad 50%
                byte[] byteFormat = stream.toByteArray();
                base64Image = Base64.encodeToString(byteFormat, Base64.DEFAULT);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show();
            }
        }

        // Guardar en Realtime Database
        updateDatabase(newName, base64Image);
    }

    private void updateDatabase(String name, String base64Image) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);

        if (base64Image != null) {
            updates.put("profileImageUrl", base64Image);
        }

        mUserRef.updateChildren(updates).addOnCompleteListener(task -> {
            btnSave.setEnabled(true);
            btnSave.setText("Guardar Cambios");
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, "Perfil Actualizado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ProfileActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}