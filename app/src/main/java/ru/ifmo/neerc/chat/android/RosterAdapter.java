package ru.ifmo.neerc.chat.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final List<UserEntry> users = new ArrayList<UserEntry>();

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
    }

    public RosterAdapter() {
    }

    @Override
    public RosterAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UserEntry user = users.get(position);
        holder.setUser(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void update() {
        users.clear();
        users.addAll(UserRegistry.getInstance().getUsers());
        Collections.sort(users);
        notifyDataSetChanged();
    }
}
