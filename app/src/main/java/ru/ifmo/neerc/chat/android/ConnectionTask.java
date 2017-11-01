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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jxmpp.util.XmppStringUtils;

public class ConnectionTask extends AsyncTask<Void, String, Boolean> {

    private static final String TAG = "ConnectionTask";

    private Context context;

    private String username;
    private String password;
    private String server;
    private int port;

    public ConnectionTask(Context context, String username, String password, String server, int port) {
        this.context = context;

        this.username = username;
        this.password = password;
        this.server = server;
        this.port = port;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
            .setUsernameAndPassword(XmppStringUtils.escapeLocalpart(username), password)
            .setServiceName(server)
            .setHost(server)
            .setPort(port)
            .setResource(StringUtils.randomString(10));

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
                TLSUtils.disableHostnameVerificationForTlsCertificicates(builder);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tm, new SecureRandom());

            builder.setCustomSSLContext(sslContext);

            Log.d(TAG, "Client certificate loaded");
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Failed to load client certificate", e);
        }

        AbstractXMPPConnection conn = new XMPPTCPConnection(builder.build());

        ReconnectionManager.getInstanceFor(conn).enableAutomaticReconnection();

        while (!isCancelled()) {
            try {
                context.sendBroadcast(new Intent(ChatService.STATUS)
                    .putExtra("status", ChatService.STATUS_CONNECTING));

                Log.i(TAG, "Connecting...");
                conn.disconnect();
                conn.connect();
                Log.i(TAG, "Authenticating...");
                conn.login();

                return true;
            } catch (SmackException | XMPPException | IOException e) {
                context.sendBroadcast(new Intent(ChatService.STATUS)
                    .putExtra("status", ChatService.STATUS_DISCONNECTED));

                Log.e(TAG, "Connection failed", e);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

        if (isCancelled()) {
            context.sendBroadcast(new Intent(ChatService.STATUS)
                .putExtra("status", ChatService.STATUS_DISCONNECTED));
            conn.disconnect();
        }

        return false;
    }
}
