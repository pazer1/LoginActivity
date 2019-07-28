package com.example.loginactivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.loginactivity.async.RestfulCmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText;
    private Button mLoginButton;

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private final String TAG = "LOGINACTIVITY";

    private static final List<String> sPermissions = Arrays.asList(
            Manifest.permission.READ_PHONE_STATE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionsIfNecessary();
        initComponents();
    }

    private void requestPermissionsIfNecessary(){
        if(!checkAllPermissions()){
            ActivityCompat.requestPermissions(
                    this,sPermissions.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_PERMISSIONS:
                if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this, "전화 정보를 허락해 주셔야 사용할 수 있습니다.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private boolean checkAllPermissions(){
        boolean hasPermissions = true;
        for(String permission : sPermissions){
            hasPermissions &= ContextCompat.checkSelfPermission(
                    this,permission)==PackageManager.PERMISSION_GRANTED;
        }
        return hasPermissions;
    }



    private void initComponents() {
        mUsernameText = findViewById(R.id.editText);
        mPasswordText = findViewById(R.id.editText2);
        mLoginButton = findViewById(R.id.email_sign_in_button);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLoginInfo();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void sendLoginInfo() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        telephonyManager.getDeviceId();
        try {
            RestfulCmd cmd = new RestfulCmd("/rest/v1/auth/loginPs", RestfulCmd.RequestMethod.POST); // 정보를 보낼 API URL, 메스드 타입(post) 설정
            cmd.addParam("userId", mUsernameText.getText().toString()); // 아이디 속성 추가
            cmd.addParam("userPw", mPasswordText.getText().toString()); // 비번 속성 추가
            Toast.makeText(this,telephonyManager.getDeviceId(), Toast.LENGTH_SHORT).show();
            cmd.execute();
            cmd.setCallbacksFunc(new RestfulCmd.RestfulCmdResultCb() {
                @Override
                public void onPostExcuted(String result) throws JSONException, NullPointerException {
                    JSONObject jObject = new JSONObject(result);
                    if(jObject.optString(RestfulCmd.JSON_RESULT_OK).equals("Y")){
                        loginSuccess(jObject);
                    }else{
                        loginFail(jObject);
                    }
                }
            });
        }catch (Exception e){

        }
    }
    public void loginSuccess(JSONObject data){
        try{
            SharedPreferences sharedPreferences = getSharedPreferences("shared",MODE_MULTI_PROCESS);
            sharedPreferences.edit().putString("shared","deviceId").apply();
        }catch (Exception e){
            Log.d(TAG,e.getMessage());
        }
    }
    public void loginFail(JSONObject result){
        try{

        }catch (Exception e){
            Log.d(TAG,e.getMessage());
        }
    }
}
