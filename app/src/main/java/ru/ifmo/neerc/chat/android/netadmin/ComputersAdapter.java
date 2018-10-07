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

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.ifmo.neerc.chat.android.ChatService;
import ru.ifmo.neerc.chat.android.R;

public class ComputersAdapter extends RecyclerView.Adapter<ComputersAdapter.ViewHolder> {
    public final List<Computer> computers;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameView;

        public ViewHolder(View view) {
            super(view);

            nameView = (TextView) view.findViewById(R.id.name);
        }
    }

    public ComputersAdapter(Collection<Computer> computers) {
        this.computers = new ArrayList<Computer>(computers);
    }

    @Override
    public ComputersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.computer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Computer computer = computers.get(position);
        holder.nameView.setText(computer.getName());
        holder.itemView.setBackgroundColor(computer.isReachable() ? Color.GREEN : Color.RED);
    }

    @Override
    public int getItemCount() {
        return computers.size();
    }
}
