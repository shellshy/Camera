package com.glacier.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.glacier.camera.CameraController.CameraController;
import com.glacier.camera.CameraController.CameraControllerException;
import com.glacier.camera.Preview.Preview;
import com.glacier.camera.Util.CamUtil;

import java.util.List;

public class TestActivity extends Activity {

    private Camera camera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Button btnStart = (Button) findViewById(R.id.btnStart);
        CameraController controller = null;
        try {
            controller = CamUtil.getCameraController(TestActivity.this);
            CameraController.CameraFeatures camera_features = controller.getCameraFeatures();
            List<CameraController.Size> view_sizes = camera_features.video_sizes;
            for (CameraController.Size size : view_sizes) {
                Log.i("camera video size", size.height + " " + size.width);
            }
            CamUtil.setVideoResolution(TestActivity.this,2);
            CameraController.SupportedValues iso = controller.setISO("1600");
            List<String> iso_values = iso.values;
            for (String i : iso_values) {
                Log.i("camera iso", i);
            }
            CamUtil.setIso(TestActivity.this,"800");
            //最小曝光补尝
            Log.i("camera min_exposure",camera_features.min_exposure+"");
            //最大曝光补尝
            Log.i("camera max_exposure",camera_features.max_exposure+"");
            CamUtil.setExposureCompensation(TestActivity.this,-2);
            //是否支持锁定曝光
            Log.i("camera exposure lock",camera_features.is_exposure_lock_supported+"");
            //拍照时长
            CamUtil.setVideoDuration(TestActivity.this,10);
        } catch (CameraControllerException e) {
            e.printStackTrace();
        }
        if (controller != null) {
            controller.release();
        }
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //auto_run为自动开始拍摄,auto_lock为自动锁定曝光
                startActivity(new Intent(TestActivity.this, MainActivity.class).putExtra("auto_run",true).putExtra("auto_lock",true));
            }
        });
    }
}
