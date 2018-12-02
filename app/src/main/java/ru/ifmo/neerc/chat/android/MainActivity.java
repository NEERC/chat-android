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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.util.Log;

import ru.ifmo.neerc.chat.android.netadmin.NetAdminFragment;
import ru.ifmo.neerc.chat.android.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ACTION_CHAT = "ru.ifmo.neerc.chat.android.action.CHAT";
    public static final String ACTION_TASKS = "ru.ifmo.neerc.chat.android.action.TASKS";

    static final int LOGIN_REQUEST = 1;

    private BroadcastReceiver statusReceiver;
    private BroadcastReceiver taskReceiver;

    private ChatService chatService;

    private ViewPager viewPager;
    private ChatPagerAdapter pagerAdapter;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            chatService = null;

            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ChatService.getInstance() == null) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.add);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment fragment = new CreateTaskDialogFragment();
                fragment.show(getSupportFragmentManager(), "task_create");
            }
        });

        viewPager = (ViewPager) findViewById(R.id.viewpager);

        pagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(ChatPagerAdapter.FRAGMENT_CHAT);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == ChatPagerAdapter.FRAGMENT_TASKS) {
                    if (chatService != null && chatService.isPowerUser()) {
                        addButton.show();
                    }
                } else {
                    addButton.hide();

                    Fragment fragment = pagerAdapter.getFragment(ChatPagerAdapter.FRAGMENT_TASKS);
                    if (fragment != null)
                        ((TasksFragment) fragment).onTabUnselected();
                }
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        bindService(new Intent(this, ChatService.class), connection, 0);
    }

    public class ChatPagerAdapter extends FragmentPagerAdapter {
        private static final int FRAGMENT_ROSTER = 0;
        private static final int FRAGMENT_CHAT = 1;
        private static final int FRAGMENT_TASKS = 2;
        private static final int FRAGMENT_NETADMIN = 3;

        private static final int COUNT = 4;

        private final Map<Integer, Fragment> fragments = new TreeMap<Integer, Fragment>();

        public ChatPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            int count = COUNT;
            if (!ChatService.getInstance().isNetAdminEnabled()) {
                count -= 1;
            }
            return count;
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
                case FRAGMENT_NETADMIN:
                    return new NetAdminFragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ROSTER:
                    return getResources().getText(R.string.tab_roster).toString().toUpperCase();
                case FRAGMENT_CHAT:
                    return getResources().getText(R.string.tab_chat).toString().toUpperCase();
                case FRAGMENT_TASKS:
                    SpannableStringBuilder sb = new SpannableStringBuilder();
                    sb.append(getResources().getText(R.string.tab_tasks).toString().toUpperCase());
                    int newTasksCount = ChatService.getInstance().getNewTasks().size();
                    if (newTasksCount > 0) {
                        sb.append(" ");
                        RoundedBackgroundSpan span = new RoundedBackgroundSpan(
                            getResources().getColor(R.color.badgeBackgroundColor),
                            getResources().getColor(R.color.badgeTextColor)
                        );
                        int start = sb.length();
                        sb.append(Integer.toString(newTasksCount));
                        sb.setSpan(span, start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    return sb;
                case FRAGMENT_NETADMIN:
                    return getResources().getText(R.string.tab_netadmin).toString().toUpperCase();
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

    protected void updateTitle() {
        SharedPreferences preferences = getSharedPreferences(ChatService.CONNECTION, MODE_PRIVATE);
        String title = preferences.getString("username", "");

        if (ChatService.getInstance() != null) {
            String username = ChatService.getInstance().getUser();
            if (username != null && !username.equals(title)) {
                title = username + " (" + title + ")";
            }
        }

        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateTitle();

        pagerAdapter.notifyDataSetChanged();

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTitle();

                int status = intent.getIntExtra("status", 0);
                switch (status) {
                    case ChatService.STATUS_DISCONNECTED:
                        getSupportActionBar().setSubtitle(R.string.status_disconnected);
                        break;
                    case ChatService.STATUS_CONNECTING:
                        getSupportActionBar().setSubtitle(R.string.status_connecting);
                        break;
                    case ChatService.STATUS_CONNECTED:
                        getSupportActionBar().setSubtitle(R.string.status_connected);
                        break;
                }
            }
        };

        taskReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pagerAdapter.notifyDataSetChanged();
            }
        };

        registerReceiver(statusReceiver, new IntentFilter(ChatService.STATUS));
        registerReceiver(taskReceiver, new IntentFilter(ChatService.TASK));
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        switch (intent.getAction()) {
            case ACTION_CHAT: {
                viewPager.setCurrentItem(ChatPagerAdapter.FRAGMENT_CHAT);
                String username = intent.getStringExtra(ChatService.EXTRA_USERNAME);
                Fragment fragment = pagerAdapter.getFragment(ChatPagerAdapter.FRAGMENT_CHAT);
                if (username != null && (fragment instanceof ChatFragment)) {
                    ((ChatFragment) fragment).setPrivateAddress(username);
                }
                break;
            }
            case ACTION_TASKS: {
                viewPager.setCurrentItem(ChatPagerAdapter.FRAGMENT_TASKS);
                String taskId = intent.getStringExtra(ChatService.EXTRA_TASK_ID);
                Fragment fragment = pagerAdapter.getFragment(ChatPagerAdapter.FRAGMENT_TASKS);
                if (fragment instanceof TasksFragment) {
                    ((TasksFragment) fragment).setSelectedTask(taskId);
                }
                break;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (chatService != null)
            chatService.getConnectionManager().setForeground(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatService != null)
            chatService.getConnectionManager().setForeground(true);
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceiver(statusReceiver);
        unregisterReceiver(taskReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatService != null) {
            unbindService(connection);
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
            case R.id.settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.logout:
                getSharedPreferences(ChatService.CONNECTION, MODE_PRIVATE)
                    .edit()
                    .putBoolean("login", false)
                    .remove("last_message_date")
                    .apply();
                stopService(new Intent(this, ChatService.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
