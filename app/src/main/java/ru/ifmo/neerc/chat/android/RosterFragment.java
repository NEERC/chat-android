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
