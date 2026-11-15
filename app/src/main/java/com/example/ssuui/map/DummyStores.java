package com.example.ssuui.map;

import java.util.ArrayList;
import java.util.List;

// 서버 연결 전 테스트용 데이터
public final class DummyStores {

    private DummyStores() {
    }

    public static List<MapStore> load() {
        final String open = MapStore.STATUS_OPEN;
        final String paused = MapStore.STATUS_PAUSED;

        List<MapStore> list = new ArrayList<>();
        list.add(new MapStore("cafe-ongi",   "카페 온기",          37.49714, 126.95631,  0,  5, false, open));
        list.add(new MapStore("sc-tteok",    "학생회 떡볶이 부스",  37.49481, 126.95992,  3,  8, true,  open));
        list.add(new MapStore("ss-pasta",    "성수동 파스타",      37.49875, 126.95981, 12, 10, false, open));
        list.add(new MapStore("gamja-jip",   "감자집",            37.49363, 126.95337,  0,  6, false, paused));
        list.add(new MapStore("sd-gukbap",   "상도역 국밥",        37.50130, 126.95100, 15,  7, false, open));
        list.add(new MapStore("club-skewer", "총동아리 닭꼬치 부스", 37.49670, 126.95970, 20,  3, true,  open));
        return list;
    }
}
