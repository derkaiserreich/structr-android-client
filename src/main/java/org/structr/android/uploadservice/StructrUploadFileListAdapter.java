package org.structr.android.uploadservice;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * <p>Use this adapter for when you want to use the {@link StructrUploadService} and its upload queue.
 * You can define your own layout with the {@link #getViewForItem(StructrUploadFile, View, ViewGroup)} method. </p>
 *
 * <p>Take a look at {@link StructrUploadFile} to see, what information you can use for the View.</p>
 *
 * <p>Make shure you're calling the matching methods in your {@link StructrUploadStatusReceiver} methods </p>
 *
 * @author Lukas Reich
 */
public abstract class StructrUploadFileListAdapter extends BaseAdapter {
    private ListView listView = null;
    private ArrayList<StructrUploadFile> uploadFileList = null;
    protected StructrUploadServiceConnection uploadServiceConnection;

    /**
     *
     * @param listView the listView object you want to fill with a uploadFileList of {@link StructrUploadFile}s
     * @param connection the {@link StructrUploadServiceConnection} you got from the binding to the {@link StructrUploadService}
     */
    public StructrUploadFileListAdapter(ListView listView, StructrUploadServiceConnection connection){
        this.listView = listView;
        this.uploadServiceConnection = connection;
        listView.setAdapter(this);
    }

    /**
     * use this method to define your own view for every StructrUploadFile in your ListView container.
     *
     * @param item the StructrUploadFile for the view.
     * @param view the view object that maybe can be recycled for a new View.
     * @param vg the viewgroup.
     * @return  the custom, new view for the StructrUploadFile.
     */
    public abstract View getViewForItem(final StructrUploadFile item, final View view, final ViewGroup vg);

    /**
     *
     *
     * @param list the uploadFileList you want to display in your ListView.
     */
    public void handleResults(ArrayList<StructrUploadFile> list) {
        if(list != null) {
            this.uploadFileList = list;
            notifyDataSetChanged();
            listView.invalidateViews();
        }
    }

    /**
     * Updates the fileObject will the current progress of the upload. Call this method in the onProgress
     * method of your StructrUploadStatusReceiver.
     * @param fileId The id of the file, that is currently being uploaded.
     * @param progress  the progress of the upload in percent.
     */
    public void onProgress(int fileId, int progress){
        if(uploadFileList == null) {
            fetchCurrentUploads();
        }
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setUploadProgress(progress);
                    break;
                }
            }
        }
        this.notifyDataSetChanged();
        listView.invalidateViews();
    }

    /**
     * this will set the canceled flag of the file that was canceled. Call this method in the onCancel
     * method of your StructrUploadStatusReceiver.
     * @param fileId the id of the file whose upload was canceled.
     */
    public void onCancel(int fileId){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setCanceled(true);
                    this.notifyDataSetChanged();
                    listView.invalidateViews();
                    break;
                }
            }
        }
    }

    /**
     * this will set the finishedflag, the serverresponse code and the serverresponse body
     * of the file that was uploaded. Call this method in the onFinished method of your
     * StructrUploadStatusReceiver.
     *
     * @param fileId the id of the file, that was uploaded
     * @param serverCode the http statuscode of the upload.
     * @param serverResponse the http response message of the upload.
     */
    public void onFinished(int fileId, int serverCode, String serverResponse){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setFinished(true);
                    file.setServerCode(serverCode);
                    file.setServerResponse(serverResponse);
                    this.notifyDataSetChanged();
                    listView.invalidateViews();
                    break;
                }
            }
        }
    }

    /**
     * this will set the throwable of the file whose upload was ended with an error. Call this method
     * in the onError method of your StructrUploadStatusReceiver. Check the Throwable to see want went wrong.
     *
     * @param fileId the id of the file whose upload was finished with an error.
     * @param t the throwable that caused the error.
     */
    public void onError(int fileId, Throwable t){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setThrowable(t);
                    this.notifyDataSetChanged();
                    listView.invalidateViews();
                    break;
                }
            }
        }
    }

    protected void fetchCurrentUploads(){
            uploadFileList = uploadServiceConnection.getCurrentUploadList();
    }

    /**
     * if you want to get an item of the List of this adapter
     * @param fileId the id of the file you want to get
     * @return the file you wanted (if it exists)
     */
    public StructrUploadFile getListItem(int fileId){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * removes an item from the uploadFileList of the adapter and refreshes the ListView.
     * @param fileId the id of the file you want to remove.
     */
    public void removeListItem(int fileId){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    uploadServiceConnection.dismissUpload(fileId);
                    uploadFileList = uploadServiceConnection.getCurrentUploadList();
                    notifyDataSetChanged();
                    listView.invalidateViews();
                    break;
                }
            }
        }
    }


    /**
     * Gets you the uploadFileList of this adapter. Please note that the uploadFileList could be null.
     * @return the uploadFileList of this adapter, or null
     */
    public ArrayList<StructrUploadFile> getUploadFileList(){
        return this.uploadFileList;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        if(uploadFileList != null)
            return uploadFileList.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if(uploadFileList != null)
            return uploadFileList.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (uploadFileList != null) {
            return uploadFileList.get(position).hashCode();
        }
        return -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewForItem(uploadFileList.get(position), convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return uploadFileList != null && uploadFileList.isEmpty();
    }

    public void clear() {
        if (uploadFileList != null) {
            uploadFileList.clear();
        }
    }
}
