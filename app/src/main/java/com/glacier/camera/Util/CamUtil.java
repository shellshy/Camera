package com.glacier.camera.Util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.glacier.camera.CameraController.CameraController;
import com.glacier.camera.CameraController.CameraController1;
import com.glacier.camera.CameraController.CameraController2;
import com.glacier.camera.CameraController.CameraControllerException;
import com.glacier.camera.CameraController.CameraControllerManager;
import com.glacier.camera.CameraController.CameraControllerManager1;
import com.glacier.camera.CameraController.CameraControllerManager2;
import com.glacier.camera.PreferenceKeys;

/**
 * Created by shy on 2016/4/13.
 */
public class CamUtil {

    private static boolean supports_camera2 = false;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean supportCamera2(Context context) {
        supports_camera2 = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraControllerManager2 manager2 = new CameraControllerManager2(context);
            supports_camera2 = true;
            if (manager2.getNumberOfCameras() == 0) {
                supports_camera2 = false;
            }
            for (int i = 0; i < manager2.getNumberOfCameras() && supports_camera2; i++) {
                if (!manager2.allowCamera2Support(i)) {
                    supports_camera2 = false;
                }
            }
        }
        return supports_camera2;
    }

    public static CameraControllerManager getCameraManager(Context context) {
        CameraControllerManager manager = null;
        if (supportCamera2(context)) {
            manager = new CameraControllerManager2(context);
        } else {
            manager = new CameraControllerManager1();
        }
        return manager;
    }

    public static CameraController getCameraController(Context context) throws CameraControllerException {
        CameraController controller = null;
        if (supportCamera2(context)) {
            controller = new CameraController2(context, Camera.CameraInfo.CAMERA_FACING_BACK, null);
        } else {
            controller = new CameraController1(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        return controller;
    }

    /**
     * 设置视频分辨率
     *
     * @param context
     * @param i
     */
    public static void setVideoResolution(Context context, int i) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getVideoQualityPreferenceKey(Camera.CameraInfo.CAMERA_FACING_BACK), String.valueOf(i));
        editor.apply();
    }

    /**
     * 设置iso值
     *
     * @param context
     * @param iso
     */
    public static void setIso(Context context, String iso) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getISOPreferenceKey(), iso);
        editor.apply();
    }

    /**
     * 设置曝光补尝
     *
     * @param context
     * @param exposure
     */
    public static void setExposureCompensation(Context context, int exposure) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getExposurePreferenceKey(), "" + exposure);
        editor.apply();
    }

    /**
     * 设置时间
     * @param context
     * @param time
     */
    public static void setVideoDuration(Context context, int time) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), String.valueOf(time)).commit();
    }
}
