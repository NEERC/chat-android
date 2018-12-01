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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import ru.ifmo.neerc.chat.android.AutoGridLayoutManager;
import ru.ifmo.neerc.chat.android.ChatService;
import ru.ifmo.neerc.chat.android.R;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.ViewHolder> {
    private static final String TAG = "RoomsAdapter";

    public final List<String> rooms = new ArrayList<String>();

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameView;
        private RecyclerView computerList;

        public ViewHolder(View view) {
            super(view);

            nameView = (TextView) view.findViewById(R.id.name);
            computerList = (RecyclerView) view.findViewById(R.id.computer_list);

            AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(view.getContext(), 32);
            computerList.setLayoutManager(layoutManager);
        }
    }

    @Override
    public RoomsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String roomName = rooms.get(position);
        holder.nameView.setText(roomName);

        NetAdminManager netAdminManager = ChatService.getInstance().getNetAdminManager();
        ComputersAdapter adapter = new ComputersAdapter(netAdminManager.getComputers(roomName));
        holder.computerList.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void update(Computer computer) {
        Log.d(TAG, "update " + computer);
        if (computer != null) {
            notifyDataSetChanged(); // XXX
        } else {
            rooms.clear();
            rooms.addAll(ChatService.getInstance().getNetAdminManager().getRooms());
            notifyDataSetChanged();
        }
    }
}
