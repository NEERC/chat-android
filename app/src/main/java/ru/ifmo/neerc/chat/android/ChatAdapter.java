/*
 * Copyright 2017 NEERC team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ifmo.neerc.chat.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import ru.ifmo.neerc.chat.ChatMessage;
import ru.ifmo.neerc.chat.user.UserEntry;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private final boolean priorityOnly;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeView;
        public TextView usernameView;
        public TextView messageView;
        public ImageView photoView;

        public ViewHolder(View view) {
            super(view);

            timeView = (TextView) view.findViewById(R.id.time);
            usernameView = (TextView) view.findViewById(R.id.username);
            messageView = (TextView) view.findViewById(R.id.message);
            photoView = (ImageView) view.findViewById(R.id.message_photo);

            usernameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ChatMessage message = messages.get(getAdapterPosition());
                    UserEntry user = message.getUser();
                    if (user != null) {
                        Intent intent = new Intent(view.getContext(), MainActivity.class);
                        intent.setAction(MainActivity.ACTION_CHAT);
                        intent.putExtra(ChatService.EXTRA_USERNAME, user.getName());
                        view.getContext().startActivity(intent);
                    }
                }
            });

            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ChatMessage message = messages.get(getAdapterPosition());
                    String url = message.getUrl();
                    if (url != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(url), "image/*");
                        view.getContext().startActivity(intent);
                    }
                }
            });
        }
    }

    public ChatAdapter() {
        this(false);
    }

    public ChatAdapter(boolean priorityOnly) {
        this.priorityOnly = priorityOnly;
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.message, parent, false);

        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.timeView.setText(TIME_FORMAT.format(message.getDate()));
        holder.usernameView.setText(message.getUser().getName());
        String text = "";
        if (message.getTo() != null) {
            text += message.getTo() + "> ";
        }
        text += message.getText();
        holder.messageView.setText(text);

        int style = R.style.ChatMessage;

        if (message.getUser().isPower()) {
            switch (message.getType()) {
                case info:
                    style = R.style.ChatMessage_Info;
                    break;
                case question:
                    style = R.style.ChatMessage_Question;
                    break;
                case urgent:
                    style = R.style.ChatMessage_Urgent;
                    break;
            }
        }

        holder.messageView.setTextAppearance(holder.messageView.getContext(), style);

        if (message.getUrl() != null) {
            holder.photoView.setVisibility(View.VISIBLE);
            Glide.with(holder.photoView.getContext())
                 .load(message.getUrl())
                 .into(holder.photoView);
        } else {
            Glide.with(holder.photoView.getContext())
                 .clear(holder.photoView);
            holder.photoView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    protected boolean appendMessage(ChatMessage message) {
        if (priorityOnly && (!message.getUser().isPower() || message.getPriority() == 0)) {
            return false;
        }

        messages.add(message);
        return true;
    }

    public void update(ChatMessage message) {
        if (message != null) {
            if (appendMessage(message)) {
                notifyItemInserted(messages.size() - 1);
            }
        } else {
            messages.clear();
            Collection<ChatMessage> source = ChatService.getInstance().getMessages();
            synchronized (source) {
                for (ChatMessage msg : source) {
                    appendMessage(msg);
                }
            }
            notifyDataSetChanged();
        }
    }
}
