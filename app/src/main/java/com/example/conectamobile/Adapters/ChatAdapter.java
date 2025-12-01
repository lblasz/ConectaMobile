package com.example.conectamobile.Adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conectamobile.ChatMessage;

import java.util.List;
import com.example.conectamobile.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;
    private String myUid;

    public ChatAdapter(List<ChatMessage> messages, String myUid) {
        this.messages = messages;
        this.myUid = myUid;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvText.setText(msg.text);

        // Alineación
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvText.getLayoutParams();
        LinearLayout.LayoutParams paramsName = (LinearLayout.LayoutParams) holder.tvSender.getLayoutParams();

        if (msg.senderId.equals(myUid)) {
            // Es mío
            params.gravity = Gravity.END;
            paramsName.gravity = Gravity.END;
            holder.tvText.setBackgroundColor(0xFFDCF8C6);
            holder.tvSender.setText("Yo");
        } else {
            // Es del otro
            params.gravity = Gravity.START;
            paramsName.gravity = Gravity.START;
            holder.tvText.setBackgroundColor(0xFFE0E0E0);
            holder.tvSender.setText("Amigo");
        }

        holder.tvText.setLayoutParams(params);
        holder.tvSender.setLayoutParams(paramsName);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvText;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvMsgSender);
            tvText = itemView.findViewById(R.id.tvMsgText);
        }
    }
}