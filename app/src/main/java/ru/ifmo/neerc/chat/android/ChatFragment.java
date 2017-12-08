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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.util.Log;

import com.bumptech.glide.Glide;

import ru.ifmo.neerc.chat.ChatMessage;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    private RecyclerView chatList;
    private RecyclerView priorityChatList;
    private EditText messageInput;
    private Button sendButton;

    private RelativeLayout photoPanel;
    private ImageView photoView;
    private ProgressBar photoProgressBar;
    private ImageButton photoButton;
    private ImageButton removePhotoButton;

    private ChatAdapter adapter;
    private ChatAdapter priorityAdapter;
    private BroadcastReceiver messageReceiver;

    private File attachedPhotoFile;
    private UploadFileTask uploadFileTask;
    private String attachedPhotoURL;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chatList = (RecyclerView) getActivity().findViewById(R.id.chat_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        chatList.setLayoutManager(layoutManager);

        adapter = new ChatAdapter();
        chatList.setAdapter(adapter);

        priorityChatList = (RecyclerView) getActivity().findViewById(R.id.priority_chat_list);

        LinearLayoutManager priorityLayoutManager = new LinearLayoutManager(getActivity());
        priorityLayoutManager.setStackFromEnd(true);
        priorityChatList.setLayoutManager(priorityLayoutManager);

        priorityAdapter = new ChatAdapter(true);
        priorityChatList.setAdapter(priorityAdapter);

        photoView = (ImageView) getActivity().findViewById(R.id.photo_view);
        photoProgressBar = (ProgressBar) getActivity().findViewById(R.id.photo_progress_bar);

        messageInput = (EditText) getActivity().findViewById(R.id.message_input);
        sendButton = (Button) getActivity().findViewById(R.id.send_button);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageInput.getText().toString();
                if (message.trim().isEmpty())
                    return;
                ChatService.getInstance().sendMessage(message, attachedPhotoURL);
                messageInput.getText().clear();
                if (attachedPhotoURL != null) {
                    removePhoto();
                }
            }
        });

        photoPanel = (RelativeLayout) getActivity().findViewById(R.id.photo_panel);

        photoButton = (ImageButton) getActivity().findViewById(R.id.photo_button);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachPhoto();
            }
        });

        removePhotoButton = (ImageButton) getActivity().findViewById(R.id.remove_photo_button);
        removePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removePhoto();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ChatMessage message = (ChatMessage) intent.getSerializableExtra("message");
                updateMessages(message);
            }
        };

        getContext().registerReceiver(messageReceiver, new IntentFilter(ChatService.MESSAGE));

        updateMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        ChatService.getInstance().clearImportantMessages();
    }

    @Override
    public void onStop() {
        super.onStop();

        getContext().unregisterReceiver(messageReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            Glide.with(getActivity())
                 .load(attachedPhotoFile)
                 .into(photoView);
            photoProgressBar.setIndeterminate(true);
            photoProgressBar.setVisibility(View.VISIBLE);
            photoPanel.setVisibility(View.VISIBLE);

            if (uploadFileTask != null) {
                uploadFileTask.cancel(true);
            }

            attachedPhotoURL = null;

            uploadFileTask = new UploadFileTask(this);
            uploadFileTask.execute(attachedPhotoFile);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult, requestCode = " + requestCode);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attachPhoto();
            }
        }
    }

    public void updateMessages(ChatMessage message) {
        adapter.update(message);
        priorityAdapter.update(message);
        chatList.scrollToPosition(adapter.getItemCount() - 1);
        priorityChatList.scrollToPosition(priorityAdapter.getItemCount() - 1);
    }

    public void setPrivateAddress(String username) {
        String message = messageInput.getText().toString();
        message = message.replaceAll("\\A[a-zA-Z0-9%]+>\\s*", "");
        message = username + "> " + message;
        messageInput.setText(message);
        messageInput.setSelection(message.length());
        messageInput.requestFocus();
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFilename = "chat_" + timestamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "chat");
        storageDir.mkdirs();
        File image = File.createTempFile(
            imageFilename,
            ".jpg",
            storageDir
        );

        return image;
    }

    private void attachPhoto() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (photoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(TAG, "Failed to create image file", e);
            }

            if (photoFile != null) {
                attachedPhotoFile = photoFile;
                Uri photoURI = FileProvider.getUriForFile(
                    getActivity(),
                    "ru.ifmo.neerc.chat.android.fileprovider",
                    photoFile
                );
                Log.d(TAG, "photoURI = " + photoURI);
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(photoIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getActivity().grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivityForResult(photoIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void removePhoto() {
        if (uploadFileTask != null) {
            uploadFileTask.cancel(true);
        }

        photoPanel.setVisibility(View.GONE);
        Glide.with(getActivity())
             .clear(photoView);
        attachedPhotoFile = null;
        attachedPhotoURL = null;
    }

    public void setAttachedPhotoURL(String url) {
        attachedPhotoURL = url;

        if (attachedPhotoURL != null) {
            photoProgressBar.setVisibility(View.GONE);
        } else {
            removePhoto();
            Toast toast = Toast.makeText(getActivity(), getString(R.string.toast_photo_upload_failed), Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
