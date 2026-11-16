package com.example.ssuui.owner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.ssuui.R;
import com.example.ssuui.StoreQr;

/** view_qr_poster 를 화면에 붙이지 않고 A4 비율 비트맵으로 그린다. 미리보기와 인쇄가 같은 비트맵을 쓴다. */
final class QrPoster {

    // view_qr_poster 의 크기 (A4 595 × 842pt 를 dp 로)
    private static final int LAYOUT_WIDTH_DP = 595;
    private static final int LAYOUT_HEIGHT_DP = 842;

    // 인쇄용 비트맵: A4 150dpi (약 8.7MB)
    private static final int BITMAP_WIDTH = 1240;
    private static final int BITMAP_HEIGHT = 1754;

    private QrPoster() {
    }

    @SuppressLint("InflateParams") // 화면에 붙이지 않는 뷰라 부모가 없음
    @NonNull
    static Bitmap render(@NonNull Context context, @NonNull String storeId,
                         @NonNull String storeName, int entryLimitMinutes) {
        View poster = LayoutInflater.from(context).inflate(R.layout.view_qr_poster, null, false);
        ((TextView) poster.findViewById(R.id.poster_store_name)).setText(storeName);
        ((TextView) poster.findViewById(R.id.poster_step3_body))
                .setText(context.getString(R.string.poster_step3_body, entryLimitMinutes));
        ((ImageView) poster.findViewById(R.id.poster_qr)).setImageDrawable(
                new QrDrawable(StoreQr.contentFor(storeId), ContextCompat.getColor(context, R.color.on_surface)));

        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(LAYOUT_WIDTH_DP * density);
        int height = Math.round(LAYOUT_HEIGHT_DP * density);
        poster.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        poster.layout(0, 0, width, height);

        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale((float) BITMAP_WIDTH / width, (float) BITMAP_HEIGHT / height);
        poster.draw(canvas);
        return bitmap;
    }
}
