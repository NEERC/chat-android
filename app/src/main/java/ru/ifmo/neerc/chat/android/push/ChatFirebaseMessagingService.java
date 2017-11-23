package ru.ifmo.neerc.chat.android.push;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ru.ifmo.neerc.chat.android.ChatService;

public class ChatFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "ChatFirebaseMessaging";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received message: " + remoteMessage);

        ChatService chatService = ChatService.getInstance();
        if (chatService != null)
            chatService.getConnectionManager().connect();
    }
}
