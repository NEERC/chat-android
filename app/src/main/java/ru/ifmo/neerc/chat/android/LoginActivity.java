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
    private TextInputLayout serverWrapper;
    private TextInputLayout portWrapper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameWrapper = (TextInputLayout) findViewById(R.id.usernameWrapper);
        passwordWrapper = (TextInputLayout) findViewById(R.id.passwordWrapper);
        serverWrapper = (TextInputLayout) findViewById(R.id.serverWrapper);
        portWrapper = (TextInputLayout) findViewById(R.id.portWrapper);

        final SharedPreferences preferences = getSharedPreferences(ChatService.CONNECTION, MODE_PRIVATE);
        usernameWrapper.getEditText().setText(preferences.getString("username", ""));
        passwordWrapper.getEditText().setText(preferences.getString("password", ""));
        serverWrapper.getEditText().setText(preferences.getString("server", ""));
        portWrapper.getEditText().setText(String.valueOf(preferences.getInt("port", 5222)));

        Button login = (Button)findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validate())
                    return;

                preferences.edit()
                    .putString("username", usernameWrapper.getEditText().getText().toString())
                    .putString("password", passwordWrapper.getEditText().getText().toString())
                    .putString("server", serverWrapper.getEditText().getText().toString())
                    .putInt("port", Integer.parseInt(portWrapper.getEditText().getText().toString()))
                    .commit();
                
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private boolean validate() {
        String username = usernameWrapper.getEditText().getText().toString();
        if (username.isEmpty()) {
            usernameWrapper.setError("Username can not be empty");
            return false;
        } else {
            usernameWrapper.setError(null);
        }

        String password = passwordWrapper.getEditText().getText().toString();
        if (password.isEmpty()) {
            passwordWrapper.setError("Password can not be empty");
            return false;
        } else {
            passwordWrapper.setError(null);
        }

        String server = serverWrapper.getEditText().getText().toString();
        if (server.isEmpty()) {
            serverWrapper.setError("Server can not be empty");
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
            portWrapper.setError("Port should be in the range 1-65535");
            return false;
        } else {
            portWrapper.setError(null);
        }

        return true;
    }
}
