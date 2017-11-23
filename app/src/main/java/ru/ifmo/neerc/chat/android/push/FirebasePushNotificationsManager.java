package ru.ifmo.neerc.chat.android.push;

import java.util.Map;
import java.util.WeakHashMap;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class FirebasePushNotificationsManager {
    private static final String TAG = "PushNotifications";

    private static final Map<XMPPConnection, FirebasePushNotificationsManager> INSTANCES = new WeakHashMap<>();

    public static synchronized FirebasePushNotificationsManager getInstanceFor(XMPPConnection connection) {
        FirebasePushNotificationsManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new FirebasePushNotificationsManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    private final XMPPConnection connection;
    private final PushNotificationsManager pushNotificationsManager;
    private final AdHocCommandManager adHocCommandManager;

    private Jid serviceJid;
    private Jid pushJid;
    private boolean enabled = false;

    private FirebasePushNotificationsManager(XMPPConnection connection) {
        this.connection = connection;
        this.connection.addConnectionListener(new ConnectionListener());

        pushNotificationsManager = PushNotificationsManager.getInstanceFor(connection);
        adHocCommandManager = AdHocCommandManager.getAddHocCommandsManager(connection);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (!pushNotificationsManager.isSupportedByServer()) {
            Log.d(TAG, "Server does not support push notifications");
            return;
        }

        RemoteCommand registerCommand = adHocCommandManager.getRemoteCommand(serviceJid, "register-push-gcm");

        FormField token = new FormField("token");
        token.addValue(FirebaseInstanceId.getInstance().getToken());

        Form form = new Form(DataForm.Type.submit);
        form.addField(token);

        registerCommand.execute(form);

        String jid = registerCommand.getForm().getField("jid").getValues().get(0);
        String node = registerCommand.getForm().getField("node").getValues().get(0);
        Log.d(TAG, "Registered node " + node + " jid " + jid);

        try {
            pushJid = JidCreate.from(jid);
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Failed to create pubsub JID", e);
            return;
        }

        pushNotificationsManager.enable(pushJid, node);
        Log.d(TAG, "Enabled push notifications for node " + node + " jid " + jid);

        enabled = true;
    }

    public void disable() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (!enabled) {
            return;
        }

        try {
            pushNotificationsManager.disableAll(pushJid);
            Log.d(TAG, "Disabled push notifications for jid " + pushJid);

            RemoteCommand unregisterCommand = adHocCommandManager.getRemoteCommand(serviceJid, "unregister-push");
            unregisterCommand.execute();
            Log.d(TAG, "Unregistered node");
        } finally {
            enabled = false;
        }
    }

    private class ConnectionListener extends AbstractConnectionListener {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            if (((XMPPTCPConnection) connection).streamWasResumed())
                return;

            try {
                serviceJid = JidCreate.from("push." + connection.getServiceName());
            } catch (XmppStringprepException e) {
                Log.e(TAG, "Failed to create push service JID", e);
                return;
            }

            try {
                enable();
            } catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
                Log.e(TAG, "Failed to enable push notifications", e);
            }
        }
    }
};
