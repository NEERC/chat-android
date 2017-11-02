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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;

import org.jxmpp.util.XmppStringUtils;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskActions;
import ru.ifmo.neerc.task.TaskStatus;
import ru.ifmo.neerc.task.TaskRegistry;
import ru.ifmo.neerc.task.TaskRegistryListener;
import ru.ifmo.neerc.chat.ChatMessage;
import ru.ifmo.neerc.chat.packet.TaskExtension;
import ru.ifmo.neerc.chat.packet.TaskExtensionProvider;
import ru.ifmo.neerc.chat.packet.TaskIQ;
import ru.ifmo.neerc.chat.packet.TaskList;
import ru.ifmo.neerc.chat.packet.TaskListProvider;
import ru.ifmo.neerc.chat.packet.TaskStatusIQ;
import ru.ifmo.neerc.chat.packet.UserList;
import ru.ifmo.neerc.chat.packet.UserListProvider;
import ru.ifmo.neerc.chat.user.UserEntry;
import ru.ifmo.neerc.chat.user.UserRegistry;

public class ChatService extends Service {

    private static final String TAG = "ChatService";

    public static final String CONNECTION = "User";

    public static final String STATUS = "ru.ifmo.neerc.chat.android.STATUS";
    public static final String MESSAGE = "ru.ifmo.neerc.chat.android.MESSAGE";
    public static final String TASK = "ru.ifmo.neerc.chat.android.TASK";
    public static final String USER = "ru.ifmo.neerc.chat.android.USER";

    public static final int STATUS_DISCONNECTED = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_CONNECTED = 2;

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static ChatService instance;

    private final IBinder binder = new LocalBinder();

    private NotificationManager notificationManager;

    private String room;
    private boolean hasCredentials = false;

    private ConnectionTask connectionTask;

    private AbstractXMPPConnection connection;
    private MultiUserChat muc;

    private TaskRegistry taskRegistry;
    private UserRegistry userRegistry;

    private Set<ChatMessage> messages = Collections.synchronizedSortedSet(new TreeSet<ChatMessage>());
    private Set<ChatMessage> importantMessages = Collections.synchronizedSortedSet(new TreeSet<ChatMessage>(Collections.reverseOrder()));

    private static final int NOTIFICATION_TASKS = 1;
    private static final int NOTIFICATION_MESSAGES = 2;

    public class LocalBinder extends Binder {
        ChatService getService() {
            return ChatService.this;
        }
    }

    public static ChatService getInstance() {
        return instance;
    }

    private final ConnectionCreationListener connectionCreationListener = new ConnectionCreationListener() {
        @Override
        public void connectionCreated(XMPPConnection connection) {
            ChatService.this.connection = (AbstractXMPPConnection) connection;

            connection.addConnectionListener(connectionListener);
            connection.addAsyncStanzaListener(taskListener,
                new OrFilter(
                    new StanzaTypeFilter(TaskList.class),
                    new StanzaExtensionFilter(new TaskExtension())
                )
            );
            connection.addAsyncStanzaListener(userListener,
                new StanzaTypeFilter(UserList.class)
            );
        }
    };

    private final ConnectionListener connectionListener = new AbstractConnectionListener() {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            UserList usersQuery = new UserList();
            usersQuery.setTo(room + "@neerc." + connection.getServiceName());

            try {
                connection.sendStanza(usersQuery);
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "Failed to query users", e);
            }

            TaskList tasksQuery = new TaskList();
            tasksQuery.setTo(room + "@neerc." + connection.getServiceName());

