package org.structr.android.uploadservice;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a file that is being uploaded with the StructrUploadService. It encapsulates
 * all necessary data and information for the upload itself and the ui.
 *
 * @author Lukas Reich
 */

public class StructrUploadFile implements Parcelable {

    private boolean     canceled;
    private boolean     finished;
    private Uri         uri;
    private String      mimeType;
    private String      realPath;
    private int         fileId;
    private int         uploadProgress;
    private int         serverCode;
    private String      serverResponse;
    private String      fileName;
    private Throwable   throwable;

    public StructrUploadFile(Uri uri, int fileId){
        this.canceled = false;
        this.finished = false;
        this.uri = uri;
        this.mimeType = null;
        this.realPath = null;
        this.fileId = fileId;
        this.uploadProgress = 0;
        this.serverCode = 400;
        this.serverResponse = null;
        this.fileName = null;
        this.throwable = null;
    }

    public StructrUploadFile(String realPath, int fileId){
        this.realPath = realPath;
        this.fileId = fileId;
    }

    protected StructrUploadFile(Parcel in) {
        this.canceled = in.readByte() != 0;
        this.finished = in.readByte() != 0;
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.mimeType = in.readString();
        this.realPath = in.readString();
        this.fileId = in.readInt();
        this.uploadProgress = in.readInt();
        this.serverCode = in.readInt();
        this.serverResponse = in.readString();
        this.fileName = in.readString();
        this.throwable = (Throwable) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (this.canceled ? 1 : 0));
        dest.writeByte((byte) (this.finished ? 1 : 0));
        dest.writeParcelable(this.uri, flags);
        dest.writeString(this.mimeType);
        dest.writeString(this.realPath);
        dest.writeInt(this.fileId);
        dest.writeInt(this.uploadProgress);
        dest.writeInt(this.serverCode);
        dest.writeString(this.serverResponse);
        dest.writeString(this.fileName);
        dest.writeSerializable(this.throwable);
    }

    public static final Creator<StructrUploadFile> CREATOR = new Creator<StructrUploadFile>() {
        @Override
        public StructrUploadFile createFromParcel(Parcel in) {
            return new StructrUploadFile(in);
        }

        @Override
        public StructrUploadFile[] newArray(int size) {
            return new StructrUploadFile[size];
        }
    };


    public boolean isCanceled(){
        return canceled;
    }

    public boolean isFinished(){
        return this.finished;
    }

    public Uri getUri(){
        return this.uri;
    }

    public String getMimeType(){return this.mimeType;}

    public String getRealPath(){
        return this.realPath;
    }

    public int getFileId(){
        return this.fileId;
    }

    public int getUploadProgress(){
        return this.uploadProgress;
    }

    public int getServerCode(){
        return this.serverCode;
    }

    public String getServerResponse(){
        return this.serverResponse;
    }

    public String getFileName(){
        return this.fileName;
    }

    public Throwable getThrowable(){
        return this.throwable;
    }


    public void setUploadProgress(int progress){this.uploadProgress = progress;}

    public void setUri(Uri uri){this.uri = uri;}

    public void setFileName(String fileName){this.fileName = fileName;}

    public void setMimeType(String mimeType){this.mimeType = mimeType;}

    public void setServerResponse(String response){this.serverResponse = response;}

    public void setServerCode(int code){this.serverCode = code;}

    public void setCanceled(boolean status){this.canceled = status;}

    public void setFinished(boolean status){this.finished = status;}

    public void setThrowable(Throwable t){this.throwable = t;}

    public void setRealPath(String realPath){this.realPath = realPath;}
}
