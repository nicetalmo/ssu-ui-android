package com.example.ssuui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 매장 QR 에 담는 내용: maeumdaegi://store/{storeId} (stores 문서 id).
 * 앱의 QR 스캔 화면과, 기본 카메라 앱으로 찍었을 때 여는 링크(AndroidManifest 의 intent-filter)가 같은 형식을 쓴다.
 */
public final class StoreQr {

    private static final String PREFIX = "maeumdaegi://store/";
    // Firestore 자동 id 는 영문·숫자 20자. 문서 경로에 그대로 쓰므로 '/' 같은 글자는 받지 않음
    private static final Pattern CONTENT = Pattern.compile(Pattern.quote(PREFIX) + "([A-Za-z0-9_-]{1,64})");

    private StoreQr() {
    }

    @NonNull
    public static String contentFor(@NonNull String storeId) {
        return PREFIX + storeId;
    }

    /** QR·링크 내용에서 매장 id. 마음대기 매장 QR 이 아니면 null */
    @Nullable
    public static String parse(@Nullable String content) {
        if (content == null) return null;
        Matcher matcher = CONTENT.matcher(content.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }
}
