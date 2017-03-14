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

import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
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

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        taskId = getArguments().getString("taskId");

        Task task = TaskRegistry.getInstanceFor(ChatService.getInstance().getRoom()).getById(taskId);

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

        UserRegistry userRegistry = UserRegistry.getInstanceFor(ChatService.getInstance().getRoom());
        Set<UserEntry> users = new HashSet<UserEntry>();
        for (String userName : task.getStatuses().keySet()) {
            users.add(userRegistry.findOrRegister(userName));
        }

        usersAdapter.setSelectedUsers(users);

        userList.setAdapter(usersAdapter);

        builder.setTitle(R.string.assign_task_title)
            .setView(view)
            .setPositiveButton(R.string.assign_task_assign, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    assign();
                }
            })
            .setNegativeButton(R.string.all_cancel, new DialogInterface.OnClickListener() {
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
        Task task = TaskRegistry.getInstanceFor(ChatService.getInstance().getRoom()).getById(taskId);
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
