package ru.ifmo.neerc.chat.android;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

import org.jxmpp.util.XmppStringUtils;

public class ChatMessage implements Comparable<ChatMessage> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private String body;
    private String user;
    private Date time;

    public ChatMessage(Message message) {
        body = message.getBody();
        user = XmppStringUtils.parseResource(message.getFrom());

        DelayInformation delay = (DelayInformation) message.getExtension(DelayInformation.NAMESPACE);
        if (delay != null)
            time = delay.getStamp();
        else
            time = new Date();
    }

    public ChatMessage(String body, Date time) {
        this.body = body;
        this.time = time;
    }

    public String toString() {
        String result = dateFormat.format(time) + " ";
        if (user != null)
            result += user + ": ";
        result += body;
        return result;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ChatMessage))
            return false;

        ChatMessage msg = (ChatMessage) o;
        return body.equals(msg.body) &&
               user.equals(msg.user) &&
               time.equals(msg.time);
    }

    public int compareTo(ChatMessage msg) {
        return time.compareTo(msg.time);
    }
}
