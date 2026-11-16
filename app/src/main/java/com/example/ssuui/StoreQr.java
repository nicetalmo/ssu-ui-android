package com.example.ssuui;

import androidx.annotation.NonNull;

/** 매장 QR 에 담는 내용: maeumdaegi://store/{storeId} (stores 문서 id) */
public final class StoreQr {

    private static final String PREFIX = "maeumdaegi://store/";

    private StoreQr() {
    }

    @NonNull
    public static String contentFor(@NonNull String storeId) {
        return PREFIX + storeId;
    }
}
