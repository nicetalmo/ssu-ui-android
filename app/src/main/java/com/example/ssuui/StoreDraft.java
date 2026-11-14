package com.example.ssuui;

import android.view.Menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 사용 방법
 StoreDraft sd = new StoreDraft();

 sd.name = "~식당";
 sd.intro = "~~ 입니다.";
 ...
 sd.menus.add(new StoreDraft.Menu(메뉴이름, 가격, 사진uri, 대표 메뉴 유무))
 ...

 필드 기본값 전부 설정하기.
 */

public class StoreDraft {

    // 매장 정보
    public String name;
    public String intro;
    public List<String> photoUris = new ArrayList<>();

    // 메뉴 등록
    public List<Menu> menus = new ArrayList<>();

    // 운영 설정
    public int minutesPerTeam;
    public int maxTeams;
    public int entryLimitMinutes;
    public String notice;

    public static class Menu {
        public String name;
        public Integer price;
        public String photoUri;
        public boolean representative;

        public Menu() {}

        public Menu(String name, Integer price, String photoUri, boolean representative){
            this.name = name;
            this.price = price;
            this.photoUri = photoUri;
            this.representative = representative;
        }
    }
}
