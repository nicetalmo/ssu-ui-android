package com.example.ssuui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;

import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.example.ssuui.StoreDraft;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * 대기열 만들기 단계 화면(1 / 3 매장 정보 → 2 / 3 메뉴 → 3 / 3 운영 설정)의 공통 틀.
 * <p>
 * 단계마다 Activity 하나이고, 쓰고 있는 StoreDraft 를 Intent 에 담아 다음 단계로 넘긴다.
 * 뒤로 가면 지금까지 쓴 내용을 이전 단계에 돌려줘서 다시 앞으로 와도 입력이 남아 있고,
 * 마지막 단계에서 대기열이 만들어지면 storeId 가 첫 단계까지 돌아가 QR 포스터 화면이 열린다.
 */
abstract class StoreStepActivity extends AppCompatActivity {

    private static final int STEP_COUNT = 3;
    private static final String EXTRA_DRAFT = "com.example.ssuui.owner.extra.DRAFT";
    private static final String EXTRA_STORE_ID = "com.example.ssuui.owner.extra.STORE_ID";

    protected StoreDraft draft;

    private final ActivityResultLauncher<Intent> nextStep = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onNextStepResult);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StoreDraft restored = savedInstanceState != null
                ? BundleCompat.getSerializable(savedInstanceState, EXTRA_DRAFT, StoreDraft.class)
                : IntentCompat.getSerializableExtra(getIntent(), EXTRA_DRAFT, StoreDraft.class);
        draft = restored != null ? restored : new StoreDraft();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isBusy()) return;
                saveInputs();
                setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_DRAFT, draft));
                finish();
            }
        });
    }

    /** layout 을 붙이고 상단 바와 "n / 3" 단계 제목을 채운다 (view_store_step_bar, view_store_step_heading) */
    protected final void setUpStep(@LayoutRes int layout, int step,
                                   @StringRes int heading, @StringRes int description) {
        Screens.setContentView(this, layout);

        MaterialToolbar toolbar = findViewById(R.id.step_toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        LinearProgressIndicator progress = findViewById(R.id.step_progress);
        progress.setMax(STEP_COUNT);
        progress.setProgress(step);

        ((TextView) findViewById(R.id.step_label)).setText(getString(R.string.store_step, step, STEP_COUNT));
        ((TextView) findViewById(R.id.step_heading)).setText(heading);
        ((TextView) findViewById(R.id.step_description)).setText(description);
    }

    /** 화면에 입력한 값을 draft 에 옮긴다 (다음 단계로 갈 때, 뒤로 갈 때, 화면 상태 저장 때) */
    protected abstract void saveInputs();

    /** draft 가 다음 단계에서 돌아온 값으로 바뀌었을 때 다시 그릴 것이 있으면 */
    protected void onDraftReturned() {
    }

    /** 대기열을 만드는 중처럼 화면을 떠나면 안 되는 동안 true */
    protected boolean isBusy() {
        return false;
    }

    protected final void openNextStep(@NonNull Class<? extends StoreStepActivity> step) {
        saveInputs();
        nextStep.launch(new Intent(this, step).putExtra(EXTRA_DRAFT, draft));
    }

    /** 대기열이 만들어졌을 때. 첫 단계(StoreInfoActivity)만 QR 포스터 화면을 열고, 나머지는 앞 단계로 넘긴다 */
    protected void onStoreCreated(@NonNull String storeId) {
        finishWithStore(storeId);
    }

    protected final void finishWithStore(@NonNull String storeId) {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_STORE_ID, storeId));
        finish();
    }

    private void onNextStepResult(@NonNull ActivityResult result) {
        Intent data = result.getData();
        if (data == null) return;

        String storeId = data.getStringExtra(EXTRA_STORE_ID);
        if (result.getResultCode() == RESULT_OK && storeId != null) {
            onStoreCreated(storeId);
            return;
        }

        StoreDraft returned = IntentCompat.getSerializableExtra(data, EXTRA_DRAFT, StoreDraft.class);
        if (returned != null) {
            draft = returned;
            onDraftReturned();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInputs();
        outState.putSerializable(EXTRA_DRAFT, draft);
    }
}
