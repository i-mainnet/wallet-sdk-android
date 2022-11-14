package com.vident.inwallet.sdk;

/**
 * 결과를 전달 받을 interface<p/>
 *
 * @author ybjeon
 */
public interface ResponseResultHandler{

    /**
     * wallet 앱에서 인증요청에 대한 결과가 왔을 때 호출
     * @param requestId  requestId
     * @param response responseData
     */
    void onResult(String requestId, A2AResponse response);

}
