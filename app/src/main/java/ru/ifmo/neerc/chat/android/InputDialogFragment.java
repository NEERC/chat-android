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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;

public class InputDialogFragment extends DialogFragment {
    
    private DialogInterface.OnClickListener listener;
    private String text;
    private EditText inputEdit;

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        this.listener = listener;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        if (inputEdit == null)
            return null;
        return inputEdit.getText().toString();
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.input_dialog, null);

        inputEdit = (EditText) view.findViewById(R.id.input);
        inputEdit.setText(text);

        builder.setView(view)
               .setTitle(R.string.input_title)
               .setPositiveButton(R.string.all_ok, listener)
               .setNegativeButton(R.string.all_cancel, listener);

        return builder.create();
    }
}
