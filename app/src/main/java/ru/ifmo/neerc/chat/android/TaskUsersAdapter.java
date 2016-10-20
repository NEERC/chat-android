package ru.ifmo.neerc.chat.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskRegistry;
import ru.ifmo.neerc.task.TaskRegistryListener;
import ru.ifmo.neerc.task.TaskStatus;

public class TaskUsersAdapter extends RecyclerView.Adapter<TaskUsersAdapter.ViewHolder> {

    private List<String> users = new ArrayList<String>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public ViewHolder(TextView view) {
            super(view);
            textView = view;
        }

        public void setUser(String user) {
            textView.setText(getShortName(user));
        }

        private String getShortName(String user) {
            return user.substring(0, 1) + user.substring(user.length() - 1, user.length());
        }
    }

    @Override
    public TaskUsersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.task_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String user = users.get(position);
        holder.setUser(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<String> users) {
        this.users = users;
        notifyDataSetChanged();
    }
}
