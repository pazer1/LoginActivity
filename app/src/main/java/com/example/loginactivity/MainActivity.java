package com.example.loginactivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.loginactivity.async.RestfulCmd;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText;
    private Button mLoginButton,mRegisterButton;
    TextInputLayout layoutId,layoutId1;

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
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

    class DoneOnEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                mLoginButton.performClick();
                mLoginButton.setPressed(true);
                mLoginButton.invalidate();
                mLoginButton.setPressed(false);
                mLoginButton.invalidate();
                return true;
            }
            return false;
        }
    }

    private void initComponents() {
        mUsernameText = findViewById(R.id.editText);
        mPasswordText = findViewById(R.id.editText2);
        mLoginButton = findViewById(R.id.email_sign_in_button);
        mRegisterButton = findViewById(R.id.register_btn);
        layoutId = findViewById(R.id.layoutId);
        layoutId1 = findViewById(R.id.layoutId1);

        mPasswordText.setOnEditorActionListener(new DoneOnEditorActionListener());

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(mUsernameText.getText().toString()))
                    layoutId.setError("빈 칸을 채워주세요");
                else layoutId.setError(null);
                if(TextUtils.isEmpty(mPasswordText.getText().toString()))
                    layoutId1.setError("빈 칸을 채워주세요");
                else layoutId1.setError(null);

                if(TextUtils.isEmpty(mUsernameText.getText().toString()) ||TextUtils.isEmpty(mPasswordText.getText().toString()))
                    return;

                sendLoginInfo();
            }
        });
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,RegisterActivity.class));
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void sendLoginInfo() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        telephonyManager.getDeviceId();
        try {
            RestfulCmd cmd = new RestfulCmd("http://dipdoo.dothome.co.kr/LoginTest/Login.php", RestfulCmd.RequestMethod.POST); // 정보를 보낼 API URL, 메스드 타입(post) 설정
            cmd.addParam("userId", mUsernameText.getText().toString()); // 아이디 속성 추가
            cmd.addParam("userPw", mPasswordText.getText().toString()); // 비번 속성 추가

            //만일 서버쪽 deviceId와 기기의 deviceId가 다르다면 (회원 가입후 device를 교체했을 경우) 로그인 시, 현재의 디바이스 아이디
            //를 보내 서버쪽의 deviceId를 최신화 시킬 필요가 있다. 즉 로그인할 때 deviceId를 보내 서버의 device 아이디를 최신화 시키는 방법
            Log.d("deviceId",telephonyManager.getDeviceId()+"");
            cmd.execute();
            cmd.setCallbacksFunc(new RestfulCmd.RestfulCmdResultCb() {
                @Override
                public void onPostExcuted(String result) throws JSONException, NullPointerException {
                    JSONObject jObject = new JSONObject(result);
                    Log.d("mainLogin",jObject.optString(RestfulCmd.JSON_RESULT_OK));
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
        SharedPreferences sharedPreferences =getSharedPreferences("deviceShared",MODE_MULTI_PROCESS);
        if(sharedPreferences.contains("deviceId")){
            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
        }else{
            sharedPreferences.edit().putString("deviceId",data.optString("deviceId")).apply();
        }
    }
    public void loginFail(JSONObject result){
        layoutId.setError("로그인 할 수 없습니다.");
        layoutId1.setError("아이디와 비밀번호를 확인해주세요");
    }
}
