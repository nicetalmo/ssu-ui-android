package com.example.ssuui.customer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ssuui.R;

/**
 * QR 스캔 창 바깥을 어둡게 덮는 뷰 (Figma: C2 · 검정 68%).
 * setWindow 로 받은 뷰 자리만 둥근 모서리(img_qr_scan_frame 바깥 반경 12dp)로 비워 둔다.
 */
public final class ScanDimView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF hole = new RectF();
    private final int[] windowLocation = new int[2];
    private final int[] ownLocation = new int[2];
    private final float radius;
    @Nullable private View window;

    public ScanDimView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(ContextCompat.getColor(context, R.color.qr_scan_dim));
        radius = getResources().getDimension(R.dimen.radius_lg);
        path.setFillType(Path.FillType.EVEN_ODD);
    }

    /** 비워 둘 스캔 창. 창이 움직이거나 크기가 바뀌면 다시 그린다 */
    void setWindow(@NonNull View window) {
        this.window = window;
        window.addOnLayoutChangeListener((v, left, top, right, bottom,
                                          oldLeft, oldTop, oldRight, oldBottom) -> invalidate());
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        path.rewind();
        path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        if (window != null && window.getWidth() > 0) {
            // 창과 이 뷰의 부모가 달라도 되도록 화면 위치 차이로 계산
            window.getLocationInWindow(windowLocation);
            getLocationInWindow(ownLocation);
            float left = windowLocation[0] - ownLocation[0];
            float top = windowLocation[1] - ownLocation[1];
            hole.set(left, top, left + window.getWidth(), top + window.getHeight());
            path.addRoundRect(hole, radius, radius, Path.Direction.CW);
        }
        canvas.drawPath(path, paint);
    }
}
