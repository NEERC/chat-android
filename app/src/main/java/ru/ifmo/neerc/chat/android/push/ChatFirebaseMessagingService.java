package ru.ifmo.neerc.chat.android.push;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;

import ru.ifmo.neerc.chat.android.ChatService;

public class ChatFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "ChatFirebaseMessaging";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received message: " + remoteMessage.getData());

        try {
            Stanza stanza = PacketParserUtils.parseStanza(remoteMessage.getData().get("stanza"));
            if (!shouldResumeConnection(stanza))
                return;
        } catch (Exception e) {
            // ignore
        }

        ChatService chatService = ChatService.getInstance();
        if (chatService != null)
            chatService.getConnectionManager().connect();
    }

    private boolean shouldResumeConnection(Stanza stanza) {
        // Don't resume on presence changes
        if (stanza instanceof Presence)
            return false;

        // Don't resume on clock ticks
        if (stanza.getExtension("x", "http://neerc.ifmo.ru/protocol/neerc#clock") != null)
            return false;

        return true;
    }
}
