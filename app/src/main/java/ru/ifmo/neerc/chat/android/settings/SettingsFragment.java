package ru.ifmo.neerc.chat.android.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import ru.ifmo.neerc.chat.android.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
