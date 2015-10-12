/*
 * Copyright (C) 2007-2008 OpenIntents.org
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
 *
 */

package org.structr.android.uploadservice;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;

/**
 *
 * This class is based on the FileUtils.class from Paul Burke wich can be found here: https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 * the class stands under the Apache License, Version 2.0. See the file header for more information.
 *
 * @author Lukas Reich
 */
public class FilePathResolver {


    /**
     *
     * This method tries to resolve the absolute path from a given Uri. Note, that not every Content Provider is supoorted at this momtent
     *
     * @param uploadService Only needed for the context
     * @param uri the Uri that you want to translate to the absolute path
     * @return the absolute path from the given Uri
     */
    public static String getPath(StructrUploadService uploadService, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(uploadService, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)){

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(uploadService, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(uploadService, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(uploadService, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Gives you the path and the name of an Uri only with one call.
     *
     * @param uploadService only necessary for the context
     * @param uri the uri to resolve
     * @return a String[] with two entries. The first is the absolute path of the given Uri. The second the name of the File
     */
    public static String[] getPathAndName(StructrUploadService uploadService, Uri uri){
        String[] pathAndName = new String[2];
        pathAndName[0] = getPath(uploadService, uri);

        if(pathAndName[0] != null){
            File file = new File(pathAndName[0]);
            pathAndName[1] = file.getName();
        }
        else
            pathAndName[1] = null;

        return pathAndName;
    }

    /**
     * Gives you the name of the file that the absolute path shows
     *
     * @param path absolute the path to the file
     * @return the name of the file.
     */
    public static String getName(String path){
        return new File(path).getName();
    }

    /**
     * Gives you an Uri from the given absolute path
     *
     * @param path the absolute path you want the Uri of.
     * @return the Uri from the file of the path
     */
    public static Uri getUri(String path){
       return Uri.fromFile(new File(path));
    }


    /**
     * retrievs the absolute path with a content resolver and the given selection arguments
     * @param uploadService needed for the context
     * @param uri the uri the resolve
     * @param selection
     * @param selectionArgs
     * @return the absolute path of the Uri
     */
    public static String getDataColumn(StructrUploadService uploadService, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = uploadService.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch(Throwable e){
            uploadService.broadcastError(-1, e);
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
