package com.vident.inwallet.sdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.vident.inwallet.sdk.data.ExecuteContract;
import com.vident.inwallet.sdk.data.Metadata;
import com.vident.inwallet.sdk.data.SendData;
import com.vident.inwallet.sdk.data.SendNFT;
import com.vident.inwallet.sdk.data.SendToken;
import com.vident.inwallet.sdk.data.SendCoin;
import com.vident.inwallet.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Wallet 앱과 연동하는 SDK
 *
 * @author ybjeon
 */
@SuppressWarnings("unused")
public class WalletSDK{
    private static final String WALLET_PACKAGE_NAME = "com.vident.inwallet";

    private static final int REQUEST_CODE_SIGN = 1001;
    public static final int REQUEST_CODE_RESULT = 1003;
    public static final String REQUEST_ID = "requestId";
    public static final String SCHEME = "inwallet";
    private static final String LOG_TAG = WalletSDK.class.getSimpleName();

    public enum RequestType{
        auth,
        send,
        send_token,
        send_nft,
        contract_execute
    }

    private final Gson gson = new Gson();

    static{
        // set tag, level of tag
        Logger.setDefaultTag(LOG_TAG);
        Logger.setLoggerLevel(Logger.Level.DEBUG);
    }

    private final ProposalResultHandler resultHandler;

    private final ActivityWrapper activityWrapper;

    private String serverDomain;

    /**
     * SDK constructor for activity
     *
     * @param activity      요청 대상 activity
     * @param resultHandler 결과를 통보 받을 instance
     */
    public WalletSDK(@NonNull Activity activity, @NonNull ProposalResultHandler resultHandler){
        this(new ActivityWrapper(activity), resultHandler);
    }

    /**
     * SDK constructor for fragment
     *
     * @param fragment      요청 대상 fragment
     * @param resultHandler 결과를 받을 instance
     */
    public WalletSDK(@NonNull Fragment fragment, @NonNull ProposalResultHandler resultHandler){
        this(new ActivityWrapper(fragment), resultHandler);
    }

    /**
     * SDK constructor
     *
     * @param activityWrapper activity or fragment
     * @param resultHandler   결과를 받을 instance
     */
    private WalletSDK(@NonNull ActivityWrapper activityWrapper, @NonNull ProposalResultHandler resultHandler){
        this.resultHandler = resultHandler;
        this.activityWrapper = activityWrapper;

        // set service_id in manifest
        try{
            Context context = activityWrapper.getContext();
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if(applicationInfo != null){
                if(applicationInfo.metaData != null){
                    serverDomain = applicationInfo.metaData.getString("A2A_SERVER_DOMAIN");
                }
            }
        }catch(PackageManager.NameNotFoundException e){
            throw new RuntimeException("Not found Inwallet App.");
        }
    }

    /**
     * Check installed wallet App.
     *
     * @return if App is installed and enable, <code>true</code>
     */
    private boolean checkInstall(){
        // check install or enable
        try{
            ApplicationInfo info = activityWrapper.getContext().getPackageManager().getApplicationInfo(WALLET_PACKAGE_NAME, 0);
            if(!info.enabled){
                Logger.warn("Not enabled package : " + WALLET_PACKAGE_NAME);
                return false;
            }
        }catch(Exception e){
            // package not found
            Logger.debug("Not found package : " + WALLET_PACKAGE_NAME);
            return false;
        }
        return true;
    }

    /**
     * @param metadata 요청 app data
     */
    public void proposal(@NonNull Metadata metadata, @Nullable SendData sendData){
        A2AProposalRequest requestData;
        if(sendData == null){
            requestData = new A2AProposalRequest(metadata, RequestType.auth.toString(), null);
        }else{
            requestData = new A2AProposalRequest(metadata, sendData.getRequestType(), sendData.getTransactionData());
        }
        requestProposalA2A(requestData);
    }

    /**
     * @param metadata 요청 app data
     */
    public void auth(@NonNull Metadata metadata){
        A2AProposalRequest requestData = new A2AProposalRequest(metadata, RequestType.auth.toString(), null);
        requestProposalA2A(requestData);
    }

