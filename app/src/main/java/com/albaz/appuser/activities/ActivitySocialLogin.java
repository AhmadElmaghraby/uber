package com.albaz.appuser.activities;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.albaz.appuser.base.ServiceActivity;
import com.android.volley.toolbox.StringRequest;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.phonenumberui.PhoneNumberActivity;
import com.splunk.mint.Mint;
import com.albaz.appuser.App;
import com.albaz.appuser.helper.ConnectionHelper;
import com.albaz.appuser.helper.SharedHelper;
import com.albaz.appuser.helper.URLHelper;
import com.albaz.appuser.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.albaz.appuser.App.trimMessage;


public class ActivitySocialLogin extends ServiceActivity implements GoogleApiClient.OnConnectionFailedListener {

    /*----------Facebook Login---------------*/

    ImageView backArrow;
    AccessTokenTracker accessTokenTracker;
    JSONObject json;
    ConnectionHelper helper; // Check Internet
    LinearLayout facebook_layout;
    LinearLayout google_layout;

    CallbackManager callbackManager;
    GoogleApiClient mGoogleApiClient;
    private static final int GOOGLE_SIGN_IN = 100;

    // Facebook Data & Facebook
    private Profile profile;
    private String accessTocken;
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String link;
    private Uri linkPic;

    private String IDPhone,TokenPhone; // Device ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext()); // Set SDK Initialize In Activity
        setContentView(R.layout.activity_social);

        // Set Activity Name In Tag Var (_Tag)
        setLogTag("ActivitySocialLogin");

        // Initialize Elements
        facebook_layout = findViewById(R.id.facebook_layout); // Layout Facebook
        google_layout = findViewById(R.id.google_layout); // Layout Google
        backArrow = findViewById(R.id.backArrow); // Back Arrow

        helper = new ConnectionHelper(this); // Set Context Activity To Check Internet

        // Info Device
        IDPhone = getDeviceUDID();
        TokenPhone = getDeviceToken();

        /*----------Clicked Back Arrow---------------*/
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        /*----------Clicked Google Login---------------*/
        google_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleLogin();
            }
        });

        /*----------Clicked Facebook Login---------------*/
        facebook_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookLogin();
            }
        });

        // Permission SDK
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Key Signatures For My APK
        try {
            @SuppressLint("PackageManagerGetSignatures") PackageInfo info = getPackageManager().getPackageInfo(
                    "com.digitalcurv.citycab_passenger", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException ignored) {

        }

    } // End onCreate

    // Result CallBack Google API
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (data == null) return;

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) { // Check Google Request Code = Ok

            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext()); // Get Instance From Account Google
            setGoogleData(acct); // Invoke Get Data Account
        }

    }

    // If Backed Or Destroy Activity Social
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            accessTokenTracker.stopTracking();
        } catch (Exception e) {
            //Error
        }
    }

    // Failed Access In APIS
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // Invoking Method When Pause App & Stopped Dialog Loading
    @Override
    protected void onPause() {
        super.onPause();
        dismissLoading();
    }

    // Build And Show Popup Google Sign in
    private void googleLogin() {

        if (helper.isConnectingToInternet()) { // Check Internet

            //Configure Google Sign-In:
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            // Build a GoogleApiClient with access to the Google Sign-In API
            // options specified by gso.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient); // Set Intent Activity
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN); // Show Popup Google Sign in


        } else {
            //mProgressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySocialLogin.this);
            builder.setMessage("Check your Internet").setCancelable(false);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Intent NetworkAction = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(NetworkAction);

                }
            });
            builder.show();
        }
    }

    // Build And Show Popup Facebook Sign in
    private void facebookLogin() {

        callbackManager = CallbackManager.Factory.create(); // Now create a callbackManager to handle login responses

        if (helper.isConnectingToInternet()) { // Check Internet

            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));

            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                        // Execute Any Operation When Login Success
                        public void onSuccess(LoginResult loginResult) {

                            if (AccessToken.getCurrentAccessToken() != null) {
                                SharedHelper.putKey(ActivitySocialLogin.this, "accessToken", loginResult.getAccessToken().getToken()); // Saved Access Token In Shared Prefrence
                                setFacebookData(loginResult);
                            }
                        }

                        @Override
                        public void onCancel() {
                            // App code
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            // App code
                            Toast.makeText(ActivitySocialLogin.this, "Error Login", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {

            // mProgressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySocialLogin.this);
            builder.setMessage("Check your Internet").setCancelable(false);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Intent NetworkAction = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(NetworkAction);
                }
            });
            builder.show();
        }
    }

    // Method Getting Data Profile From Google API
    private void setGoogleData(final GoogleSignInAccount acct) {

        if (acct != null) {
            firstName = acct.getGivenName();
            lastName = acct.getFamilyName();
            email = acct.getEmail();
            id = acct.getId();
            linkPic = acct.getPhotoUrl();
            accessTocken = acct.getIdToken();

            check(URLHelper.CHECK,"google"); // Invoke Method Check

        }else {
            Toast.makeText(this, "No Google Login", Toast.LENGTH_SHORT).show();
        }
    }

    // Method Getting Data Profile From Facebook API
    private void setFacebookData(final LoginResult loginResult) {

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {

                if (Profile.getCurrentProfile() != null) { // Check Current Profile
                    profile = Profile.getCurrentProfile();
                }
                    if(response != null) {

                    accessTocken = loginResult.getAccessToken().getToken().toString(); // Set Access Token
                    id = profile.getId();
                    link = profile.getLinkUri().toString();
                    linkPic = profile.getProfilePictureUri(150,150);
                    firstName = response.getJSONObject().optString("first_name");
                    lastName = response.getJSONObject().optString("last_name");

                    if(!response.getJSONObject().optString("email").equals("")) {

                        email = response.getJSONObject().optString("email");
                    } else {
                        email = "";
                    }

                    check(URLHelper.CHECK,"facebook"); // Invoke Method Check
                } else {

                    Toast.makeText(getApplicationContext(), "No Facebook Login", Toast.LENGTH_SHORT).show();
                }

            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email,first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    // Check ID Member Exist in DB Or Not
    public void check( final String URL, final String Login_by) {

        showLoading(); // Start Dialog Loading

        // Prepare The Request
        StringRequest postRequest = new StringRequest(Request.Method.GET, URL + "?social_unique_id=" + id, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("true")) {

                            Intent intent = new Intent(getApplicationContext(),ActivityPassword.class);
                            SharedHelper.putKey(getApplicationContext(),"id_social",id);
                            SharedHelper.putKey(getApplicationContext(),"email",email);
                            SharedHelper.putKey(getApplicationContext(),"login_by",Login_by);
                            startActivity(intent);

                        } else {

                            Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
                            intent.putExtra("firstName",firstName);
                            intent.putExtra("lastName",lastName);
                            intent.putExtra("email",email);
                            SharedHelper.putKey(getApplicationContext(),"login_by",Login_by);
                            SharedHelper.putKey(getApplicationContext(),"id_social",id);
                            SharedHelper.putKey(getApplicationContext(),"picture", linkPic.toString());
                            startActivity(intent);

                        }
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Toast.makeText(ActivitySocialLogin.this, "Please Try Again Latter", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Authorization", SharedHelper.getKey(ActivitySocialLogin.this, "access_token"));
                params.put("login_by", Login_by);
                return params;
            }
        };

        // add it to the RequestQueue
        App.getInstance().addToRequestQueue(postRequest);

    }

}
