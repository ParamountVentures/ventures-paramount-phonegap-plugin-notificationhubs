package ventures.paramount.phonegap.notificationhub;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.microsoft.windowsazure.messaging.NativeRegistration;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.String;
import java.util.Iterator;

public class NotificationHubPlugin extends CordovaPlugin {
    public static final String VENTURES_PARAMOUNT_PHONEGAP_NOTIFICATIONHUB = "ventures.paramount.phonegap.notificationhub";

    public static final String LOG_TAG = "NotificationHubPlugin";

    public static final String INITIALIZE = "init";
    public static final String UNREGISTER = "unregister";
    public static final String EXIT = "exit";

    private static CallbackContext pushContext;
    private static CordovaWebView gWebView;
    private static String gSenderID;
    private static Bundle gCachedExtras = null;
    private static boolean gForeground = false;
    private static String gCallback = null;

    /**
     * Gets the application context from cordova's main activity.
     * @return the application context
     */
    private Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {

        boolean result = false;
        String hubName = null;
        String connectionString = null;
        String[] tags = null;

        Log.v(LOG_TAG, "execute: action=" + action);

        // this will initialize the session, registering and listening for notifications
        if (INITIALIZE.equals(action)) {
            pushContext = callbackContext;
            JSONObject jo = null;

            Log.v(LOG_TAG, "execute: data=" + data.toString());

            try {
                jo = data.getJSONObject(3).getJSONObject("android");

                hubName = data.getString(0);
                connectionString = data.getString(1);

                // get the tags
                if (data.getString(2) != null) {
                    tags = data.getString(2).substring(1, data.getString(2).length() - 1).replace("\"", "").split(",");
                }

                Log.v(LOG_TAG, "execute: notificationHubPath=" + hubName);
                Log.v(LOG_TAG, "execute: tags=" + data.getString(2));
                Log.v(LOG_TAG, "execute: connectionString=" + connectionString);

                // get an instance of the messaging cloud
                final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(cordova.getActivity());

                // get an instance of the azure hub
                final com.microsoft.windowsazure.messaging.NotificationHub hub =
                        new com.microsoft.windowsazure.messaging.NotificationHub(hubName, connectionString, cordova.getActivity());

                gWebView = this.webView;
                Log.v(LOG_TAG, "execute: jo=" + jo.toString());

                gSenderID = jo.getString("senderID");

                Log.v(LOG_TAG, "execute: senderID=" + gSenderID);

                // performs the actual registration
                //GCMRegistrar.register(getApplicationContext(), gSenderID);
                String gcmId = gcm.register(gSenderID);
                Log.v(LOG_TAG, "gcmId=" + gcmId);

                // register with the hub
                NativeRegistration registrationInfo = hub.register(gcmId, tags);
                Log.v(LOG_TAG, "gcmId..getRegistrationId()=" + registrationInfo.getRegistrationId());
                Log.v(LOG_TAG, "gcmId..getGCMRegistrationId()=" + registrationInfo.getGCMRegistrationId());

                result = true;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "execute: Got JSON Exception " + e.getMessage());
                result = false;
                callbackContext.error(e.getMessage());
            } catch (Exception e) {
                Log.e(LOG_TAG, "execute: Got General Exception " + e.getMessage());
                result = false;
                callbackContext.error(e.getMessage());
            }

            if (jo != null) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(VENTURES_PARAMOUNT_PHONEGAP_NOTIFICATIONHUB, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                try {
                    editor.putString("icon", jo.getString("icon"));
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "no icon option");
                }
                try {
                    editor.putString("iconColor", jo.getString("iconColor"));
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "no iconColor option");
                }
                editor.putBoolean("sound", jo.optBoolean("sound", true));
                editor.putBoolean("vibrate", jo.optBoolean("vibrate", true));
                editor.putBoolean("clearNotifications", jo.optBoolean("clearNotifications", true));
                editor.commit();
            }

            if (gCachedExtras != null) {
                Log.v(LOG_TAG, "sending cached extras");
                sendExtras(gCachedExtras);
                gCachedExtras = null;
            }

        } else if (UNREGISTER.equals(action)) {

            JSONObject jo = null;
            try {
                jo = data.getJSONObject(2).getJSONObject("android");
                hubName = data.getString(0);
                connectionString = data.getString(1);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "execute: Got JSON Exception " + e.getMessage());
                result = false;
                callbackContext.error(e.getMessage());
            }

            final com.microsoft.windowsazure.messaging.NotificationHub hub =
                    new com.microsoft.windowsazure.messaging.NotificationHub(hubName, connectionString, cordova.getActivity());

            // get an instance of the messaging cloud
            //final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(cordova.getActivity());

            //GCMRegistrar.unregister(getApplicationContext());
            try {
                hub.unregister();
            } catch (Exception e) {
                Log.e(LOG_TAG, "execute: Got General Exception " + e.getMessage());
                result = false;
                callbackContext.error(e.getMessage());
            }

            Log.v(LOG_TAG, "UNREGISTER");
            result = true;
            callbackContext.success();
        } else {
            result = false;
            Log.e(LOG_TAG, "Invalid action : " + action);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
        }

        return result;
    }

    public static void sendEvent(JSONObject _json) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, _json);
        pluginResult.setKeepCallback(true);
        pushContext.sendPluginResult(pluginResult);
    }

    public static void sendError(String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        pushContext.sendPluginResult(pluginResult);
    }

    /*
     * Sends the pushbundle extras to the client application.
     * If the client application isn't currently active, it is cached for later processing.
     */
    public static void sendExtras(Bundle extras) {
        if (extras != null) {
            if (gWebView != null) {
                sendEvent(convertBundleToJson(extras));
            } else {
                Log.v(LOG_TAG, "sendExtras: caching extras to send at a later time.");
                gCachedExtras = extras;
            }
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gForeground = true;
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        gForeground = false;

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(NotificationHubPlugin.VENTURES_PARAMOUNT_PHONEGAP_NOTIFICATIONHUB, Context.MODE_PRIVATE);
        if (prefs.getBoolean("clearNotifications", true)) {
            final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        gForeground = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gForeground = false;
        gWebView = null;
    }

    /*
     * serializes a bundle to JSON.
     */
    private static JSONObject convertBundleToJson(Bundle extras) {
        try {
            JSONObject json = new JSONObject();
            JSONObject additionalData = new JSONObject();
            Iterator<String> it = extras.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = extras.get(key);
                 
                Log.d(LOG_TAG, "key = " + key);

                // System data from Android
                if (key.equals("from") || key.equals("collapse_key")) {
                    additionalData.put(key, value);
                }
                else if (key.equals("foreground")) {
                    additionalData.put(key, extras.getBoolean("foreground"));
                }
                else if (key.equals("coldstart")){
                    additionalData.put(key, extras.getBoolean("coldstart"));
                } else if (key.equals("message") || key.equals("body") ||
                        key.equals("gcm.notification.message") || 
                        key.equals("gcm.notification.body")) {
                    json.put("message", value);
                } else if (key.equals("title") || key.equals("gcm.notification.title")) {
                    json.put("title", value);
                } else if (key.equals("msgcnt") || key.equals("badge") ||
                           key.equals("gcm.notification.msgcnt") || 
                           key.equals("gcm.notification.badge")) {
                    json.put("count", value);
                } else if (key.equals("soundname") || key.equals("sound") ||
                           key.equals("gcm.notification.soundname") || 
                           key.equals("gcm.notification.sound")) {
                    json.put("sound", value);
                } else if (key.equals("image") || key.equals("gcm.notification.image")) {
                    json.put("image", value);
                } else if (key.equals("callback")) {
                    json.put("callback", value);
                }
                else if ( value instanceof String ) {
                    String strValue = (String)value;
                    try {
                        // Try to figure out if the value is another JSON object
                        if (strValue.startsWith("{")) {
                            additionalData.put(key, new JSONObject(strValue));
                        }
                        // Try to figure out if the value is another JSON array
                        else if (strValue.startsWith("[")) {
                            additionalData.put(key, new JSONArray(strValue));
                        }
                        else {
                            additionalData.put(key, value);
                        }                       
                    } catch (Exception e) {
                        additionalData.put(key, value);
                    }
                }
            } // while
            
            json.put("additionalData", additionalData);
            Log.v(LOG_TAG, "extrasToJSON: " + json.toString());

            return json;
        }
        catch( JSONException e) {
            Log.e(LOG_TAG, "extrasToJSON: JSON exception");
        }
        return null;
    }

    public static boolean isInForeground() {
      return gForeground;
    }

    public static boolean isActive() {
        return gWebView != null;
    }
}