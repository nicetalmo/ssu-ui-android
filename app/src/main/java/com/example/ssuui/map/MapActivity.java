package com.example.ssuui.map;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.example.ssuui.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final LatLng CAMPUS = new LatLng(37.4963, 126.9575); // 숭실대
    private static final double START_ZOOM = 15.5;
    private static final int SHEET_PEEK_DP = 340; // 접힌 시트 높이: 제목 + 목록 3줄 정도 (activity_map.xml과 같은 값)

    private View root;
    private View topPanel;
    private View sheet;
    private View storeList;
    private int listBasePaddingBottom;

    private BottomSheetBehavior<View> sheetBehavior;
    @Nullable private NaverMap naverMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow()); // MainActivity와 같은 방식: 지도가 상태 표시줄 뒤까지
        setContentView(R.layout.activity_map);

        root = findViewById(R.id.map_root);
        topPanel = findViewById(R.id.map_top_panel);
        sheet = findViewById(R.id.map_bottom_sheet);
        storeList = findViewById(R.id.map_store_list);
        listBasePaddingBottom = storeList.getPaddingBottom();

        MaterialToolbar toolbar = findViewById(R.id.map_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setUpBottomSheet();
        setUpInsets();
        attachMap();
    }

    // ───────────────────────────── 화면 준비 ─────────────────────────────

    private void setUpBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(sheet);
        sheetBehavior.setHideable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                updateMapPadding();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                updateMapPadding();
            }
        });
        root.addOnLayoutChangeListener((v, left, top, right, bottom,
                                        oldLeft, oldTop, oldRight, oldBottom) -> root.post(this::fitSheetToScreen));
    }


    private void fitSheetToScreen() {
        int rootHeight = root.getHeight();
        if (rootHeight == 0) return;

        int peek = Math.min(dp(SHEET_PEEK_DP), rootHeight / 2);
        if (sheetBehavior.getPeekHeight() != peek) {
            sheetBehavior.setPeekHeight(peek);
        }

        int sheetHeight = rootHeight - topPanel.getBottom();
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        if (sheetHeight > 0 && params.height != sheetHeight) {
            params.height = sheetHeight;
            sheet.setLayoutParams(params);
        }
        updateMapPadding();
    }

    /** 상태 표시줄·내비게이션 바 높이만큼 상단 바와 목록 끝에 여백을 준다. */
    private void setUpInsets() {
        View titleBar = findViewById(R.id.map_title_bar);
        ViewCompat.setOnApplyWindowInsetsListener(topPanel, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            titleBar.setPadding(bars.left, bars.top, bars.right, 0);
            storeList.setPadding(storeList.getPaddingLeft(), storeList.getPaddingTop(),
                    storeList.getPaddingRight(), listBasePaddingBottom + bars.bottom);
            return insets;
        });
    }

    // ───────────────────────────── 지도 ─────────────────────────────

    private void attachMap() {
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .camera(new CameraPosition(CAMPUS, START_ZOOM))
                    .zoomControlEnabled(false);
            mapFragment = MapFragment.newInstance(options);
            fm.beginTransaction().add(R.id.map_container, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;

        UiSettings ui = map.getUiSettings();
        ui.setZoomControlEnabled(false);
        ui.setScaleBarEnabled(false);
        ui.setCompassEnabled(false);
        ui.setTiltGesturesEnabled(false);
        ui.setRotateGesturesEnabled(false);
        ui.setLocationButtonEnabled(false);

        updateMapPadding();
    }


    private void updateMapPadding() {
        if (naverMap == null || root.getHeight() == 0) return;
        int top = topPanel.getBottom();
        int bottom = Math.max(0, root.getHeight() - sheet.getTop());
        naverMap.setContentPadding(0, top, 0, bottom);
        naverMap.getUiSettings().setLogoMargin(dp(12), 0, 0, bottom + dp(8));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}