package org.structr.android.uploadservice;

import android.os.Build;

import org.structr.android.restclient.StructrConnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Lukas Reich
 */
public class StructrFileUploader {

    private StructrUploadService uploadService = null;

    private String CRLF = "\r\n";
    private String twoHyphens = "--";
    private String boundary = null;

    private StructrUploadFile fileToUpload = null;

    public StructrFileUploader(StructrUploadService uploadService) {
        this.uploadService = uploadService;
        boundary = ""+System.currentTimeMillis();
    }

    /**
     * start the upload.
     *
     * @param fileToUpload the file to upload
     * @return  <li>true when the upload was finished without an error</li>
     *          <li>false when an error occured during the upload</li>
     */
    public void doUpload(StructrUploadFile fileToUpload) throws Throwable {
        this.fileToUpload = fileToUpload;
        HttpURLConnection connection = null;
        Throwable throwable = null;

        try {
            String fileMimeType = fileToUpload.getMimeType();

            //Get the real Path on the storage and create an fileobject with it
            File file = new File(fileToUpload.getRealPath());

            //open the communication to the server
            URL url = new URL(buildUploadUrl());
            connection = (HttpURLConnection) url.openConnection();

            //prepare the requestheaders, requestbody and the requestend
            byte[] uploadRequestBody = prepareUploadRequestBody(boundary, file, fileMimeType);
            byte[] uploadRequestFooter = prepareRequestFooter(boundary);
            long requestLength = (long) uploadRequestBody.length + (long) uploadRequestFooter.length + file.length();
            prepareRequestHeader(connection, boundary, requestLength);

            //write the file to the server
            writeFile(connection, uploadRequestBody, uploadRequestFooter, file);

            //Get the result data from the server and broadcast that the upload is finished
            int responseCode = connection.getResponseCode();
            String response = getResponseBody(connection);
            connection.disconnect();
            uploadService.broadcastFinished(fileToUpload.getFileId(), responseCode, response);

        } catch (Throwable t) {
              throwable = t;
        }

        if(throwable != null)
            throw throwable;

    }

    //Build the URL
    private static String buildUploadUrl() {
        StringBuilder path = new StringBuilder();

        String base = StructrConnector.getServer();
        path.append(base);
        if (base.endsWith("/"))
            path.append("upload");
        else
            path.append("/upload");
        return path.toString();
    }

    //Set the Headerfields of the uploadrequest
    private void prepareRequestHeader(HttpURLConnection connection, String boundary, long requestLength) throws IOException {
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(3000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-User", StructrConnector.getUserName());
        connection.setRequestProperty("X-Password", StructrConnector.getPassword());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        //Check if the file exceeds the max. file size the structr upload servlet can handle..
        if(requestLength > Integer.MAX_VALUE)
            //TODO: chunked encoding if Build.VERSION.SDK_INT < 19
            throw new IOException("File is bigger than 2GB! Structr doesn't support files bigger than 2GB at the Moment");
        else if(Build.VERSION.SDK_INT >= 19)
            connection.setFixedLengthStreamingMode(requestLength);
        else
            connection.setFixedLengthStreamingMode((int) requestLength);

    }

    private String getContentType(String mimeType) {
        String contentType = "File";
        if (mimeType != null) {
            if (mimeType.startsWith("image"))
                contentType = "Image";
            else if (mimeType.startsWith("video"))
                contentType = "VideoFile";
        }
        return contentType;
    }

    //Write the requestbody and get the bytes of it
    private byte[] prepareUploadRequestBody(String boundary, File fileToUpload, String mimeType) throws IOException{
        StringBuilder sb = new StringBuilder();

        sb.append(twoHyphens).append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"type\"").append(CRLF);
        sb.append(CRLF).append(getContentType(mimeType)).append(CRLF);
        sb.append(twoHyphens).append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileToUpload.getName()).append("\"").append(CRLF);

        if (mimeType != null)
            sb.append("Content-Type: \"").append(mimeType).append("\"").append(CRLF);

        else
            sb.append("Content-Type: \"application/octet-stream\"").append(CRLF);

        sb.append("Content-Transfer-Encoding: binary").append(CRLF);
        sb.append(CRLF);

        return sb.toString().getBytes("UTF-8");
    }

    //Write the end of the requestbody and get the bytes of it
    private byte[] prepareRequestFooter(String boundary) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(CRLF).append(CRLF).append(twoHyphens).append(boundary).append(twoHyphens).append(CRLF);
        return sb.toString().getBytes("UTF-8");
    }

    //Write the prepared requeststrings and upload the actual data
    private void writeFile(HttpURLConnection connection, byte[] uploadRequestBody, byte[] uploadRequestFooter, File file) throws IOException {
        uploadService.broadcastProgress(fileToUpload.getFileId(), 0);
        connection.connect();

        long uploadedBytes, fileSize;
        int readBytes;
        byte[] buffer;

        int maxBufferSize = 1 * 1024 * 1024;

        OutputStream output = connection.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);

        //Write Requestbody
        output.write(uploadRequestBody);

        fileSize = file.length();
        uploadedBytes = 0;
        buffer = new byte[maxBufferSize];

        //Write File
        while ((readBytes = fileInputStream.read(buffer, 0, maxBufferSize)) > 0 && (!fileToUpload.isCanceled())) {
            output.write(buffer, 0, readBytes);
            uploadedBytes += readBytes;
            int i = (int) (uploadedBytes * 100 / fileSize);
            uploadService.broadcastProgress(fileToUpload.getFileId(), i);
        }

        if(!fileToUpload.isCanceled()){
            //Write Request footer
            output.write(uploadRequestFooter);
            output.flush();
        }

        fileInputStream.close();
        output.close();
    }

    public String getResponseBody(HttpURLConnection connection) throws IOException{
        String response = "";

        //Read the response message
        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        while((line = br.readLine()) != null){
            response+=line;
        }

        return response;
    }

}
