package com.glacier.camera;

import com.glacier.camera.Preview.Preview;
import com.glacier.camera.UI.FolderChooserDialog;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;


public class MyPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    private static final String TAG = "MyPreferenceFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (MyDebug.LOG)
            Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final Bundle bundle = getArguments();
        final int cameraId = bundle.getInt("cameraId");
        if (MyDebug.LOG)
            Log.d(TAG, "cameraId: " + cameraId);

        final String camera_api = bundle.getString("camera_api");

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        final boolean supports_auto_stabilise = bundle.getBoolean("supports_auto_stabilise");
        if (MyDebug.LOG)
            Log.d(TAG, "supports_auto_stabilise: " + supports_auto_stabilise);








        final boolean supports_face_detection = bundle.getBoolean("supports_face_detection");
        if (MyDebug.LOG)
            Log.d(TAG, "supports_face_detection: " + supports_face_detection);

        if (!supports_face_detection) {
            Preference pref = findPreference("preference_face_detection");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_category_camera_effects");
            pg.removePreference(pref);
        }

        final int preview_width = bundle.getInt("preview_width");
        final int preview_height = bundle.getInt("preview_height");
        final int[] preview_widths = bundle.getIntArray("preview_widths");
        final int[] preview_heights = bundle.getIntArray("preview_heights");
        final int[] video_widths = bundle.getIntArray("video_widths");
        final int[] video_heights = bundle.getIntArray("video_heights");

        final int resolution_width = bundle.getInt("resolution_width");
        final int resolution_height = bundle.getInt("resolution_height");
        final int[] widths = bundle.getIntArray("resolution_widths");
        final int[] heights = bundle.getIntArray("resolution_heights");
        if (widths != null && heights != null) {
            CharSequence[] entries = new CharSequence[widths.length];
            CharSequence[] values = new CharSequence[widths.length];
            for (int i = 0; i < widths.length; i++) {
                entries[i] = widths[i] + " x " + heights[i] + " " + Preview.getAspectRatioMPString(widths[i], heights[i]);
                values[i] = widths[i] + " " + heights[i];
            }
            ListPreference lp = (ListPreference) findPreference("preference_resolution");
            lp.setEntries(entries);
            lp.setEntryValues(values);
            String resolution_preference_key = PreferenceKeys.getResolutionPreferenceKey(cameraId);
            String resolution_value = sharedPreferences.getString(resolution_preference_key, "");
            if (MyDebug.LOG)
                Log.d(TAG, "resolution_value: " + resolution_value);
            lp.setValue(resolution_value);

            lp.setKey(resolution_preference_key);
        } else {
            Preference pref = findPreference("preference_resolution");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_photo_settings");
            pg.removePreference(pref);
        }

        {
            final int n_quality = 100;
            CharSequence[] entries = new CharSequence[n_quality];
            CharSequence[] values = new CharSequence[n_quality];
            for (int i = 0; i < n_quality; i++) {
                entries[i] = "" + (i + 1) + "%";
                values[i] = "" + (i + 1);
            }
            ListPreference lp = (ListPreference) findPreference("preference_quality");
            lp.setEntries(entries);
            lp.setEntryValues(values);
        }

        final String[] video_quality = bundle.getStringArray("video_quality");
        final String[] video_quality_string = bundle.getStringArray("video_quality_string");
        if (video_quality != null && video_quality_string != null) {
            CharSequence[] entries = new CharSequence[video_quality.length];
            CharSequence[] values = new CharSequence[video_quality.length];
            for (int i = 0; i < video_quality.length; i++) {
                entries[i] = video_quality_string[i];
                values[i] = video_quality[i];
            }
            ListPreference lp = (ListPreference) findPreference("preference_video_quality");
            lp.setEntries(entries);
            lp.setEntryValues(values);
            String video_quality_preference_key = PreferenceKeys.getVideoQualityPreferenceKey(cameraId);
            String video_quality_value = sharedPreferences.getString(video_quality_preference_key, "");
            if (MyDebug.LOG)
                Log.d(TAG, "video_quality_value: " + video_quality_value);
            lp.setValue(video_quality_value);

            lp.setKey(video_quality_preference_key);
        } else {
            Preference pref = findPreference("preference_video_quality");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_video_settings");
            pg.removePreference(pref);
        }
        final String current_video_quality = bundle.getString("current_video_quality");
        final int video_frame_width = bundle.getInt("video_frame_width");
        final int video_frame_height = bundle.getInt("video_frame_height");
        final int video_bit_rate = bundle.getInt("video_bit_rate");
        final int video_frame_rate = bundle.getInt("video_frame_rate");

        final boolean supports_force_video_4k = bundle.getBoolean("supports_force_video_4k");
        if (MyDebug.LOG)
            Log.d(TAG, "supports_force_video_4k: " + supports_force_video_4k);
        if (!supports_force_video_4k || video_quality == null || video_quality_string == null) {
            Preference pref = findPreference("preference_force_video_4k");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_video_settings");
            pg.removePreference(pref);
        }

        final boolean supports_video_stabilization = bundle.getBoolean("supports_video_stabilization");
        if (MyDebug.LOG)
            Log.d(TAG, "supports_video_stabilization: " + supports_video_stabilization);
        if (!supports_video_stabilization) {
            Preference pref = findPreference("preference_video_stabilization");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_video_settings");
            pg.removePreference(pref);
        }

        final boolean can_disable_shutter_sound = bundle.getBoolean("can_disable_shutter_sound");
        if (MyDebug.LOG)
            Log.d(TAG, "can_disable_shutter_sound: " + can_disable_shutter_sound);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !can_disable_shutter_sound) {

            Preference pref = findPreference("preference_shutter_sound");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_camera_controls_more");
            pg.removePreference(pref);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {

            Preference pref = findPreference("preference_immersive_mode");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_gui");
            pg.removePreference(pref);
        }

        final boolean using_android_l = bundle.getBoolean("using_android_l");
        if (!using_android_l) {
            Preference pref = findPreference("preference_show_iso");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_gui");
            pg.removePreference(pref);
        }

        final boolean supports_camera2 = bundle.getBoolean("supports_camera2");
        if (MyDebug.LOG)
            Log.d(TAG, "supports_camera2: " + supports_camera2);
        if (supports_camera2) {
            final Preference pref = findPreference("preference_use_camera2");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (pref.getKey().equals("preference_use_camera2")) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "user clicked camera2 API - need to restart");

                        Intent i = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        return false;
                    }
                    return false;
                }
            });
        }


        {
            Preference pref = findPreference("preference_save_location");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "clicked save location");
                    MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
                    if (main_activity.getStorageUtils().isUsingSAF()) {
                        main_activity.openFolderChooserDialogSAF();
                        return true;
                    } else {
                        FolderChooserDialog fragment = new FolderChooserDialog();
                        fragment.show(getFragmentManager(), "FOLDER_FRAGMENT");
                        return true;
                    }
                }
            });
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Preference pref = findPreference("preference_using_saf");
            PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_camera_controls_more");
            pg.removePreference(pref);
        } else {
            final Preference pref = findPreference("preference_using_saf");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (pref.getKey().equals("preference_using_saf")) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "user clicked saf");
                        if (sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false)) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "saf is now enabled");



                            {
                                MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
                                Toast.makeText(main_activity, R.string.saf_select_save_location, Toast.LENGTH_SHORT).show();
                                main_activity.openFolderChooserDialogSAF();
                            }
                        } else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "saf is now disabled");
                        }
                    }
                    return false;
                }
            });
        }

    }


    public void onResume() {
        super.onResume();




        TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground
        });
        int backgroundColor = array.getColor(0, Color.BLACK);

        getView().setBackgroundColor(backgroundColor);
        array.recycle();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void onPause() {
        super.onPause();
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSharedPreferenceChanged");
        Preference pref = findPreference(key);
        if (pref instanceof CheckBoxPreference) {
            CheckBoxPreference checkBoxPref = (CheckBoxPreference) pref;
            checkBoxPref.setChecked(prefs.getBoolean(key, true));
        }
    }
}
