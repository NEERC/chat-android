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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

public class ConnectionTask extends AsyncTask<Void, String, Boolean> {

    private static final String TAG = "ConnectionTask";

    private Context context;

    private String username;
    private String password;
    private String server;
    private String hostname;
    private int port;

    public ConnectionTask(Context context, String username, String password, String server, String hostname, int port) {
        this.context = context;

        this.username = username;
        this.password = password;
        this.server = server;
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
            .setUsernameAndPassword(username, password)
            .setServiceName(hostname)
            .setHost(server)
            .setPort(port)
            .setResource(StringUtils.randomString(10));

        try {
            TLSUtils.acceptAllCertificates(builder);
            TLSUtils.disableHostnameVerificationForTlsCertificicates(builder);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "Failed to configure connection", e);
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
