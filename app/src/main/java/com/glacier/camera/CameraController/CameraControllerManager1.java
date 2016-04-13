package com.glacier.camera.CameraController;

import com.glacier.camera.MyDebug;

import android.hardware.Camera;
import android.util.Log;


@SuppressWarnings("deprecation")
public class CameraControllerManager1 extends CameraControllerManager {
	private static final String TAG = "CameraControllerManager1";
	public int getNumberOfCameras() {
		return Camera.getNumberOfCameras();
	}

	public boolean isFrontFacing(int cameraId) {
	    try {
		    Camera.CameraInfo camera_info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, camera_info);
			return (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
	    }
	    catch(RuntimeException e) {


    		if( MyDebug.LOG )
    			Log.d(TAG, "failed to set parameters");
	    	e.printStackTrace();
	    	return false;
	    }
	}
}
