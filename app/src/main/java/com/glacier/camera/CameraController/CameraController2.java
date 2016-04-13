package com.glacier.camera.CameraController;

import com.glacier.camera.MyDebug;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraController2 extends CameraController {
	private static final String TAG = "CameraController2";

	private Context context = null;
	private CameraDevice camera = null;
	private String cameraIdS = null;
	private CameraCharacteristics characteristics = null;
	private List<Integer> zoom_ratios = null;
	private int current_zoom_value = 0;
	private ErrorCallback preview_error_cb = null;
	private CameraCaptureSession captureSession = null;
	private CaptureRequest.Builder previewBuilder = null;
	private AutoFocusCallback autofocus_cb = null;
	private FaceDetectionListener face_detection_listener = null;
	private ImageReader imageReader = null;
	private PictureCallback jpeg_cb = null;
	private ErrorCallback take_picture_error_cb = null;

	private SurfaceTexture texture = null;
	private Surface surface_texture = null;
	private HandlerThread thread = null; 
	Handler handler = null;
	
	private int preview_width = 0;
	private int preview_height = 0;
	
	private int picture_width = 0;
	private int picture_height = 0;
	
	private static final int STATE_NORMAL = 0;
	private static final int STATE_WAITING_AUTOFOCUS = 1;


	private int state = STATE_NORMAL;
	
	private MediaActionSound media_action_sound = new MediaActionSound();
	private boolean sounds_enabled = true;
	
	private boolean capture_result_has_iso = false;
	private int capture_result_iso = 0;
	private boolean capture_result_has_exposure_time = false;
	private long capture_result_exposure_time = 0;
	private boolean capture_result_has_frame_duration = false;
	private long capture_result_frame_duration = 0;
	
	private static enum RequestTag {
		CAPTURE
	}
	
	private class CameraSettings {

		private int rotation = 0;
		private Location location = null;
		private byte jpeg_quality = 90;


		private int scene_mode = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
		private int color_effect = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
		private int white_balance = CameraMetadata.CONTROL_AWB_MODE_AUTO;
		private String flash_value = "flash_off";
		private boolean has_iso = false;


		private int iso = 0;
		private long exposure_time = 1000000000l/30;
		private Rect scalar_crop_region = null;
		private boolean has_ae_exposure_compensation = false;
		private int ae_exposure_compensation = 0;
		private boolean has_af_mode = false;
		private int af_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
		private float focus_distance = 0.0f;
		private float focus_distance_manual = 0.0f;
		private boolean ae_lock = false;
		private MeteringRectangle [] af_regions = null;
		private MeteringRectangle [] ae_regions = null;
		private boolean has_face_detect_mode = false;
		private int face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
		private boolean video_stabilization = false;

		private void setupBuilder(CaptureRequest.Builder builder, boolean is_still) {






			builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

			setSceneMode(builder);
			setColorEffect(builder);
			setWhiteBalance(builder);
			setAEMode(builder, is_still);
			setCropRegion(builder);
			setExposureCompensation(builder);
			setFocusMode(builder);
			setFocusDistance(builder);
			setAutoExposureLock(builder);
			setAFRegions(builder);
			setAERegions(builder);
			setFaceDetectMode(builder);
			setVideoStabilization(builder);


			if( is_still ) {
				if( location != null ) {
					builder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
				}
				builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
				builder.set(CaptureRequest.JPEG_QUALITY, jpeg_quality);
			}
		}

		private boolean setSceneMode(CaptureRequest.Builder builder) {
			if( MyDebug.LOG ) {
				Log.d(TAG, "setSceneMode");
				Log.d(TAG, "builder: " + builder);
			}
			if( builder.get(CaptureRequest.CONTROL_SCENE_MODE) == null && scene_mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED ) {

			}
			else if( builder.get(CaptureRequest.CONTROL_SCENE_MODE) == null || builder.get(CaptureRequest.CONTROL_SCENE_MODE) != scene_mode ) {
				if( MyDebug.LOG )
					Log.d(TAG, "setting scene mode: " + scene_mode);
				if( scene_mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED ) {
					builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
				}
				else {
					builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
				}
				builder.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode);
				return true;
			}
			return false;
		}

		private boolean setColorEffect(CaptureRequest.Builder builder) {
			if( builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null && color_effect == CameraMetadata.CONTROL_EFFECT_MODE_OFF ) {

			}
			else if( builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null || builder.get(CaptureRequest.CONTROL_EFFECT_MODE) != color_effect ) {
				if( MyDebug.LOG )
					Log.d(TAG, "setting color effect: " + color_effect);
				builder.set(CaptureRequest.CONTROL_EFFECT_MODE, color_effect);
				return true;
			}
			return false;
		}

		private boolean setWhiteBalance(CaptureRequest.Builder builder) {
			if( builder.get(CaptureRequest.CONTROL_AWB_MODE) == null && white_balance == CameraMetadata.CONTROL_AWB_MODE_AUTO ) {

			}
			else if( builder.get(CaptureRequest.CONTROL_AWB_MODE) == null || builder.get(CaptureRequest.CONTROL_AWB_MODE) != white_balance ) {
				if( MyDebug.LOG )
					Log.d(TAG, "setting white balance: " + white_balance);
				builder.set(CaptureRequest.CONTROL_AWB_MODE, white_balance);
				return true;
			}
			return false;
		}

		private boolean setAEMode(CaptureRequest.Builder builder, boolean is_still) {
			if( MyDebug.LOG )
				Log.d(TAG, "setAEMode");
			if( has_iso ) {
				if( MyDebug.LOG ) {
					Log.d(TAG, "manual mode");
					Log.d(TAG, "iso: " + iso);
					Log.d(TAG, "exposure_time: " + exposure_time);
				}
				builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
				builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
				builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure_time);

		    	if( flash_value.equals("flash_off") ) {
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_auto") ) {
					builder.set(CaptureRequest.FLASH_MODE, is_still ? CameraMetadata.FLASH_MODE_SINGLE : CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_on") ) {
					builder.set(CaptureRequest.FLASH_MODE, is_still ? CameraMetadata.FLASH_MODE_SINGLE : CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_torch") ) {
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
		    	}
		    	else if( flash_value.equals("flash_red_eye") ) {
					builder.set(CaptureRequest.FLASH_MODE, is_still ? CameraMetadata.FLASH_MODE_SINGLE : CameraMetadata.FLASH_MODE_OFF);
		    	}
			}
			else {
				if( MyDebug.LOG ) {
					Log.d(TAG, "auto mode");
					Log.d(TAG, "flash_value: " + flash_value);
				}

		    	if( flash_value.equals("flash_off") ) {
					builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_auto") ) {
					builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_on") ) {
					builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		    	}
		    	else if( flash_value.equals("flash_torch") ) {
					builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
		    	}
		    	else if( flash_value.equals("flash_red_eye") ) {
					builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
					builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
		    	}
			}
			return true;
		}

		private void setCropRegion(CaptureRequest.Builder builder) {
			if( scalar_crop_region != null ) {
				builder.set(CaptureRequest.SCALER_CROP_REGION, scalar_crop_region);
			}
		}

		private boolean setExposureCompensation(CaptureRequest.Builder builder) {
			if( !has_ae_exposure_compensation )
				return false;
			if( has_iso ) {
				if( MyDebug.LOG )
					Log.d(TAG, "don't set exposure compensation in manual iso mode");
				return false;
			}
			if( builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) == null || ae_exposure_compensation != builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ) {
				if( MyDebug.LOG )
					Log.d(TAG, "change exposure to " + ae_exposure_compensation);
				builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae_exposure_compensation);
	        	return true;
			}
			return false;
		}

		private void setFocusMode(CaptureRequest.Builder builder) {
			if( has_af_mode ) {
				if( MyDebug.LOG )
					Log.d(TAG, "change af mode to " + af_mode);
				builder.set(CaptureRequest.CONTROL_AF_MODE, af_mode);
			}
		}
		
		private void setFocusDistance(CaptureRequest.Builder builder) {
			if( MyDebug.LOG )
				Log.d(TAG, "change focus distance to " + focus_distance);
			builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus_distance);
		}

		private void setAutoExposureLock(CaptureRequest.Builder builder) {
	    	builder.set(CaptureRequest.CONTROL_AE_LOCK, ae_lock);
		}

		private void setAFRegions(CaptureRequest.Builder builder) {
			if( af_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0 ) {
				builder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			}
		}

		private void setAERegions(CaptureRequest.Builder builder) {
			if( ae_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0 ) {
				builder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
			}
		}

		private void setFaceDetectMode(CaptureRequest.Builder builder) {
			if( has_face_detect_mode )
				builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, face_detect_mode);
		}
		
		private void setVideoStabilization(CaptureRequest.Builder builder) {
			builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, video_stabilization ? CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON : CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
		}
		

	}
	
	private CameraSettings camera_settings = new CameraSettings();
	private boolean push_repeating_request_when_torch_off = false;
	private CaptureRequest push_repeating_request_when_torch_off_id = null;
	private boolean push_set_ae_lock = false;
	private CaptureRequest push_set_ae_lock_id = null;

	public CameraController2(Context context, int cameraId, ErrorCallback preview_error_cb) throws CameraControllerException {
		super(cameraId);
		if( MyDebug.LOG )
			Log.d(TAG, "create new CameraController2: " + cameraId);

		this.context = context;
		this.preview_error_cb = preview_error_cb;

		thread = new HandlerThread("CameraBackground"); 
		thread.start(); 
		handler = new Handler(thread.getLooper());

		final CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);

		class MyStateCallback extends CameraDevice.StateCallback {
			boolean callback_done = false;
			boolean first_callback = true;
			@Override
			public void onOpened(CameraDevice cam) {
				if( MyDebug.LOG )
					Log.d(TAG, "camera opened");
				if( first_callback ) {
					first_callback = false;

				    try {

						characteristics = manager.getCameraCharacteristics(cameraIdS);

						CameraController2.this.camera = cam;


						createPreviewRequest();
					}
				    catch(CameraAccessException e) {
						if( MyDebug.LOG ) {
							Log.e(TAG, "failed to get camera characteristics");
							Log.e(TAG, "reason: " + e.getReason());
							Log.e(TAG, "message: " + e.getMessage());
						}
						e.printStackTrace();

					}

					callback_done = true;
				}
			}

			@Override
			public void onClosed(CameraDevice cam) {
				if( MyDebug.LOG )
					Log.d(TAG, "camera closed");

				if( first_callback ) {
					first_callback = false;
				}
			}

			@Override
			public void onDisconnected(CameraDevice cam) {
				if( MyDebug.LOG )
					Log.d(TAG, "camera disconnected");
				if( first_callback ) {
					first_callback = false;


					CameraController2.this.camera = null;
					if( MyDebug.LOG )
						Log.d(TAG, "onDisconnected: camera is now set to null");
					cam.close();
					if( MyDebug.LOG )
						Log.d(TAG, "onDisconnected: camera is now closed");
					callback_done = true;
				}
			}

			@Override
			public void onError(CameraDevice cam, int error) {
				if( MyDebug.LOG ) {
					Log.d(TAG, "camera error: " + error);
					Log.d(TAG, "received camera: " + cam);
					Log.d(TAG, "actual camera: " + CameraController2.this.camera);
				}
				if( first_callback ) {
					first_callback = false;
				}
				else {
					if( MyDebug.LOG )
						Log.d(TAG, "error occurred after camera was opened");
				}

				CameraController2.this.camera = null;
				if( MyDebug.LOG )
					Log.d(TAG, "onError: camera is now set to null");
				cam.close();
				if( MyDebug.LOG )
					Log.d(TAG, "onError: camera is now closed");
				callback_done = true;
			}
		};
		MyStateCallback myStateCallback = new MyStateCallback();

		try {
			this.cameraIdS = manager.getCameraIdList()[cameraId];
			manager.openCamera(cameraIdS, myStateCallback, handler);
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to open camera: CameraAccessException");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			throw new CameraControllerException();
		}
		catch(UnsupportedOperationException e) {

			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to open camera: UnsupportedOperationException");
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			throw new CameraControllerException();
		}
		catch(SecurityException e) {

			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to open camera: SecurityException");
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			throw new CameraControllerException();
		}

		if( MyDebug.LOG )
			Log.d(TAG, "wait until camera opened...");

		while( !myStateCallback.callback_done ) {
		}
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.e(TAG, "camera failed to open");
			throw new CameraControllerException();
		}
		if( MyDebug.LOG )
			Log.d(TAG, "camera now opened: " + camera);


	}

	@Override
	public void release() {
		if( MyDebug.LOG )
			Log.d(TAG, "release");
		if( thread != null ) {
			thread.quitSafely();
			try {
				thread.join();
				thread = null;
				handler = null;
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		if( captureSession != null ) {
			captureSession.close();
			captureSession = null;
		}
		previewBuilder = null;
		if( camera != null ) {
			camera.close();
			camera = null;
		}
		if( imageReader != null ) {
			imageReader.close();
			imageReader = null;
		}

	}

	private List<String> convertFocusModesToValues(int [] supported_focus_modes_arr, float minimum_focus_distance) {
		if( MyDebug.LOG )
			Log.d(TAG, "convertFocusModesToValues()");
	    List<Integer> supported_focus_modes = new ArrayList<Integer>();
	    for(int i=0;i<supported_focus_modes_arr.length;i++)
	    	supported_focus_modes.add(supported_focus_modes_arr[i]);
	    List<String> output_modes = new Vector<String>();
		if( supported_focus_modes != null ) {

			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) ) {
				output_modes.add("focus_mode_auto");
				if( MyDebug.LOG ) {
					Log.d(TAG, " supports focus_mode_auto");
				}
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_MACRO) ) {
				output_modes.add("focus_mode_macro");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_macro");
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) ) {
				output_modes.add("focus_mode_locked");
				if( MyDebug.LOG ) {
					Log.d(TAG, " supports focus_mode_locked");
				}
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_OFF) ) {
				output_modes.add("focus_mode_infinity");
				if( minimum_focus_distance > 0.0f ) {
					output_modes.add("focus_mode_manual2");
					if( MyDebug.LOG ) {
						Log.d(TAG, " supports focus_mode_manual2");
					}
				}
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_EDOF) ) {
				output_modes.add("focus_mode_edof");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_edof");
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) ) {
				output_modes.add("focus_mode_continuous_picture");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_continuous_picture");
			}
			if( supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO) ) {
				output_modes.add("focus_mode_continuous_video");
				if( MyDebug.LOG )
					Log.d(TAG, " supports focus_mode_continuous_video");
			}
		}
		return output_modes;
	}

	public String getAPI() {
		return "Camera2 (Android L)";
	}
	
	@Override
	public CameraFeatures getCameraFeatures() {
		if( MyDebug.LOG )
			Log.d(TAG, "getCameraFeatures()");
		CameraFeatures camera_features = new CameraFeatures();
		if( MyDebug.LOG ) {
			int hardware_level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
			if( hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY )
				Log.d(TAG, "Hardware Level: LEGACY");
			else if( hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED )
				Log.d(TAG, "Hardware Level: LIMITED");
			else if( hardware_level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL )
				Log.d(TAG, "Hardware Level: FULL");
			else
				Log.e(TAG, "Unknown Hardware Level!");
		}

		float max_zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
		camera_features.is_zoom_supported = max_zoom > 0.0f;
		if( MyDebug.LOG )
			Log.d(TAG, "max_zoom: " + max_zoom);
		if( camera_features.is_zoom_supported ) {

			final int steps_per_2x_factor = 20;

			int n_steps =(int)( (steps_per_2x_factor * Math.log(max_zoom + 1.0e-11)) / Math.log(2.0));
			final double scale_factor = Math.pow(max_zoom, 1.0/(double)n_steps);
			if( MyDebug.LOG ) {
				Log.d(TAG, "n_steps: " + n_steps);
				Log.d(TAG, "scale_factor: " + scale_factor);
			}
			camera_features.zoom_ratios = new ArrayList<Integer>();
			camera_features.zoom_ratios.add(100);
			double zoom = 1.0;
			for(int i=0;i<n_steps-1;i++) {
				zoom *= scale_factor;
				camera_features.zoom_ratios.add((int)(zoom*100));
			}
			camera_features.zoom_ratios.add((int)(max_zoom*100));
			camera_features.max_zoom = camera_features.zoom_ratios.size()-1;
			this.zoom_ratios = camera_features.zoom_ratios;
		}
		else {
			this.zoom_ratios = null;
		}

		int [] face_modes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
		camera_features.supports_face_detection = false;
		for(int i=0;i<face_modes.length;i++) {
			if( MyDebug.LOG )
				Log.d(TAG, "face detection mode: " + face_modes[i]);


			if( face_modes[i] == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL ) {
				camera_features.supports_face_detection = true;
			}
		}
		if( camera_features.supports_face_detection ) {
			int face_count = characteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
			if( face_count <= 0 ) {
				camera_features.supports_face_detection = false;
			}
		}

		StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

	    android.util.Size [] camera_picture_sizes = configs.getOutputSizes(ImageFormat.JPEG);
		camera_features.picture_sizes = new ArrayList<CameraController.Size>();
		for(android.util.Size camera_size : camera_picture_sizes) {
			if( MyDebug.LOG )
				Log.d(TAG, "picture size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
			camera_features.picture_sizes.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
		}

	    android.util.Size [] camera_video_sizes = configs.getOutputSizes(MediaRecorder.class);
		camera_features.video_sizes = new ArrayList<CameraController.Size>();
		for(android.util.Size camera_size : camera_video_sizes) {
			if( MyDebug.LOG )
				Log.d(TAG, "video size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
			if( camera_size.getWidth() > 3840 || camera_size.getHeight() > 2160 )
				continue;
			camera_features.video_sizes.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
		}

		android.util.Size [] camera_preview_sizes = configs.getOutputSizes(SurfaceTexture.class);
		camera_features.preview_sizes = new ArrayList<CameraController.Size>();
        Point display_size = new Point();
		Activity activity = (Activity)context;
        {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getRealSize(display_size);
    		if( MyDebug.LOG )
    			Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
		for(android.util.Size camera_size : camera_preview_sizes) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview size: " + camera_size.getWidth() + " x " + camera_size.getHeight());
			if( camera_size.getWidth() > display_size.x || camera_size.getHeight() > display_size.y ) {


				continue;
			}
			camera_features.preview_sizes.add(new CameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
		}
		
		if( characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ) {
			camera_features.supported_flash_values = new ArrayList<String>();
			camera_features.supported_flash_values.add("flash_off");
			camera_features.supported_flash_values.add("flash_auto");
			camera_features.supported_flash_values.add("flash_on");
			camera_features.supported_flash_values.add("flash_torch");
			camera_features.supported_flash_values.add("flash_red_eye");
		}

		camera_features.minimum_focus_distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
		if( MyDebug.LOG )
			Log.d(TAG, "minimum_focus_distance: " + camera_features.minimum_focus_distance);
		int [] supported_focus_modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
		camera_features.supported_focus_values = convertFocusModesToValues(supported_focus_modes, camera_features.minimum_focus_distance);
		camera_features.max_num_focus_areas = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);

		camera_features.is_exposure_lock_supported = true;
		
        camera_features.is_video_stabilization_supported = true;

		Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
		if( iso_range != null ) {
			camera_features.supports_iso_range = true;
			camera_features.min_iso = iso_range.getLower();
			camera_features.max_iso = iso_range.getUpper();

			Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
			if( exposure_time_range != null ) {
				camera_features.supports_exposure_time = true;
				camera_features.min_exposure_time = exposure_time_range.getLower();
				camera_features.max_exposure_time = exposure_time_range.getUpper();
			}
		}

		Range<Integer> exposure_range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
		camera_features.min_exposure = exposure_range.getLower();
		camera_features.max_exposure = exposure_range.getUpper();
		camera_features.exposure_step = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();

		camera_features.can_disable_shutter_sound = true;

		return camera_features;
	}

	private String convertSceneMode(int value2) {
		String value = null;
		switch( value2 ) {
		case CameraMetadata.CONTROL_SCENE_MODE_ACTION:
			value = "action";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_BARCODE:
			value = "barcode";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_BEACH:
			value = "beach";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT:
			value = "candlelight";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_DISABLED:
			value = "auto";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS:
			value = "fireworks";
			break;


		case CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE:
			value = "landscape";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_NIGHT:
			value = "night";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT:
			value = "night-portrait";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_PARTY:
			value = "party";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT:
			value = "portrait";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_SNOW:
			value = "snow";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_SPORTS:
			value = "sports";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO:
			value = "steadyphoto";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_SUNSET:
			value = "sunset";
			break;
		case CameraMetadata.CONTROL_SCENE_MODE_THEATRE:
			value = "theatre";
			break;
		default:
			if( MyDebug.LOG )
				Log.d(TAG, "unknown scene mode: " + value2);
			value = null;
			break;
		}
		return value;
	}

	@Override
	public SupportedValues setSceneMode(String value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setSceneMode: " + value);

		String default_value = getDefaultSceneMode();
		int [] values2 = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
		boolean has_disabled = false;
		List<String> values = new ArrayList<String>();
		for(int i=0;i<values2.length;i++) {
			if( values2[i] == CameraMetadata.CONTROL_SCENE_MODE_DISABLED )
				has_disabled = true;
			String this_value = convertSceneMode(values2[i]);
			if( this_value != null ) {
				values.add(this_value);
			}
		}
		if( !has_disabled ) {
			values.add(0, "auto");
		}
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);
		if( supported_values != null ) {
			int selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
			if( supported_values.selected_value.equals("action") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_ACTION;
			}
			else if( supported_values.selected_value.equals("barcode") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_BARCODE;
			}
			else if( supported_values.selected_value.equals("beach") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_BEACH;
			}
			else if( supported_values.selected_value.equals("candlelight") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT;
			}
			else if( supported_values.selected_value.equals("auto") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
			}
			else if( supported_values.selected_value.equals("fireworks") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS;
			}

			else if( supported_values.selected_value.equals("landscape") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE;
			}
			else if( supported_values.selected_value.equals("night") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_NIGHT;
			}
			else if( supported_values.selected_value.equals("night-portrait") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT;
			}
			else if( supported_values.selected_value.equals("party") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_PARTY;
			}
			else if( supported_values.selected_value.equals("portrait") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT;
			}
			else if( supported_values.selected_value.equals("snow") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SNOW;
			}
			else if( supported_values.selected_value.equals("sports") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SPORTS;
			}
			else if( supported_values.selected_value.equals("steadyphoto") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO;
			}
			else if( supported_values.selected_value.equals("sunset") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SUNSET;
			}
			else if( supported_values.selected_value.equals("theatre") ) {
				selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_THEATRE;
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "unknown selected_value: " + supported_values.selected_value);
			}

			camera_settings.scene_mode = selected_value2;
			if( camera_settings.setSceneMode(previewBuilder) ) {
				try {
					setRepeatingRequest();
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to set scene mode");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
				} 
			}
		}
		return supported_values;
	}
	
	@Override
	public String getSceneMode() {
		if( previewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE) == null )
			return null;
		int value2 = previewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE);
		String value = convertSceneMode(value2);
		return value;
	}

	private String convertColorEffect(int value2) {
		String value = null;
		switch( value2 ) {
		case CameraMetadata.CONTROL_EFFECT_MODE_AQUA:
			value = "aqua";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD:
			value = "blackboard";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_MONO:
			value = "mono";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE:
			value = "negative";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_OFF:
			value = "none";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE:
			value = "posterize";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_SEPIA:
			value = "sepia";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE:
			value = "solarize";
			break;
		case CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD:
			value = "whiteboard";
			break;
		default:
			if( MyDebug.LOG )
				Log.d(TAG, "unknown effect mode: " + value2);
			value = null;
			break;
		}
		return value;
	}

	@Override
	public SupportedValues setColorEffect(String value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setColorEffect: " + value);

		String default_value = getDefaultColorEffect();
		int [] values2 = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
		List<String> values = new ArrayList<String>();
		for(int i=0;i<values2.length;i++) {
			String this_value = convertColorEffect(values2[i]);
			if( this_value != null ) {
				values.add(this_value);
			}
		}
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);
		if( supported_values != null ) {
			int selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
			if( supported_values.selected_value.equals("aqua") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_AQUA;
			}
			else if( supported_values.selected_value.equals("blackboard") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD;
			}
			else if( supported_values.selected_value.equals("mono") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_MONO;
			}
			else if( supported_values.selected_value.equals("negative") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE;
			}
			else if( supported_values.selected_value.equals("none") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
			}
			else if( supported_values.selected_value.equals("posterize") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE;
			}
			else if( supported_values.selected_value.equals("sepia") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_SEPIA;
			}
			else if( supported_values.selected_value.equals("solarize") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE;
			}
			else if( supported_values.selected_value.equals("whiteboard") ) {
				selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD;
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "unknown selected_value: " + supported_values.selected_value);
			}

			camera_settings.color_effect = selected_value2;
			if( camera_settings.setColorEffect(previewBuilder) ) {
				try {
					setRepeatingRequest();
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to set color effect");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
				} 
			}
		}
		return supported_values;
	}

	@Override
	public String getColorEffect() {
		if( previewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null )
			return null;
		int value2 = previewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE);
		String value = convertColorEffect(value2);
		return value;
	}

	private String convertWhiteBalance(int value2) {
		String value = null;
		switch( value2 ) {
		case CameraMetadata.CONTROL_AWB_MODE_AUTO:
			value = "auto";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
			value = "cloudy-daylight";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT:
			value = "daylight";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT:
			value = "fluorescent";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT:
			value = "incandescent";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_SHADE:
			value = "shade";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_TWILIGHT:
			value = "twilight";
			break;
		case CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT:
			value = "warm-fluorescent";
			break;
		default:
			if( MyDebug.LOG )
				Log.d(TAG, "unknown white balance: " + value2);
			value = null;
			break;
		}
		return value;
	}

	@Override
	public SupportedValues setWhiteBalance(String value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setWhiteBalance: " + value);

		String default_value = getDefaultWhiteBalance();
		int [] values2 = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
		List<String> values = new ArrayList<String>();
		for(int i=0;i<values2.length;i++) {
			String this_value = convertWhiteBalance(values2[i]);
			if( this_value != null ) {
				values.add(this_value);
			}
		}
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);
		if( supported_values != null ) {
			int selected_value2 = CameraMetadata.CONTROL_AWB_MODE_AUTO;
			if( supported_values.selected_value.equals("auto") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_AUTO;
			}
			else if( supported_values.selected_value.equals("cloudy-daylight") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
			}
			else if( supported_values.selected_value.equals("daylight") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT;
			}
			else if( supported_values.selected_value.equals("fluorescent") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT;
			}
			else if( supported_values.selected_value.equals("incandescent") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT;
			}
			else if( supported_values.selected_value.equals("shade") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_SHADE;
			}
			else if( supported_values.selected_value.equals("twilight") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_TWILIGHT;
			}
			else if( supported_values.selected_value.equals("warm-fluorescent") ) {
				selected_value2 = CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT;
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "unknown selected_value: " + supported_values.selected_value);
			}

			camera_settings.white_balance = selected_value2;
			if( camera_settings.setWhiteBalance(previewBuilder) ) {
				try {
					setRepeatingRequest();
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to set white balance");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
				} 
			}
		}
		return supported_values;
	}

	@Override
	public String getWhiteBalance() {
		if( previewBuilder.get(CaptureRequest.CONTROL_AWB_MODE) == null )
			return null;
		int value2 = previewBuilder.get(CaptureRequest.CONTROL_AWB_MODE);
		String value = convertWhiteBalance(value2);
		return value;
	}

	@Override
	public SupportedValues setISO(String value) {
		String default_value = getDefaultISO();
		Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
		if( iso_range == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "iso not supported");
			return null;
		}
		if( MyDebug.LOG )
			Log.d(TAG, "iso range from " + iso_range.getLower() + " to " + iso_range.getUpper());
		List<String> values = new ArrayList<String>();
		values.add(default_value);
		int [] iso_values = {50, 100, 200, 400, 800, 1600, 3200, 6400};
		values.add("" + iso_range.getLower());
		for(int i=0;i<iso_values.length;i++) {
			if( iso_values[i] > iso_range.getLower() && iso_values[i] < iso_range.getUpper() ) {
				values.add("" + iso_values[i]);
			}
		}
		values.add("" + iso_range.getUpper());


		SupportedValues supported_values = null;
		try {
			if( value.equals(default_value) ) {
				supported_values = new SupportedValues(values, value);
				camera_settings.has_iso = false;
				camera_settings.iso = 0;
				if( camera_settings.setAEMode(previewBuilder, false) ) {
			    	setRepeatingRequest();
				}
			}
			else {
				try {
					int selected_value2 = Integer.parseInt(value);
					if( selected_value2 < iso_range.getLower() )
						selected_value2 = iso_range.getLower();
					if( selected_value2 > iso_range.getUpper() )
						selected_value2 = iso_range.getUpper();
					if( MyDebug.LOG )
						Log.d(TAG, "iso: " + selected_value2);
					supported_values = new SupportedValues(values, "" + selected_value2);
					camera_settings.has_iso = true;
					camera_settings.iso = selected_value2;
					if( camera_settings.setAEMode(previewBuilder, false) ) {
				    	setRepeatingRequest();
					}
				}
				catch(NumberFormatException exception) {
					if( MyDebug.LOG )
						Log.d(TAG, "iso invalid format, can't parse to int");
					supported_values = new SupportedValues(values, default_value);
					camera_settings.has_iso = false;
					camera_settings.iso = 0;
					if( camera_settings.setAEMode(previewBuilder, false) ) {
				    	setRepeatingRequest();
					}
				}
			}
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set ISO");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 

		return supported_values;
	}

	@Override
	public String getISOKey() {
		return "";
	}

	@Override
	public int getISO() {
		return camera_settings.iso;
	}
	
	@Override


	public boolean setISO(int iso) {
		if( MyDebug.LOG )
			Log.d(TAG, "setISO: " + iso);
		if( camera_settings.iso == iso ) {
			if( MyDebug.LOG )
				Log.d(TAG, "already set");
			return false;
		}
		try {
			camera_settings.iso = iso;
			if( camera_settings.setAEMode(previewBuilder, false) ) {
		    	setRepeatingRequest();
			}
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set ISO");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
		return true;
	}

	@Override
	public long getExposureTime() {
		return camera_settings.exposure_time;
	}

	@Override


	public boolean setExposureTime(long exposure_time) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "setExposureTime: " + exposure_time);
			Log.d(TAG, "current exposure time: " + camera_settings.exposure_time);
		}
		if( camera_settings.exposure_time == exposure_time ) {
			if( MyDebug.LOG )
				Log.d(TAG, "already set");
			return false;
		}
		try {
			camera_settings.exposure_time = exposure_time;
			if( camera_settings.setAEMode(previewBuilder, false) ) {
		    	setRepeatingRequest();
			}
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set exposure time");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
		return true;
	}

	@Override
	public Size getPictureSize() {
		Size size = new Size(picture_width, picture_height);
		return size;
	}

	@Override
	public void setPictureSize(int width, int height) {
		if( MyDebug.LOG )
			Log.d(TAG, "setPictureSize: " + width + " x " + height);
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.e(TAG, "no camera");
			return;
		}
		if( captureSession != null ) {

			if( MyDebug.LOG )
				Log.e(TAG, "can't set picture size when captureSession running!");
			throw new RuntimeException();
		}
		this.picture_width = width;
		this.picture_height = height;
	}

	private void createPictureImageReader() {
		if( MyDebug.LOG )
			Log.d(TAG, "createPictureImageReader");
		if( captureSession != null ) {

			if( MyDebug.LOG )
				Log.e(TAG, "can't create picture image reader when captureSession running!");
			throw new RuntimeException();
		}
		if( imageReader != null ) {
			imageReader.close();
		}
		if( picture_width == 0 || picture_height == 0 ) {
			if( MyDebug.LOG )
				Log.e(TAG, "application needs to call setPictureSize()");
			throw new RuntimeException();
		}
		imageReader = ImageReader.newInstance(picture_width, picture_height, ImageFormat.JPEG, 2); 
		if( MyDebug.LOG ) {
			Log.d(TAG, "created new imageReader: " + imageReader.toString());
			Log.d(TAG, "imageReader surface: " + imageReader.getSurface().toString());
		}
		imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
			@Override
			public void onImageAvailable(ImageReader reader) {
				if( MyDebug.LOG )
					Log.d(TAG, "new still image available");
				if( jpeg_cb == null ) {
					if( MyDebug.LOG )
						Log.d(TAG, "no picture callback available");
					return;
				}
				Image image = reader.acquireNextImage();
	            ByteBuffer buffer = image.getPlanes()[0].getBuffer(); 
	            byte [] bytes = new byte[buffer.remaining()]; 
				if( MyDebug.LOG )
					Log.d(TAG, "read " + bytes.length + " bytes");
	            buffer.get(bytes);
	            image.close();

	            PictureCallback cb = jpeg_cb;
	            jpeg_cb = null;
	            take_picture_error_cb = null;
	            cb.onPictureTaken(bytes);
				if( MyDebug.LOG )
					Log.d(TAG, "done onImageAvailable");
			}
		}, null);
	}
	@Override
	public Size getPreviewSize() {
		return new Size(preview_width, preview_height);
	}

	@Override
	public void setPreviewSize(int width, int height) {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewSize: " + width + " , " + height);

		preview_width = width;
		preview_height = height;

	}

	@Override
	public void setVideoStabilization(boolean enabled) {
		camera_settings.video_stabilization = enabled;
		camera_settings.setVideoStabilization(previewBuilder);
		try {
			setRepeatingRequest();
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set video stabilization");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}

	@Override
	public boolean getVideoStabilization() {
		return camera_settings.video_stabilization;
	}

	@Override
	public int getJpegQuality() {
		return this.camera_settings.jpeg_quality;
	}

	@Override
	public void setJpegQuality(int quality) {
		if( quality < 0 || quality > 100 ) {
			if( MyDebug.LOG )
				Log.e(TAG, "invalid jpeg quality" + quality);
			throw new RuntimeException();
		}
		this.camera_settings.jpeg_quality = (byte)quality;
	}

	@Override
	public int getZoom() {
		return this.current_zoom_value;
	}

	@Override
	public void setZoom(int value) {
		if( zoom_ratios == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "zoom not supported");
			return;
		}
		if( value < 0 || value > zoom_ratios.size() ) {
			if( MyDebug.LOG )
				Log.e(TAG, "invalid zoom value" + value);
			throw new RuntimeException();
		}
		float zoom = zoom_ratios.get(value)/100.0f;
		Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		int left = sensor_rect.width()/2;
		int right = left;
		int top = sensor_rect.height()/2;
		int bottom = top;
		int hwidth = (int)(sensor_rect.width() / (2.0*zoom));
		int hheight = (int)(sensor_rect.height() / (2.0*zoom));
		left -= hwidth;
		right += hwidth;
		top -= hheight;
		bottom += hheight;
		if( MyDebug.LOG ) {
			Log.d(TAG, "zoom: " + zoom);
			Log.d(TAG, "hwidth: " + hwidth);
			Log.d(TAG, "hheight: " + hheight);
			Log.d(TAG, "sensor_rect left: " + sensor_rect.left);
			Log.d(TAG, "sensor_rect top: " + sensor_rect.top);
			Log.d(TAG, "sensor_rect right: " + sensor_rect.right);
			Log.d(TAG, "sensor_rect bottom: " + sensor_rect.bottom);
			Log.d(TAG, "left: " + left);
			Log.d(TAG, "top: " + top);
			Log.d(TAG, "right: " + right);
			Log.d(TAG, "bottom: " + bottom);

		}
		camera_settings.scalar_crop_region = new Rect(left, top, right, bottom);
		camera_settings.setCropRegion(previewBuilder);
    	this.current_zoom_value = value;
    	try {
    		setRepeatingRequest();
    	}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set zoom");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}
	
	@Override
	public int getExposureCompensation() {
		if( previewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) == null )
			return 0;
		return previewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
	}

	@Override

	public boolean setExposureCompensation(int new_exposure) {
		camera_settings.has_ae_exposure_compensation = true;
		camera_settings.ae_exposure_compensation = new_exposure;
		if( camera_settings.setExposureCompensation(previewBuilder) ) {
			try {
				setRepeatingRequest();
			}
			catch(CameraAccessException e) {
				if( MyDebug.LOG ) {
					Log.e(TAG, "failed to set exposure compensation");
					Log.e(TAG, "reason: " + e.getReason());
					Log.e(TAG, "message: " + e.getMessage());
				}
				e.printStackTrace();
			} 
        	return true;
		}
		return false;
	}
	
	@Override
	public void setPreviewFpsRange(int min, int max) {


	}

	@Override
	public List<int[]> getSupportedPreviewFpsRange() {

		return null;
	}

	@Override

	public long getDefaultExposureTime() {
		return 1000000000l/30;
	}

	@Override
	public void setFocusValue(String focus_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setFocusValue: " + focus_value);
		int focus_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
    	if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_locked") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
    	}
    	else if( focus_value.equals("focus_mode_infinity") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_OFF;
        	camera_settings.focus_distance = 0.0f;
    	}
    	else if( focus_value.equals("focus_mode_manual2") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_OFF;
        	camera_settings.focus_distance = camera_settings.focus_distance_manual;
    	}
    	else if( focus_value.equals("focus_mode_macro") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_MACRO;
    	}
    	else if( focus_value.equals("focus_mode_edof") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_EDOF;
    	}
    	else if( focus_value.equals("focus_mode_continuous_picture") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    	}
    	else if( focus_value.equals("focus_mode_continuous_video") ) {
    		focus_mode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
    	}
    	else {
    		if( MyDebug.LOG )
    			Log.d(TAG, "setFocusValue() received unknown focus value " + focus_value);
    		return;
    	}
    	camera_settings.has_af_mode = true;
    	camera_settings.af_mode = focus_mode;
    	camera_settings.setFocusMode(previewBuilder);
    	camera_settings.setFocusDistance(previewBuilder);
    	try {
    		setRepeatingRequest();
    	}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set focus mode");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}
	
	private String convertFocusModeToValue(int focus_mode) {
		if( MyDebug.LOG )
			Log.d(TAG, "convertFocusModeToValue: " + focus_mode);
		String focus_value = "";
		if( focus_mode == CaptureRequest.CONTROL_AF_MODE_AUTO ) {
    		focus_value = "focus_mode_auto";
    	}
		else if( focus_mode == CaptureRequest.CONTROL_AF_MODE_MACRO ) {
    		focus_value = "focus_mode_macro";
    	}
		else if( focus_mode == CaptureRequest.CONTROL_AF_MODE_EDOF ) {
    		focus_value = "focus_mode_edof";
    	}
		else if( focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE ) {
    		focus_value = "focus_mode_continuous_picture";
    	}
		else if( focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO ) {
    		focus_value = "focus_mode_continuous_video";
    	}
		else if( focus_mode == CaptureRequest.CONTROL_AF_MODE_OFF ) {
    		focus_value = "focus_mode_manual2";
		}
    	return focus_value;
	}
	
	@Override
	public String getFocusValue() {
		int focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE) != null ?
				previewBuilder.get(CaptureRequest.CONTROL_AF_MODE) : CaptureRequest.CONTROL_AF_MODE_AUTO;
		return convertFocusModeToValue(focus_mode);
	}

	@Override
	public float getFocusDistance() {
		return camera_settings.focus_distance;
	}

	@Override
	public boolean setFocusDistance(float focus_distance) {
		if( MyDebug.LOG )
			Log.d(TAG, "setFocusDistance: " + focus_distance);
		if( camera_settings.focus_distance == focus_distance ) {
			if( MyDebug.LOG )
				Log.d(TAG, "already set");
			return false;
		}
    	camera_settings.focus_distance = focus_distance;
    	camera_settings.focus_distance_manual = focus_distance;
    	camera_settings.setFocusDistance(previewBuilder);
    	try {
    		setRepeatingRequest();
    	}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set focus distance");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
    	return true;
	}

	@Override
	public void setFlashValue(String flash_value) {
		if( MyDebug.LOG )
			Log.d(TAG, "setFlashValue: " + flash_value);
		if( !characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ) {
			return;
		}
		else if( camera_settings.flash_value.equals(flash_value) ) {
			return;
		}

		try {
			if( camera_settings.flash_value.equals("flash_torch") ) {

				camera_settings.flash_value = "flash_off";
				camera_settings.setAEMode(previewBuilder, false);
				CaptureRequest request = previewBuilder.build();
	

		    	camera_settings.flash_value = flash_value;
				camera_settings.setAEMode(previewBuilder, false);
				push_repeating_request_when_torch_off = true;
				push_repeating_request_when_torch_off_id = request;
	
				setRepeatingRequest(request);
			}
			else {
				camera_settings.flash_value = flash_value;
				if( camera_settings.setAEMode(previewBuilder, false) ) {
			    	setRepeatingRequest();
				}
			}
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set flash mode");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}

	@Override
	public String getFlashValue() {

		if( !characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ) {
			return "";
		}
		return camera_settings.flash_value;
	}

	@Override
	public void setRecordingHint(boolean hint) {

	}

	@Override
	public void setAutoExposureLock(boolean enabled) {
		camera_settings.ae_lock = enabled;
		camera_settings.setAutoExposureLock(previewBuilder);
		try {
			setRepeatingRequest();
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set auto exposure lock");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}
	
	@Override
	public boolean getAutoExposureLock() {
		if( previewBuilder.get(CaptureRequest.CONTROL_AE_LOCK) == null )
			return false;
    	return previewBuilder.get(CaptureRequest.CONTROL_AE_LOCK);
	}

	@Override
	public void setRotation(int rotation) {
		this.camera_settings.rotation = rotation;
	}

	@Override
	public void setLocationInfo(Location location) {
		if( MyDebug.LOG )
			Log.d(TAG, "setLocationInfo: " + location.getLongitude() + " , " + location.getLatitude());
		this.camera_settings.location = location;
	}

	@Override
	public void removeLocationInfo() {
		this.camera_settings.location = null;
	}

	@Override
	public void enableShutterSound(boolean enabled) {
		this.sounds_enabled = enabled;
	}

	private Rect convertRectToCamera2(Rect sensor_rect, Rect rect) {


		double left_f = (rect.left+1000)/2000.0;
		double top_f = (rect.top+1000)/2000.0;
		double right_f = (rect.right+1000)/2000.0;
		double bottom_f = (rect.bottom+1000)/2000.0;
		int left = (int)(left_f * (sensor_rect.width()-1));
		int right = (int)(right_f * (sensor_rect.width()-1));
		int top = (int)(top_f * (sensor_rect.height()-1));
		int bottom = (int)(bottom_f * (sensor_rect.height()-1));
		left = Math.max(left, 0);
		right = Math.max(right, 0);
		top = Math.max(top, 0);
		bottom = Math.max(bottom, 0);
		left = Math.min(left, sensor_rect.width()-1);
		right = Math.min(right, sensor_rect.width()-1);
		top = Math.min(top, sensor_rect.height()-1);
		bottom = Math.min(bottom, sensor_rect.height()-1);

		Rect camera2_rect = new Rect(left, top, right, bottom);
		return camera2_rect;
	}

	private MeteringRectangle convertAreaToMeteringRectangle(Rect sensor_rect, Area area) {
		Rect camera2_rect = convertRectToCamera2(sensor_rect, area.rect);
		MeteringRectangle metering_rectangle = new MeteringRectangle(camera2_rect, area.weight);
		return metering_rectangle;
	}

	private Rect convertRectFromCamera2(Rect sensor_rect, Rect camera2_rect) {

		double left_f = camera2_rect.left/(double)(sensor_rect.width()-1);
		double top_f = camera2_rect.top/(double)(sensor_rect.height()-1);
		double right_f = camera2_rect.right/(double)(sensor_rect.width()-1);
		double bottom_f = camera2_rect.bottom/(double)(sensor_rect.height()-1);
		int left = (int)(left_f * 2000) - 1000;
		int right = (int)(right_f * 2000) - 1000;
		int top = (int)(top_f * 2000) - 1000;
		int bottom = (int)(bottom_f * 2000) - 1000;

		left = Math.max(left, -1000);
		right = Math.max(right, -1000);
		top = Math.max(top, -1000);
		bottom = Math.max(bottom, -1000);
		left = Math.min(left, 1000);
		right = Math.min(right, 1000);
		top = Math.min(top, 1000);
		bottom = Math.min(bottom, 1000);

		Rect rect = new Rect(left, top, right, bottom);
		return rect;
	}

	private Area convertMeteringRectangleToArea(Rect sensor_rect, MeteringRectangle metering_rectangle) {
		Rect area_rect = convertRectFromCamera2(sensor_rect, metering_rectangle.getRect());
		Area area = new Area(area_rect, metering_rectangle.getMeteringWeight());
		return area;
	}
	
	private CameraController.Face convertFromCameraFace(Rect sensor_rect, android.hardware.camera2.params.Face camera2_face) {
		Rect area_rect = convertRectFromCamera2(sensor_rect, camera2_face.getBounds());
		CameraController.Face face = new CameraController.Face(camera2_face.getScore(), area_rect);
		return face;
	}

	@Override
	public boolean setFocusAndMeteringArea(List<Area> areas) {
		Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		if( MyDebug.LOG )
			Log.d(TAG, "sensor_rect: " + sensor_rect.left + " , " + sensor_rect.top + " x " + sensor_rect.right + " , " + sensor_rect.bottom);
		boolean has_focus = false;
		boolean has_metering = false;
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0 ) {
			has_focus = true;
			camera_settings.af_regions = new MeteringRectangle[areas.size()];
			int i = 0;
			for(CameraController.Area area : areas) {
				camera_settings.af_regions[i++] = convertAreaToMeteringRectangle(sensor_rect, area);
			}
			camera_settings.setAFRegions(previewBuilder);
		}
		else
			camera_settings.af_regions = null;
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0 ) {
			has_metering = true;
			camera_settings.ae_regions = new MeteringRectangle[areas.size()];
			int i = 0;
			for(CameraController.Area area : areas) {
				camera_settings.ae_regions[i++] = convertAreaToMeteringRectangle(sensor_rect, area);
			}
			camera_settings.setAERegions(previewBuilder);
		}
		else
			camera_settings.ae_regions = null;
		if( has_focus || has_metering ) {
			try {
				setRepeatingRequest();
			}
			catch(CameraAccessException e) {
				if( MyDebug.LOG ) {
					Log.e(TAG, "failed to set focus and/or metering regions");
					Log.e(TAG, "reason: " + e.getReason());
					Log.e(TAG, "message: " + e.getMessage());
				}
				e.printStackTrace();
			} 
		}
		return has_focus;
	}
	
	@Override
	public void clearFocusAndMetering() {
		Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		boolean has_focus = false;
		boolean has_metering = false;
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0 ) {
			has_focus = true;
			camera_settings.af_regions = new MeteringRectangle[1];
			camera_settings.af_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width()-1, sensor_rect.height()-1, 0);
			camera_settings.setAFRegions(previewBuilder);
		}
		else
			camera_settings.af_regions = null;
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0 ) {
			has_metering = true;
			camera_settings.ae_regions = new MeteringRectangle[1];
			camera_settings.ae_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width()-1, sensor_rect.height()-1, 0);
			camera_settings.setAERegions(previewBuilder);
		}
		else
			camera_settings.ae_regions = null;
		if( has_focus || has_metering ) {
			try {
				setRepeatingRequest();
			}
			catch(CameraAccessException e) {
				if( MyDebug.LOG ) {
					Log.e(TAG, "failed to clear focus and metering regions");
					Log.e(TAG, "reason: " + e.getReason());
					Log.e(TAG, "message: " + e.getMessage());
				}
				e.printStackTrace();
			} 
		}
	}

	@Override
	public List<Area> getFocusAreas() {
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) == 0 )
			return null;
    	MeteringRectangle [] metering_rectangles = previewBuilder.get(CaptureRequest.CONTROL_AF_REGIONS);
    	if( metering_rectangles == null )
    		return null;
		Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		camera_settings.af_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width()-1, sensor_rect.height()-1, 0);
		if( metering_rectangles.length == 1 && metering_rectangles[0].getRect().left == 0 && metering_rectangles[0].getRect().top == 0 && metering_rectangles[0].getRect().right == sensor_rect.width()-1 && metering_rectangles[0].getRect().bottom == sensor_rect.height()-1 ) {

			return null;
		}
		List<Area> areas = new ArrayList<CameraController.Area>();
		for(int i=0;i<metering_rectangles.length;i++) {
			areas.add(convertMeteringRectangleToArea(sensor_rect, metering_rectangles[i]));
		}
		return areas;
	}

	@Override
	public List<Area> getMeteringAreas() {
		if( characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) == 0 )
			return null;
    	MeteringRectangle [] metering_rectangles = previewBuilder.get(CaptureRequest.CONTROL_AE_REGIONS);
    	if( metering_rectangles == null )
    		return null;
		Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		if( metering_rectangles.length == 1 && metering_rectangles[0].getRect().left == 0 && metering_rectangles[0].getRect().top == 0 && metering_rectangles[0].getRect().right == sensor_rect.width()-1 && metering_rectangles[0].getRect().bottom == sensor_rect.height()-1 ) {

			return null;
		}
		List<Area> areas = new ArrayList<CameraController.Area>();
		for(int i=0;i<metering_rectangles.length;i++) {
			areas.add(convertMeteringRectangleToArea(sensor_rect, metering_rectangles[i]));
		}
		return areas;
	}

	@Override
	public boolean supportsAutoFocus() {
		if( previewBuilder.get(CaptureRequest.CONTROL_AF_MODE) == null )
			return true;
		int focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
		if( focus_mode == CaptureRequest.CONTROL_AF_MODE_AUTO || focus_mode == CaptureRequest.CONTROL_AF_MODE_MACRO )
			return true;
		return false;
	}

	@Override
	public boolean focusIsVideo() {
		if( previewBuilder.get(CaptureRequest.CONTROL_AF_MODE) == null )
			return false;
		int focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
		if( focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO ) {
			return true;
		}
		return false;
	}

	@Override
	public void setPreviewDisplay(SurfaceHolder holder) throws CameraControllerException {
		if( MyDebug.LOG ) {
			Log.d(TAG, "setPreviewDisplay");
			Log.e(TAG, "SurfaceHolder not supported for CameraController2!");
			Log.e(TAG, "Should use setPreviewTexture() instead");
		}
		throw new RuntimeException();
	}

	@Override
	public void setPreviewTexture(SurfaceTexture texture) throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewTexture");
		if( this.texture != null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "preview texture already set");
			throw new RuntimeException();
		}
		this.texture = texture;
	}

	private void setRepeatingRequest() throws CameraAccessException {
		setRepeatingRequest(previewBuilder.build());
	}

	private void setRepeatingRequest(CaptureRequest request) throws CameraAccessException {
		if( MyDebug.LOG )
			Log.d(TAG, "setRepeatingRequest");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			return;
		}
		captureSession.setRepeatingRequest(request, previewCaptureCallback, handler);
	}

	private void capture() throws CameraAccessException {
		capture(previewBuilder.build());
	}

	private void capture(CaptureRequest request) throws CameraAccessException {
		if( MyDebug.LOG )
			Log.d(TAG, "capture");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			return;
		}
		captureSession.capture(request, previewCaptureCallback, handler);
	}
	
	private void createPreviewRequest() {
		if( MyDebug.LOG )
			Log.d(TAG, "createPreviewRequest");
		if( camera == null  ) {
			if( MyDebug.LOG )
				Log.d(TAG, "camera not available!");
			return;
		}
		if( MyDebug.LOG )
			Log.d(TAG, "camera: " + camera);
		try {
			previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			camera_settings.setupBuilder(previewBuilder, false);
		}
		catch(CameraAccessException e) {

			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to create capture request");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}

	private Surface getPreviewSurface() {
		return surface_texture;
	}

	private void createCaptureSession(final MediaRecorder video_recorder) throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "create capture session");
		
		if( previewBuilder == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "previewBuilder not present!");
			throw new RuntimeException();
		}
		if( camera == null ) {
			if( MyDebug.LOG )
				Log.e(TAG, "no camera");
			return;
		}

		if( captureSession != null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "close old capture session");
			captureSession.close();
			captureSession = null;
		}

		try {
			captureSession = null;

			if( video_recorder != null ) {
				if( imageReader != null ) {
					imageReader.close();
					imageReader = null;
				}
			}
			else {

				createPictureImageReader();
			}
			if( texture != null ) {

				if( MyDebug.LOG )
					Log.d(TAG, "set size of preview texture");
				if( preview_width == 0 || preview_height == 0 ) {
					if( MyDebug.LOG )
						Log.e(TAG, "application needs to call setPreviewSize()");
					throw new RuntimeException();
				}
				texture.setDefaultBufferSize(preview_width, preview_height);

				if( surface_texture != null ) {
					if( MyDebug.LOG )
						Log.d(TAG, "remove old target: " + surface_texture);
					previewBuilder.removeTarget(surface_texture);
				}
				this.surface_texture = new Surface(texture);
				if( MyDebug.LOG )
					Log.d(TAG, "created new target: " + surface_texture);
			}
			if( video_recorder != null ) {
				if( MyDebug.LOG )
					Log.d(TAG, "creating capture session for video recording");
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "picture size: " + imageReader.getWidth() + " x " + imageReader.getHeight());
			}

			if( MyDebug.LOG )
				Log.d(TAG, "preview size: " + this.preview_width + " x " + this.preview_height);

			class MyStateCallback extends CameraCaptureSession.StateCallback {
				boolean callback_done = false;
				@Override
				public void onConfigured(CameraCaptureSession session) {
					if( MyDebug.LOG ) {
						Log.d(TAG, "onConfigured: " + session);
						Log.d(TAG, "captureSession was: " + captureSession);
					}
					if( camera == null ) {
						if( MyDebug.LOG ) {
							Log.d(TAG, "camera is closed");
						}
						callback_done = true;
						return;
					}
					captureSession = session;
		        	Surface surface = getPreviewSurface();
	        		previewBuilder.addTarget(surface);
	        		if( video_recorder != null )
	        			previewBuilder.addTarget(video_recorder.getSurface());
	        		try {
	        			setRepeatingRequest();
	        		}
					catch(CameraAccessException e) {
						if( MyDebug.LOG ) {
							Log.e(TAG, "failed to start preview");
							Log.e(TAG, "reason: " + e.getReason());
							Log.e(TAG, "message: " + e.getMessage());
						}
						e.printStackTrace();
						preview_error_cb.onError();
					} 
					callback_done = true;
				}

				@Override
				public void onConfigureFailed(CameraCaptureSession session) {
					if( MyDebug.LOG ) {
						Log.d(TAG, "onConfigureFailed: " + session);
						Log.d(TAG, "captureSession was: " + captureSession);
					}
					callback_done = true;

				}
			}
			MyStateCallback myStateCallback = new MyStateCallback();

        	Surface preview_surface = getPreviewSurface();
        	Surface capture_surface = video_recorder != null ? video_recorder.getSurface() : imageReader.getSurface();
			if( MyDebug.LOG ) {
				Log.d(TAG, "texture: " + texture);
				Log.d(TAG, "preview_surface: " + preview_surface);
				Log.d(TAG, "capture_surface: " + capture_surface);
				if( video_recorder == null ) {
					Log.d(TAG, "imageReader: " + imageReader);
					Log.d(TAG, "imageReader: " + imageReader.getWidth());
					Log.d(TAG, "imageReader: " + imageReader.getHeight());
					Log.d(TAG, "imageReader: " + imageReader.getImageFormat());
				}
			}
			camera.createCaptureSession(Arrays.asList(preview_surface, capture_surface),
				myStateCallback,
		 		handler);
			if( MyDebug.LOG )
				Log.d(TAG, "wait until session created...");
			while( !myStateCallback.callback_done ) {
			}
			if( MyDebug.LOG ) {
				Log.d(TAG, "created captureSession: " + captureSession);
			}
			if( captureSession == null ) {
				if( MyDebug.LOG )
					Log.e(TAG, "failed to create capture session");
				throw new CameraControllerException();
			}
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "CameraAccessException trying to create capture session");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}

	@Override
	public void startPreview() throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "startPreview");
		if( captureSession != null ) {
			try {
				setRepeatingRequest();
			}
			catch(CameraAccessException e) {
				if( MyDebug.LOG ) {
					Log.e(TAG, "failed to start preview");
					Log.e(TAG, "reason: " + e.getReason());
					Log.e(TAG, "message: " + e.getMessage());
				}
				e.printStackTrace();

				throw new CameraControllerException();
			} 
			return;
		}
		createCaptureSession(null);
	}

	@Override
	public void stopPreview() {
		if( MyDebug.LOG )
			Log.d(TAG, "stopPreview");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			return;
		}
		try {
			captureSession.stopRepeating();

			if( MyDebug.LOG )
				Log.d(TAG, "close capture session");
			captureSession.close();
			captureSession = null;
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to stop repeating");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		}
	}

	@Override
	public boolean startFaceDetection() {
    	if( previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL ) {
    		return false;
    	}
    	camera_settings.has_face_detect_mode = true;
    	camera_settings.face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL;
    	camera_settings.setFaceDetectMode(previewBuilder);
    	try {
    		setRepeatingRequest();
    	}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to start face detection");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
		return true;
	}
	
	@Override
	public void setFaceDetectionListener(final FaceDetectionListener listener) {
		this.face_detection_listener = listener;
	}

	@Override
	public void autoFocus(final AutoFocusCallback cb) {
		if( MyDebug.LOG )
			Log.d(TAG, "autoFocus");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");

			cb.onAutoFocus(false);
			return;
		}

    	previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
		if( MyDebug.LOG ) {
			{
				MeteringRectangle [] areas = previewBuilder.get(CaptureRequest.CONTROL_AF_REGIONS);
				for(int i=0;areas != null && i<areas.length;i++) {
					Log.d(TAG, i + " focus area: " + areas[i].getX() + " , " + areas[i].getY() + " : " + areas[i].getWidth() + " x " + areas[i].getHeight() + " weight " + areas[i].getMeteringWeight());
				}
			}
			{
				MeteringRectangle [] areas = previewBuilder.get(CaptureRequest.CONTROL_AE_REGIONS);
				for(int i=0;areas != null && i<areas.length;i++) {
					Log.d(TAG, i + " metering area: " + areas[i].getX() + " , " + areas[i].getY() + " : " + areas[i].getWidth() + " x " + areas[i].getHeight() + " weight " + areas[i].getMeteringWeight());
				}
			}
		}



		state = STATE_WAITING_AUTOFOCUS;
		this.autofocus_cb = cb;


		try {
			capture();
	    	previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
			setRepeatingRequest();
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to autofocus");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			state = STATE_NORMAL;
			autofocus_cb.onAutoFocus(false);
			autofocus_cb = null;
		} 
		previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
	}

	@Override
	public void cancelAutoFocus() {
		if( MyDebug.LOG )
			Log.d(TAG, "cancelAutoFocus");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			return;
		}
    	previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

    	try {
    		capture();
    	}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to cancel autofocus [capture]");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		}
    	previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
		this.autofocus_cb = null;
		state = STATE_NORMAL;
		try {
			setRepeatingRequest();
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to set repeating request after cancelling autofocus");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
		} 
	}
	
	@Override
	public void setContinuousFocusMoveCallback(ContinuousFocusMoveCallback cb) {
		if( MyDebug.LOG )
			Log.d(TAG, "setContinuousFocusMoveCallback");

	}

	private void takePictureAfterPrecapture() {
		if( MyDebug.LOG )
			Log.d(TAG, "takePictureAfterPrecapture");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			return;
		}
		try {
			if( MyDebug.LOG ) {
				Log.d(TAG, "imageReader: " + imageReader.toString());
				Log.d(TAG, "imageReader surface: " + imageReader.getSurface().toString());
			}
			CaptureRequest.Builder stillBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			stillBuilder.setTag(RequestTag.CAPTURE);
			camera_settings.setupBuilder(stillBuilder, true);
        	Surface surface = getPreviewSurface();
        	stillBuilder.addTarget(surface);
			stillBuilder.addTarget(imageReader.getSurface());


			captureSession.stopRepeating();

			captureSession.capture(stillBuilder.build(), previewCaptureCallback, handler);
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to take picture");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			jpeg_cb = null;
			if( take_picture_error_cb != null ) {
				take_picture_error_cb.onError();
				take_picture_error_cb = null;
				return;
			}
		}
	}




	
	@Override
	public void takePicture(final PictureCallback raw, final PictureCallback jpeg, final ErrorCallback error) {
		if( MyDebug.LOG )
			Log.d(TAG, "takePicture");
		if( camera == null || captureSession == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "no camera or capture session");
			error.onError();
			return;
		}
		this.jpeg_cb = jpeg;
		this.take_picture_error_cb = error;
		takePictureAfterPrecapture();



	}

	@Override
	public void setDisplayOrientation(int degrees) {

		if( MyDebug.LOG )
			Log.d(TAG, "setDisplayOrientation not supported by this API");
		throw new RuntimeException();
	}

	@Override
	public int getDisplayOrientation() {
		if( MyDebug.LOG )
			Log.d(TAG, "getDisplayOrientation not supported by this API");
		throw new RuntimeException();
	}

	@Override
	public int getCameraOrientation() {
		return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
	}

	@Override
	public boolean isFrontFacing() {
		return characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
	}

	@Override
	public void unlock() {

	}

	@Override
	public void initVideoRecorderPrePrepare(MediaRecorder video_recorder) {

	}

	@Override
	public void initVideoRecorderPostPrepare(MediaRecorder video_recorder) throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "initVideoRecorderPostPrepare");
		try {
			if( MyDebug.LOG )
				Log.d(TAG, "obtain video_recorder surface");
			if( MyDebug.LOG )
				Log.d(TAG, "done");
			previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
			camera_settings.setupBuilder(previewBuilder, false);
			createCaptureSession(video_recorder);
		}
		catch(CameraAccessException e) {
			if( MyDebug.LOG ) {
				Log.e(TAG, "failed to create capture request for video");
				Log.e(TAG, "reason: " + e.getReason());
				Log.e(TAG, "message: " + e.getMessage());
			}
			e.printStackTrace();
			throw new CameraControllerException();
		}
	}

	@Override
	public void reconnect() throws CameraControllerException {
		if( MyDebug.LOG )
			Log.d(TAG, "reconnect");
		createPreviewRequest();
		createCaptureSession(null);


	}

	@Override
	public String getParametersString() {
		return null;
	}

	@Override
	public boolean captureResultHasIso() {
		return capture_result_has_iso;
	}

	@Override
	public int captureResultIso() {
		return capture_result_iso;
	}

	@Override
	public boolean captureResultHasExposureTime() {
		return capture_result_has_exposure_time;
	}

	@Override
	public long captureResultExposureTime() {
		return capture_result_exposure_time;
	}

	@Override
	public boolean captureResultHasFrameDuration() {
		return capture_result_has_frame_duration;
	}

	@Override
	public long captureResultFrameDuration() {
		return capture_result_frame_duration;
	}

	private CameraCaptureSession.CaptureCallback previewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
		private long last_af_state_frame_number = 0;
		private int last_af_state = -1;

		public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
			if( request.getTag() == RequestTag.CAPTURE ) {
				if( MyDebug.LOG )
					Log.d(TAG, "onCaptureStarted: capture");
				if( sounds_enabled )
					media_action_sound.play(MediaActionSound.SHUTTER_CLICK);
			}
		}

		public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
			processAF(request, partialResult);
			super.onCaptureProgressed(session, request, partialResult);
		}

		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
			processAF(request, result);
			processCompleted(request, result);
			super.onCaptureCompleted(session, request, result);
		}

		private void processAF(CaptureRequest request, CaptureResult result) {

			if( result.getFrameNumber() < last_af_state_frame_number ) {

				return;
			}
			if( result.get(CaptureResult.CONTROL_AF_STATE) == null) {
				if( MyDebug.LOG )
					Log.d(TAG, "processAF discared as no af state info");

				return;
			}
			last_af_state_frame_number = result.getFrameNumber();
			int af_state = result.get(CaptureResult.CONTROL_AF_STATE);

			if( state == STATE_NORMAL ) {

			}
			else if( state == STATE_WAITING_AUTOFOCUS && af_state != last_af_state ) {


				if( af_state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af_state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ||
						af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED || af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED
						) {
					boolean focus_success = af_state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED;
					if( MyDebug.LOG ) {
						if( focus_success )
							Log.d(TAG, "onCaptureCompleted: autofocus success");
						else
							Log.d(TAG, "onCaptureCompleted: autofocus failed");
						Log.d(TAG, "af_state: " + af_state);
					}
					state = STATE_NORMAL;



					 if( autofocus_cb != null ) {
						autofocus_cb.onAutoFocus(focus_success);
						autofocus_cb = null;
					}
				}
			}

			if( af_state != last_af_state ) {
				if( MyDebug.LOG )
					Log.d(TAG, "CONTROL_AF_STATE changed from " + last_af_state + " to " + af_state);
			}
			last_af_state = af_state;
		}
		
		private void processCompleted(CaptureRequest request, CaptureResult result) {

			
			if( result.get(CaptureResult.SENSOR_SENSITIVITY) != null ) {
				capture_result_has_iso = true;
				capture_result_iso = result.get(CaptureResult.SENSOR_SENSITIVITY);

				if( camera_settings.has_iso && camera_settings.iso != capture_result_iso ) {




					if( MyDebug.LOG ) {
						Log.d(TAG, "ISO " + capture_result_iso + " different to requested ISO " + camera_settings.iso);
						Log.d(TAG, "    requested ISO was: " + request.get(CaptureRequest.SENSOR_SENSITIVITY));
						Log.d(TAG, "    requested AE mode was: " + request.get(CaptureRequest.CONTROL_AE_MODE));
					}
					try {
						setRepeatingRequest();
					}
					catch(CameraAccessException e) {
						if( MyDebug.LOG ) {
							Log.e(TAG, "failed to set repeating request after ISO hack");
							Log.e(TAG, "reason: " + e.getReason());
							Log.e(TAG, "message: " + e.getMessage());
						}
						e.printStackTrace();
					} 
				}
			}
			else {
				capture_result_has_iso = false;
			}
			if( result.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null ) {
				capture_result_has_exposure_time = true;
				capture_result_exposure_time = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
			}
			else {
				capture_result_has_exposure_time = false;
			}
			if( result.get(CaptureResult.SENSOR_FRAME_DURATION) != null ) {
				capture_result_has_frame_duration = true;
				capture_result_frame_duration = result.get(CaptureResult.SENSOR_FRAME_DURATION);
			}
			else {
				capture_result_has_frame_duration = false;
			}


			if( face_detection_listener != null && previewBuilder != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL ) {
				Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
				android.hardware.camera2.params.Face [] camera_faces = result.get(CaptureResult.STATISTICS_FACES);
				if( camera_faces != null ) {
					CameraController.Face [] faces = new CameraController.Face[camera_faces.length];
					for(int i=0;i<camera_faces.length;i++) {
						faces[i] = convertFromCameraFace(sensor_rect, camera_faces[i]);
					}
					face_detection_listener.onFaceDetection(faces);
				}
			}
			
			if( push_repeating_request_when_torch_off && push_repeating_request_when_torch_off_id == request ) {
				if( MyDebug.LOG )
					Log.d(TAG, "received push_repeating_request_when_torch_off");
				Integer flash_state = result.get(CaptureResult.FLASH_STATE);
				if( MyDebug.LOG ) {
					if( flash_state != null )
						Log.d(TAG, "flash_state: " + flash_state);
					else
						Log.d(TAG, "flash_state is null");
				}
				if( flash_state != null && flash_state == CaptureResult.FLASH_STATE_READY ) {
					push_repeating_request_when_torch_off = false;
					push_repeating_request_when_torch_off_id = null;
					try {
						setRepeatingRequest();
					}
					catch(CameraAccessException e) {
						if( MyDebug.LOG ) {
							Log.e(TAG, "failed to set flash [from torch/flash off hack]");
							Log.e(TAG, "reason: " + e.getReason());
							Log.e(TAG, "message: " + e.getMessage());
						}
						e.printStackTrace();
					} 
				}
			}
			if( push_set_ae_lock && push_set_ae_lock_id == request ) {
				if( MyDebug.LOG )
					Log.d(TAG, "received push_set_ae_lock");
				push_set_ae_lock = false;
				push_set_ae_lock_id = null;
				camera_settings.setAutoExposureLock(previewBuilder);
				try {
					setRepeatingRequest();
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to set ae lock [from ae lock hack]");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
				} 
			}
			
			if( request.getTag() == RequestTag.CAPTURE ) {
				if( MyDebug.LOG )
					Log.d(TAG, "capture request completed");


				previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
				camera_settings.setAEMode(previewBuilder, false);

				try {
		             {
		            	capture();
		            }
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to cancel autofocus after taking photo");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
				}
				try {
					setRepeatingRequest();
				}
				catch(CameraAccessException e) {
					if( MyDebug.LOG ) {
						Log.e(TAG, "failed to start preview after taking photo");
						Log.e(TAG, "reason: " + e.getReason());
						Log.e(TAG, "message: " + e.getMessage());
					}
					e.printStackTrace();
					preview_error_cb.onError();
				}
			}
		}
	};
}
