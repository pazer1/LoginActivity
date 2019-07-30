package com.example.loginactivity.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

public class RestfulCmd extends AsyncTask<String, Integer, String> {
    private static final String TAG = RestfulCmd.class.getName();

    public static final String JSON_RESULT_OK = "isOk"; // restful 결과 OK
    public static final String JSON_ERROR_CODE = "errCode";
    public static final String JSON_ERROR_MSG = "errMsg";
    public static final String JSON_RESULT_CASH = "cash";
    //서버의 first separator 를 바꾸어 주어야 한다.
    public static final String SERVER_URL = "http://117.4.237.188"; //http://192.168.0.67:8080/api/v1.0/contents
    //    public static final String SERVER_URL = "http://192.168.0.67:8080"; //http://192.168.0.67:8080/api/v1.0/contents
    public static final String ERROR_CODE_NO_EMAIL = "ERR01"; // 등록되지 않은 이메일

    public static final int ERRCODE_EXCEPTION = -1;        // 예외사항 발생

    public enum RequestMethod {
        GET,
        POST,
        POST_MT,
        POST_MT_PROGRESS,
        DELETE,
        HEAD,
        PUT;

        private RequestMethod() {
        }
    }

    private HttpClient mHttpClient;
    private HttpPost mHttpPostRequest;

    // 인터페이스
    public interface RestfulCmdResultCb {
        void onPostExcuted(String result) throws JSONException, NullPointerException;
    }
    private RestfulCmdResultCb mRestfulCmdResultCb;
    public void setCallbacksFunc(RestfulCmdResultCb cb) {
        mRestfulCmdResultCb = cb;
    }

    public interface RestfulCmdExceptionCb {
        void onException(Exception e);
    }
    private RestfulCmdExceptionCb mRestfulCmdExceptionCb;
    public void setCallbacksFunc(RestfulCmdExceptionCb cb) {
        mRestfulCmdExceptionCb = cb;
    }

    // 인터페이스

    private String mPathSeperator;
    private ArrayList<NameValuePair> mHeaders;
    private ArrayList<NameValuePair> mParams;
    private MultipartEntityBuilder mBuilder;

    private Context mContext;
    private CustomMultipartEntityBuilder mCustomBuilder;
    private ProgressDialog mDialog;

    private int responseCode;
    private String message;

    private RequestMethod mReqMethod;

    private String response;

    public String getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public RestfulCmd() { // for okhttp3
    }

    public RestfulCmd(String path) {
        this(path, RequestMethod.GET);
    }

    public RestfulCmd(String path, RequestMethod method) {
        this(path, method, null);
    }

