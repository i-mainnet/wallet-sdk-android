package com.vident.inwallet.sdk.data;

import androidx.annotation.NonNull;

import com.vident.inwallet.sdk.WalletSDK;
import com.google.gson.annotations.SerializedName;

public class ExecuteContract extends SendData{
    @SerializedName("abi")
    private String abi;
    @SerializedName("params")
    private String params;

    public ExecuteContract(@NonNull String from, @NonNull String to, @NonNull String abi, @NonNull String params){
        super(from, to);
        this.abi = abi;
        this.params = params;
    }

    public String getAbi(){
        return abi;
    }

    public void setAbi(String abi){
        this.abi = abi;
    }

    public String getParams(){
        return params;
    }

    public void setParams(String params){
        this.params = params;
    }

    @Override
    public TransactionData getTransactionData(){
        return  new TransactionData(getFrom(),getTo(), null, null,null,abi,params);
    }

    @Override
    public String getRequestType(){
        return WalletSDK.RequestType.contract_execute.toString();
    }
}
