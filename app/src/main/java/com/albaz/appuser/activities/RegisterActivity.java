package com.albaz.appuser.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.albaz.appuser.helper.SharedHelper;
import com.albaz.appuser.helper.URLHelper;
import com.albaz.appuser.R;
import com.albaz.appuser.utils.Utilities;
import com.albaz.appuser.base.ServiceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.albaz.appuser.App.trimMessage;

import java.util.regex.Matcher;

public class RegisterActivity extends ServiceActivity {
    private static String TAG="REGISTERACTIVITY";

    private ImageView backArrow;
    private FloatingActionButton nextICON;
    private EditText email, first_name, last_name, password, connfirmpassword;
    private Boolean fromActivity = false;

    // Data Registration By Social
    private String firstNameS, lastNameS, emailS, LoginbyS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setLogTag("RegisterActivity");

        try {
            Intent intent = getIntent();
            if (intent != null) {
                if (getIntent().getExtras().getBoolean("isFromMailActivity")) {
                    fromActivity = true;
                } else if (!getIntent().getExtras().getBoolean("isFromMailActivity")) {
                    fromActivity = false;
                } else {
                    fromActivity = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fromActivity = false;
        }

        // Invoke Initialize Elements
        findViewById();

        // Getting Data From Activity SocialLogin
        Intent intentR = getIntent();
        if (intentR != null) {

            firstNameS = intentR.getStringExtra("firstName");
            lastNameS = intentR.getStringExtra("lastName");
            emailS = intentR.getStringExtra("email");
            LoginbyS = intentR.getStringExtra("Loginby");

            email.setText(emailS);
            first_name.setText(firstNameS);
            last_name.setText(lastNameS);

        }

        // Check Version SDK
        if (Build.VERSION.SDK_INT > 15) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Check All Fields By Btn Next
        nextICON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Pattern ps = Pattern.compile(".*[0-9].*");
                Matcher firstName = ps.matcher(first_name.getText().toString());
                Matcher lastName = ps.matcher(last_name.getText().toString());

                if (email.getText().toString().equalsIgnoreCase(getString(R.string.sample_mail_id))) {
                    displaySnackbar(getString(R.string.email_validation));
                } else if (first_name.getText().toString().equals("") || first_name.getText().toString().equalsIgnoreCase(getString(R.string.first_name))) {
                    displaySnackbar(getString(R.string.first_name_empty));
                } else if (firstName.matches()) {
                    displaySnackbar(getString(R.string.first_name_no_number));
                } else if (last_name.getText().toString().equals("") || last_name.getText().toString().equalsIgnoreCase(getString(R.string.last_name))) {
                    displaySnackbar(getString(R.string.last_name_empty));
                } else if (lastName.matches()) {
                    displaySnackbar(getString(R.string.last_name_no_number));
                } else if (password.getText().toString().equals("") || password.getText().toString().equalsIgnoreCase(getString(R.string.password_txt))) {
                    displaySnackbar(getString(R.string.password_validation));
                } else if (password.length() < 6) {
                    displaySnackbar(getString(R.string.password_size));
                }else if (!password.getText().toString().matches(connfirmpassword.getText().toString())) {                    displaySnackbar(getString(R.string.password_not_matched));
                } else {
                    if (hasInternet()) {
                        if(!SharedHelper.getKey(getApplicationContext(),"putMobile").equals("")) {
                            verifyPhone(SharedHelper.getKey(getApplicationContext(),"putMobile")); // Invoke Verify Method By Phone Number
                        } else {
                            verifyPhone(""); // Invoke Verify Method
                        }

                    } else {
                        displaySnackbar(getString(R.string.something_went_wrong_net));
                    }
                }
            }
        });

        // Cliked Back Btn
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        dismissLoading();
    }

    // Defined Elements
    public void findViewById() {
        email = (EditText) findViewById(R.id.email);
        first_name = (EditText) findViewById(R.id.first_name);
        last_name = (EditText) findViewById(R.id.last_name);
        //mobile_no = (EditText) findViewById(R.id.mobile_no);
        password = (EditText) findViewById(R.id.password);
        connfirmpassword = findViewById(R.id.connfirmpassword);
        nextICON = (FloatingActionButton) findViewById(R.id.nextIcon);
        backArrow = (ImageView) findViewById(R.id.backArrow);

        if (!fromActivity) {
            email.setText(SharedHelper.getKey(this, "email"));
        }
    }

    // Method Invoked When Sending Message Verified in the Phone
    @Override
    public void onPhoneVerified(String uid, String phone, String token) {
        /*if (AccountKit.getCurrentAccessToken().getToken() != null) {*/
        SharedHelper.putKey(RegisterActivity.this, "account_kit_token", token);
        SharedHelper.putKey(RegisterActivity.this, "mobile", phone); // Set Mobile
        registerAPI(); // Invoke Registered Method To Adding Data To DataBase And Set It In SharedPreference  & Invoke (signIn) & getProfile
    }

    // Method Registration With Sending Verification Message
    private void registerAPI() {
        showLoading();
        JSONObject object = new JSONObject();
        try {
            object.put("device_type", "android");
            object.put("device_id", getDeviceUDID());
            object.put("device_token", "" + getDeviceToken());
            object.put("first_name", first_name.getText().toString());
            object.put("last_name", last_name.getText().toString());
            object.put("email", email.getText().toString());
            object.put("password", password.getText().toString());
            object.put("mobile", SharedHelper.getKey(RegisterActivity.this, "mobile"));
            if(SharedHelper.getKey(RegisterActivity.this, "login_by").equals("facebook") || SharedHelper.getKey(RegisterActivity.this, "login_by").equals("google")) { // If Login With Social
                object.put("social_unique_id", SharedHelper.getKey(RegisterActivity.this, "id_social"));
                object.put("picture", SharedHelper.getKey(RegisterActivity.this, "picture"));
                object.put("login_by", SharedHelper.getKey(RegisterActivity.this, "login_by"));
                Toast.makeText(this, "in Social", Toast.LENGTH_SHORT).show();
            } else {
                object.put("login_by", "manual");
                Toast.makeText(this, "Out Social", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URLHelper.register, object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissLoading();
                Toast.makeText(RegisterActivity.this, "Registred Success", Toast.LENGTH_SHORT).show();
                SharedHelper.putKey(RegisterActivity.this, "email", email.getText().toString());
                SharedHelper.putKey(RegisterActivity.this, "password", password.getText().toString());
                signIn(); // Invoke Method SignIn
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissLoading();
                NetworkResponse response = error.networkResponse;

                if (response != null && response.data != null) {
                    try {
                        JSONObject errorObj = new JSONObject(new String(response.data));

                        if (response.statusCode == 400 || response.statusCode == 405 || response.statusCode == 500) {
                            try {
                                displaySnackbar(errorObj.optString("message"));
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }
                        } else if (response.statusCode == 401) {
                            try {
                                if (errorObj.optString("message").equalsIgnoreCase("invalid_token")) {
                                    //   Refresh token
                                } else {
                                    displaySnackbar(errorObj.optString("message"));
                                }
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }

                        } else {
                                displaySnackbar(getString(R.string.please_try_again));
                        }

                    } catch (Exception e) {
                        displaySnackbar(getString(R.string.something_went_wrong));
                    }

                } else {
                    if (error instanceof NoConnectionError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof NetworkError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof TimeoutError) {
                        registerAPI();
                    }
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("X-Requested-With", "XMLHttpRequest");
                headers.put("Authorization", SharedHelper.getKey(RegisterActivity.this, "access_token"));
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

    // Sign in Acc After Registration
    public void signIn() {
        if (!hasInternet()) {
            displaySnackbar(getString(R.string.oops_connect_your_internet));
            return;
        }
        showLoading();
        JSONObject object = new JSONObject();
        try {
            object.put("grant_type", "password");
            object.put("client_id", URLHelper.client_id);
            object.put("client_secret", URLHelper.client_secret);
            object.put("mobile", SharedHelper.getKey(RegisterActivity.this, "mobile"));
            object.put("username", SharedHelper.getKey(RegisterActivity.this, "email"));
            object.put("password", SharedHelper.getKey(RegisterActivity.this, "password"));
            object.put("scope", "*");
            object.put("device_type", "android");
            object.put("device_id", getDeviceUDID());
            object.put("device_token", getDeviceToken());
            object.put("login_by", SharedHelper.getKey(RegisterActivity.this, "login_by"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URLHelper.login, object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissLoading();
                Toast.makeText(RegisterActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                SharedHelper.putKey(RegisterActivity.this, "access_token", response.optString("access_token"));
                SharedHelper.putKey(RegisterActivity.this, "refresh_token", response.optString("refresh_token"));
                SharedHelper.putKey(RegisterActivity.this, "token_type", response.optString("token_type"));
                getProfile();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissLoading();
                String json = null;
                Log.w(TAG,"Register SIN ERRor"+error.toString());
                NetworkResponse response = error.networkResponse;
                if (response != null && response.data != null) {
                    try {
                        JSONObject errorObj = new JSONObject(new String(response.data));

                        if (response.statusCode == 400 || response.statusCode == 405 || response.statusCode == 500) {
                            try {
                                displaySnackbar(errorObj.optString("message"));
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }
                        } else if (response.statusCode == 401) {
                            try {
                                if (errorObj.optString("message").equalsIgnoreCase("invalid_token")) {
                                    //Call Refresh token
                                } else {
                                    displaySnackbar(errorObj.optString("message"));
                                }
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }

                        } else if (response.statusCode == 422) {

                            json = trimMessage(new String(response.data));
                            if (json != "" && json != null) {
                                displaySnackbar(json);
                            } else {
                                displaySnackbar(getString(R.string.please_try_again));
                            }

                        } else {
                            displaySnackbar(getString(R.string.please_try_again));
                        }

                    } catch (Exception e) {
                        displaySnackbar(getString(R.string.something_went_wrong));
                    }

                } else {
                    if (error instanceof NoConnectionError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof NetworkError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof TimeoutError) {
                        signIn();
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

    // Get Acc User Profile After SignIn
    public void getProfile() {
        if (!hasInternet()) {
            displaySnackbar(getString(R.string.oops_connect_your_internet));
            return;
        }
        showLoading();
        JSONObject object = new JSONObject();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URLHelper.UserProfile, object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissLoading();
                Log.w(TAG,"Register profile"+response.toString());
                //loadingDialog.dismiss();

                SharedHelper.putKey(RegisterActivity.this, "id", response.optString("id"));
                SharedHelper.putKey(RegisterActivity.this, "first_name", response.optString("first_name"));
                SharedHelper.putKey(RegisterActivity.this, "last_name", response.optString("last_name"));
                SharedHelper.putKey(RegisterActivity.this, "email", response.optString("email"));
                SharedHelper.putKey(RegisterActivity.this, "gender", response.optString("gender"));
                SharedHelper.putKey(RegisterActivity.this, "mobile", response.optString("mobile"));
                SharedHelper.putKey(RegisterActivity.this, "wallet_balance", response.optString("wallet_balance"));
                SharedHelper.putKey(RegisterActivity.this, "payment_mode", response.optString("payment_mode"));
                if (!response.optString("currency").equalsIgnoreCase("") && response.optString("currency") != null)
                    SharedHelper.putKey(RegisterActivity.this, "currency", response.optString("currency"));
                else
                    SharedHelper.putKey(RegisterActivity.this, "currency", "$");

                if(SharedHelper.getKey(RegisterActivity.this,"login_by").equals("facebook") || SharedHelper.getKey(RegisterActivity.this,"login_by").equals("google") ) {
                    if (response.optString("picture").startsWith("http"))
                        SharedHelper.putKey(RegisterActivity.this, "picture", response.optString("picture"));
                } else {
                    SharedHelper.putKey(RegisterActivity.this, "picture", URLHelper.base_pic + response.optString("picture"));
                }

                SharedHelper.putKey(RegisterActivity.this, "sos", response.optString("sos"));
                SharedHelper.putKey(RegisterActivity.this, "loggedIn", getString(R.string.True));

                //phoneLogin();
                goToMainActivity();
                   /* if (!SharedHelper.getKey(activity,"account_kit_token").equalsIgnoreCase("")) {

                    }else {
                        GoToMainActivity();
                    }*/

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissLoading();
                String json = null;
                String Message;
                Log.w(TAG,"Register profile"+error.toString());
                NetworkResponse response = error.networkResponse;
                if (response != null && response.data != null) {
                    try {
                        JSONObject errorObj = new JSONObject(new String(response.data));

                        if (response.statusCode == 400 || response.statusCode == 405 || response.statusCode == 500) {
                            try {
                                displaySnackbar(errorObj.optString("message"));
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }
                        } else if (response.statusCode == 401) {
                            try {
                                if (errorObj.optString("message").equalsIgnoreCase("invalid_token")) {

                                } else {
                                    displaySnackbar(errorObj.optString("message"));
                                }
                            } catch (Exception e) {
                                displaySnackbar(getString(R.string.something_went_wrong));
                            }

                        } else if (response.statusCode == 422) {

                            json = trimMessage(new String(response.data));
                            if (json != "" && json != null) {
                                displaySnackbar(json);
                            } else {
                                displaySnackbar(getString(R.string.please_try_again));
                            }

                        } else {
                            displaySnackbar(getString(R.string.please_try_again));
                        }

                    } catch (Exception e) {
                        displaySnackbar(getString(R.string.something_went_wrong));
                    }


                } else {
                    if (error instanceof NoConnectionError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof NetworkError) {
                        displaySnackbar(getString(R.string.oops_connect_your_internet));
                    } else if (error instanceof TimeoutError) {
                        getProfile();
                    }
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("X-Requested-With", "XMLHttpRequest");
                headers.put("Authorization", "" + SharedHelper.getKey(RegisterActivity.this, "token_type") + " " + SharedHelper.getKey(RegisterActivity.this, "access_token"));
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

    // On Back Pressed
    @Override
    public void onBackPressed() {
            Intent mainIntent = new Intent(RegisterActivity.this, ActivityPhone.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            RegisterActivity.this.finish();
    }

}
