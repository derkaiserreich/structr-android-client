package org.structr.android.uploadservice;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.UUID;


/**
 * A service that uploads files to a structr server in the background.
 * To communicate with the service take a look at the {@link StructrUploadServiceConnection} and {@link StructrUploadStatusReceiver}.
 * <br><br>
 * To start the service, simply create an intent and put the all the STARTINTENT_EXTRAs in it. Then start the service with {@link #startService(Intent)}.
 *
 * @author Lukas Reich
 */
public class StructrUploadService extends IntentService{


    /**
     * One of the intent extras, that must be set in the intent that ist used to start this the service.
     * The intentextra value should be a valid URI. Please note that the service' build in path resolving is limited. When possible, resolve the
     * absolute path to your file fr yourself and use the {@link #STARTINTENT_EXTRA_ABSOLUTE_FILEPATH} instead.
     */
    public static final String STARTINTENT_EXTRA_URI = "org.structr.upload.fileuri";

    /**
     * Can be used instead of {@link #STARTINTENT_EXTRA_URI}.
     */
    public static final String STARTINTENT_EXTRA_ABSOLUTE_FILEPATH  = "org.structr.upload.absolutefilepath";

    /**
     * One of the intent extras, that must be set in the intent that ist used to start this the service.
     * The value of the intentextra must be the exact name of the activityclass, that should be called, when the service notification
     * ist clicked by the user. The best way the get the right name of the activity's class would be the getClass() +
     * getName() methods.
     */
    public static final String STARTINTENT_EXTRA_CALLING_ACTIVITY_NAME = "org.structr.upload.callingactivityname";

    /**
     * One of the intent extras, that must be set in the intent that ist used to start this the service.
     * The value of the intentextra should be the id of the drawable you want to see in the notification of the service.
     */
    public static final String STARTINTENT_EXTRA_NOTIFICATION_DRAWABLE = "org.structr.upload.notification.drawable";

    /**
     * One of the intent extras, that must be set in the intent that ist used to start this the service.
     * The value of the intentextra will be the Title of the Notification of the service.
     */
    public static final String STARTINTENT_EXTRA_NOTIFICATION_TITLE = "org.structr.upload.notification.title";

    /**
     * One of the intent extras, that must be set in the intent that ist used to start this the service.
     * The value of the intentextra will be the text underneath the ProgressBar of the Notification.
     * If you just want the text to display the files name that is currently being uploaded you can set the vaulue
     * to {@link #STRUCTRUPLOAD_NOTIFICATION_TEXT_USEFILENAME}
     */
    public static final String STARTINTENT_EXTRA_NOTIFICATION_TEXT = "org.structr.upload.notification.text";

    /**
     * An optional value for the {@link #STARTINTENT_EXTRA_NOTIFICATION_TEXT} intentextra. When set, the text of the service'
     * Notification will display the name of the file that is currently being uploaded.
     */
    public static final String STRUCTRUPLOAD_NOTIFICATION_TEXT_USEFILENAME = "org.structr.upload.notification.text.usefilename";

    /**
     * An optional value for the {@link #STARTINTENT_EXTRA_NOTIFICATION_TEXT} intentextra. When set, the text of the service'
     * Notification will display the progress in percent of the file that is currently being uploaded.
     */
    public static final String STRUCTRUPLOAD_NOTOFICATION_TEXT_USEPROGRESS = "org.structr.upload.notification.text.useprogress";