    public RestfulCmd(String path, RequestMethod method, Context context) {
        mPathSeperator = path;
        mReqMethod = method;
        mContext = context;
        if (mReqMethod == RequestMethod.POST_MT) {
            mBuilder = MultipartEntityBuilder.create();
            mHeaders = new ArrayList<NameValuePair>();
        }
        else if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
            mCustomBuilder = new CustomMultipartEntityBuilder(new CustomMultipartFormEntity.ProgressListener() {
                @Override
                public void transferred(long num, float totalSize) {
                    publishProgress((int)((num /totalSize) * 100));
                }
            });
        }
        else {
            mParams = new ArrayList<NameValuePair>();
            mHeaders = new ArrayList<NameValuePair>();
        }
    }

    public boolean addParam(String name, String value) {
        if (TextUtils.isEmpty(name) || value == null) {
            return false;
        }

        if (mReqMethod == RequestMethod.POST_MT) {
            mBuilder.addTextBody(name, value, ContentType.create("multipart/form-data", "UTF-8"));
        }
        else if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
            mCustomBuilder.addTextBody(name, value, ContentType.create("multipart/form-data", "UTF-8"));
        }
        else {
            mParams.add(new BasicNameValuePair(name, value));
        }

        return true;
    }

    public boolean addParam(String name, byte[] value) {
        if (TextUtils.isEmpty(name) || value == null) {
            return false;
        }

        if (mReqMethod == RequestMethod.POST_MT) {
            mBuilder.addBinaryBody(name, value);
        }
        else if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
            mCustomBuilder.addBinaryBody(name, value);
        }

        return true;
    }

    public boolean addParam(String name, File file) {
        if (TextUtils.isEmpty(name) || file == null) {
            return false;
        }

        if (mReqMethod == RequestMethod.POST_MT) {
            mBuilder.addBinaryBody(name, file);
        }
        else if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
            mCustomBuilder.addBinaryBody(name, file);
        }

        return true;
    }

    public void addHeader(String name, String value) {
        mHeaders.add(new BasicNameValuePair(name, value));
    }

    public void Execute(RequestMethod method) throws Exception {

    }

    private String executeRequest(HttpUriRequest request, String url) {
        mHttpClient = new DefaultHttpClient();
        StringBuffer output = new StringBuffer();
        HttpResponse httpResponse;

        try {
            httpResponse = mHttpClient.execute(request);
            responseCode = httpResponse.getStatusLine().getStatusCode();
            message = httpResponse.getStatusLine().getReasonPhrase();

            Log.d(TAG,"Sending " + request.getMethod() + " request to URL : " + url);
            if (mReqMethod == RequestMethod.POST_MT) {
                String decodeUTF8 = URLDecoder.decode(mBuilder.toString(), "UTF-8");
                Log.d(TAG,"Post multipart : " + decodeUTF8);
            }
            else if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
                String decodeUTF8 = URLDecoder.decode(mCustomBuilder.toString(), "UTF-8");
                Log.d(TAG,"Post multipart : " + decodeUTF8);
            }
            else {
                Log.d(TAG,"Parameters : " + mParams.toString());
            }
            Log.d(TAG,"Response Code : " + responseCode);
            Log.d(TAG,"Response message : " + message);


            HttpEntity entity = httpResponse.getEntity();

            if (entity != null) {
                InputStream inputStream = entity.getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    output.append(line);
                }

                bufferedReader.close();
                inputStream.close();
            }

            Log.d(TAG,"Response output : " + output.toString());

        } catch (ClientProtocolException e) {
            mHttpClient.getConnectionManager().shutdown();
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e) {
            mHttpClient.getConnectionManager().shutdown();
            e.printStackTrace();
        } catch (IOException e) {
            mHttpClient.getConnectionManager().shutdown();
            e.printStackTrace();
        } finally {
            if (TextUtils.isEmpty(output)) {
                try {
                    JSONObject object = new JSONObject();
                    object.put(JSON_RESULT_OK, "N");
                    object.put(JSON_ERROR_CODE, ERRCODE_EXCEPTION);
                    output.append(object);

                    return output.toString();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return output.toString();
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    protected String doInBackground(String... params) {
        String url = mPathSeperator;
//        if (!mPathSeperator.isEmpty()) {
//            url += mPathSeperator;
//        }

        try {
            switch (mReqMethod) {
                case GET: {
                    if (mParams != null && mParams.size() > 0) {
                        String paramString = URLEncodedUtils.format(mParams, HTTP.UTF_8);
                        url += "?" + paramString;
                    }

                    HttpGet request = new HttpGet(url);

                    //add mHeaders
//                    for (NameValuePair h : mHeaders) {
//                        request.addHeader(h.getName(), h.getValue());
//                    }

                    return executeRequest(request, url);
                }
                case POST: {
//                    if (!mHeaders.isEmpty()) {
//                        for (NameValuePair p : mHeaders) {
//                            String paramString = null;
//                            paramString = p.getName() + "/" + URLEncoder.encode(p.getValue(), "UTF-8");
//
//                            if (url.length() > 1) {
//                                url += "/" + paramString;
//                            } else {
//                                url += paramString;
//                            }
//                        }
//                    }
                    HttpPost request = new HttpPost(url);

                    //add mHeaders
                    for (NameValuePair h : mHeaders) {
                        request.addHeader(h.getName(), h.getValue());
                    }

                    if (!mParams.isEmpty()) {
                        request.setEntity(new UrlEncodedFormEntity(mParams, HTTP.UTF_8));
                    }

                    return executeRequest(request, url);
                }
                case POST_MT : {
                    HttpPost request = new HttpPost(url);


                    //add mHeaders
                    for (NameValuePair h : mHeaders) {
                        request.addHeader(h.getName(), h.getValue());
                    }

                    request.setEntity(mBuilder.build());

                    return executeRequest(request, url);
                }
                case POST_MT_PROGRESS : {
                    mHttpPostRequest = new HttpPost(url);
                    mHttpPostRequest.setEntity(mCustomBuilder.build());

                    return executeRequest(mHttpPostRequest, url);
                }
                case PUT : {
                    HttpPut request = new HttpPut(url);
                    if (!mParams.isEmpty()) {
                        request.setEntity(new UrlEncodedFormEntity(mParams, HTTP.UTF_8));
                    }

                    return executeRequest(request, url);
                }
                case DELETE: {
                    if (mParams != null && mParams.size() > 0) {
                        String paramString = URLEncodedUtils.format(mParams, HTTP.UTF_8);
                        url += "?" + paramString;
                    }

                    Delete request = new Delete(url);
                    return executeRequest(request, url);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        mHttpClient.getConnectionManager().shutdown();

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mReqMethod == RequestMethod.POST_MT_PROGRESS) {
            mDialog = new ProgressDialog(mContext);
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setCancelable(false);
            mDialog.setOnCancelListener(cancelListener);
            mDialog.setOnDismissListener(dismissListener);
            mDialog.setMessage("업로드중입니다.");
//            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    if (mHttpPostRequest != null) {
//                        mHttpPostRequest.abort();
//                    }
//                }
//            });
            mDialog.show();
        }
    }

    DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            if (mHttpPostRequest != null) {
                //mHttpPostRequest.abort();
            }
        }
    };
    DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            mDialog = null;
        }
    };

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        //Progress 업데이트
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setProgress((int) progress[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (mRestfulCmdResultCb == null) {
            Log.d(TAG, "It's not callback function.");
        } else {
            try {
                mRestfulCmdResultCb.onPostExcuted(result);
            } catch (JSONException e) {
                e.printStackTrace();
                if (mRestfulCmdExceptionCb != null) {
                    mRestfulCmdExceptionCb.onException(e);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                if (mRestfulCmdExceptionCb != null) {
                    mRestfulCmdExceptionCb.onException(e);
                }
            }
        }

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private class Delete extends HttpPost
    {
        public Delete(String url){
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }


}