package ru.ifmo.neerc.chat.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.support.v7.widget.LinearLayoutManager;
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

    private static int TYPE_HEADER = 0;
    private static int TYPE_TASK = 1;
    private static int TYPE_DETAILS = 2;

    private final List<Task> tasks = new ArrayList<Task>();
    private final List<String> users = new ArrayList<String>();

    private int selectedPosition = 0;
    private boolean expanded = false;

    private View.OnClickListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private RecyclerView statusList;
        private TaskStatusesAdapter adapter;

        private RecyclerView userList;
        private TaskUsersAdapter usersAdapter;

        public ViewHolder(View view, int viewType) {
            super(view);

            if (viewType == TYPE_HEADER) {
                userList = (RecyclerView) view.findViewById(R.id.user_list);

                LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false);
                layoutManager.setStackFromEnd(true);
                userList.setLayoutManager(layoutManager);

                usersAdapter = new TaskUsersAdapter();
                userList.setAdapter(usersAdapter);
            } else {
                view.setOnClickListener(listener);
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Log.d("TasksAdapter", "Long click on " + view);
                        if (selectedPosition > 0)
                            notifyItemChanged(selectedPosition);
                        int position = getAdapterPosition();
                        if (expanded && position == selectedPosition) {
                            selectedPosition = 0;
                            expanded = false;
                        } else {
                            selectedPosition = position;
                            expanded = true;
                            notifyItemChanged(position);
                        }
                        return true;
                    }
                });

                titleView = (TextView) view.findViewById(R.id.title);

                statusList = (RecyclerView) view.findViewById(R.id.status_list);
                statusList.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("TasksAdapter", "Clicked on " + view);
                    }
                });

                LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext(), (viewType == TYPE_TASK) ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL, false);
                layoutManager.setStackFromEnd(true);
                statusList.setLayoutManager(layoutManager);

                adapter = new TaskStatusesAdapter(viewType == TYPE_DETAILS);
                statusList.setAdapter(adapter);
            }
        }

        public void setTitle(String title) {
            titleView.setText(title);
        }

        public void setStatuses(List<String> users, List<TaskStatus> statuses) {
            adapter.setUsers(users);
            adapter.setStatuses(statuses);
        }

        public void setUsers(List<String> users) {
            usersAdapter.setUsers(users);
        }
    }

    public TasksAdapter(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        else if (expanded && position == selectedPosition)
            return TYPE_DETAILS;

        return TYPE_TASK;
    }

    @Override
    public TasksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = R.layout.task;
        if (viewType == TYPE_HEADER)
            layout = R.layout.tasks_header;
        else if (viewType == TYPE_DETAILS)
            layout = R.layout.task_details;

        View view = LayoutInflater.from(parent.getContext())
            .inflate(layout, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_HEADER) {
            holder.setUsers(users);
        } else {
            Task task = tasks.get(position - 1);
            List<TaskStatus> statuses = new ArrayList<TaskStatus>();
            for (String user : users) {
                statuses.add(task.getStatus(user));
            }
            holder.itemView.setActivated(position == selectedPosition);
            holder.setTitle(task.getTitle());
            holder.setStatuses(users, statuses);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size() + 1;
    }

    public int getUserCount() {
        return users.size();
    }

    public void update() {
        tasks.clear();
        tasks.addAll(TaskRegistry.getInstance().getTasks());
        Set<String> usersSet = new TreeSet<String>();
        for (Task task : tasks) {
            usersSet.addAll(task.getStatuses().keySet());
        }
        users.clear();
        users.addAll(usersSet);
        notifyDataSetChanged();
    }

    public void setSelectedTask(int position) {
        if (selectedPosition > 0)
            notifyItemChanged(selectedPosition);
        selectedPosition = position;
        expanded = false;
        if (position > 0)
            notifyItemChanged(position);
    }

    public Task getSelectedTask() {
        int index = selectedPosition - 1;
        if (index < 0 || index >= tasks.size())
            return null;
        return tasks.get(index);
    }

    public Task getTask(int position) {
        return tasks.get(position);
    }
}
