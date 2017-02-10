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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import ru.ifmo.neerc.task.TaskActions;
import ru.ifmo.neerc.task.Task;

public class CreateTaskDialogFragment extends DialogFragment {

    public static class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

        private static final String[] TYPES = new String[] {
            TaskActions.TYPE_TODO,
            TaskActions.TYPE_TODOFAIL,
            TaskActions.TYPE_CONFIRM,
            TaskActions.TYPE_REASON,
            TaskActions.TYPE_QUESTION
        };

        private int selectedPosition = 0;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView iconView;

            public ViewHolder(View view) {
                super(view);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int newPosition = getAdapterPosition();
                        if (newPosition != selectedPosition) {
                            notifyItemChanged(selectedPosition);
                            selectedPosition = newPosition;
                            notifyItemChanged(selectedPosition);
                        }
                    }
                });

                iconView = (ImageView) view.findViewById(R.id.type_icon);
            }

            public void setType(String type) {
                iconView.setImageResource(getResourceForType(type));
            }

            public void setSelected(boolean selected) {
                itemView.setActivated(selected);
            }

            private int getResourceForType(String type) {
                switch (type) {
                    case TaskActions.TYPE_TODO:
                        return R.drawable.ic_play_circle_outline_white_24dp;
                    case TaskActions.TYPE_TODOFAIL:
                        return R.drawable.ic_alert_circle_outline_white_24dp;
                    case TaskActions.TYPE_CONFIRM:
                        return R.drawable.ic_checkbox_marked_circle_outline_white_24dp;
                    case TaskActions.TYPE_REASON:
                        return R.drawable.ic_information_outline_white_24dp;
                    case TaskActions.TYPE_QUESTION:
                        return R.drawable.ic_help_circle_outline_white_24dp;
                    default:
                        return android.R.color.transparent;
                }
            }
        }

        @Override
        public TypeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setType(TYPES[position]);
            holder.setSelected(position == selectedPosition);
        }

        @Override
        public int getItemCount() {
            return TYPES.length;
        }

        public String getSelectedType() {
            return TYPES[selectedPosition];
        }
    }

    private TextInputLayout titleWrapper;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.task_create, null);

        RecyclerView typeList = (RecyclerView) view.findViewById(R.id.type_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        typeList.setLayoutManager(layoutManager);

        final TypeAdapter adapter = new TypeAdapter();
        typeList.setAdapter(adapter);

        titleWrapper = (TextInputLayout) view.findViewById(R.id.titleWrapper);

        builder.setTitle(R.string.create_task_title)
            .setView(view)
            .setPositiveButton(R.string.create_task_create, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            })
            .setNegativeButton(R.string.all_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!validate())
                            return;

                        Task task = new Task(adapter.getSelectedType(), getTaskTitle());
                        ChatService.getInstance().sendTask(task);

                        dialog.dismiss();
                    }
                });
            }
        });

        return dialog;
    }

    private String getTaskTitle() {
        return titleWrapper.getEditText().getText().toString().trim();
    }

    private boolean validate() {
        if (getTaskTitle().isEmpty()) {
            titleWrapper.setError(getResources().getText(R.string.create_task_title_error));
            return false;
        } else {
            titleWrapper.setError(null);
        }

        return true;
    }
}
