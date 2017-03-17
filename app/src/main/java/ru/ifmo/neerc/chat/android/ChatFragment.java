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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import ru.ifmo.neerc.chat.ChatMessage;

public class ChatFragment extends Fragment {

    private RecyclerView chatList;
    private RecyclerView priorityChatList;
    private EditText messageInput;
    private Button sendButton;

    private ChatAdapter adapter;
    private ChatAdapter priorityAdapter;
    private BroadcastReceiver messageReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chatList = (RecyclerView) getActivity().findViewById(R.id.chat_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        chatList.setLayoutManager(layoutManager);

        adapter = new ChatAdapter();
        chatList.setAdapter(adapter);

        priorityChatList = (RecyclerView) getActivity().findViewById(R.id.priority_chat_list);

        LinearLayoutManager priorityLayoutManager = new LinearLayoutManager(getActivity());
        priorityLayoutManager.setStackFromEnd(true);
        priorityChatList.setLayoutManager(priorityLayoutManager);

        priorityAdapter = new ChatAdapter(true);
        priorityChatList.setAdapter(priorityAdapter);

        messageInput = (EditText) getActivity().findViewById(R.id.message_input);
        sendButton = (Button) getActivity().findViewById(R.id.send_button);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageInput.getText().toString();
                if (message.trim().isEmpty())
                    return;
                ChatService.getInstance().sendMessage(message);
                messageInput.getText().clear();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ChatMessage message = (ChatMessage) intent.getSerializableExtra("message");
                updateMessages(message);
            }
        };

        getContext().registerReceiver(messageReceiver, new IntentFilter(ChatService.MESSAGE));

        updateMessages(null);
    }

    @Override
    public void onStop() {
        super.onStop();

        getContext().unregisterReceiver(messageReceiver);
    }

    public void updateMessages(ChatMessage message) {
        adapter.update(message);
        priorityAdapter.update(message);
        chatList.scrollToPosition(adapter.getItemCount() - 1);
        priorityChatList.scrollToPosition(priorityAdapter.getItemCount() - 1);
    }
}
