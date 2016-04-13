package com.glacier.camera;

import com.glacier.camera.MyDebug;
import com.glacier.camera.MainActivity;
import com.glacier.camera.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class MyWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "MyWidgetProvider";
	

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	if( MyDebug.LOG )
    		Log.d(TAG, "onUpdate");
        final int N = appWidgetIds.length;
    	if( MyDebug.LOG )
    		Log.d(TAG, "N = " + N);


        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
        	if( MyDebug.LOG )
        		Log.d(TAG, "appWidgetId: " + appWidgetId);

            PendingIntent pendingIntent = null;

			 {

	            Intent intent = new Intent(context, MainActivity.class);
	            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			}



            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget_launch_open_camera, pendingIntent);



            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


}
