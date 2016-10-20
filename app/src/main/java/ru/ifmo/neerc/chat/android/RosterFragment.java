package ru.ifmo.neerc.chat.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log; 

public class RosterFragment extends Fragment {

    private RecyclerView userList;
    private LinearLayoutManager layoutManager;

    private RosterAdapter adapter;
    private BroadcastReceiver userReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roster, container, false);

        userList = (RecyclerView) view.findViewById(R.id.user_list);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        layoutManager = new LinearLayoutManager(getActivity());
        userList.setLayoutManager(layoutManager);

        adapter = new RosterAdapter();
        userList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        userReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUsers();
            }
        };

        getContext().registerReceiver(userReceiver, new IntentFilter(ChatService.USER));

        updateUsers();
    }

    @Override
    public void onStop() {
        super.onStop();

        getContext().unregisterReceiver(userReceiver);
    }

    public void updateUsers() {
        adapter.update();
    }
}
