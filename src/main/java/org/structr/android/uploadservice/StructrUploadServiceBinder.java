package org.structr.android.uploadservice;

import android.os.Binder;

/**
 * The binder class for the {@link StructrUploadService}
 * @author Lukas Reich
 */
public class StructrUploadServiceBinder extends Binder {

    private StructrUploadService service;

    public StructrUploadServiceBinder(StructrUploadService service){
        this.service = service;
    }

    public StructrUploadService getService(){
        return service;
    }
}
