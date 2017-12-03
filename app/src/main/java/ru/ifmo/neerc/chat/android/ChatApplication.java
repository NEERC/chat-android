package ru.ifmo.neerc.chat.android;

import android.app.Application;

import com.bugfender.sdk.Bugfender;

public class ChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Bugfender.init(this, "MkGbha2ybbqwLdTmH4MG3D8SJ800jlSO", BuildConfig.DEBUG);
        Bugfender.enableLogcatLogging();
        Bugfender.enableUIEventLogging(this);
    }
}
