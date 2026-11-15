package com.example.ssuui.map;

import android.location.Location;

import androidx.annotation.NonNull;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationSource;

// 에뮬레이터 시연용: 내 위치를 한 곳으로 고정
final class DemoLocationSource implements LocationSource {

    private final Location location;

    DemoLocationSource(@NonNull LatLng position) {
        location = new Location("demo");
        location.setLatitude(position.latitude);
        location.setLongitude(position.longitude);
        location.setAccuracy(10f);
        location.setTime(System.currentTimeMillis());
    }

    @Override
    public void activate(@NonNull LocationSource.OnLocationChangedListener listener) {
        listener.onLocationChanged(location);
    }

    @Override
    public void deactivate() {
    }
}
