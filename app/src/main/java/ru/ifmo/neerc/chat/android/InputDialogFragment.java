package ru.ifmo.neerc.chat.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.input_dialog, null);

        inputEdit = (EditText) view.findViewById(R.id.input);
        inputEdit.setText(text);

        builder.setView(view)
               .setTitle("Message")
               .setPositiveButton("OK", listener)
               .setNegativeButton("Cancel", listener);

        return builder.create();
    }
}
