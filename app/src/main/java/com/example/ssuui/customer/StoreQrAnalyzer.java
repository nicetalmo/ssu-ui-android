package com.example.ssuui.customer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.ssuui.StoreQr;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;

/**
 * 카메라 프레임에서 QR 을 찾는 분석기 (CameraX ImageAnalysis, 분석 스레드 하나에서만 불림).
 * YUV 프레임의 밝기(Y) 면만 ZXing 으로 읽는다. QR 은 돌아가 있어도 읽히므로 화면 회전은 신경 쓰지 않음.
 */
final class StoreQrAnalyzer implements ImageAnalysis.Analyzer {

    interface Listener {
        /** 마음대기 매장 QR 을 찾았을 때 (분석 스레드) */
        void onStoreQr(@NonNull String storeId);

        /** 다른 QR 을 찾았을 때 (분석 스레드) */
        void onOtherQr();
    }

    private final Listener listener;
    private final QRCodeReader reader = new QRCodeReader();
    private byte[] luminance = new byte[0]; // 프레임마다 새로 만들지 않고 다시 씀

    StoreQrAnalyzer(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        try {
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            if (luminance.length != buffer.remaining()) luminance = new byte[buffer.remaining()];
            buffer.get(luminance);

            String content = decode(luminance, plane.getRowStride(), image.getWidth(), image.getHeight());
            if (content == null) return;
            String storeId = StoreQr.parse(content);
            if (storeId != null) {
                listener.onStoreQr(storeId);
            } else {
                listener.onOtherQr();
            }
        } finally {
            image.close();
        }
    }

    /**
     * 밝기 데이터에서 QR 내용을 읽는다. 없으면 null.
     * rowStride: 한 줄의 바이트 수 (기기에 따라 width 보다 큼, 마지막 줄은 width 만큼만 있음)
     */
    @Nullable
    String decode(@NonNull byte[] luminance, int rowStride, int width, int height) {
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                luminance, rowStride, height, 0, 0, width, height, false);
        try {
            return reader.decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
        } catch (NotFoundException | ChecksumException | FormatException e) {
            return null;
        } finally {
            reader.reset();
        }
    }
}
