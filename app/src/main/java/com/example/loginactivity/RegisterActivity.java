package com.example.loginactivity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.loginactivity.async.RestfulCmd;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText,mPasswordTextComfirm;
    private Button mLoginButton,mExitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initComponenet();
    }

    private void initComponenet(){
        mUsernameText = findViewById(R.id.editText);
        mPasswordText = findViewById(R.id.editText2);
        mPasswordTextComfirm = findViewById(R.id.editText3);
        mLoginButton = findViewById(R.id.email_sign_in_button);
        mExitButton = findViewById(R.id.register_btn);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(mUsernameText.getText().toString())
                || TextUtils.isEmpty(mPasswordText.getText().toString())
                || TextUtils.isEmpty(mPasswordTextComfirm.getText().toString())){
                    Toast.makeText(RegisterActivity.this, "빈 칸을 채워주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!mPasswordText.getText().toString().equals(mPasswordTextComfirm.getText().toString())){
                    Toast.makeText(RegisterActivity.this, "비밀번호가 같지 않습니다.", Toast.LENGTH_SHORT).show();
                    mPasswordTextComfirm.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                sendRegisterInfo();
            }
        });

        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void sendRegisterInfo(){
        try{
            String path ="";
            RestfulCmd cmd = new RestfulCmd(path, RestfulCmd.RequestMethod.POST);
            cmd.addParam("userId", mUsernameText.getText().toString());
            cmd.addParam("userPw", mPasswordText.getText().toString());
            cmd.execute();
            cmd.setCallbacksFunc(new RestfulCmd.RestfulCmdResultCb() {
                @Override
                public void onPostExcuted(String result) throws JSONException, NullPointerException {
                    JSONObject jObject = new JSONObject(result);
                    if(jObject.optString(RestfulCmd.JSON_RESULT_OK).equals("Y")){
                        registerSuccess(jObject);
                    }else{
                        registerFailed(jObject);
                    }
                }
            });
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void registerSuccess(JSONObject jObject){
        //자동 로그인?
    }

    private void registerFailed(JSONObject jObject){

    }

}
