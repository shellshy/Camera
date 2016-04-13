package com.glacier.cameralib;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class StorageUtils {
	private static final String TAG = "StorageUtils";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

	Context context = null;
    private Uri last_media_scanned = null;


	public boolean failed_to_scan = false;
	
	StorageUtils(Context context) {
		this.context = context;
	}
	
	Uri getLastMediaScanned() {
		return last_media_scanned;
	}
	void clearLastMediaScanned() {
		last_media_scanned = null;
	}

	void announceUri(Uri uri, boolean is_new_picture, boolean is_new_video) {
		if( MyDebug.LOG )
			Log.d(TAG, "announceUri");
    	if( is_new_picture ) {

    		context.sendBroadcast(new Intent( "android.hardware.action.NEW_PICTURE" , uri));

    		context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));

 			if( MyDebug.LOG )
 			{
    	        String[] CONTENT_PROJECTION = { Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE, Images.Media.DATE_TAKEN, Images.Media.DATE_ADDED }; 
    	        Cursor c = context.getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null); 
    	        if( c == null ) { 
		 			if( MyDebug.LOG )
		 				Log.e(TAG, "Couldn't resolve given uri [1]: " + uri); 
    	        }
    	        else if( !c.moveToFirst() ) { 
		 			if( MyDebug.LOG )
		 				Log.e(TAG, "Couldn't resolve given uri [2]: " + uri); 
    	        }
    	        else {
        	        String file_path = c.getString(c.getColumnIndex(Images.Media.DATA)); 
        	        String file_name = c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME)); 
        	        String mime_type = c.getString(c.getColumnIndex(Images.Media.MIME_TYPE)); 
        	        long date_taken = c.getLong(c.getColumnIndex(Images.Media.DATE_TAKEN)); 
        	        long date_added = c.getLong(c.getColumnIndex(Images.Media.DATE_ADDED)); 
	 				Log.d(TAG, "file_path: " + file_path); 
	 				Log.d(TAG, "file_name: " + file_name); 
	 				Log.d(TAG, "mime_type: " + mime_type); 
	 				Log.d(TAG, "date_taken: " + date_taken); 
	 				Log.d(TAG, "date_added: " + date_added); 
        	        c.close(); 
    	        }
    		}

    	}
    	else if( is_new_video ) {
    		context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));


    	}
	}
	
    public void broadcastFile(final File file, final boolean is_new_picture, final boolean is_new_video) {
		if( MyDebug.LOG )
			Log.d(TAG, "broadcastFile: " + file.getAbsolutePath());

    	if( file.isDirectory() ) {




    	}
    	else {


 			failed_to_scan = true;
 			if( MyDebug.LOG )
 				Log.d(TAG, "failed_to_scan set to true");
        	MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
        			new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
    		 			failed_to_scan = false;
    		 			if( MyDebug.LOG ) {
    		 				Log.d(TAG, "Scanned " + path + ":");
    		 				Log.d(TAG, "-> uri=" + uri);
    		 			}
    		 			last_media_scanned = uri;
    		 			announceUri(uri, is_new_picture, is_new_video);


    		 			Activity activity = (Activity)context;
    		 			String action = activity.getIntent().getAction();
    		 	        if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) ) {
    		    			if( MyDebug.LOG )
    		    				Log.d(TAG, "from video capture intent");
	    		 			Intent output = new Intent();
	    		 			output.setData(uri);
	    		 			activity.setResult(Activity.RESULT_OK, output);
	    		 			activity.finish();
    		 	        }
    		 		}
    			}
    		);

    	}
	}

    boolean isUsingSAF() {

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			if( sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) ) {
				return true;
			}
        }
        return false;
    }


    String getSaveLocation() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String folder_name = sharedPreferences.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "Movies");
		return folder_name;
    }
    
    public static File getBaseFolder() {
    	return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getImageFolder(String folder_name) {
		File file = null;
		if( folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length()-1 ) {

			folder_name = folder_name.substring(0, folder_name.length()-1);
		}

		if( folder_name.startsWith("/") ) {
			file = new File(folder_name);
		}
		else {
	        file = new File(getBaseFolder(), folder_name);
		}
        return file;
    }


    Uri getTreeUriSAF() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Uri treeUri = Uri.parse(sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), ""));
		return treeUri;
    }



	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	String getImageFolderNameSAF() {
		if( MyDebug.LOG )
			Log.d(TAG, "getImageFolderNameSAF");
	    String filename = null;
		Uri uri = getTreeUriSAF();
		if( MyDebug.LOG )
			Log.d(TAG, "uri: " + uri);
		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) ) {
            final String id = DocumentsContract.getTreeDocumentId(uri);
    		if( MyDebug.LOG )
    			Log.d(TAG, "id: " + id);
            String [] split = id.split(":");
            if( split.length >= 2 ) {
                String type = split[0];
    		    String path = split[1];
        		if( MyDebug.LOG ) {
        			Log.d(TAG, "type: " + type);
        			Log.d(TAG, "path: " + path);
        		}
        		filename = path;
            }
		}
		return filename;
	}


	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	File getFileFromDocumentIdSAF(String id) {
	    File file = null;
        String [] split = id.split(":");
        if( split.length >= 2 ) {
            String type = split[0];
		    String path = split[1];

		    File [] storagePoints = new File("/storage").listFiles();

            if( "primary".equalsIgnoreCase(type) ) {
    			final File externalStorage = Environment.getExternalStorageDirectory();
    			file = new File(externalStorage, path);
            }
	        for(int i=0;storagePoints != null && i<storagePoints.length && file==null;i++) {
	            File externalFile = new File(storagePoints[i], path);
	            if( externalFile.exists() ) {
	            	file = externalFile;
	            }
	        }
		}
		return file;
	}



	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder() {
		File file = null;
    	if( isUsingSAF() ) {
    		Uri uri = getTreeUriSAF();

    		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) ) {
                final String id = DocumentsContract.getTreeDocumentId(uri);

        		file = getFileFromDocumentIdSAF(id);
    		}
    	}
    	else {
    		String folder_name = getSaveLocation();
    		file = getImageFolder(folder_name);
    	}
    	return file;
    }








	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	File getFileFromDocumentUriSAF(Uri uri) {
		if( MyDebug.LOG )
			Log.d(TAG, "getFileFromDocumentUriSAF: " + uri);
	    File file = null;
		if( "com.android.externalstorage.documents".equals(uri.getAuthority()) ) {
            final String id = DocumentsContract.getDocumentId(uri);
    		if( MyDebug.LOG )
    			Log.d(TAG, "id: " + id);
    		file = getFileFromDocumentIdSAF(id);
		}
		if( MyDebug.LOG ) {
			if( file != null )
				Log.d(TAG, "file: " + file.getAbsolutePath());
			else
				Log.d(TAG, "failed to find file");
		}
		return file;
	}
	
	private String createMediaFilename(int type, int count) {
        String index = "";
        if( count > 0 ) {
            index = "_" + count;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String mediaFilename = null;
        if( type == MEDIA_TYPE_IMAGE ) {
    		String prefix = sharedPreferences.getString(PreferenceKeys.getSavePhotoPrefixPreferenceKey(), "IMG_");
    		mediaFilename = prefix + timeStamp + index + ".jpg";
        }
        else if( type == MEDIA_TYPE_VIDEO ) {
    		String prefix = sharedPreferences.getString(PreferenceKeys.getSaveVideoPrefixPreferenceKey(), "ook");
    		mediaFilename = prefix + ".mp4";
        }
        else {

    		if( MyDebug.LOG )
    			Log.e(TAG, "unknown type: " + type);
        	throw new RuntimeException();
        }
        return mediaFilename;
    }
    

    @SuppressLint("SimpleDateFormat")
	File createOutputMediaFile(int type) throws IOException {
    	File mediaStorageDir = getImageFolder();


        if( !mediaStorageDir.exists() ) {
            if( !mediaStorageDir.mkdirs() ) {
        		if( MyDebug.LOG )
        			Log.e(TAG, "failed to create directory");
        		throw new IOException();
            }
            broadcastFile(mediaStorageDir, false, false);
        }


        File mediaFile = null;
        for(int count=0;count<100;count++) {
        	String mediaFilename = createMediaFilename(type, count);
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mediaFilename);
            if( !mediaFile.exists() ) {
            	break;
            }
        }

		if( MyDebug.LOG ) {
			Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
		}
		if( mediaFile == null )
			throw new IOException();
        return mediaFile;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputMediaFileSAF(int type) throws IOException {
    	try {
	    	Uri treeUri = getTreeUriSAF();
		    if( MyDebug.LOG )
		    	Log.d(TAG, "treeUri: " + treeUri);
	        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
		    if( MyDebug.LOG )
		    	Log.d(TAG, "docUri: " + docUri);
		    String mimeType = "";
	        if( type == MEDIA_TYPE_IMAGE ) {
	        	mimeType = "image/jpeg";
	        }
	        else if( type == MEDIA_TYPE_VIDEO ) {
	        	mimeType = "video/mp4";
	        }
	        else {

	    		if( MyDebug.LOG )
	    			Log.e(TAG, "unknown type: " + type);
	        	throw new RuntimeException();
	        }

	        String mediaFilename = createMediaFilename(type, 0);
		    Uri fileUri = DocumentsContract.createDocument(context.getContentResolver(), docUri, mimeType, mediaFilename);   
		    if( MyDebug.LOG )
		    	Log.d(TAG, "returned fileUri: " + fileUri);
			if( fileUri == null )
				throw new IOException();
	    	return fileUri;
    	}
    	catch(IllegalArgumentException e) {

		    if( MyDebug.LOG )
		    	Log.e(TAG, "createOutputMediaFileSAF failed");
		    e.printStackTrace();
		    throw new IOException();
    	}
    }

    static class Media {
    	long id;
    	boolean video;
    	Uri uri;
    	long date;
    	int orientation;

    	Media(long id, boolean video, Uri uri, long date, int orientation) {
    		this.id = id;
    		this.video = video;
    		this.uri = uri;
    		this.date = date;
    		this.orientation = orientation;
    	}
    }
    
    private Media getLatestMedia(boolean video) {
		if( MyDebug.LOG )
			Log.d(TAG, "getLatestMedia: " + (video ? "video" : "images"));
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {




			if( MyDebug.LOG )
				Log.e(TAG, "don't have READ_EXTERNAL_STORAGE permission");
			return null;
		}
    	Media media = null;
		Uri baseUri = video ? Video.Media.EXTERNAL_CONTENT_URI : Images.Media.EXTERNAL_CONTENT_URI;

		Uri query = baseUri;
		final int column_id_c = 0;
		final int column_date_taken_c = 1;
		final int column_data_c = 2;
		final int column_orientation_c = 3;
		String [] projection = video ? new String[] {VideoColumns._ID, VideoColumns.DATE_TAKEN, VideoColumns.DATA} : new String[] {ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.DATA, ImageColumns.ORIENTATION};
		String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg'";
		String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(query, projection, selection, null, order);
			if( cursor != null && cursor.moveToFirst() ) {
				if( MyDebug.LOG )
					Log.d(TAG, "found: " + cursor.getCount());

				boolean found = false;
				File save_folder = isUsingSAF() ? null : getImageFolder();
				String save_folder_string = isUsingSAF() ? null : save_folder.getAbsolutePath() + File.separator;
				if( MyDebug.LOG )
					Log.d(TAG, "save_folder_string: " + save_folder_string);
				do {
					String path = cursor.getString(column_data_c);
					if( MyDebug.LOG )
						Log.d(TAG, "path: " + path);


					if( isUsingSAF() || (path != null && path.contains(save_folder_string) ) ) {
						if( MyDebug.LOG )
							Log.d(TAG, "found most recent in Open Camera folder");


						long date = cursor.getLong(column_date_taken_c);
				    	long current_time = System.currentTimeMillis();
						if( date > current_time + 172800000 ) {
							if( MyDebug.LOG )
								Log.d(TAG, "skip date in the future!");
						}
						else {
							found = true;
							break;
						}
					}
				} while( cursor.moveToNext() );
				if( !found ) {
					if( MyDebug.LOG )
						Log.d(TAG, "can't find suitable in Open Camera folder, so just go with most recent");
					cursor.moveToFirst();
				}
				long id = cursor.getLong(column_id_c);
				long date = cursor.getLong(column_date_taken_c);
				int orientation = video ? 0 : cursor.getInt(column_orientation_c);
				Uri uri = ContentUris.withAppendedId(baseUri, id);
				if( MyDebug.LOG )
					Log.d(TAG, "found most recent uri for " + (video ? "video" : "images") + ": " + uri);
				media = new Media(id, video, uri, date, orientation);
			}
		}
		catch(SQLiteException e) {

			if( MyDebug.LOG )
				Log.e(TAG, "SQLiteException trying to find latest media");
			e.printStackTrace();
		}
		finally {
			if( cursor != null ) {
				cursor.close();
			}
		}
		return media;
    }
    
    Media getLatestMedia() {
		Media image_media = getLatestMedia(false);
		Media video_media = getLatestMedia(true);
		Media media = null;
		if( image_media != null && video_media == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "only found images");
			media = image_media;
		}
		else if( image_media == null && video_media != null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "only found videos");
			media = video_media;
		}
		else if( image_media != null && video_media != null ) {
			if( MyDebug.LOG ) {
				Log.d(TAG, "found images and videos");
				Log.d(TAG, "latest image date: " + image_media.date);
				Log.d(TAG, "latest video date: " + video_media.date);
			}
			if( image_media.date >= video_media.date ) {
				if( MyDebug.LOG )
					Log.d(TAG, "latest image is newer");
				media = image_media;
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "latest video is newer");
				media = video_media;
			}
		}
		if( MyDebug.LOG )
			Log.d(TAG, "return latest media: " + media);
		return media;
    }
}
