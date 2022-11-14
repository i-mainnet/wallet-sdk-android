package com.vident.inwallet.sdk.data;

import androidx.annotation.NonNull;

import com.vident.inwallet.sdk.WalletSDK;
import com.google.gson.annotations.SerializedName;

public class SendCoin extends SendData{

    @SerializedName("value")
    private String value;

    public SendCoin(@NonNull String from, @NonNull String to, @NonNull String value){
        super(from, to);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public TransactionData getTransactionData(){
        return  new TransactionData(getFrom(),getTo(), value, null,null,null,null);
    }

    @Override
    public String getRequestType(){
        return WalletSDK.RequestType.send.toString();
    }


}
