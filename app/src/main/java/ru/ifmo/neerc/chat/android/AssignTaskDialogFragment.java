package ru.ifmo.neerc.chat.android;

import java.util.HashSet;
import java.util.Set;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import ru.ifmo.neerc.chat.user.UserEntry;
import ru.ifmo.neerc.chat.user.UserRegistry;
import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskActions;
import ru.ifmo.neerc.task.TaskStatus;
import ru.ifmo.neerc.task.TaskRegistry;

public class AssignTaskDialogFragment extends DialogFragment {

    private RosterAdapter usersAdapter;

    private String taskId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        taskId = getArguments().getString("taskId");

        Task task = TaskRegistry.getInstance().getById(taskId);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.task_assign, null);

        TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(task.getTitle());

        RecyclerView userList = (RecyclerView) view.findViewById(R.id.user_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        userList.setLayoutManager(layoutManager);

        usersAdapter = new RosterAdapter();
        usersAdapter.setSelectable(true);

        UserRegistry userRegistry = UserRegistry.getInstance();
        Set<UserEntry> users = new HashSet<UserEntry>();
        for (String userName : task.getStatuses().keySet()) {
            users.add(userRegistry.findOrRegister(userName));
        }

        usersAdapter.setSelectedUsers(users);

        userList.setAdapter(usersAdapter);

        builder.setTitle("Assign task")
            .setView(view)
            .setPositiveButton("Assign", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    assign();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        usersAdapter.update();
    }

    private void assign() {
        Task task = TaskRegistry.getInstance().getById(taskId);
        if (task == null)
            return;

        Set<UserEntry> users = usersAdapter.getSelectedUsers();
        Task updatedTask = new Task(task.getId(), task.getType(), task.getTitle());
        for (UserEntry user : users) {
            TaskStatus status = task.getStatus(user.getName());
            if (status != null) {
                updatedTask.setStatus(user.getName(), status.getType(), status.getValue());
            } else {
                updatedTask.setStatus(user.getName(), TaskActions.STATUS_NEW, "");
            }
        }

        ChatService.getInstance().sendTask(updatedTask);
    }
}