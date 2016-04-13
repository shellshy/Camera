package com.glacier.cameralib.CameraController;


public abstract class CameraControllerManager {
	public abstract int getNumberOfCameras();
	public abstract boolean isFrontFacing(int cameraId);
}
