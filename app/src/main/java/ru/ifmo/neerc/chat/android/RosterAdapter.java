package ru.ifmo.neerc.chat.android;

import java.util.ArrayList;
import java.util.Collections;
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

    private final List<Object> items = new ArrayList<Object>();

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView nameView;

        public ViewHolder(View view) {
            super(view);
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void update() {
        Map<String, Set<UserEntry>> groups = new TreeMap<String, Set<UserEntry>>();
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
}
