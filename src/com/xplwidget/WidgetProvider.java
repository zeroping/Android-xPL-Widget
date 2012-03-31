
package com.xplwidget;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.xplwidget.R;

import com.xpl.XplSenderService;

/**
 * A widget provider for sending xPL commands.  We have two strings that we pull from a preference in order
 * to get a name to show, and to get the actual xPL message to send.
 */
public class WidgetProvider extends AppWidgetProvider {
    // log tag
    //private static final String TAG = "com.xplwidget";
    public static String ACTION_PRESS = "pressed";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	super.onUpdate(context, appWidgetManager, appWidgetIds);	
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            //String titlePrefix = WidgetConfigure.loadTitlePref(context, appWidgetId);

            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        
        
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            WidgetConfigure.deleteTitlePref(context, appWidgetIds[i]);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	if (ACTION_PRESS.equals(intent.getAction())) {
            int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Bundle extras = intent.getExtras();
            if (extras != null) {
            	mAppWidgetId = extras.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID);
            } else {
            	//Log.d(TAG,"no extras!");
            }
    		
            String toSend = WidgetConfigure.loadPref(context, mAppWidgetId, R.string.configure_default_message) ;
    		//Log.d(TAG, "press received from: " + mAppWidgetId + " , need to perform: " + toSend);
    		
            Intent intent2 = new Intent(context.getApplicationContext(), XplSenderService.class);
            intent2.putExtra(XplSenderService.EXTRA_XPLMESSAGE, toSend);
            context.getApplicationContext().startService(intent2);
           
    	} else {
    		super.onReceive(context, intent);
    		
    	}
    }
    
    

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        //Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " titlePrefix=" + titlePrefix);
        // Getting the string this way allows the string to be localized.  The format
        // string is filled in using java.util.Formatter-style format strings.
        String text = WidgetConfigure.loadPref(context, appWidgetId, R.string.configure_default_name);
                
        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_provider);
        views.setTextViewText(R.id.appwidget_text, text);

        //set up our click listener
        Intent intent = new Intent(context, WidgetProvider.class);
        intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setAction(ACTION_PRESS);
        //record the appID
        //Log.d(TAG, "setting widgetID to " + appWidgetId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);

        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        //Log.d(TAG, "views: " + views.toString());
        
        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    
    

    
   
}


