package com.albaz.appuser.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.albaz.appuser.base.ServiceActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.albaz.appuser.App;
import com.albaz.appuser.helper.ConnectionHelper;
import com.albaz.appuser.helper.SharedHelper;
import com.albaz.appuser.helper.URLHelper;
import com.albaz.appuser.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.albaz.appuser.App.trimMessage;


public class SplashScreen extends ServiceActivity {

    String TAG = "SplashActivity";
    public Activity activity = SplashScreen.this;
    public Context context = SplashScreen.this;
    ConnectionHelper helper;
    Boolean isInternet;
    String device_token, device_UDID;
    Handler handleCheckStatus;
    int retryCount = 0;
    AlertDialog alert;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        helper = new ConnectionHelper(context);
        isInternet = helper.isConnectingToInternet();

//        // Set Default Value "manual" Login_by
//        SharedHelper.putKey(SplashScreen.this, "login_by","manual");

        //check status every 3 sec
        handleCheckStatus = new Handler();

        // Check SDK Environment & Set Attributes
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Check Status LogIn Or Not After 3Second
        handleCheckStatus.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "Handler Called");
                if (helper.isConnectingToInternet()) {
                    if (SharedHelper.getKey(context,"loggedIn").equalsIgnoreCase(getString(R.string.True))) {
                            // Invoke Method To Retrieve All Data App in SharedPrefaced
                            getDetails();
                            GetToken();
                            getProfile();
                    } else {
                        GoToBeginActivity();
                        // Invoke Method To Retrieve All Data App in SharedPrefaced
                        getDetails();
                    }
                    if(alert != null && alert.isShowing()){
                        alert.dismiss();
                    }
                }else{
                    showDialog();
                    handleCheckStatus.postDelayed(this, 3000);
                }
            }
        }, 3000);

    }

    // Method Getting Profile
    public void getProfile() {

           retryCount++;
           Log.e(TAG,""+URLHelper.UserProfile+"?device_type=android&device_id="+device_UDID+"&device_token="+device_token);
            JSONObject object = new JSONObject();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URLHelper.UserProfile+"?device_type=android&device_id="+device_UDID+"&device_token="+device_token, object , new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.w(TAG,"GetProfile"+response.toString());
                    SharedHelper.putKey(context, "id", response.optString("id"));
                    SharedHelper.putKey(context, "first_name", response.optString("first_name"));
                    SharedHelper.putKey(context, "last_name", response.optString("last_name"));
                    SharedHelper.putKey(context, "email", response.optString("email"));
                    SharedHelper.putKey(context, "rating", response.optString("rating"));
                    //Toast.makeText(activity, SharedHelper.getKey(context,"login_by"), Toast.LENGTH_SHORT).show();
                    if(SharedHelper.getKey(context,"login_by").equals("facebook")|| SharedHelper.getKey(context,"login_by").equals("google") ) {
                        if (response.optString("picture").startsWith("http"))
                            SharedHelper.putKey(context, "picture", response.optString("picture"));
                    } else {
                        SharedHelper.putKey(context, "picture", URLHelper.base_pic + response.optString("picture"));
                    }

                    Toast.makeText(activity,  response.optString("booking_id"), Toast.LENGTH_SHORT).show();

                    SharedHelper.putKey(context, "gender", response.optString("gender"));
                    SharedHelper.putKey(context, "mobile", response.optString("mobile"));
                    SharedHelper.putKey(context, "wallet_balance", response.optString("wallet_balance"));
                    SharedHelper.putKey(context, "payment_mode", response.optString("payment_mode"));
                    if(!response.optString("currency").equalsIgnoreCase("") && response.optString("currency") != null)
                        SharedHelper.putKey(context, "currency",response.optString("currency"));
                    else
                        SharedHelper.putKey(context, "currency","$");
                    SharedHelper.putKey(context,"sos",response.optString("sos"));
                    Log.e(TAG, "onResponse: Sos Call" + response.optString("sos"));
                    SharedHelper.putKey(context,"loggedIn",getString(R.string.True));
                    GoToMainActivity();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (retryCount < 5){
                        getProfile();
                    }else{
                        GoToBeginActivity();
                    }
                    String json = null;
                    String Message;
                    NetworkResponse response = error.networkResponse;
                    Log.w(TAG,"SplashError in profile"+error);
                    Log.w(TAG,"SplashError in profile+ "+response);
                    if(response != null && response.data != null){

                        try {
                            JSONObject errorObj = new JSONObject(new String(response.data));

                            if(response.statusCode == 400 || response.statusCode == 405 || response.statusCode == 500){
                                try{
                                    displayMessage(errorObj.optString("message"));
                                }catch (Exception e){
                                    displayMessage(getString(R.string.something_went_wrong));
                                }
                            }else if(response.statusCode == 401){
                                refreshAccessToken();
                            }else if(response.statusCode == 422){

                                json = trimMessage(new String(response.data));
                                if(json !="" && json != null) {
                                    displayMessage(json);
                                }else{
                                    displayMessage(getString(R.string.please_try_again));
                                }

                            }else if(response.statusCode == 503){
                                displayMessage(getString(R.string.server_down));
                            }
                        }catch (Exception e){
                            displayMessage(getString(R.string.something_went_wrong));
                        }

                    }
                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("X-Requested-With", "XMLHttpRequest");
                    headers.put("Authorization",""+SharedHelper.getKey(context, "token_type")+" "+SharedHelper.getKey(context, "access_token"));
                    return headers;
                }
            };
        jsonObjectRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });

            App.getInstance().addToRequestQueue(jsonObjectRequest);

        }

    // Method Invoked When Destroy App
    @Override
    protected void onDestroy() {
        handleCheckStatus.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // Method Refresh Access Token
    private void refreshAccessToken() {

            JSONObject object = new JSONObject();
            try {

                object.put("grant_type", "refresh_token");
                object.put("client_id", URLHelper.client_id);
                object.put("client_secret", URLHelper.client_secret);
                object.put("refresh_token", SharedHelper.getKey(context, "refresh_token"));
                object.put("scope", "");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URLHelper.login, object, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    Log.v("SignUpResponse", response.toString());
                    SharedHelper.putKey(context, "access_token", response.optString("access_token"));
                    SharedHelper.putKey(context, "refresh_token", response.optString("refresh_token"));
                    SharedHelper.putKey(context, "token_type", response.optString("token_type"));
                    getProfile();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String json = null;
                    String Message;
                    NetworkResponse response = error.networkResponse;

                    if (response != null && response.data != null) {
                        SharedHelper.putKey(context,"loggedIn",getString(R.string.False));
                        GoToBeginActivity();
                    } else {
                        if (error instanceof NoConnectionError) {
                            displayMessage(getString(R.string.oops_connect_your_internet));
                        } else if (error instanceof NetworkError) {
                            displayMessage(getString(R.string.oops_connect_your_internet));
                        } else if (error instanceof TimeoutError) {
                            refreshAccessToken();
                        }
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("X-Requested-With", "XMLHttpRequest");
                    return headers;
                }
            };

            App.getInstance().addToRequestQueue(jsonObjectRequest);
    }

    // Method Invoked When App Pause
    @Override
    protected void onPause() {
        super.onPause();
    }

    // Method GoToMainActivity
    public void GoToMainActivity(){
        Intent mainIntent = new Intent(activity, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        activity.finish();
    }

    // Method GoToBeginActivity
    public void GoToBeginActivity(){
        Intent mainIntent = new Intent(activity, BeginScreen.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        activity.finish();
    }

    // Show Toast Message
    public void displayMessage(String toastString){
        Log.e("displayMessage",""+toastString);
        Toast.makeText(activity, toastString, Toast.LENGTH_SHORT).show();
    }

    // Method Getting Token
    public void GetToken() {
        try {
            if(!SharedHelper.getKey(context,"device_token").equals("") && SharedHelper.getKey(context,"device_token") != null) {
                device_token = SharedHelper.getKey(context, "device_token");
                Log.i(TAG, "GCM Registration Token: " + device_token);
            }else{
                device_token = "COULD NOT GET FCM TOKEN";
                Log.i(TAG, "Failed to complete token refresh: " + device_token);
            }
        }catch (Exception e) {
            device_token = "COULD NOT GET FCM TOKEN";
            Log.d(TAG, "Failed to complete token refresh", e);
        }

        try {
            device_UDID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            //Toast.makeText(activity, "device_UDID : " + device_UDID, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Device UDID:" + device_UDID);
        }catch (Exception e) {
            device_UDID = "COULD NOT GET UDID";
            e.printStackTrace();
            Log.d(TAG, "Failed to complete device UDID");
        }
    }

    // Alert When No have Internet
    private void showDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.connect_to_network))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.connect_to_internet), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.quit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        if(alert == null){
            alert = builder.create();
            alert.show();
        }
    }

    // Get All Details App And Profile User
    void getDetails() {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URLHelper.appDetails, new Response.Listener<JSONObject>() {

             String App_Name = null,
                    App_Icon = null,
                    App_Logo = null,
                    App_Splash = null,
                    App_Status = null,
                    App_Msg = null,
                    Phone_Number = null,
                    Email = null,
                    Interval_Time = null;

            @Override
            public void onResponse(JSONObject response) {

                try {
                    App_Name = response.getString("App_Name");
                    App_Icon = response.getString("App_Icon");
                    App_Logo = response.getString("App_Logo");
                    App_Splash = response.getString("App_Splash");
                    App_Status = response.getString("App_Status");
                    App_Msg = response.getString("App_Msg");
                    Phone_Number = response.getString("Phone_Number");
                    Email = response.getString("Email");
                    Interval_Time = response.getString("Interval_Time");

                    // Set Currency & SOS
                    if(!response.optString("Currency").equalsIgnoreCase("") && response.optString("Currency") != null)
                        SharedHelper.putKey(context, "currency",response.optString("Currency"));
                    else
                        SharedHelper.putKey(context, "currency","$");
                    SharedHelper.putKey(context,"sos",response.optString("Sos"));

                    SharedPreferences sharedPreferences =getSharedPreferences("app_details", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("App_Name", App_Name);
                    editor.putString("App_Icon", App_Icon);
                    editor.putString("App_Logo", App_Logo);
                    editor.putString("App_Splash", App_Splash);
                    editor.putString("App_Status", App_Status);
                    editor.putString("App_Msg", App_Msg);
                    editor.putString("Phone_Number", Phone_Number);
                    editor.putString("Email", Email);
                    editor.putString("Interval_Time", Interval_Time);
                    editor.apply();

                    // Show Message
                    if(App_Status.contains("0")) {

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                Intent intent = new Intent(getApplicationContext(),message.class);
                                intent.putExtra("message", App_Msg);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }, 500);


                    } else if(App_Status.contains("1")) {

                        if(App_Msg != null || !App_Msg.contains("")) {
                            Toast.makeText(activity, App_Msg, Toast.LENGTH_LONG).show();
                        }

                        return;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, "error:" + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayMessage(getString(R.string.please_try_again));
            }
        });

        App.getInstance().addToRequestQueue(jsonObjectRequest);

    }

}
