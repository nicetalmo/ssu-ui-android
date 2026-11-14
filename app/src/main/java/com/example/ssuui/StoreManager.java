package com.example.ssuui;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 사용 방법

 StoreManager.getInstance().createStore(StoreDraft d, new StoreManager.CreateCallback() {
            @Override
            public void onSuccess(String storeId) {
                성공시 Intent에 storeId 값을 넣어서 O3 인쇄 화면으로 전달하기 등 성공 처리
            }

            @Override
            public void onFailure(String message) {
                draft에 입력한 값이 그대로 남아있으므로 토스트를 띄우는 등 잘못된 부분만 알려주고
                기존 입력값은 유지하기 등 재입력 처리
                혹은 이미 만든 대기열이 있다면 O4 관리 화면으로 넘어가기
            }
        });
 */

public class StoreManager {

    public interface CreateCallback {
        void onSuccess(String storeId);
        void onFailure(String message);
    }

    public interface StoreIdCallback{
        void onResult(@Nullable String storeId);
        void onFailure(String message);
    }

    private static final List<Integer> ENTRY_LIMIT_OPTIONS = Arrays.asList(3, 5, 10);

    private static StoreManager instance;

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private StoreManager() {}

    public static synchronized StoreManager getInstance() {
        if (instance == null) instance = new StoreManager();
        return instance;
    }

    // -------------------------- 로그인 직후 --------------------------
    public void getMyStoreId(StoreIdCallback callback){
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null){
            callback.onFailure("로그인이 필요해요");
            return;
        }

