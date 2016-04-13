package com.glacier.cameralib;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class MyWidgetProviderTakePhoto extends AppWidgetProvider {
	private static final String TAG = "MyWidgetProviderTakePhoto";
	

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

            Intent intent = new Intent(context, TakePhoto.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);



            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_take_photo);
            views.setOnClickPendingIntent(R.id.widget_take_photo, pendingIntent);


            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


}
