/*
 * Copyright 2018 NEERC team
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

package ru.ifmo.neerc.chat.android.netadmin;

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

import ru.ifmo.neerc.chat.android.ChatService;
import ru.ifmo.neerc.chat.android.R;

public class NetAdminFragment extends Fragment {

    private RecyclerView roomList;

    private RoomsAdapter adapter;
    private BroadcastReceiver netadminReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.netadmin, container, false);

        roomList = (RecyclerView) view.findViewById(R.id.room_list);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        roomList.setLayoutManager(layoutManager);

        adapter = new RoomsAdapter();
        roomList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        netadminReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Computer computer = (Computer) intent.getSerializableExtra("computer");
                adapter.update(computer);
            }
        };

        getContext().registerReceiver(netadminReceiver, new IntentFilter(ChatService.NETADMIN));

        adapter.update(null);
    }

    @Override
    public void onStop() {
        super.onStop();

        getContext().unregisterReceiver(netadminReceiver);
    }
}
