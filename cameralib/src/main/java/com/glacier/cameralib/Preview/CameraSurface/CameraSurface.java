package com.glacier.cameralib.Preview.CameraSurface;

import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.view.View;

import com.glacier.cameralib.CameraController.CameraController;


public interface CameraSurface {
	abstract View getView();
	abstract void setPreviewDisplay(CameraController camera_controller);
	abstract void setVideoRecorder(MediaRecorder video_recorder);
	abstract void setTransform(Matrix matrix);
}
