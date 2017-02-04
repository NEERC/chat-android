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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import ru.ifmo.neerc.chat.user.UserEntry;
import ru.ifmo.neerc.chat.user.UserRegistry;

public class RosterAdapter extends RecyclerView.Adapter<RosterAdapter.ViewHolder> {

    private static int TYPE_USER  = 0;
    private static int TYPE_GROUP = 1;

    private boolean selectable = false;

    private final Map<String, Set<UserEntry>> groups = new TreeMap<String, Set<UserEntry>>();
    private final List<Object> items = new ArrayList<Object>();
    private final Set<Object> selection = new HashSet<Object>();

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView nameView;

        public ViewHolder(View view) {
            super(view);

            if (isSelectable()) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        toggleSelection(items.get(getAdapterPosition()));
                    }
                });
            }

            iconView = (ImageView) view.findViewById(R.id.icon);
            nameView = (TextView) view.findViewById(R.id.name);
        }

        public void setUser(UserEntry user) {
            if (user.isPower())
                iconView.setImageResource(R.drawable.ic_bulb);
            else
                iconView.setImageResource(R.drawable.ic_chat);
            iconView.setAlpha(user.isOnline() ? 1.0f : 0.3f);
            nameView.setText(user.getName());
        }

        public void setGroup(String group) {
            nameView.setText(group);
        }
    }

    public RosterAdapter() {
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);

        if (item instanceof UserEntry)
            return TYPE_USER;
        else if (item instanceof String)
            return TYPE_GROUP;

        throw new RuntimeException("Unsupported item type");
    }

    @Override
    public RosterAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final int layout = (viewType == TYPE_USER) ? R.layout.user : R.layout.group;

        View view = LayoutInflater.from(parent.getContext())
            .inflate(layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Object item = items.get(position);
        if (item instanceof UserEntry)
            holder.setUser((UserEntry) item);
        else if (item instanceof String)
            holder.setGroup((String) item);
        holder.itemView.setActivated(isSelected(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void update() {
        groups.clear();
        for (UserEntry user : UserRegistry.getInstance().getUsers()) {
            if (!groups.containsKey(user.getGroup()))
                groups.put(user.getGroup(), new TreeSet<UserEntry>());

            groups.get(user.getGroup()).add(user);
        }

        items.clear();
        for (String group : groups.keySet()) {
            items.add(group);
            for (UserEntry user : groups.get(group)) {
                items.add(user);
            }
        }

        notifyDataSetChanged();
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    private void toggleSelection(Object item) {
        setSelected(item, !isSelected(item));

        if (item instanceof UserEntry) {
            UserEntry user = (UserEntry) item;
            if (!isSelected(user)) {
                setSelected(user.getGroup(), false);
            }
        } else if (item instanceof String) {
            String group = (String) item;
            for (UserEntry user : groups.get(group)) {
                setSelected(user, isSelected(group));
            }
        }
    }

    private boolean isSelected(Object item) {
        return selection.contains(item);
    }

    private void setSelected(Object item, boolean selected) {
        if (selected)
            selection.add(item);
        else
            selection.remove(item);

        notifyItemChanged(items.indexOf(item));
    }

    public Set<UserEntry> getSelectedUsers() {
        Set<UserEntry> users = new HashSet<UserEntry>();
        for (Object item : selection) {
            if (item instanceof UserEntry)
                users.add((UserEntry) item);
        }
        return users;
    }

    public void setSelectedUsers(Set<UserEntry> users) {
        selection.clear();
        selection.addAll(users);
        notifyDataSetChanged();
    }
}
