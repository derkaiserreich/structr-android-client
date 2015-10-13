structr Android Client
======================

The classes in this repository enable your Android App to connect to a REST web service built with structr. You can use structr-core and structr-rest to build a domain-specific REST API. structr is backed by the Neo4j graph database.

## Features
- Asynchronous connectors to keep the user interface responsive while loading data in the background
- SSL support
- Fully automatic serialization and deserialization with GSON
- Use POJOs in your android code
- Use @Expose annotation to map POJO fields to structr REST output
- Backgroundupload of file(s) to a structr backend with automatic and customizable notification

Please note that this software is early alpha status. Use carefully at your own risk.

## Integrate structr-android-client in your Android app
- Clone this repository
- Add these two lines in your root gradle file:
    - include ':structr-android-client'
    - project (':structr-android-client').projectDir=new File('path/to/the/repository')

# Restclient

#### Step 1 :
Call StructrConnector.initialize() in the onCreate() method of your main activity

#### Step 2: Map your structr entities to POJOs
    public class MyEntitiy extends StructrObject {
        @Expose private String name;
        @Expose private Date timestamp;
        @Expose private String location;
    }

#### Step 3: Use one of the various connectors to access the REST server
    new IdEntityLoader(new EntityHandler() {
    
        public void handleProgress(Progress... progress) {
            // handle progress / exception
        }
        
        public void handleResults(StructrObject result) {
            // handle result
        }
        
    }).execute(MyEntity.class, id");


# Upload Service

#### Step 1 :
Call StructrConnector.initialize() in the onCreate() method of your main activity

#### Step 2: Use the StructrServiceStatusReceiver to handle the events of the service
 
    private StructrUploadStatusReceiver receiver = new StructrUploadStatusReceiver() {
        @Override
        public void onProgress(int fileId, int progress) {
            // handle progress of StructrUploadFile with fileId
        }

        @Override
        public void onFinished(int fileId, int serverCode, String serverResponse) {
            // handle the serverCode / serverRespone of the StructrUploadFile with FileId
        }

        @Override
        public void onCanceled(int fileId) {
            // handle upload cancellation of StructrUploadFile with FileId
        }

        @Override
        public void onError(int fileId, Throwable t) {
            // handle the the error of the StructrUploadFile with FileId
        }

        @Override
        public void onNewUploadAdded(ArrayList<StructrUploadFile> files) {
            // handle the list of StructrUploadFiles from the service
        }
    }
    
#### Step 3: Add a StructrUploadServiceConnection in your activity and bind the service in the onStart() method

     @Override
    protected void onStart() {
        Intent intent = new Intent(this, StructrUploadService.class);
        bindService(intent, structrUploadServiceConnection, Context.BIND_AUTO_CREATE);
        super.onStart();
    }
    
unbind it in the onStop() method:
    
    @Override
    protected void onStop() {
        unbindService(uploadServiceConnection);
        super.onStop();
    }

#### Step 4: Register the activity in the StructrUploadStatusReceiver in the onResume() method of your activity.
    
    @Override
    protected void onResume() {
        receiver.register(this);
        super.onResume();
    }
    
unregister it in the onPause() Method:

     @Override
    protected void onPause() {
        receiver.unregister(this);
        super.onPause();
    }
    
#### Step 5: Prepare an intent for the service and start it with the startService(Intent intent) method

    Intent intent = new Intent(this, StructrUploadService.class);
    intent.putExtra(StructrUploadService.STARTINTENT_EXTRA_CALLING_ACTIVITY_NAME, this.getClass().getName());
    intent.putExtra(StructrUploadService.STARTINTENT_EXTRA_NOTIFICATION_TITLE, "Title of the Notification")
    intent.putExtra(StructrUploadService.STARTINTENT_EXTRA_NOTIFICATION_DRAWABLE, R.drawable.Logo_Of_The_Notification);
    intent.putExtra(StructrUploadService.STARTINTENT_EXTRA_NOTIFICATION_TEXT, StructrUploadService.STRUCTRUPLOAD_NOTOFICATION_TEXT_USEPROGRESS);
    intent.putExtra(StructrUploadService.STARTINTENT_EXTRA_URI, UriOfTheFile);

    startService(intent);
    
    
    

    



