package com.example.ssuui;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/*
 사용 방법

 AuthManager.getInstance().signUp(아이디, 패스워드, 폰넘버, new AuthManager.AuthCallback() {
    public void onSuccess(FirebaseUser user) { ... 성공 시 처리( O2 화면으로 이동 등 ) ... }
    public void onFailure(String message) { ... 실패 시 처리( 토스트 및 재로그인 등 ) ... }
 });

 */

public class AuthManager {

    public interface AuthCallback{
        void onSuccess(FirebaseUser user);
        void onFailure(String message);
    }

    private static AuthManager instance;

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private AuthManager() {}

    public static synchronized AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    // 회원가입
    public void signUp(String id, String password, String phone, AuthCallback callback){
        String error = validataSignUp(id, password, phone);
        if(error != null){
            callback.onFailure(error);
            return;
        }

        mAuth.createUserWithEmailAndPassword(toEmail(id), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    saveUserDoc(user, id, phone, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(toMessage(e)));
    }

    // Firestore로 회원 정보 저장
    private void saveUserDoc(FirebaseUser user, String id, String phone, AuthCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("loginId", normalizeId(id));
        data.put("phone", phone);
        data.put("storeId", null);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(e -> {
                    user.delete();
                    callback.onFailure("가입 정보를 저장하지 못했어요. 다시 시도해 주세요");
                });
    }

    // 로그인
    public void signIn(String id, String password, AuthCallback callback){
        if(isEmpty(id) || isEmpty(password)){
            callback.onFailure("아이디와 비밀번호를 입력해 주세요");
            return;
        }
        mAuth.signInWithEmailAndPassword(toEmail(id), password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(e -> callback.onFailure(toMessage(e)));
    }

    // 로그아웃
    public void signOut(){
        mAuth.signOut();
    }

    // 현재 Firebase 상의 유저 정보 가져오기
    public FirebaseUser getCurrentUser(){
        return mAuth.getCurrentUser();
    }

    // 현재 로그인 중인지 상태 반환 null 이면 로그아웃 상태
    public boolean isLoggedIn(){
        return mAuth.getCurrentUser() != null;
    }

    // id 일반화
    private String normalizeId(String id){
        return id.trim().toLowerCase();
    }

    // id -> 임의의 email 형식으로 변환
    private String toEmail(String id){
        return normalizeId(id) + "@maeumdaegi.app";
    }

    // id, password, phone 조건 확인
    private String validataSignUp(String id, String password, String phone){
        if (isEmpty(id) || !id.trim().matches("[A-Za-z0-9]{4,12}"))
            return "아이디는 영문, 숫자 4~12자로 입력해 주세요.";
        if (isEmpty(password) || password.length() < 6)
            return "비밀번호는 6자 이상으로 입력해 주세요.";
        if (isEmpty(phone) || !phone.replace("-", "").matches("01[0-9]{8,9}"))
            return "휴대전화 번호를 확인해 주세요.";
        return null;
    }

    // 회원가입 및 로그인 시 오류 메시지 반환
    private String toMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException)
            return "이미 사용 중인 아이디예요.";
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException)
            return "아이디 또는 비밀번호가 올바르지 않아요.";
        if (e instanceof FirebaseNetworkException)
            return "인터넷 연결을 확인해 주세요.";
        return "오류가 발생했어요. 잠시 후 다시 시도해 주세요.";
    }

    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}
