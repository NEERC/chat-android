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

import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import ru.ifmo.neerc.chat.android.push.FirebasePushNotificationsManager;

public class ConnectionManager {

    private static final String TAG = "ConnectionManager";

    private static final int SUSPEND_DELAY = 5000;

    private Context context;

    private XMPPTCPConnection connection = null;
    private Thread connectionThread = null;

    private boolean foreground = true;
    private Handler suspendHandler = new Handler();

    public ConnectionManager(Context context, String username, String password, String server, int port) {
        this.context = context;

        createConnection(username, password, server, port);
    }

    private InetAddress getInetAddress(String server) {
        String[] parts = server.split(".");
        if (parts.length != 4)
            return null;
        try {
            byte[] addr = new byte[4];
            for (int i = 0; i < addr.length; i++)
                addr[i] = Byte.valueOf(parts[i]);
            return InetAddress.getByAddress(addr);
        } catch (NumberFormatException | UnknownHostException e) {
            return null;
        }
    }

    private void createConnection(String username, String password, String server, int port) {
        DomainBareJid serverJid;
        try {
            serverJid = JidCreate.domainBareFrom(server);
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Failed to create server JID", e);
            return;
        }

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
            .setUsernameAndPassword(XmppStringUtils.escapeLocalpart(username), password)
            .setServiceName(serverJid)
            .setPort(port);

        InetAddress address = getInetAddress(server);
        if (address != null)
            builder.setHostAddress(address);
        else
            builder.setHost(server);

        try {
            builder.setResource(username + "_" + StringUtils.randomString(10));
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Failed to set resource", e);
        }

        if (!BuildConfig.DEBUG) {
            builder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.required);
        }

        try {
            final char[] PASSWORD = "neercchat".toCharArray();

            AssetManager assetManager = context.getAssets();
            InputStream keystoreInputStream = assetManager.open("chat.bks");

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(keystoreInputStream, PASSWORD);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, PASSWORD);

            TrustManager[] tm = null;
            if (BuildConfig.DEBUG) {
                tm = new TrustManager[] { new TLSUtils.AcceptAllTrustManager() };
                TLSUtils.disableHostnameVerificationForTlsCertificates(builder);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tm, new SecureRandom());

            builder.setCustomSSLContext(sslContext);

            Log.d(TAG, "Client certificate loaded");
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Failed to load client certificate", e);
        }

        connection = new XMPPTCPConnection(builder.build());
        connection.addConnectionListener(new ConnectionListener());
    }

    public void connect() {
        if (connection == null || connection.isAuthenticated())
            return;

        if (connectionThread == null || !connectionThread.isAlive()) {
            connectionThread = new Thread(new ConnectionRunnable());
            connectionThread.start();
        }
    }

    public void disconnect() {
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }

        connectionThread = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection != null && connection.isConnected()) {
                    try {
                        FirebasePushNotificationsManager.getInstanceFor(connection).disable();
                    } catch (SmackException | XMPPException | InterruptedException e) {
                        Log.e(TAG, "Failed to disable push notifications", e);
                    }

                    connection.disconnect();
                }
            }
        }).start();
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;

        if (this.foreground) {
            connect();
        } else if (canSuspend()) {
            suspendHandler.postDelayed(new SuspendRunnable(), SUSPEND_DELAY);
        }
    }

    public boolean canSuspend() {
        if (foreground)
            return false;

        if (connection == null || !connection.isAuthenticated())
            return false;

        if (!connection.isSmResumptionPossible())
            return false;

        if (!FirebasePushNotificationsManager.getInstanceFor(connection).isEnabled())
            return false;

        return true;
    }

    private class ConnectionRunnable implements Runnable {
        private int getReconnectionDelay() {
            return 5;
        }

        @Override
        public void run() {
            if (connection == null)
                return;
            
            while (!Thread.interrupted()) {
                try {
                    context.sendBroadcast(new Intent(ChatService.STATUS)
                        .putExtra("status", ChatService.STATUS_CONNECTING));

                    try {
                        Log.i(TAG, "Connecting...");
                        connection.connect();
                    } catch (SmackException.AlreadyConnectedException e) {
                        Log.w(TAG, "Was already connected", e);
                    }

                    try {
                        Log.i(TAG, "Authenticating...");
                        connection.login();
                    } catch (SmackException.AlreadyLoggedInException e) {
                        Log.w(TAG, "Was already logged in", e);
                    }

                    return;
                } catch (SmackException | XMPPException | IOException e) {
                    context.sendBroadcast(new Intent(ChatService.STATUS)
                        .putExtra("status", ChatService.STATUS_DISCONNECTED));

                    Log.e(TAG, "Connection failed", e);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Connection thread was interrupted", e);
                    break;
                }

                int delay = getReconnectionDelay();

                while (delay > 0) {
                    try {
                        Log.i(TAG, "Connecting in " + delay + " seconds...");
                        Thread.sleep(1000);
                        delay--;
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Connection thread was interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            context.sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_DISCONNECTED));
            connection.disconnect();
        }
    }

    private class SuspendRunnable implements Runnable {
        @Override
        public void run() {
            if (!canSuspend())
                return;

            connection.instantShutdown();
            Log.d(TAG, "Connection was suspended");
        }
    }

    private class ConnectionListener extends AbstractConnectionListener {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            if (canSuspend()) {
                suspendHandler.postDelayed(new SuspendRunnable(), SUSPEND_DELAY);
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connect();
        }
    }
}
