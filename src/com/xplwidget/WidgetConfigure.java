
package com.xplwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.xplwidget.R;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
//import com.example.android.apis.R;

/**
 * The configuration screen for the xPL widget.
 */
public class WidgetConfigure extends Activity {
    static final String TAG = "com.xplwidget";

    private static final String PREFS_NAME
            = "com.xplwidget";
    
    //we need a mapping to keep the config options straight across changes of the R.string.blah values.
    static Map<Integer,Integer> prefmap;
    
    
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText mWidgetNameText;
    EditText mWidgetMessageText;

    
    /**
     * This exists to map the R.string IDs to a consistent number, as the R.string ID's may shift between versions.
     * We do this so we can look up the configurable values by their default values from R.string.
     * @return a int,int map, keyed to the R.string IDs.
     */
    public static Map<Integer, Integer> getPrefmap() {
    	//we face a problem that prefmap may not always be initialized, so we have to get it this way, and init if needed
    	if (prefmap == null){
            prefmap = new HashMap<Integer,Integer>();
            //Log.d(TAG, "pref put ID : \"" + R.string.configure_default_message);
            prefmap.put(R.string.configure_default_message,1);
            //Log.d(TAG, "pref put ID : \"" + R.string.configure_default_name);
            prefmap.put(R.string.configure_default_name,2);
            //Log.d(TAG, "prefmap was null");
    	}
    	return prefmap;
    }
    
    public WidgetConfigure() {
        super();
        //initialize the prefmap
        getPrefmap();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.widget_configure);

        // Find the text boxes
        mWidgetNameText = (EditText)findViewById(R.id.configure_name);
        mWidgetMessageText = (EditText)findViewById(R.id.configure_message);

        // Bind the action for the save and clear buttons.
        findViewById(R.id.save_button).setOnClickListener(mOnClickListener);
        findViewById(R.id.clear_button).setOnClickListener(mOnClickListener);


        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

       	// set the textboxes' text with value from the stored preferences, but using the default values from R.string 
        mWidgetNameText.setText(loadPref(WidgetConfigure.this, mAppWidgetId, R.string.configure_default_name));
        mWidgetMessageText.setText(loadPref(WidgetConfigure.this, mAppWidgetId, R.string.configure_default_message));
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = WidgetConfigure.this;
            
            if(v.equals(findViewById(R.id.save_button))) {
            	 // When the save button is clicked, save the string in our prefs and return that they
                // clicked OK.
                String wname = mWidgetNameText.getText().toString();
                savePref(context, mAppWidgetId, R.string.configure_default_name, wname);
                //Log.d(TAG, "saving name" + wname);
                String wmessage = mWidgetMessageText.getText().toString();
                savePref(context, mAppWidgetId, R.string.configure_default_message, wmessage);
                //Log.d(TAG, "saving msg" + wmessage);

                // Push widget update to surface with newly set prefix
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                WidgetProvider.updateAppWidget(context, appWidgetManager,
                        mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);

                finish();
            	
            } else if (v.equals(findViewById(R.id.clear_button))){
            	// When the clear button is clicked, wipe the text fields
                mWidgetNameText.setText("");
                mWidgetMessageText.setText("");
            	
            } else {
            	Log.e(TAG, "unknown button press?");
            }
           
        }
    };

    /**
     * Save a string to the SharedPreferences object, keyed by the stringID and widget 
     * @param context - passed in
     * @param appWidgetId - which widget we're saving for
     * @param resID - the value from R.string that we'll use to key the string, and used to get a default value
     * @param value - the string value we want to store
     */
    static void savePref(Context context, int appWidgetId, int resID, String value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        //Log.d(TAG, "pref save ID : \"" + resID + " to " + prefmap);
        Integer key = getPrefmap().get(resID);
        if(key==null){
        	//don't have a mapping for that
        	return;
        }
        
        prefs.putString(appWidgetId + "_" + key, value);
        //Log.d(TAG, "pref save: \"" + appWidgetId + "_" + resID + "\" is \"" + value +  "\"");
        prefs.commit();
    }

    /**
     * @param context - passed in
     * @param appWidgetId - which widget we're loading for
     * @param resID - the value from R.string that we'll use to key the string, and used to get a default value
     * @return the string value we loaded, or a default value from  R.string 
     */
    static String loadPref(Context context, int appWidgetId, int resID) {
    	//Log.d(TAG, "pref load ID : \"" + resID + " from " + prefmap);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        
        Integer key = getPrefmap().get(resID);
        if(key==null){
        	//don't have a mapping for that
        	return "";
        }
        
        // get setting by <widgetid>_<resourceid>
        String value = prefs.getString(appWidgetId + "_" + key, 
        		context.getResources().getString(resID));
        
        value.replaceAll("\\n", "\n");
        //Log.d(TAG, "pref load: \"" + appWidgetId + "_" + key+ "\" is \"" + value +  "\"");
        return value;
        
    }

    /**
     * Deletes all preference for a given widgetID 
     * @param context - passed in
     * @param appWidgetId - to delete for
     */
    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        ArrayList<String> toDelete = new ArrayList<String>();
        
        //make a list of all preferences that are for that widget
        Map<String, ?> all = prefs.getAll();
        Iterator<String> sit = all.keySet().iterator();
        while(sit.hasNext()) {
        	String s = sit.next();
        	if (s.contains(Integer.toString(appWidgetId) + "_")) {
        		toDelete.add(s);
        	}
        }
        
        //get an editor and delete those prefs
        sit = toDelete.iterator();
        Editor edit = prefs.edit();
        while(sit.hasNext()) {
        	String s = sit.next();
        	//Log.d(TAG, "pref delete: \"" + s);
            edit.remove(s);
        }
        edit.commit();
    }

    static void loadAllTitlePrefs(Context context, ArrayList<Integer> appWidgetIds,
            ArrayList<String> texts) {
    	
    }
}



