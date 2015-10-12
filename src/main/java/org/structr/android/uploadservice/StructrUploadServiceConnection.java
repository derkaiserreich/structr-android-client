package org.structr.android.uploadservice;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;

/**
 *
 * This is the service connection that can be used to communicate with the {@link StructrUploadService}.
 * Use it when you bind your activity to the service.
 *
 * @author Lukas Reich
 */

public class StructrUploadServiceConnection implements ServiceConnection {

    private StructrUploadService service;
    private boolean isBound = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        StructrUploadServiceBinder binder = (StructrUploadServiceBinder) service;
        this.service = binder.getService();
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
    }

    /**
     * method that will cancel the upload with the given fileId
     *
     * @param fileId the id of the file that should not continue to upload
     */
    public void cancel(int fileId){
        if(isBound)
            service.cancelUpload(fileId);
    }

    /**
     * Gives you an ArrayList of all files that are in the uploadqueue of the service
     *
     * @return An ArrayList with all the files in the uploadqueue of the service, reprecented by
     * StructrUploadFile objects
     */
    public ArrayList<StructrUploadFile> getCurrentUploadList(){
        if(isBound)
            return service.getCurrentUploadList();
        else
            return null;
    }

    /**
     * Removes a file from the uploadqueue of the Service. If the specified file is currently being uploaded,
     * the upload will be canceled.
     *
     * @param fileId the id of the file that shoult be removed from the uploadqueue
     */
    public void dismissUpload(int fileId){
        if(isBound)
            service.dismissUpload(fileId);
    }

    public boolean serviceIsActive(){
        return service.isActive();
    }
}
