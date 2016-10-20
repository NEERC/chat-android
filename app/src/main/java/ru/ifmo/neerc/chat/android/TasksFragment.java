package ru.ifmo.neerc.chat.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log; 
import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskActions;
import ru.ifmo.neerc.task.TaskStatus;

public class TasksFragment extends Fragment {

    private RecyclerView taskList;
    private LinearLayoutManager layoutManager;

    private TasksAdapter adapter;
    private BroadcastReceiver tasksReceiver;

    private ActionMode actionMode = null;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.task, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Task task = adapter.getSelectedTask();
            String user = ChatService.getInstance().getUser();
            
            menu.findItem(R.id.started)
                .setVisible(TaskActions.isActionSupported(task, user, TaskActions.ACTION_START));
            menu.findItem(R.id.done)
                .setVisible(TaskActions.isActionSupported(task, user, TaskActions.ACTION_DONE));
            menu.findItem(R.id.failed)
                .setVisible(TaskActions.isActionSupported(task, user, TaskActions.ACTION_FAIL));

            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            Task task = adapter.getSelectedTask();
            String user = ChatService.getInstance().getUser();

            final int action;

            switch (item.getItemId()) {
                case R.id.started:
                    action = TaskActions.ACTION_START;
                    break;
                case R.id.done:
                    action = TaskActions.ACTION_DONE;
                    break;
                case R.id.failed:
                    action = TaskActions.ACTION_FAIL;
                    break;
                default:
                    return false;
            }

            if (action == TaskActions.ACTION_FAIL || task.getType().equals(TaskActions.TYPE_QUESTION)) {
                final InputDialogFragment inputDialog = new InputDialogFragment();
                inputDialog.setText(task.getStatuses().get(user).getValue());
                inputDialog.setOnClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                performAction(action, inputDialog.getText());
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                });
                inputDialog.show(getActivity().getSupportFragmentManager(), null);
                return true;
            }

            performAction(action, "");
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setSelectedTask(0);
        }
    };

    public void performAction(int action, String value) {
        Task task = adapter.getSelectedTask();
        String user = ChatService.getInstance().getUser();

        if (!TaskActions.isActionSupported(task, user, action))
            return;

        String type = TaskActions.getNewStatus(task, user, action);
        ChatService.getInstance().sendStatus(task, new TaskStatus(type, value));
        actionMode.finish();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tasks, container, false);

        taskList = (RecyclerView) view.findViewById(R.id.task_list);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        layoutManager = new LinearLayoutManager(getActivity());
        taskList.setLayoutManager(layoutManager);

        adapter = new TasksAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = taskList.getChildAdapterPosition(view);
                adapter.setSelectedTask(position);

                if (actionMode == null)
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                else
                    actionMode.invalidate();
            }
        });
        taskList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        tasksReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTasks();
            }
        };

        getContext().registerReceiver(tasksReceiver, new IntentFilter(ChatService.TASK));

        updateTasks();
    }

    @Override
    public void onStop() {
        super.onStop();

        getContext().unregisterReceiver(tasksReceiver);
    }

    public void updateTasks() {
        adapter.update();
    }

    public void onTabUnselected() {
        if (actionMode != null)
            actionMode.finish();
    }
}
