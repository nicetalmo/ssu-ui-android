package com.example.ssuui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.ssuui.R;
import com.google.android.material.chip.Chip;
import com.naver.maps.map.overlay.OverlayImage;

// 마커 칩을 비트맵으로 그려 지도 마커 아이콘으로 사용
final class MarkerIconFactory {

    enum Style { NORMAL, BEST, PAUSED }

    private final Context context;
    private final LayoutInflater inflater;

    MarkerIconFactory(@NonNull Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @SuppressLint("InflateParams")
    @NonNull
    OverlayImage create(@NonNull String label, @NonNull Style style) {
        int layout = style == Style.BEST ? R.layout.view_map_marker_primary : R.layout.view_map_marker;
        Chip chip = (Chip) inflater.inflate(layout, null, false);
        chip.setText(label);
        if (style == Style.PAUSED) {
            chip.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            chip.setChipIconTintResource(R.color.outline);
        }
        return OverlayImage.fromBitmap(drawToBitmap(chip));
    }

    @NonNull
    private static Bitmap drawToBitmap(@NonNull View view) {
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(unspecified, unspecified);
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        view.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }
}
