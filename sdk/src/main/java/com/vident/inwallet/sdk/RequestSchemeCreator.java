package com.vident.inwallet.sdk;

import static com.vident.inwallet.sdk.WalletSDK.REQUEST_ID;
import static com.vident.inwallet.sdk.WalletSDK.SCHEME;

import android.net.Uri;

import androidx.annotation.NonNull;

public class RequestSchemeCreator{
    public static @NonNull Uri create(@NonNull ActivityWrapper activityWrapper, @NonNull String requestId) {
        // Create scheme
        Uri.Builder appSchemebuilder = new Uri.Builder().scheme(SCHEME)
                .authority("wallet")
                .appendQueryParameter(REQUEST_ID, requestId);

        return appSchemebuilder.build();
    }
}