            try {
                connection.sendStanza(tasksQuery);
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "Failed to query tasks", e);
            }

            if (muc == null) {
                muc = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(room + "@conference." + connection.getServiceName());
                muc.addMessageListener(messageListener);
                muc.addParticipantStatusListener(participantStatusListener);
            }

            String username = connection.getUser();
            username = username.substring(0, username.indexOf('@'));

            try {
                muc.join(username);
            } catch (SmackException | XMPPException e) {
                Log.e(TAG, "Failed to join the room", e);
            }

            sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_CONNECTED));
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_DISCONNECTED));
        }

        @Override
        public void reconnectingIn(int seconds) {
            if (seconds == 0) {
                sendBroadcast(new Intent(ChatService.STATUS)
                    .putExtra("status", ChatService.STATUS_CONNECTING));
            }
        }
    };

    private final MessageListener messageListener = new MessageListener() {
        @Override
        public void processMessage(Message message) {
            String resource = XmppStringUtils.parseResource(message.getFrom());
            if (resource.isEmpty()) {
                return;
            }

            UserEntry user = userRegistry.findOrRegister(resource);
            Date time = new Date();
            DelayInformation delay = (DelayInformation) message.getExtension(DelayInformation.NAMESPACE);
            if (delay != null) {
                time = delay.getStamp();
            }
            ChatMessage chatMessage = new ChatMessage(message.getBody(), user, null, time);
            Log.d(TAG, chatMessage.getUser().getName() + ": " + chatMessage.getText());

            if (chatMessage.getTo() != null
                    && !chatMessage.getUser().getName().equals(getUser())
                    && !chatMessage.getTo().equals(getUser())) {
                return;
            }

            synchronized (messages) {
                messages.add(chatMessage);
            }

            UserEntry currentUser = userRegistry.findOrRegister(getUser());
            if (chatMessage.isImportantFor(currentUser)) {
                addImportantMessage(chatMessage);
            }

            sendBroadcast(new Intent(ChatService.MESSAGE)
                .putExtra("message", chatMessage));
        }
    };

    private final ParticipantStatusListener participantStatusListener = new DefaultParticipantStatusListener() {
        @Override
        public void joined(String participant) {
            Log.d(TAG, "User joined: " + participant);
            userRegistry.putOnline(participant);
            sendBroadcast(new Intent(ChatService.USER));
        }

        @Override
        public void left(String participant) {
            Log.d(TAG, "User left: " + participant);
            userRegistry.putOffline(participant);
            sendBroadcast(new Intent(ChatService.USER));
        }
    };

    private final StanzaListener taskListener = new StanzaListener() {
        @Override
        public void processPacket(Stanza packet) {
            if (packet instanceof TaskList) {
                TaskList taskList = (TaskList) packet;
                Log.d(TAG, "Received task list:");
                for (Task task : taskList.getTasks()) {
                    Log.d(TAG, task.getTitle());
                    taskRegistry.update(task);
                }
                sendBroadcast(new Intent(ChatService.TASK));
            } else {
                TaskExtension extension = packet.getExtension(TaskExtension.ELEMENT_NAME, TaskExtension.NAMESPACE);
                if (extension != null) {
                    Log.d(TAG, "Received task:");
                    Log.d(TAG, extension.getTask().getTitle());
                    taskRegistry.update(extension.getTask());
                }
                sendBroadcast(new Intent(ChatService.TASK));
            }
        }
    };

    private final StanzaListener userListener = new StanzaListener() {
        @Override
        public void processPacket(Stanza packet) {
            if (packet instanceof UserList) {
                UserList userList = (UserList) packet;
                Log.d(TAG, "Received user list:");
                for (UserEntry user : userList.getUsers()) {
                    Log.d(TAG, user.toString());
                    UserEntry u = userRegistry.findOrRegister(user.getName());
                    u.setPower(user.isPower());
                    u.setGroup(user.getGroup());
                }
                sendBroadcast(new Intent(ChatService.USER));
            }
        }
    };

    private final TaskRegistryListener taskRegistryListener = new TaskRegistryListener() {
        private Set<String> notifiedTasks = new HashSet<String>();

        @Override
        public void taskChanged(Task task) {
            boolean alert = false;

            TaskStatus status = task.getStatus(getUser());
            if (status != null && TaskActions.STATUS_NEW.equals(status.getType()) &&
                !notifiedTasks.contains(task.getId())) {
                alert = true;
                notifiedTasks.add(task.getId());
            }

            updateTasksNotification(alert);
        }

        @Override
        public void tasksReset() {
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service created");
        instance = this;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ProviderManager.addIQProvider(TaskList.ELEMENT_NAME, TaskList.NAMESPACE, new TaskListProvider());
        ProviderManager.addIQProvider(UserList.ELEMENT_NAME, UserList.NAMESPACE, new UserListProvider());
        ProviderManager.addExtensionProvider(TaskExtension.ELEMENT_NAME, TaskExtension.NAMESPACE, new TaskExtensionProvider());

        SharedPreferences preferences = getSharedPreferences(CONNECTION, MODE_PRIVATE);
        room = preferences.getString("room", "neerc");

        taskRegistry = TaskRegistry.getInstanceFor(room);
        userRegistry = UserRegistry.getInstanceFor(room);

        XMPPConnectionRegistry.addConnectionCreationListener(connectionCreationListener);
        taskRegistry.addListener(taskRegistryListener);

        connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");

        disconnect();

        taskRegistry.removeListener(taskRegistryListener);
        XMPPConnectionRegistry.removeConnectionCreationListener(connectionCreationListener);

        notificationManager.cancelAll();

        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public Set<Task> getNewTasks() {
        Set<Task> tasks = new TreeSet<>();

        if (taskRegistry == null)
            return tasks;

        for (Task task : taskRegistry.getTasks()) {
            TaskStatus status = task.getStatus(getUser());
            if (status != null && TaskActions.STATUS_NEW.equals(status.getType()))
                tasks.add(task);
        }

        return tasks;
    }

    private void updateTasksNotification(boolean alert) {
        Set<Task> tasks = getNewTasks();
        if (tasks.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_TASKS);
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setContentTitle(getResources().getQuantityString(R.plurals.notification_tasks_count, tasks.size(), tasks.size()))
            .setSmallIcon(R.drawable.ic_bulb_24dp)
            .setContentIntent(contentIntent)
            .setOngoing(true);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        Iterator<Task> it = tasks.iterator();
        for (int i = 0; it.hasNext() && i < 5; i++) {
            style.addLine(it.next().getTitle());
        }

        if (tasks.size() > 5) {
            style.setSummaryText(getString(R.string.notification_more, tasks.size() - 5));
        }

        builder.setStyle(style);

        if (alert) {
            builder.setVibrate(new long[] {0, 1000});
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(NOTIFICATION_TASKS, builder.build());
    }

    private void updateMessagesNotification() {
        if (importantMessages.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_MESSAGES);
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setContentTitle(getResources().getQuantityString(R.plurals.notification_messages_count, importantMessages.size(), importantMessages.size()))
            .setSmallIcon(R.drawable.ic_chat_24dp)
            .setContentIntent(contentIntent)
            .setVibrate(new long[] {0, 1000})
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(importantMessages.size());

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        Iterator<ChatMessage> it = importantMessages.iterator();
        for (int i = 0; it.hasNext() && i < 5; i++) {
            ChatMessage message = it.next();

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(TIME_FORMAT.format(message.getDate()) + " ");
            int start = sb.length();
            sb.append(message.getUser().getName());
            StyleSpan span = new StyleSpan(Typeface.BOLD);
            sb.setSpan(span, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(" " + message.getText());

            style.addLine(sb);
        }

        if (importantMessages.size() > 5) {
            style.setSummaryText(getString(R.string.notification_more, importantMessages.size() - 5));
        }

        builder.setStyle(style);

        notificationManager.notify(NOTIFICATION_MESSAGES, builder.build());
    }

    private void addImportantMessage(ChatMessage message) {
        synchronized (importantMessages) {
            importantMessages.add(message);
            updateMessagesNotification();
        }
    }

    public void clearImportantMessages() {
        synchronized (importantMessages) {
            importantMessages.clear();
            updateMessagesNotification();
        }
    }

    private void showMessageNotification(ChatMessage message) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
    }

    public boolean hasCredentials() {
        return hasCredentials;
    }

    public void connect() {
        SharedPreferences preferences = getSharedPreferences(CONNECTION, MODE_PRIVATE);
        String username = preferences.getString("username", null);
        String password = preferences.getString("password", null);
        String server = preferences.getString("server", null);
        int port = preferences.getInt("port", 5222);

        if (username == null ||
            password == null ||
            server == null) {
            hasCredentials = false;
            return;
        }

        hasCredentials = true;

        connectionTask = new ConnectionTask(this, username, password, server, port);
        connectionTask.execute();
    }

    public void disconnect() {
        if (connectionTask != null)
            connectionTask.cancel(false);

        if (connection != null)
            connection.disconnect();

        sendBroadcast(new Intent(ChatService.STATUS)
            .putExtra("status", ChatService.STATUS_DISCONNECTED));
    }

    public String getUser() {
        if (connection == null)
            return null;
        String username = connection.getUser();
        if (username != null)
            username = username.substring(0, username.indexOf('@'));
        return username;
    }

    public boolean isPowerUser() {
        String user = getUser();
        if (user == null)
            return false;
        UserEntry userEntry = userRegistry.findOrRegister(user);
        return userEntry.isPower();
    }

    public String getRoom() {
        return room;
    }

    public Collection<ChatMessage> getMessages() {
        return messages;
    }

    public void sendMessage(String text) {
        if (muc == null)
            return;

        try {
            muc.sendMessage(text);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to send message", e);
        }
    }

    public void sendStatus(Task task, TaskStatus status) {
        TaskStatusIQ iq = new TaskStatusIQ(task, status);
        iq.setType(TaskStatusIQ.Type.set);
        iq.setTo(room + "@neerc." + connection.getServiceName());

        Log.d(TAG, "Sending status:");
        Log.d(TAG, iq.toString());

        try {
            connection.sendStanza(iq);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to send status", e);
        }
    }

    public void sendTask(Task task) {
        TaskIQ iq = new TaskIQ(task);
        iq.setType(TaskIQ.Type.set);
        iq.setTo(room + "@neerc." + connection.getServiceName());

        Log.d(TAG, "Sending task:");
        Log.d(TAG, iq.toString());

        try {
            connection.sendStanza(iq);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to send task", e);
        }
    }
}