        // 지점 고유 아이디 가져오기 "storeId"를 통해 대기 관리 화면 이동
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snap -> callback.onResult(snap.getString("storeId")))
                .addOnFailureListener(e -> callback.onFailure("매장 정보를 불러오지 못했어요."));
    }

    // -------------------------- 대기열 생성 --------------------------
    public void createStore(StoreDraft draft, CreateCallback callback){
        FirebaseUser user = mAuth.getCurrentUser();

        // 로그인 중인지 확인
        if (user == null){
            callback.onFailure("로그인이 필요해요");
            return;
        }

        // 사전에 관리자가 설정한 데이터에 문제가 없는지 확인
        String error = validata(draft);
        if(error != null){
            callback.onFailure(error);
            return;
        }

        String uid = user.getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        // 로그인 및 데이터에 문제가 없다면 대기열 생성
        userRef.get()
                .addOnSuccessListener(snap -> {
                    if(snap.getString("storeId") != null){
                        callback.onFailure("이미 만든 대기열이 있어요.");
                        return;
                    }

                    DocumentReference storeRef = db.collection("stores").document();
                    uploadPhotosAndSave(uid, userRef, storeRef, draft, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("인터넷 연결을 확인해 주세요."));
    }

    // 매장 및 메뉴 사진 업로드
    private void uploadPhotosAndSave(String uid, DocumentReference userRef, DocumentReference storeRef, StoreDraft draft, CreateCallback callback) {
        String folder = "stores/" + storeRef.getId() + "/";

        List<String> menuPhotoUris = new ArrayList<>();
        for (StoreDraft.Menu m : draft.menus) menuPhotoUris.add(m.photoUri);

        Task<List<String>> storePhotos = uploadAll(draft.photoUris, folder + "store_");
        Task<List<String>> menuPhotos = uploadAll(menuPhotoUris, folder + "menu_");

        Tasks.whenAllSuccess(storePhotos, menuPhotos)
                .addOnSuccessListener(results -> {
                    List<String> storeUrls = storePhotos.getResult();
                    List<String> menuUrls = menuPhotos.getResult();
                    saveStore(uid, userRef, storeRef, draft, storeUrls, menuUrls, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("사진을 올리지 못했어요. 다시 시도해 주세요."));
    }

    private void saveStore(String uid, DocumentReference userRef, DocumentReference storeRef, StoreDraft d, List<String> storeUrls, List<String> menuUrls, CreateCallback callback){
        List<Map<String, Object>> menus = new ArrayList<>();
        for(int i = 0; i< d.menus.size(); i++){
            StoreDraft.Menu m = d.menus.get(i);
            Map<String, Object> menu = new HashMap<>();
            menu.put("id", "m" + i);
            menu.put("name", m.name.trim());
            menu.put("price", m.price);
            menu.put("photoUrl", menuUrls.get(i));
            menu.put("representative", m.representative);
            menus.add(menu);
        }

        Map<String, Object> store = new HashMap<>();

        // 소유 / 권한
        store.put("ownerUid", uid);
        store.put("adminUids", Arrays.asList(uid));    // 여러 관리자 초대 시 이 부분에 추가
        store.put("plan", "free");                  // free / pro

        // 1 / 3 매장 정보
        store.put("name", d.name.trim());
        store.put("intro", emptyToNull(d.intro));
        store.put("photoUrls", storeUrls);

        // 2 / 3 메뉴
        store.put("menu", menus);

        // 3 / 3 운영 설정
        store.put("minutesPerTeam", d.minutesPerTeam);
        store.put("maxTeams", d.maxTeams);
        store.put("entryLimitMinutes", d.entryLimitMinutes);
        store.put("notice", d.notice);

        // 접수 상태 (04 일시중단 버튼)
        store.put("status", "open");        // "open", "paused", "closed"
        store.put("pauseReason", null);     // "재료 소진", "휴식 시간", "영업 시간"

        // 대기열 카운터
        store.put("lastNumber", 0);         // 마지막 발급 대기 번호
        store.put("waitingCount", 0);       // 현재 대기팀 수

        // 매장 문서 생성
        WriteBatch batch = db.batch();
        batch.set(storeRef, store);
        batch.update(userRef, "storeId", storeRef.getId());
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess((storeRef.getId())))
                .addOnFailureListener(e -> callback.onFailure("대기열을 만들지 못했어요. 다시 시도해 주세요."));
    }

    private Task<List<String>> uploadAll(List<String> uris, String pathPrefix){
        boolean hasAny = false;
        for (String u : uris) if(u != null) {
            hasAny = true;
            break;
        }

        if(!hasAny){
            List<String> nulls = new ArrayList<>();
            for (int i = 0; i<uris.size(); i++) nulls.add(null);
            return Tasks.forResult(nulls);
        }

        StorageReference root = FirebaseStorage.getInstance().getReference();
        List<Task<String>> tasks = new ArrayList<>();
        for (int i = 0; i<uris.size(); i++){
            String u = uris.get(i);
            if(u == null){
                tasks.add(Tasks.forResult(null));
                continue;
            }

            StorageReference ref = root.child(pathPrefix + i + ".jpg");
            tasks.add(ref.putFile(Uri.parse(u))
                    .continueWithTask(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return ref.getDownloadUrl();
                    })
                    .continueWith(t -> t.getResult().toString()));
        }

        return Tasks.whenAllSuccess(tasks).continueWith(t -> {
            List<String> urls = new ArrayList<>();
            for (Object o : t.getResult()) urls.add((String) o);
            return urls;
        });
    }

    private String validata(StoreDraft d){
        if (d.name == null || d.name.trim().isEmpty()) return "상호명을 입력해 주세요.";
        if (d.name.trim().length() > 30) return "상호명은 30자 이내로 입력해 주세요.";
        if (d.intro != null && d.intro.length() > 60) return "매장 소개는 60자 이내로 입력해 주세요.";
        if (d.photoUris.size() > 3) return "매장 사진은 최대 3장까지 올릴 수 있어요.";

        int reps = 0;
        for (StoreDraft.Menu m : d.menus) {
            if (m.name == null || m.name.trim().isEmpty()) return "메뉴 이름을 입력해 주세요.";
            if (m.price != null && m.price < 0) return "메뉴 가격을 확인해 주세요.";
            if (m.representative) reps++;
        }
        if (reps > 1) return "대표 메뉴는 하나만 정할 수 있어요.";

        if (d.minutesPerTeam < 1 || d.minutesPerTeam > 120) return "1팀당 소요시간을 확인해 주세요.";
        if (d.maxTeams < 1 || d.maxTeams > 500) return "최대 팀 수를 확인해 주세요.";
        if (!ENTRY_LIMIT_OPTIONS.contains(d.entryLimitMinutes)) return "입장 제한시간은 3, 5, 10분 중에서 골라 주세요.";
        return null;
    }

    private String emptyToNull(String s){
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /*
     2026 / 11 / 14 까지의 기록
     AuthManager.java
     StoreDraft.java
     StoreManager.java
     구현
     */
}
