package com.example.ssuui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssuui.AuthManager;
import com.example.ssuui.R;
import com.example.ssuui.StoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

/**
 * 사장님 로그인 (사장님 화면의 시작).
 * 로그인되면 내 매장이 있는지 보고, 없으면 대기열 만들기(1 / 3)로, 있으면 그 매장의 QR 포스터 화면으로 간다.
 * 이미 로그인한 상태로 들어오면 폼 없이 바로 넘어간다.
 */
public class OwnerLoginActivity extends AppCompatActivity {

    private View form;
    private View actions;
    private View checking;
    private TextInputEditText idInput;
    private TextInputEditText passwordInput;
    private TextView errorView;
    private Button submitButton;
    private Button signUpButton;

    // 회원가입을 마치면 가입한 계정으로 로그인된 상태라 바로 내 매장으로 이어 간다
    private final ActivityResultLauncher<Intent> signUp = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setBusy(true);
                    openMyStore();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OwnerScreen.setContentView(this, R.layout.activity_owner_login);

        MaterialToolbar toolbar = findViewById(R.id.login_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        form = findViewById(R.id.login_form);
        actions = findViewById(R.id.login_actions);
        checking = findViewById(R.id.login_checking);
        idInput = findViewById(R.id.login_id);
        passwordInput = findViewById(R.id.login_password);
        errorView = findViewById(R.id.login_error);
        submitButton = findViewById(R.id.login_submit);
        signUpButton = findViewById(R.id.login_sign_up);

        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });
        submitButton.setOnClickListener(v -> submit());
        signUpButton.setOnClickListener(v ->
                signUp.launch(new Intent(this, OwnerSignUpActivity.class)));

        if (AuthManager.getInstance().isLoggedIn()) {
            showChecking(true);
            openMyStore();
        }
    }

    private void submit() {
        if (!submitButton.isEnabled()) return;
        OwnerScreen.showError(errorView, null);
        setBusy(true);
        AuthManager.getInstance().signIn(OwnerScreen.text(idInput), OwnerScreen.rawText(passwordInput),
                new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        if (OwnerScreen.isGone(OwnerLoginActivity.this)) return;
                        openMyStore();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (OwnerScreen.isGone(OwnerLoginActivity.this)) return;
                        setBusy(false);
                        OwnerScreen.showError(errorView, message);
                    }
                });
    }

    /** 로그인된 계정의 매장으로: 매장이 없으면 대기열 만들기, 있으면 QR 포스터 */
    private void openMyStore() {
        StoreManager.getInstance().getMyStoreId(new StoreManager.StoreIdCallback() {
            @Override
            public void onResult(@Nullable String storeId) {
                if (OwnerScreen.isGone(OwnerLoginActivity.this)) return;
                startActivity(storeId == null
                        ? new Intent(OwnerLoginActivity.this, StoreInfoActivity.class)
                        : QrPosterActivity.newIntent(OwnerLoginActivity.this, storeId));
                finish();
            }

            @Override
            public void onFailure(String message) {
                if (OwnerScreen.isGone(OwnerLoginActivity.this)) return;
                showChecking(false);
                setBusy(false);
                OwnerScreen.showError(errorView, message);
            }
        });
    }

    /** 이미 로그인한 계정을 확인하는 동안 폼 대신 진행 표시 */
    private void showChecking(boolean show) {
        checking.setVisibility(show ? View.VISIBLE : View.GONE);
        form.setVisibility(show ? View.GONE : View.VISIBLE);
        actions.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setBusy(boolean busy) {
        submitButton.setEnabled(!busy);
        signUpButton.setEnabled(!busy);
        idInput.setEnabled(!busy);
        passwordInput.setEnabled(!busy);
        submitButton.setText(busy ? R.string.login_submitting : R.string.login_submit);
    }
}
