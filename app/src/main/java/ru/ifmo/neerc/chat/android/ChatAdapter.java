package ru.ifmo.neerc.chat.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.ifmo.neerc.chat.ChatMessage;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(TextView view) {
            super(view);
            textView = view;
        }
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.message, parent, false);

        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String text = "";
        text += TIME_FORMAT.format(message.getDate()) + " ";
        text += message.getUser().getName() + ": ";
        if (message.getTo() != null) {
            text += message.getTo() + "> ";
        }
        text += message.getText();
        holder.textView.setText(text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void update(ChatMessage message) {
        if (message != null) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        } else {
            messages.clear();
            Collection<ChatMessage> source = ChatService.getInstance().getMessages();
            synchronized (source) {
                messages.addAll(source);
            }
            notifyDataSetChanged();
        }
    }
}
