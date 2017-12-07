package ru.ifmo.neerc.chat.android;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;

public class UploadFileTask extends AsyncTask<File, Integer, String> {

    private static final String TAG = "UploadFileTask";

    private ChatFragment chatFragment;

    public UploadFileTask(ChatFragment chatFragment) {
        this.chatFragment = chatFragment;
    }

    @Override
    protected String doInBackground(File... files) {
        if (files.length <= 0 || files[0] == null) {
            return null;
        }

        HttpFileUploadManager httpFileUploadManager = HttpFileUploadManager.getInstanceFor(ChatService.getInstance().getConnection());

        Log.d(TAG, "Uploading file...");

        try {
            URL url = httpFileUploadManager.uploadFile(files[0]);
            Log.d(TAG, "File uploaded at " + url);
            return url.toString();
        } catch (SmackException | XMPPException | IOException e) {
            Log.e(TAG, "File upload failed", e);
            return null;
        } catch (InterruptedException e) {
            Log.d(TAG, "File upload cancelled", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String url) {
        chatFragment.setAttachedPhotoURL(url);
    }
}
