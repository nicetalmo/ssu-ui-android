package com.example.ssuui.customer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ssuui.QueueManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 대기 등록 화면에서 보여 주는 매장 정보. 필드는 StoreManager 가 만드는 stores 문서와 같음 */
final class StoreDetail {

    static final class Menu {
        final String name;
        @Nullable final Long price;
        @Nullable final String photoUrl;
        final boolean representative;

        Menu(String name, @Nullable Long price, @Nullable String photoUrl, boolean representative) {
            this.name = name;
            this.price = price;
            this.photoUrl = photoUrl;
            this.representative = representative;
        }
    }

    final String id;
    final String name;
    @Nullable final String intro;
    final List<String> photoUrls;
    final List<Menu> menus;           // 대표 메뉴가 맨 앞
    @Nullable final String notice;
    @Nullable final String status;     // "open", "paused", "closed"
    @Nullable final String pauseReason;
    final long waitingCount;
    final long maxTeams;
    final long minutesPerTeam;
    final long entryLimitMinutes;

    StoreDetail(String id, String name, @Nullable String intro, List<String> photoUrls, List<Menu> menus,
                @Nullable String notice, @Nullable String status, @Nullable String pauseReason,
                long waitingCount, long maxTeams, long minutesPerTeam, long entryLimitMinutes) {
        this.id = id;
        this.name = name;
        this.intro = intro;
        this.photoUrls = photoUrls;
        this.menus = menus;
        this.notice = notice;
        this.status = status;
        this.pauseReason = pauseReason;
        this.waitingCount = waitingCount;
        this.maxTeams = maxTeams;
        this.minutesPerTeam = minutesPerTeam;
        this.entryLimitMinutes = entryLimitMinutes;
    }

    @NonNull
    static StoreDetail from(@NonNull DocumentSnapshot snap) {
        List<String> photos = new ArrayList<>();
        Object rawPhotos = snap.get("photoUrls");
        if (rawPhotos instanceof List) {
            for (Object url : (List<?>) rawPhotos) {
                if (url instanceof String) photos.add((String) url);
            }
        }

        List<Menu> menus = new ArrayList<>();
        Object rawMenus = snap.get("menu");
        if (rawMenus instanceof List) {
            for (Object item : (List<?>) rawMenus) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> menu = (Map<?, ?>) item;
                Object name = menu.get("name");
                if (!(name instanceof String)) continue;
                Object price = menu.get("price");
                Object photo = menu.get("photoUrl");
                menus.add(new Menu((String) name,
                        price instanceof Number ? ((Number) price).longValue() : null,
                        photo instanceof String ? (String) photo : null,
                        Boolean.TRUE.equals(menu.get("representative"))));
            }
        }
        // 대표 메뉴를 맨 앞으로 (나머지는 사장님이 등록한 순서 그대로)
        Collections.sort(menus, (a, b) -> Boolean.compare(b.representative, a.representative));

        String name = snap.getString("name");
        return new StoreDetail(snap.getId(), name != null ? name : "", snap.getString("intro"), photos, menus,
                snap.getString("notice"), snap.getString("status"), snap.getString("pauseReason"),
                longOf(snap, "waitingCount", 0), longOf(snap, "maxTeams", Long.MAX_VALUE),
                longOf(snap, "minutesPerTeam", 0), longOf(snap, "entryLimitMinutes", 0));
    }

    /** 지금 대기 등록을 받지 않는 이유, 받으면 null (등록 기준은 QueueManager 와 같음) */
    @Nullable
    String blockReason() {
        return QueueManager.blockReason(status, pauseReason, waitingCount, maxTeams);
    }

    /** 지금 등록하면 예상 대기 시간(분) */
    long expectedWaitMinutes() {
        return waitingCount * minutesPerTeam;
    }

    private static long longOf(DocumentSnapshot snap, String field, long fallback) {
        Long value = snap.getLong(field);
        return value == null ? fallback : value;
    }
}
