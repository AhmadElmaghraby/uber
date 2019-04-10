package com.albaz.appuser.fcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.albaz.appuser.helper.SharedHelper;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName(); // Get Name Of Class

    // Method To Getting Device Token & Set It In SharedPreference
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken(); // Getting Device Token
        SharedHelper.putKey(getApplicationContext(),"device_token",""+refreshedToken); // Set Device Token
        Log.e(TAG,"device_token : "+refreshedToken);
    }
}