    public static final String BROADCAST_ACTION_STRUCTRUPLOAD_PROGRESS          = "org.structr.android.broadcast.uploadprogress";
    public static final String BROADCAST_ACTION_STRUCTRUPLOAD_FINISHED          = "org.structr.android.broadcast.uploadfinished";
    public static final String BROADCAST_ACTION_STRUCTRUPLOAD_CANCELED          = "org.structr.android.broadcast.uploadcanceled";
    public static final String BROADCAST_ACTION_STRUCTRUPLOAD_ERROR             = "org.structr.android.broadcast.uploaderror";
    public static final String BROADCAST_ACTION_STRUCTRUPLOAD_LISTUPDATE        = "org.structr.android.broadcast.listupdate";

    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID            = "org.structr.android.broadcast.fileid";
    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_LIST              = "org.structr.android.broadcast.uploadlist";
    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_PROGRESS          = "org.structr.android.broadcastextra.uploadprogress";
    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERCODE        = "org.structr.android.broadcastextra.uploadservercode";
    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERRESPONSE    = "org.structr.android.broadcastextra.uploadserverresponse";
    public static final String BROADCAST_EXTRAS_STRUCTRUPLOAD_ERRORTHROWABLE    = "org.structr.android.broadcastextra.uploaderrorthrowable";

    private static StructrFileUploader structrFileUploader = null;
    private final IBinder binder = new StructrUploadServiceBinder(this);

    private ArrayList<StructrUploadFile> uploadFileList = null;

    private NotificationCompat.Builder builder = null;
    private boolean useProgressAsNotificationText = false;


    public StructrUploadService(){
        super(StructrUploadService.class.getName());
    }

