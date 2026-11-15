package com.example.ssuui.map;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssuui.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final LatLng CAMPUS = new LatLng(37.4963, 126.9575); // 숭실대
    private static final double START_ZOOM = 15.5;
    private static final int SHEET_PEEK_DP = 340; // 접힌 시트 높이: 제목 + 목록 3줄 정도 (activity_map.xml과 같은 값)
    private static final float WALK_METERS_PER_MINUTE = 67f; // 시속 약 4km
    private static final float MIN_MOVE_TO_REFRESH_METERS = 30f;

    private View root;
    private View topPanel;
    private View sheet;
    private TextView countView;
    private View emptyView;
    private RecyclerView storeList;
    private int listBasePaddingBottom;

    private BottomSheetBehavior<View> sheetBehavior;
    private StoreListAdapter adapter;
    private MarkerIconFactory iconFactory;
    @Nullable private NaverMap naverMap;

    private final List<MapStore> allStores = new ArrayList<>();
    private final List<StoreListAdapter.Row> visibleRows = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();

    private double myLat = CAMPUS.latitude;
    private double myLng = CAMPUS.longitude;
    private boolean hasLocationFix = false;
    @Nullable private String bestId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow()); // MainActivity와 같은 방식: 지도가 상태 표시줄 뒤까지
        setContentView(R.layout.activity_map);

        root = findViewById(R.id.map_root);
        topPanel = findViewById(R.id.map_top_panel);
        sheet = findViewById(R.id.map_bottom_sheet);
        countView = findViewById(R.id.map_store_count);
        emptyView = findViewById(R.id.map_empty);
        storeList = findViewById(R.id.map_store_list);

        MaterialToolbar toolbar = findViewById(R.id.map_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setUpList();
        setUpBottomSheet();
        setUpInsets();

        iconFactory = new MarkerIconFactory(this);
        showStores(DummyStores.load()); // TODO: Firestore stores 컬렉션으로 교체

        attachMap();
    }

    // ───────────────────────────── 화면 준비 ─────────────────────────────

    private void setUpList() {
        adapter = new StoreListAdapter();
        storeList.setLayoutManager(new LinearLayoutManager(this));
        storeList.setAdapter(adapter);
        listBasePaddingBottom = storeList.getPaddingBottom();
    }

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

        map.addOnLocationChangeListener(this::onMyLocationChanged);
        map.setLocationSource(new DemoLocationSource(CAMPUS));
        map.setLocationTrackingMode(LocationTrackingMode.Follow);

        updateMapPadding();
        renderMarkers();
    }


    private void updateMapPadding() {
        if (naverMap == null || root.getHeight() == 0) return;
        int top = topPanel.getBottom();
        int bottom = Math.max(0, root.getHeight() - sheet.getTop());
        naverMap.setContentPadding(0, top, 0, bottom);
        naverMap.getUiSettings().setLogoMargin(dp(12), 0, 0, bottom + dp(8));
    }

    private void onMyLocationChanged(@NonNull Location location) {
        if (hasLocationFix) {
            float[] moved = new float[1];
            Location.distanceBetween(myLat, myLng, location.getLatitude(), location.getLongitude(), moved);
            if (moved[0] < MIN_MOVE_TO_REFRESH_METERS) return;
        }
        hasLocationFix = true;
        myLat = location.getLatitude();
        myLng = location.getLongitude();
        refresh();
    }

    private void renderMarkers() {
        if (naverMap == null) return;

        for (Marker marker : markers) {
            marker.setMap(null);
        }
        markers.clear();

        for (StoreListAdapter.Row row : visibleRows) {
            MapStore store = row.store;
            MarkerIconFactory.Style style = styleOf(store);

            Marker marker = new Marker(new LatLng(store.getLat(), store.getLng()));
            marker.setIcon(iconFactory.create(markerLabel(store), style));
            marker.setZIndex(style == MarkerIconFactory.Style.BEST ? 2
                    : style == MarkerIconFactory.Style.PAUSED ? 0 : 1);
            marker.setMap(naverMap);
            markers.add(marker);
        }
    }

    @NonNull
    private MarkerIconFactory.Style styleOf(@NonNull MapStore store) {
        if (!store.acceptsQueue()) return MarkerIconFactory.Style.PAUSED;
        if (store.getId().equals(bestId)) return MarkerIconFactory.Style.BEST;
        return MarkerIconFactory.Style.NORMAL;
    }

    @NonNull
    private String markerLabel(@NonNull MapStore store) {
        return store.acceptsQueue()
                ? getString(R.string.map_marker_waiting, store.getName(), store.getWaitingCount())
                : getString(R.string.map_marker_paused, store.getName());
    }

    // ───────────────────────────── 데이터 → 화면 ─────────────────────────────

    private void showStores(@NonNull List<MapStore> stores) {
        allStores.clear();
        allStores.addAll(stores);
        refresh();
    }

    private void refresh() {
        visibleRows.clear();
        for (MapStore store : allStores) {
            if (store.hiddenOnMap()) continue;
            visibleRows.add(new StoreListAdapter.Row(store, walkMinutesTo(store)));
        }
        Collections.sort(visibleRows, MapActivity::compareRows);
        bestId = findBestId(visibleRows);

        adapter.submit(visibleRows);
        countView.setText(getString(R.string.map_store_count, visibleRows.size()));
        emptyView.setVisibility(visibleRows.isEmpty() ? View.VISIBLE : View.GONE);
        renderMarkers();
    }

    // 접수중단은 맨 아래, 나머지는 대기 적은 순 → 가까운 순
    private static int compareRows(@NonNull StoreListAdapter.Row a, @NonNull StoreListAdapter.Row b) {
        if (a.store.acceptsQueue() != b.store.acceptsQueue()) {
            return a.store.acceptsQueue() ? -1 : 1;
        }
        int byTeams = Integer.compare(a.store.getWaitingCount(), b.store.getWaitingCount());
        return byTeams != 0 ? byTeams : Integer.compare(a.walkMinutes, b.walkMinutes);
    }

    @Nullable
    private static String findBestId(@NonNull List<StoreListAdapter.Row> rows) {
        StoreListAdapter.Row best = null;
        for (StoreListAdapter.Row row : rows) {
            if (!row.store.acceptsQueue()) continue;
            if (best == null
                    || row.store.getWaitingCount() < best.store.getWaitingCount()
                    || (row.store.getWaitingCount() == best.store.getWaitingCount()
                        && row.walkMinutes < best.walkMinutes)) {
                best = row;
            }
        }
        return best == null ? null : best.store.getId();
    }

    private int walkMinutesTo(@NonNull MapStore store) {
        float[] meters = new float[1];
        Location.distanceBetween(myLat, myLng, store.getLat(), store.getLng(), meters);
        return Math.max(1, Math.round(meters[0] / WALK_METERS_PER_MINUTE));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}