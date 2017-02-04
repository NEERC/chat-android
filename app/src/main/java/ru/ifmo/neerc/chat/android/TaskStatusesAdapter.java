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
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

public class TaskStatusesAdapter extends RecyclerView.Adapter<TaskStatusesAdapter.ViewHolder> {

    private boolean expanded = false;

    private List<String> users = new ArrayList<String>();
    private List<TaskStatus> statuses = new ArrayList<TaskStatus>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView userView;
        private ImageView statusView;
        private TextView messageView;

        public ViewHolder(View view) {
            super(view);
            userView = (TextView) view.findViewById(R.id.user);
            statusView = (ImageView) view.findViewById(R.id.status);
            statusView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("TaskStatusesAdapter", "Clicked on " + view);
                }
            });
            messageView = (TextView) view.findViewById(R.id.message);
        }

        public void setUser(String user) {
            if (userView != null)
                userView.setText(user);
        }

        public void setStatus(TaskStatus status) {
            statusView.setImageResource(getResourceForStatus(status));
            if (messageView != null && status != null)
                messageView.setText(status.getValue());
        }

        private int getResourceForStatus(TaskStatus status) {
            if (status == null)
                return android.R.color.transparent;

            switch (status.getType()) {
                default:
                case "none":
                case "acknowledged":
                    return R.drawable.ic_checkbox_blank_circle_outline_white_24dp;
                case "running":
                    return R.drawable.ic_play_circle_outline_white_24dp;
                case "success":
                    return R.drawable.ic_checkbox_marked_circle_outline_white_24dp;
                case "fail":
                    return R.drawable.ic_information_outline_white_24dp;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return expanded ? 1 : 0;
    }

    @Override
    public TaskStatusesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = expanded ? R.layout.task_status_detail : R.layout.task_status;
        View view = LayoutInflater.from(parent.getContext())
            .inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String user = users.get(position);
        TaskStatus status = statuses.get(position);
        if (status == null)
            user = "";
        holder.setUser(user);
        holder.setStatus(status);
    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        notifyDataSetChanged();
    }

    public void setStatuses(Map<String, TaskStatus> statuses) {
        this.users.clear();
        this.statuses.clear();

        for (Map.Entry<String, TaskStatus> entry : statuses.entrySet()) {
            if (expanded && entry.getValue() == null)
                continue;

            this.users.add(entry.getKey());
            this.statuses.add(entry.getValue());
        }

        notifyDataSetChanged();
    }
}
