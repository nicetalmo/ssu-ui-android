package com.example.ssuui.owner;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssuui.AuthManager;
import com.example.ssuui.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

/**
 * 사장님 회원가입. 로그인 화면에서 열고, 가입에 성공하면 RESULT_OK 로 돌아간다
 * (가입하면 그 계정으로 로그인된 상태라 로그인 화면이 바로 대기열 만들기로 이어 감).
 * 입력 조건과 오류 문구는 AuthManager 가 정한 것을 그대로 보여 준다.
 */
public class OwnerSignUpActivity extends AppCompatActivity {

    private TextInputEditText idInput;
    private TextInputEditText passwordInput;
    private TextInputEditText phoneInput;
    private TextView errorView;
    private Button submitButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OwnerScreen.setContentView(this, R.layout.activity_owner_sign_up);

        MaterialToolbar toolbar = findViewById(R.id.signup_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        idInput = findViewById(R.id.signup_id);
        passwordInput = findViewById(R.id.signup_password);
        phoneInput = findViewById(R.id.signup_phone);
        errorView = findViewById(R.id.signup_error);
        submitButton = findViewById(R.id.signup_submit);

        phoneInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });
        submitButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        if (!submitButton.isEnabled()) return;
        OwnerScreen.showError(errorView, null);
        setBusy(true);
        AuthManager.getInstance().signUp(OwnerScreen.text(idInput), OwnerScreen.rawText(passwordInput),
                OwnerScreen.text(phoneInput),
                new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        if (OwnerScreen.isGone(OwnerSignUpActivity.this)) return;
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (OwnerScreen.isGone(OwnerSignUpActivity.this)) return;
                        setBusy(false);
                        OwnerScreen.showError(errorView, message);
                    }
                });
    }

    private void setBusy(boolean busy) {
        submitButton.setEnabled(!busy);
        idInput.setEnabled(!busy);
        passwordInput.setEnabled(!busy);
        phoneInput.setEnabled(!busy);
        submitButton.setText(busy ? R.string.signup_submitting : R.string.signup_submit);
    }
}
