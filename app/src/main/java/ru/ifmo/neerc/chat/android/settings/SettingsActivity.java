package ru.ifmo.neerc.chat.android.settings;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    public static final String KEY_PREF_KEEP_CONNECTION = "pref_keepConnection";
    public static final String KEY_PREF_NETADMIN = "pref_netadmin";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
    }
}
