package com.example.conectamobile.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.conectamobile.R;
import com.example.conectamobile.User;

import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<User> userList;
    private OnContactClickListener listener;


    public interface OnContactClickListener {
        void onContactClick(User user);
    }

    public ContactsAdapter(List<User> userList, OnContactClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.username);
        holder.tvEmail.setText(user.email);

        holder.itemView.setOnClickListener(v -> listener.onContactClick(user));

        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            try {
                // Convertir texto Base64 de vuelta a imagen (Bitmap)
                byte[] decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                holder.ivProfile.setImageBitmap(decodedByte);
            } catch (Exception e) {
                // Si falla, poner imagen por defecto
                holder.ivProfile.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            holder.ivProfile.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.itemView.setOnClickListener(v -> listener.onContactClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        ImageView ivProfile;
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvEmail = itemView.findViewById(R.id.tvContactEmail);
            ivProfile = itemView.findViewById(R.id.ivContactItem);

        }
    }
}