    /**
     * @param metadata  요청 app data
     * @param sendCoin transaction data
     */
    public void sendCoin(@NonNull Metadata metadata, SendCoin sendCoin){
        A2AProposalRequest requestData = new A2AProposalRequest(metadata, RequestType.send.name(), sendCoin.getTransactionData());
        requestProposalA2A(requestData);
    }

    /**
     * @param metadata  요청 app data
     * @param sendToken transaction data
     */
    public void sendToken(@NonNull Metadata metadata, SendToken sendToken){
        A2AProposalRequest requestData = new A2AProposalRequest(metadata, RequestType.send_token.name(), sendToken.getTransactionData());
        requestProposalA2A(requestData);
    }

    /**
     * @param metadata 요청 app data
     * @param sendNFT  transaction data
     */
    public void sendNFT(@NonNull Metadata metadata, SendNFT sendNFT){
        A2AProposalRequest requestData = new A2AProposalRequest(metadata, RequestType.send_nft.name(), sendNFT.getTransactionData());
        requestProposalA2A(requestData);
    }

    /**
     * @param metadata        요청 app data
     * @param executeContract transaction data
     */
    public void executeContract(@NonNull Metadata metadata, ExecuteContract executeContract){
        A2AProposalRequest requestData = new A2AProposalRequest(metadata, RequestType.contract_execute.name(), executeContract.getTransactionData());
        requestProposalA2A(requestData);
    }

