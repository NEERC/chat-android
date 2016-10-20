package ru.ifmo.neerc.chat.android;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
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
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskActions;
import ru.ifmo.neerc.task.TaskStatus;
import ru.ifmo.neerc.task.TaskRegistry;
import ru.ifmo.neerc.task.TaskRegistryListener;
import ru.ifmo.neerc.chat.packet.TaskExtension;
import ru.ifmo.neerc.chat.packet.TaskExtensionProvider;
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

    private static ChatService instance;

    private final IBinder binder = new LocalBinder();

    private NotificationManager notificationManager;

    private boolean hasCredentials = false;

    private ConnectionTask connectionTask;

    private AbstractXMPPConnection connection;
    private MultiUserChat muc;

    private Set<ChatMessage> messages = Collections.synchronizedSortedSet(new TreeSet<ChatMessage>());

    private static final int NOTIFICATION = 1;

    private int notificationId = NOTIFICATION + 1;

    public class LocalBinder extends Binder {
        ChatService getService() {
            return ChatService.this;
        }
    }

    public static ChatService getInstance() {
        return instance;
    }

    private final ConnectionListener connectionListener = new AbstractConnectionListener() {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            muc = MultiUserChatManager.getInstanceFor(connection)
                .getMultiUserChat("neerc@conference." + connection.getHost());
            muc.addMessageListener(new MessageListener() {
                @Override
                public void processMessage(Message message) {
                    Log.d(TAG, "Received message:");
                    Log.d(TAG, message.toString());
                    synchronized (messages) {
                        messages.add(new ChatMessage(message));
                    }
                    sendBroadcast(new Intent(ChatService.MESSAGE));
                }
            });
            muc.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                @Override
                public void joined(String participant) {
                    Log.d(TAG, "User joined: " + participant);
                    UserRegistry.getInstance().putOnline(participant);
                    sendBroadcast(new Intent(ChatService.USER));
                }

                @Override
                public void left(String participant) {
                    Log.d(TAG, "User left: " + participant);
                    UserRegistry.getInstance().putOffline(participant);
                    sendBroadcast(new Intent(ChatService.USER));
                }
            });

            String username = connection.getUser();
            username = username.substring(0, username.indexOf('@'));

            try {
                muc.join(username);
            } catch (SmackException | XMPPException e) {
                Log.e(TAG, "Failed to join the room", e);
            }

            TaskList tasksQuery = new TaskList();
            tasksQuery.setTo("neerc." + connection.getHost());

            try {
                connection.sendStanza(tasksQuery);
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "Failed to query tasks", e);
            }

            UserList usersQuery = new UserList();
            usersQuery.setTo("neerc." + connection.getHost());

            try {
                connection.sendStanza(usersQuery);
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "Failed to query users", e);
            }
        }

        @Override
        public void connected(XMPPConnection connection) {
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

        @Override
        public void reconnectionSuccessful() {
            sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_CONNECTED));
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
                    TaskRegistry.getInstance().update(task);
                }
                sendBroadcast(new Intent(ChatService.TASK));
            } else {
                TaskExtension extension = packet.getExtension(TaskExtension.ELEMENT_NAME, TaskExtension.NAMESPACE);
                if (extension != null) {
                    Log.d(TAG, "Received task:");
                    Log.d(TAG, extension.getTask().getTitle());
                    TaskRegistry.getInstance().update(extension.getTask());
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
                    UserEntry u = UserRegistry.getInstance().findOrRegister(user.getName());
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
            TaskStatus status = task.getStatus(getUser());
            if (status != null && TaskActions.STATUS_NEW.equals(status.getType()))
                showTaskNotification(task);
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
        showNotification();

        ProviderManager.addIQProvider(TaskList.ELEMENT_NAME, TaskList.NAMESPACE, new TaskListProvider());
        ProviderManager.addIQProvider(UserList.ELEMENT_NAME, UserList.NAMESPACE, new UserListProvider());
        ProviderManager.addExtensionProvider(TaskExtension.ELEMENT_NAME, TaskExtension.NAMESPACE, new TaskExtensionProvider());

        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
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
        });

        TaskRegistry.getInstance().addListener(taskRegistryListener);

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
        notificationManager.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void showNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_chat_24dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build();

        notificationManager.notify(NOTIFICATION, notification);
    }

    private void showTaskNotification(Task task) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_bulb_24dp)
            .setContentTitle("New Task")
            .setContentText(task.getTitle())
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVibrate(new long[] {0, 1000});

        Notification notification = builder.build();
        notificationManager.notify(notificationId++, notification);
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
        username = username.substring(0, username.indexOf('@'));
        return username;
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
        iq.setTo("neerc." + connection.getHost());

        Log.d(TAG, "Sending status:");
        Log.d(TAG, iq.toString());

        try {
            connection.sendStanza(iq);
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "Failed to send status", e);
        }
    }
}
