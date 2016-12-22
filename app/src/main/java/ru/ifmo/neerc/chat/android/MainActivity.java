package ru.ifmo.neerc.chat.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static final int LOGIN_REQUEST = 1;

    private BroadcastReceiver statusReceiver;

    private ChatService chatService;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();

            if (!chatService.hasCredentials()) {
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(loginIntent, LOGIN_REQUEST);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            chatService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        final ChatPagerAdapter pagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(ChatPagerAdapter.FRAGMENT_CHAT);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == ChatPagerAdapter.FRAGMENT_TASKS)
                    return;

                Fragment fragment = pagerAdapter.getFragment(ChatPagerAdapter.FRAGMENT_TASKS);
                if (fragment != null)
                    ((TasksFragment) fragment).onTabUnselected();
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        startService(new Intent(this, ChatService.class));
        bindService(new Intent(this, ChatService.class), connection, BIND_AUTO_CREATE);
    }

    public class ChatPagerAdapter extends FragmentPagerAdapter {
        private static final int FRAGMENT_ROSTER = 0;
        private static final int FRAGMENT_CHAT = 1;
        private static final int FRAGMENT_TASKS = 2;

        private static final int COUNT = 3;

        private final Map<Integer, Fragment> fragments = new TreeMap<Integer, Fragment>();

        public ChatPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_ROSTER:
                    return new RosterFragment();
                case FRAGMENT_CHAT:
                    return new ChatFragment();
                case FRAGMENT_TASKS:
                    return new TasksFragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ROSTER:
                    return "Roster";
                case FRAGMENT_CHAT:
                    return "Chat";
                case FRAGMENT_TASKS:
                    return "Tasks";
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            fragments.put(position, (Fragment) item);
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            fragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getFragment(int position) {
            return fragments.get(position);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra("status", 0);
                switch (status) {
                    case ChatService.STATUS_DISCONNECTED:
                        getSupportActionBar().setSubtitle("Disconnected");
                        break;
                    case ChatService.STATUS_CONNECTING:
                        getSupportActionBar().setSubtitle("Connecting...");
                        break;
                    case ChatService.STATUS_CONNECTED:
                        getSupportActionBar().setSubtitle("Connected");
                        break;
                }
            }
        };

        registerReceiver(statusReceiver, new IntentFilter(ChatService.STATUS));
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceiver(statusReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                chatService.connect();
            } else {
                exit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                if (chatService != null)
                    chatService.disconnect();
                getSharedPreferences(ChatService.USER, MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, LOGIN_REQUEST);
                return true;
            case R.id.exit:
                exit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void exit() {
        stopService(new Intent(this, ChatService.class));
        finish();
        System.exit(0);
    }
}
