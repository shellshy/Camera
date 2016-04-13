package com.glacier.camera.Preview.CameraSurface;

import com.glacier.camera.CameraController.CameraController;

import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.view.View;


public interface CameraSurface {
	abstract View getView();
	abstract void setPreviewDisplay(CameraController camera_controller);
	abstract void setVideoRecorder(MediaRecorder video_recorder);
	abstract void setTransform(Matrix matrix);
}
