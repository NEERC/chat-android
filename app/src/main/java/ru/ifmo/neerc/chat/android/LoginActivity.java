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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.lang.NumberFormatException;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameWrapper;
    private TextInputLayout passwordWrapper;
    private TextInputLayout roomWrapper;
    private TextInputLayout serverWrapper;
    private TextInputLayout portWrapper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameWrapper = (TextInputLayout) findViewById(R.id.usernameWrapper);
        passwordWrapper = (TextInputLayout) findViewById(R.id.passwordWrapper);
        roomWrapper = (TextInputLayout) findViewById(R.id.roomWrapper);
        serverWrapper = (TextInputLayout) findViewById(R.id.serverWrapper);
        portWrapper = (TextInputLayout) findViewById(R.id.portWrapper);

        final SharedPreferences preferences = getSharedPreferences(ChatService.CONNECTION, MODE_PRIVATE);
        usernameWrapper.getEditText().setText(preferences.getString("username", ""));
        passwordWrapper.getEditText().setText(preferences.getString("password", ""));
        roomWrapper.getEditText().setText(preferences.getString("room", "neerc"));
        serverWrapper.getEditText().setText(preferences.getString("server", "neerc.ifmo.ru"));
        portWrapper.getEditText().setText(String.valueOf(preferences.getInt("port", 5222)));

        Button login = (Button)findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        if (preferences.getBoolean("login", false)) {
            login();
        }
    }

    private void login() {
        if (!validate())
            return;

        final SharedPreferences preferences = getSharedPreferences(ChatService.CONNECTION, MODE_PRIVATE);
        preferences.edit()
            .putString("username", usernameWrapper.getEditText().getText().toString())
            .putString("password", passwordWrapper.getEditText().getText().toString())
            .putString("room", roomWrapper.getEditText().getText().toString())
            .putString("server", serverWrapper.getEditText().getText().toString())
            .putInt("port", Integer.parseInt(portWrapper.getEditText().getText().toString()))
            .putBoolean("login", true)
            .apply();

        startService(new Intent(LoginActivity.this, ChatService.class));

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean validate() {
        String username = usernameWrapper.getEditText().getText().toString();
        if (username.isEmpty()) {
            usernameWrapper.setError(getResources().getText(R.string.login_username_error));
            return false;
        } else {
            usernameWrapper.setError(null);
        }

        String password = passwordWrapper.getEditText().getText().toString();
        if (password.isEmpty()) {
            passwordWrapper.setError(getResources().getText(R.string.login_password_error));
            return false;
        } else {
            passwordWrapper.setError(null);
        }

        String room = roomWrapper.getEditText().getText().toString();
        if (room.isEmpty()) {
            roomWrapper.setError(getResources().getText(R.string.login_room_error));
            return false;
        } else {
            roomWrapper.setError(null);
        }

        String server = serverWrapper.getEditText().getText().toString();
        if (server.isEmpty()) {
            serverWrapper.setError(getResources().getText(R.string.login_server_error));
            return false;
        } else {
            serverWrapper.setError(null);
        }

        String port = portWrapper.getEditText().getText().toString();
        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            portNumber = 0;
        }

        if (portNumber < 1 || portNumber > 65535) {
            portWrapper.setError(getResources().getText(R.string.login_port_error));
            return false;
        } else {
            portWrapper.setError(null);
        }

        return true;
    }
}
