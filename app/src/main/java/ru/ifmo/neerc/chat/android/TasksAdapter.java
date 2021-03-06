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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskRegistry;
import ru.ifmo.neerc.task.TaskRegistryListener;
import ru.ifmo.neerc.task.TaskStatus;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.ViewHolder> {

    private final List<Task> tasks = new ArrayList<Task>();
    private final Map<String, Integer> taskPositions = new HashMap<>();
    private final List<String> users = new ArrayList<String>();
    private final Set<Task> expandedTasks = new HashSet<Task>();

    private String selectedTask = null;

    private View.OnClickListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private RecyclerView statusList;
        private ImageView expandView;
        private TaskStatusesAdapter adapter;

        public ViewHolder(View view) {
            super(view);

            view.setOnClickListener(listener);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    expandOrCollapseTask(getAdapterPosition());
                    return true;
                }
            });

            titleView = (TextView) view.findViewById(R.id.title);

            expandView = (ImageView) view.findViewById(R.id.expand);

            statusList = (RecyclerView) view.findViewById(R.id.status_list);

            AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(view.getContext(), 32);
            statusList.setLayoutManager(layoutManager);

            adapter = new TaskStatusesAdapter();
            statusList.setAdapter(adapter);
        }

        public void setTitle(String title) {
            titleView.setText(title);
        }

        public void setExpanded(boolean expanded) {
            AutoGridLayoutManager layoutManager = (AutoGridLayoutManager) statusList.getLayoutManager();
            layoutManager.setColumnWidth(expanded ? -1 : 32);
            if (expanded)
                expandView.setImageResource(R.drawable.ic_chevron_up_grey600_24dp);
            else
                expandView.setImageResource(R.drawable.ic_chevron_down_grey600_24dp);
            adapter.setExpanded(expanded);
        }

        public void setStatuses(Map<String, TaskStatus> statuses) {
            adapter.setStatuses(statuses);
        }
    }

    public TasksAdapter(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public TasksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = tasks.get(position);
        Map<String, TaskStatus> statuses = new TreeMap<String, TaskStatus>();
        for (String user : users) {
            statuses.put(user, task.getStatus(user));
        }
        holder.itemView.setActivated(task.getId().equals(selectedTask));
        holder.setTitle(task.getTitle());
        holder.setExpanded(expandedTasks.contains(task));
        holder.setStatuses(statuses);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void update() {
        tasks.clear();
        taskPositions.clear();
        tasks.addAll(TaskRegistry.getInstanceFor(ChatService.getInstance().getRoom()).getTasks());
        Set<String> usersSet = new TreeSet<String>();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            taskPositions.put(task.getId(), i);
            usersSet.addAll(task.getStatuses().keySet());
        }
        users.clear();
        users.addAll(usersSet);
        expandedTasks.retainAll(tasks);
        notifyDataSetChanged();
    }

    public void setSelectedTask(int position) {
        if (position < 0) {
            setSelectedTask(null);
            return;
        }

        if (position >= tasks.size()) {
            return;
        }

        Task task = tasks.get(position);
        setSelectedTask(task.getId());
    }

    public void setSelectedTask(String taskId) {
        if (selectedTask != null) {
            Integer oldPosition = taskPositions.get(selectedTask);
            if (oldPosition != null) {
                notifyItemChanged(oldPosition);
            }
        }

        if (taskId != null) {
            Integer position = taskPositions.get(taskId);
            if (position != null) {
                notifyItemChanged(position);
            }
        }

        selectedTask = taskId;
    }

    public Task getSelectedTask() {
        if (selectedTask == null) {
            return null;
        }

        Integer position = taskPositions.get(selectedTask);
        if (position == null || position >= tasks.size())
            return null;
        return tasks.get(position);
    }

    public Task getTask(int position) {
        return tasks.get(position);
    }

    public void expandOrCollapseTask(int position) {
        Task task = getTask(position);
        if (expandedTasks.contains(task))
            expandedTasks.remove(task);
        else
            expandedTasks.add(task);
        notifyItemChanged(position);
    }
}