    private void requestProposalA2A(@NonNull A2AProposalRequest requestData){
        String body = gson.toJson(requestData);
        Logger.debug("requestProposalA2A body : " + body);
        // make launch url
        final Uri.Builder builder;
        if(serverDomain != null){
            builder = Uri.parse(serverDomain + "/api/v1/a2a/proposal").buildUpon();
        }else{
            builder = Uri.parse(BuildConfig.A2A_SERVER_DOMAIN + "/api/v1/a2a/proposal").buildUpon();
        }
        Log.e(LOG_TAG,"url = "+builder.toString());
        // call http
        new Thread(() -> {
            try{
                Logger.debug("requestProposalA2A launch : " + builder.build().toString());

                URL url = new URL(builder.build().toString());

                // launch from auth server
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoInput(true);
                //POST로 넘겨줄 파라미터 생성
                byte[] request_data = body.getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(request_data);
                os.close();

                int status = conn.getResponseCode();
                Logger.debug("requestProposalA2A init response status : " + status);
                InputStream is;
                if(status < 400){
                    is = conn.getInputStream();
                }else{
                    is = conn.getErrorStream();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while((len = is.read(buffer, 0, buffer.length)) != -1){
                    baos.write(buffer, 0, len);
                }
                String stringBody = new String(baos.toByteArray(), StandardCharsets.UTF_8);

                Logger.debug("requestProposalA2A  response body : " + stringBody);
                final A2AResponse response = gson.fromJson(stringBody, A2AResponse.class);
                if(response.isSuccess()){
                    Logger.debug("requestProposalA2A  requestId  : " + response.getRequestId());
                    // check install or enable
                    if(checkInstall()){
                        // wallet 앱 호출
                        new Handler(Looper.getMainLooper()).post(() -> launch(response.getRequestId()));
                    }else{
                        // play store link.
                        resultHandler.onNotInstall(getIntent(RequestSchemeCreator.create(activityWrapper, response.getRequestId())));
                    }
                }else{
                    // 요청 실패
                    Logger.debug("요청 실패");
                    onRequestFailed(status);
                }
            }catch(MalformedURLException e){
                // Never happened
                Logger.debug("MalformedURLException");
            }catch(IOException e){
                Logger.debug("IOException");
                onRequestFailed(-1);
            }
        }).start();
    }

    public void getResult(@NonNull String requestId ,ResponseResultHandler responseResultHandler){
        getProposalResponseA2A(requestId, responseResultHandler);
    }

    private void getProposalResponseA2A(String requestId, ResponseResultHandler responseResultHandler){
        // make launch url
        final Uri.Builder builder;
        if(serverDomain != null){
            builder = Uri.parse(serverDomain + "/api/v1/a2a/result").buildUpon().appendQueryParameter("requestId", requestId);;
        }else{
            builder = Uri.parse(BuildConfig.A2A_SERVER_DOMAIN + "/api/v1/a2a/result").buildUpon().appendQueryParameter("requestId", requestId);;
        }
        // call http
        new Thread(() -> {
            try{
                Logger.debug("getProposalResponseA2A  : " + builder.build().toString());

                URL url = new URL(builder.build().toString());

                // launch from auth server
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");

                int status = conn.getResponseCode();
                Logger.debug("requestProposalA2A init response status : " + status);
                InputStream is;
                if(status < 400){
                    is = conn.getInputStream();
                }else{
                    is = conn.getErrorStream();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while((len = is.read(buffer, 0, buffer.length)) != -1){
                    baos.write(buffer, 0, len);
                }
                String stringBody = new String(baos.toByteArray(), StandardCharsets.UTF_8);

                Logger.debug("requestProposalA2A init response body : " + stringBody);

                final A2AResponse response = gson.fromJson(stringBody, A2AResponse.class);
                if(response.isSuccess()){
                    responseResultHandler.onResult(response.getRequestId(),response);
                }else{
                    // 요청 실패
                    onRequestFailed(status);
                }
            }catch(MalformedURLException e){
                // Never happened
            }catch(IOException e){
                onRequestFailed(-1);
            }
        }).start();
    }


    /**
     * request 서버에서 실패
     *
     * @param statusCode http status code
     */
    private void onRequestFailed(final int statusCode){
        new Handler(Looper.getMainLooper()).post(() -> {
            Logger.warn("request failed : code=" + statusCode);
            resultHandler.onAuthInitFailed(statusCode);
        });
    }

    /**
     * wallet 앱을 인증 요청
     *
     * @param requestId a2a response 의 requestId
     */
    private void launch(String requestId){
        // call wallet
        Intent intent = new Intent(Intent.ACTION_VIEW, RequestSchemeCreator.create(activityWrapper, requestId));
        intent.setPackage(WALLET_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        Logger.debug("Request to wallet : " + intent);

        try{
            activityWrapper.startActivityForResult(intent, REQUEST_CODE_SIGN);
        }catch(ActivityNotFoundException e){
            Logger.warn("Not start wallet", e);
        }
    }

    /**
     * wallet 에서 전달 준 결과를 처리한다.
     * <p/>
     * {@link Activity} 의 onActivityResult 에서 호출해줘야 한다.<p/>
     * <code>
     * protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
     * if (sdk.handleResult(requestCode, resultCode, data)) {
     * return;
     * }
     * super.onActivityResult(requestCode, resultCode, data);
     * }
     * </code>
     *
     * @param requestCode request code on onActivityResult
     * @param resultCode  result code on onActivityResult
     * @param data        intent on onActivityResult
     * @return 처리 여부
     */
    public boolean handleResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_CODE_SIGN){
            if(resultCode == Activity.RESULT_OK){
                Logger.debug("Response from wallet : resultCode=" + resultCode);
                String requestId = data.getStringExtra(REQUEST_ID);
                resultHandler.onProposalResult(resultCode, requestId);
            }else if(resultCode == Activity.RESULT_CANCELED){
                resultHandler.onProposalResult(resultCode, null);
            }
            return true;
        }
        return false;
    }


    /**
     * Get an intent to install wallet app.
     *
     * @return intent
     */
    private static Intent getIntent(Uri uri){
        String query = null;
        if(uri != null){
            try{
                query = "?id=" + WALLET_PACKAGE_NAME + "&url=" + URLEncoder.encode(uri.toString(), "utf-8");
            }catch(UnsupportedEncodingException e){
                return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details"));
            }
        }

        try{
            return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details" + query));
        }catch(Exception e){
            // web browser
            return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details" + query));
        }
    }
}
