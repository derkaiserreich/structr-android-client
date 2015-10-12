package org.structr.android.uploadservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;

/**
 * A broadcastreceiver that implements the callback methods of the StructrUploadService.
 *
 * @author Lukas Reich
 */
public abstract class StructrUploadStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_PROGRESS:
                int progress = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_PROGRESS, 0);
                int fileId = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, 0);
                onProgress(fileId, progress);
                break;

            case StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_FINISHED:
                fileId = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, 0);
                int serverCode = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERCODE, 400);
                String serverResponse = intent.getStringExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERRESPONSE);
                onFinished(fileId, serverCode, serverResponse);
                break;
            
            case StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_CANCELED:
                fileId = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, 0);
                onCanceled(fileId);
                break;

            case StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_ERROR:
                Throwable t = (Throwable) intent.getSerializableExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_ERRORTHROWABLE);
                fileId = intent.getIntExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, 0);
                onError(fileId, t);
                break;

            case StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_LISTUPDATE:
                ArrayList<StructrUploadFile> uploadList = intent.getParcelableArrayListExtra(StructrUploadService.BROADCAST_EXTRAS_STRUCTRUPLOAD_LIST);
                onNewUploadAdded(uploadList);
                break;
        }
    }

    /**
     * Register the activity as a StructrUploadStatusReceiver
     *
     * @param context Should be the Activity, that does implement the StructrUploadStatusReceiver
     */
    public void register (final Context context){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_PROGRESS);
        filter.addAction(StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_FINISHED);
        filter.addAction(StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_CANCELED);
        filter.addAction(StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_ERROR);
        filter.addAction(StructrUploadService.BROADCAST_ACTION_STRUCTRUPLOAD_LISTUPDATE);
        context.registerReceiver(this, filter);
    }

    /**
     * Unregister the activity as a StructrUploadStatusReceiver
     *
     * @param context Should be the Activity, that does implement the StructrUploadStatusReceiver
     */
    public void unregister(final Context context){
        context.unregisterReceiver(this);
    };

    /**
     * This method will be called periodically to publish the progress of the upload
     *
     * @param fileId id of the file, that is being uploaded
     * @param progress progress of the doUpload in percent
     */
    public abstract void onProgress(int fileId, int progress);

    /**
     * This method will be called, when the upload of the file is finished.
     * Check the Response Code and Message to determine wether the upload was successful or not
     *
     * @param fileId
     */
    public abstract void onFinished(int fileId, int serverCode, String serverResponse);

    /**
     * This method will be called when the upload is canceled by the user
     */
    public abstract void onCanceled(int fileId);

    /**
     * This method will be called when an error occured while uploading the file
     *
     * @param fileId the id of the file that is currently being uploaded or -1 when the error occured
     *               on the start of the service.
     * @param t The Throwable that caused the error
     */
    public abstract void onError(int fileId, Throwable t);

    /**
     * Every time the service is being started with the startService() method, the service will add a
     * StructrUploadFile to its uploadList and will broadcast the updated List. The calling activity can implement
     * all functions for the upload handling. You can use the {@link StructrUploadFileListAdapter} when you don't want to implement
     * your own handling. See {@link StructrUploadServiceConnection} for more information
     * about the communication with the StructrUploadService.
     * @param files
     */
    public abstract void onNewUploadAdded(ArrayList<StructrUploadFile> files);

}
