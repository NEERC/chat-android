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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.Build;
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
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import ru.ifmo.neerc.chat.android.push.FirebasePushNotificationsManager;
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
    public static final String TASK_ACTION = "ru.ifmo.neerc.chat.android.TASK_ACTION";

    public static final String EXTRA_TASK_ID = "ru.ifmo.neerc.chat.android.extra.TASK_ID";
    public static final String EXTRA_ACTION = "ru.ifmo.neerc.chat.android.extra.ACTION";

    public static final int STATUS_DISCONNECTED = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_CONNECTED = 2;

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final int DAY_IN_SECONDS = 24 * 60 * 60;

    private static ChatService instance;

    private final IBinder binder = new LocalBinder();

    private NotificationManager notificationManager;

    private String room;
    private boolean hasCredentials = false;

    private ConnectionManager connectionManager;
    private FirebasePushNotificationsManager pushNotificationsManager;

    private AbstractXMPPConnection connection;
    private MultiUserChat muc;

    private TaskRegistry taskRegistry;
    private UserRegistry userRegistry;

    private Map<String, Integer> taskNotificationMap = new HashMap<>();

    private Set<ChatMessage> messages = Collections.synchronizedSortedSet(new TreeSet<ChatMessage>());
    private Set<ChatMessage> importantMessages = Collections.synchronizedSortedSet(new TreeSet<ChatMessage>(Collections.reverseOrder()));

    private Date lastMessageDate = null;

    private static final int NOTIFICATION_TASKS = 1;
    private static final int NOTIFICATION_MESSAGES = 2;

    private int notificationId = NOTIFICATION_MESSAGES + 1;

    private static final String GROUP_TASKS = "tasks";

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

            pushNotificationsManager = FirebasePushNotificationsManager.getInstanceFor(connection);
        }
    };

    private final ConnectionListener connectionListener = new AbstractConnectionListener() {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            if (((XMPPTCPConnection) connection).streamWasResumed()) {
                sendBroadcast(new Intent(ChatService.STATUS)
                    .putExtra("status", ChatService.STATUS_CONNECTED));
                return;
            }

            UserList usersQuery = new UserList();
            usersQuery.setTo(room + "@neerc." + connection.getServiceName());

            try {
                connection.sendStanza(usersQuery);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                Log.e(TAG, "Failed to query users", e);
            }

            TaskList tasksQuery = new TaskList();
            tasksQuery.setTo(room + "@neerc." + connection.getServiceName());

            try {
                connection.sendStanza(tasksQuery);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                Log.e(TAG, "Failed to query tasks", e);
            }

            if (muc == null) {
                EntityBareJid roomJid;

                try {
                    roomJid = JidCreate.entityBareFrom(room + "@conference." + connection.getServiceName());
                } catch (XmppStringprepException e) {
                    Log.e(TAG, "Failed to create room JID", e);
                    return;
                }

                muc = MultiUserChatManager.getInstanceFor(connection)
                    .getMultiUserChat(roomJid);
                muc.addMessageListener(messageListener);
                muc.addParticipantListener(presenceListener);
            }

            try {
                MucEnterConfiguration.Builder builder = muc.getEnterConfigurationBuilder(Resourcepart.from(getUser()));
                if (lastMessageDate != null) {
                    builder.requestHistorySince(new Date(lastMessageDate.getTime() + 1));
                } else {
                    builder.requestHistorySince(DAY_IN_SECONDS);
                }
                muc.join(builder.build());
            } catch (SmackException | XMPPException | XmppStringprepException | InterruptedException e) {
                Log.e(TAG, "Failed to join the room", e);
            }

            sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_CONNECTED));
        }

        @Override
        public void connectionClosed() {
            sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_DISCONNECTED));
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
            Resourcepart resource = message.getFrom().getResourceOrNull();
            if (resource == null)
                return;
            String username = resource.toString();

            UserEntry user = userRegistry.findOrRegister(username);
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
            if (delay == null && chatMessage.isImportantFor(currentUser)) {
                addImportantMessage(chatMessage);
            }

            sendBroadcast(new Intent(ChatService.MESSAGE)
                .putExtra("message", chatMessage));

            lastMessageDate = time;
        }
    };

    private final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void processPresence(Presence presence) {
            Resourcepart resource = presence.getFrom().getResourceOrNull();
            if (resource == null)
                return;
            String username = resource.toString();

            if (presence.isAvailable()) {
                Log.d(TAG, "User joined: " + username);
                userRegistry.putOnline(username);
                sendBroadcast(new Intent(ChatService.USER));
            } else {
                Log.d(TAG, "User left: " + username);
                userRegistry.putOffline(username);
                sendBroadcast(new Intent(ChatService.USER));
            }
        }
    };

    private final StanzaListener taskListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza packet) {
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
        public void processStanza(Stanza packet) {
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
        @Override
        public void taskChanged(Task task) {
            updateTaskNotification(task);
        }

        @Override
        public void tasksReset() {
        }
    };

    private final BroadcastReceiver taskActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskId = intent.getStringExtra(EXTRA_TASK_ID);
            Task task = taskRegistry.getById(taskId);
            String user = getUser();
            int action = intent.getIntExtra(EXTRA_ACTION, -1);

            if (!TaskActions.isActionSupported(task, user, action)) {
                return;
            }

            String type = TaskActions.getNewStatus(task, user, action);
            sendStatus(task, new TaskStatus(type, ""));

            connectionManager.connect();
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

        registerReceiver(taskActionReceiver, new IntentFilter(TASK_ACTION));

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

        unregisterReceiver(taskActionReceiver);

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

    private void updateTaskNotificationSummary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        Set<Task> tasks = getNewTasks();
        if (tasks.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_TASKS);
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_TASKS);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                                                                NOTIFICATION_TASKS,
                                                                intent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setContentTitle(getResources().getQuantityString(R.plurals.notification_tasks_count, tasks.size(), tasks.size()))
            .setSmallIcon(R.drawable.ic_bulb_24dp)
            .setContentIntent(contentIntent)
            .setNumber(tasks.size())
            .setGroup(GROUP_TASKS)
            .setGroupSummary(true);

        notificationManager.notify(NOTIFICATION_TASKS, builder.build());
    }

    private void updateTaskNotification(Task task) {
        updateTaskNotificationSummary();

        TaskStatus status = task.getStatus(getUser());
        if (status != null && TaskActions.STATUS_NEW.equals(status.getType())) {
            if (!taskNotificationMap.containsKey(task.getId())) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(MainActivity.ACTION_TASKS);
                intent.putExtra(EXTRA_TASK_ID, task.getId());

                PendingIntent contentIntent = PendingIntent.getActivity(
                    this,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.notification_task_title))
                    .setContentText(task.getTitle())
                    .setSmallIcon(R.drawable.ic_bulb_24dp)
                    .setContentIntent(contentIntent)
                    .setGroup(GROUP_TASKS)
                    .setVibrate(new long[] {0, 1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

                addTaskAction(builder, task, TaskActions.ACTION_START);
                addTaskAction(builder, task, TaskActions.ACTION_DONE);
                addTaskAction(builder, task, TaskActions.ACTION_FAIL);

                notificationManager.notify(notificationId, builder.build());

                taskNotificationMap.put(task.getId(), notificationId++);
            }
        } else {
            Integer id = taskNotificationMap.get(task.getId());
            if (id != null) {
                notificationManager.cancel(id);
            }
        }
    }

    private void addTaskAction(NotificationCompat.Builder builder, Task task, int action) {
        if (!TaskActions.isActionSupported(task, getUser(), action))
            return;

        PendingIntent pendingIntent;

        if (action == TaskActions.ACTION_FAIL || task.getType().equals(TaskActions.TYPE_QUESTION)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(MainActivity.ACTION_TASKS);
            intent.putExtra(EXTRA_TASK_ID, task.getId());
            intent.putExtra(EXTRA_ACTION, action);

            pendingIntent = PendingIntent.getActivity(
                this,
                notificationId * 3 + action,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            );
        } else {
            Intent intent = new Intent(ChatService.TASK_ACTION);
            intent.putExtra(EXTRA_TASK_ID, task.getId());
            intent.putExtra(EXTRA_ACTION, action);

            pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId * 3 + action,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        Resources res = getResources();
        int icon = res.obtainTypedArray(R.array.notification_task_action_icons).getResourceId(action, 0);
        String title = res.getStringArray(R.array.notification_task_actions)[action];
        builder.addAction(icon, title, pendingIntent);
    }

    private void updateMessagesNotification() {
        if (importantMessages.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_MESSAGES);
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_CHAT);

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

        connectionManager = new ConnectionManager(this, username, password, server, port);
        connectionManager.connect();
    }

    public void disconnect() {
        if (muc != null) {
            muc.removeMessageListener(messageListener);
            muc.removeParticipantListener(presenceListener);
        }

        connectionManager.disconnect();
        connectionManager = null;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public String getUser() {
        if (connection == null || connection.getUser() == null)
            return null;
        Localpart username = connection.getUser().getLocalpartOrNull();
        if (username == null)
            return null;
        return username.toString();
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
        } catch (SmackException.NotConnectedException | InterruptedException e) {
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
        } catch (SmackException.NotConnectedException | InterruptedException e) {
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
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            Log.e(TAG, "Failed to send task", e);
        }
    }
}