    @Override
    public void onCreate() {
        structrFileUploader = new StructrFileUploader(this);
        uploadFileList = new ArrayList<>();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        String[] pathAndName = new String[2];

        //create unique id for the StructrUploadFile
        int fileId = UUID.randomUUID().hashCode();
        intent.putExtra("fileId", fileId);

        String absolutePath = intent.getStringExtra(STARTINTENT_EXTRA_ABSOLUTE_FILEPATH);
        StructrUploadFile uploadFile = null;
        Uri fileToUploadUri = intent.getParcelableExtra(STARTINTENT_EXTRA_URI);

        if(absolutePath == null){
            //Get the real Path and the name of the file
            uploadFile = new StructrUploadFile(fileToUploadUri, fileId);
            pathAndName = FilePathResolver.getPathAndName(this, fileToUploadUri);
        }
        else{
            pathAndName[0] = absolutePath;
            pathAndName[1] = FilePathResolver.getName(absolutePath);
            fileToUploadUri = FilePathResolver.getUri(absolutePath);
            uploadFile = new StructrUploadFile(absolutePath, fileId);
            uploadFile.setUri(fileToUploadUri);
        }

        //Check if the absolute path could be found
        if(pathAndName[0] != null && pathAndName[1] != null){
            uploadFile.setRealPath(pathAndName[0]);
            uploadFile.setFileName(pathAndName[1]);
            uploadFile.setMimeType(getContentResolver().getType(fileToUploadUri));
            uploadFileList.add(uploadFile);
            broadcastList();
        }
        //If the file could not be found was not found broadcast an error
        else{
            broadcastError(-1, new FileNotFoundException("File could not be opened with the given Uri / Path"));
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //Get all information from the intent with what the service was started
        int notificationDrawable    = intent.getIntExtra(STARTINTENT_EXTRA_NOTIFICATION_DRAWABLE, 0);
        String notificationTitle    = intent.getStringExtra(STARTINTENT_EXTRA_NOTIFICATION_TITLE);
        String notificationText     = intent.getStringExtra(STARTINTENT_EXTRA_NOTIFICATION_TEXT);
        String callingActivityName  = intent.getStringExtra(STARTINTENT_EXTRA_CALLING_ACTIVITY_NAME);
        int fileId                  = intent.getIntExtra("fileId", 0);

        //Try to get an object of the activity that should be opened when the user clicks on the service' notification
        Class callbackActivity;
        try {
            callbackActivity = Class.forName(callingActivityName);
        } catch (ClassNotFoundException e) {
            callbackActivity = null;
            broadcastError(fileId, e);
        }

        if(callbackActivity != null) {
            Intent intentForNotification = new Intent(this, callbackActivity);
            PendingIntent pendingIntentFotNotification = PendingIntent.getActivity(this, 0, intentForNotification, PendingIntent.FLAG_UPDATE_CURRENT);

            if (uploadFileList != null) {
                //look for the StructrUploadFile that was added to the uploadFilelist in the onStartCommand method
                for (StructrUploadFile file : uploadFileList) {
                    if (file.getFileId() == fileId && !file.isCanceled()) {

                        if(notificationText.equals(STRUCTRUPLOAD_NOTIFICATION_TEXT_USEFILENAME)) {
                            notificationText = file.getFileName();
                            useProgressAsNotificationText = false;
                        }
                        else if(notificationText.equals(STRUCTRUPLOAD_NOTOFICATION_TEXT_USEPROGRESS)){
                            notificationText = file.getUploadProgress() + "%";
                            useProgressAsNotificationText = true;
                        }

                        //build the notification for the service so it can run in the foreground
                        builder = new NotificationCompat.Builder(this)
                                .setSmallIcon(notificationDrawable)
                                .setContentTitle(notificationTitle)
                                .setContentText(notificationText)
                                .setProgress(100, 0, false)
                                .setContentIntent(pendingIntentFotNotification);

                        startForeground(fileId, builder.build());

                        int tries = 0;
                        int delay = 1000;
                        while(tries < 3 && !file.isCanceled()){
                            tries++;
                            try{
                                structrFileUploader.doUpload(file);
                                break;
                            }catch(Throwable t){
                                if(tries >= 3 && !file.isCanceled()){
                                    broadcastError(file.getFileId(), t);
                                }
                                else if(file.isCanceled()){
                                    broadcastCanceled(file.getFileId());
                                }
                                else{
                                    SystemClock.sleep(delay);
                                    delay += tries * 2000;
                                }
                            }
                        }

                        stopForeground(true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * a method that will cancel the upload with the given fileId
     *
     * @param fileId the id of the file that should not continue to upload
     */
    public void cancelUpload(int fileId){
        if(uploadFileList != null) {
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setCanceled(true);
                    broadcastCanceled(fileId);
                    break;
                }
            }
        }
    }

    /**
     * Gives you an ArrayList of all files that are in the uploadqueue of the service
     *
     * @return An ArrayList with all the files in the uploadqueue of the service, reprecented by
     * StructrUploadFile objects
     */
    public ArrayList<StructrUploadFile> getCurrentUploadList(){
             return uploadFileList;
    }

    /**
     * Removes a file from the uploadqueue of the Service. If the specified file is currently being uploaded,
     * the upload will be canceled.
     *
     * @param fileId the id of the file that shoult be removed from the uploadqueue
     */
    public void dismissUpload(int fileId){
        if(uploadFileList != null){
            for (StructrUploadFile file : uploadFileList) {
                if (file.getFileId() == fileId) {
                    file.setCanceled(true);
                    uploadFileList.remove(file);
                    break;
                }
            }
        }
    }

    public void broadcastProgress(int fileId, int progress) {
        builder.setProgress(100, progress, false);
        if(useProgressAsNotificationText)
            builder.setContentText(progress + "%");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(fileId, builder.build());

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_STRUCTRUPLOAD_PROGRESS);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, fileId);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_PROGRESS, progress);
        sendBroadcast(broadcastIntent);
    }

    public void broadcastCanceled(int fileId){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_STRUCTRUPLOAD_CANCELED);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, fileId);
        sendBroadcast(broadcastIntent);
    }

    public void broadcastFinished(int fileId, int serverCode, String serverResponse){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_STRUCTRUPLOAD_FINISHED);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, fileId);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERCODE, serverCode);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_SERVERRESPONSE, serverResponse);
        sendBroadcast(broadcastIntent);
    }

    public void broadcastError(int fileId, Throwable e){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_STRUCTRUPLOAD_ERROR);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_FILEID, fileId);
        broadcastIntent.putExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_ERRORTHROWABLE, e);
        sendBroadcast(broadcastIntent);
    }

    public void broadcastList(){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_STRUCTRUPLOAD_LISTUPDATE);
        broadcastIntent.putParcelableArrayListExtra(BROADCAST_EXTRAS_STRUCTRUPLOAD_LIST, uploadFileList);
        sendBroadcast(broadcastIntent);
    }
}
