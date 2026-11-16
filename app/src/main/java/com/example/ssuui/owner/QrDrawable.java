package com.example.ssuui.owner;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * QR 코드를 칸(모듈) 사각형들로 그리는 Drawable.
 * 비트맵을 늘리지 않으니 포스터를 어떤 크기로 그려도 가장자리가 흐려지지 않는다.
 * 칸들을 Path 하나로 합쳐 그려서, 칸 사이에 가는 틈(이음 자국)이 생기지 않는다.
 * 여백(quiet zone)은 넣지 않으므로 둘레에 흰 공간을 두고 쓴다.
 */
final class QrDrawable extends Drawable {

    private final int count;     // 한 변의 칸 수
    private final Path modules = new Path(); // 칸 1개 = 1 × 1
    private final Paint paint = new Paint();

    QrDrawable(@NonNull String content, @ColorInt int color) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        BitMatrix matrix;
        try {
            // 크기 0 = 한 칸을 1로 하는 최소 크기 행렬
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (WriterException e) {
            throw new IllegalArgumentException("QR 로 만들 수 없는 내용: " + content, e);
        }
        count = matrix.getWidth();
        // 한 줄에서 이어진 검은 칸은 사각형 하나로
        for (int y = 0; y < count; y++) {
            int x = 0;
            while (x < count) {
                if (!matrix.get(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < count && matrix.get(x, y)) x++;
                modules.addRect(start, y, x, y + 1, Path.Direction.CW);
            }
        }
        paint.setAntiAlias(false);
        paint.setColor(color);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float cell = Math.min(bounds.width(), bounds.height()) / (float) count;
        int save = canvas.save();
        canvas.translate(bounds.left + (bounds.width() - cell * count) / 2f,
                bounds.top + (bounds.height() - cell * count) / 2f);
        canvas.scale(cell, cell);
        canvas.drawPath(modules, paint);
        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
