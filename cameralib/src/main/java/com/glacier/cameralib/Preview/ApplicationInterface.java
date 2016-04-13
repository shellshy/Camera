package com.glacier.cameralib.Preview;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.util.Pair;
import android.view.MotionEvent;

import java.io.File;
import java.io.IOException;


public interface ApplicationInterface {
	final int VIDEOMETHOD_FILE = 0;
	final int VIDEOMETHOD_SAF = 1;
	final int VIDEOMETHOD_URI = 2;
	

	Context getContext();
	boolean useCamera2();
	Location getLocation();
	int createOutputVideoMethod();
	File createOutputVideoFile() throws IOException;
	Uri createOutputVideoSAF() throws IOException;
	Uri createOutputVideoUri() throws IOException;



	int getCameraIdPref();
	String getFlashPref();
	String getFocusPref(boolean is_video);
	boolean isVideoPref();
	String getSceneModePref();
	String getColorEffectPref();
	String getWhiteBalancePref();
	String getISOPref();
	int getExposureCompensationPref();
	Pair<Integer, Integer> getCameraResolutionPref();
	int getImageQualityPref();
	boolean getFaceDetectionPref();
	String getVideoQualityPref();
	boolean getVideoStabilizationPref();
	boolean getForce4KPref();
	String getVideoBitratePref();
	String getVideoFPSPref();
	long getVideoMaxDurationPref();
	int getVideoRestartTimesPref();
	long getVideoMaxFileSizePref();
	boolean getVideoRestartMaxFileSizePref();
	boolean getVideoFlashPref();
	String getPreviewSizePref();
	String getPreviewRotationPref();
	String getLockOrientationPref();
    boolean getTouchCapturePref();
    boolean getDoubleTapCapturePref();
	boolean getPausePreviewPref();
	boolean getShowToastsPref();
	boolean getShutterSoundPref();
	boolean getStartupFocusPref();
	long getTimerPref();
	String getRepeatPref();
	long getRepeatIntervalPref();
	boolean getGeotaggingPref();
	boolean getRequireLocationPref();
	boolean getRecordAudioPref();
	String getRecordAudioChannelsPref();
	String getRecordAudioSourcePref();
	int getZoomPref();

	long getExposureTimePref();
	float getFocusDistancePref();

	boolean isTestAlwaysFocus();


    void cameraSetup();
	void touchEvent(MotionEvent event);
	void startingVideo();
	void stoppingVideo();
	void stoppedVideo(final int video_method, final Uri uri, final String filename);
	void onFailedStartPreview();
	void onPhotoError();
	void onVideoInfo(int what, int extra);
	void onVideoError(int what, int extra);
	void onVideoRecordStartError(CamcorderProfile profile);
	void onVideoRecordStopError(CamcorderProfile profile);
	void onFailedReconnectError();
	void onFailedCreateVideoFileError();
	void hasPausedPreview(boolean paused);
	void cameraInOperation(boolean in_operation);
	void cameraClosed();
	void timerBeep(long remaining_time);


	void layoutUI();
	void multitouchZoom(int new_zoom);


	void setCameraIdPref(int cameraId);
	void setFlashPref(String flash_value);
	void setFocusPref(String focus_value, boolean is_video);
	void setVideoPref(boolean is_video);
	void setSceneModePref(String scene_mode);
	void clearSceneModePref();
	void setColorEffectPref(String color_effect);
	void clearColorEffectPref();
	void setWhiteBalancePref(String white_balance);
	void clearWhiteBalancePref();
	void setISOPref(String iso);
	void clearISOPref();
	void setExposureCompensationPref(int exposure);
	void clearExposureCompensationPref();
	void setCameraResolutionPref(int width, int height);
	void setVideoQualityPref(String video_quality);
	void setZoomPref(int zoom);

	void setExposureTimePref(long exposure_time);
	void clearExposureTimePref();
	void setFocusDistancePref(float focus_distance);
	

	void onDrawPreview(Canvas canvas);
	boolean onPictureTaken(byte[] data);
	void onContinuousFocusMove(boolean start);
}
