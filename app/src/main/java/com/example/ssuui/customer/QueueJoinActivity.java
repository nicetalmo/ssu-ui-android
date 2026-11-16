package com.example.ssuui.customer;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.example.ssuui.QueueManager;
import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.example.ssuui.StoreManager;
import com.example.ssuui.StoreQr;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 대기 등록 (손님). 매장 정보(사진·대기 현황·공지·메뉴)를 보여 주고 인원과 휴대전화 번호를 받아 대기열에 넣는다.
 * QR 스캔 화면에서 열거나, 기본 카메라 앱으로 매장 QR(maeumdaegi://store/{storeId})을 찍으면 바로 열린다.
 * 등록되면 등록 완료 화면(JoinDoneActivity)으로 넘어간다.
 */
public class QueueJoinActivity extends AppCompatActivity {

    private static final String EXTRA_STORE_ID = "com.example.ssuui.customer.extra.STORE_ID";
    private static final String STATE_PARTY_SIZE = "partySize";
    private static final int DEFAULT_PARTY_SIZE = 2;

    @Nullable private String storeId;
    @Nullable private StoreDetail store;
    private int partySize = DEFAULT_PARTY_SIZE;
    private boolean joining;

    private View content;
    private View progress;
    private View loadError;
    private TextView loadErrorText;
    private View retryButton;
    private View photosScroll;
    private LinearLayout photos;
    private TextView nameView;
    private TextView introView;
    private TextView waitTag;
    private TextView waitText;
    private View noticeCard;
    private TextView noticeText;
    private View menuSection;
    private LinearLayout menuList;
    private Button partyMinus;
    private Button partyPlus;
    private TextView partyValue;
    private TextInputEditText phoneInput;
    private CheckBox consent;
    private TextView errorView;
    private TextView blockedView;
    private Button submitButton;

    // 등록하는 동안에는 화면을 떠나지 않게 (결과를 받아 대기 번호를 보여 줘야 함)
    private final OnBackPressedCallback blockBackWhileJoining = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
        }
    };

    /** 다른 화면(QR 스캔, 지도의 매장 선택 등)에서 이 매장의 대기 등록 화면을 열 때 */
    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String storeId) {
        return new Intent(context, QueueJoinActivity.class).putExtra(EXTRA_STORE_ID, storeId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.setContentView(this, R.layout.activity_queue_join);

        MaterialToolbar toolbar = findViewById(R.id.join_toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, blockBackWhileJoining);

        content = findViewById(R.id.join_content);
        progress = findViewById(R.id.join_progress);
        loadError = findViewById(R.id.join_load_error);
        loadErrorText = findViewById(R.id.join_load_error_text);
        retryButton = findViewById(R.id.join_retry);
        photosScroll = findViewById(R.id.join_photos_scroll);
        photos = findViewById(R.id.join_photos);
        nameView = findViewById(R.id.join_name);
        introView = findViewById(R.id.join_intro);
        waitTag = findViewById(R.id.join_wait_tag);
        waitText = findViewById(R.id.join_wait_text);
        noticeCard = findViewById(R.id.join_notice);
        noticeText = findViewById(R.id.join_notice_text);
        menuSection = findViewById(R.id.join_menu_section);
        menuList = findViewById(R.id.join_menu_list);
        partyMinus = findViewById(R.id.join_party_minus);
        partyPlus = findViewById(R.id.join_party_plus);
        partyValue = findViewById(R.id.join_party_value);
        phoneInput = findViewById(R.id.join_phone);
        consent = findViewById(R.id.join_consent);
        errorView = findViewById(R.id.join_error);
        blockedView = findViewById(R.id.join_blocked);
        submitButton = findViewById(R.id.join_submit);

        if (savedInstanceState != null) {
            partySize = savedInstanceState.getInt(STATE_PARTY_SIZE, DEFAULT_PARTY_SIZE);
        }
        partyMinus.setOnClickListener(v -> setPartySize(partySize - 1));
        partyPlus.setOnClickListener(v -> setPartySize(partySize + 1));
        setPartySize(partySize);

        phoneInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });
        submitButton.setOnClickListener(v -> submit());
        retryButton.setOnClickListener(v -> load());

        // QR 스캔 화면에서 오면 extra, 기본 카메라 앱의 링크로 오면 maeumdaegi://store/{storeId}
        storeId = getIntent().getStringExtra(EXTRA_STORE_ID);
        if (storeId == null) storeId = StoreQr.parse(getIntent().getDataString());
        if (storeId == null) {
            showLoadError(getString(R.string.join_not_store_qr), false);
            return;
        }
        load();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PARTY_SIZE, partySize);
    }

    // ───────────────────────────── 매장 정보 ─────────────────────────────

    private void load() {
        if (storeId == null) return;
        showState(progress);
        StoreManager.getInstance().getStore(storeId, new StoreManager.StoreCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snap) {
                if (Screens.isGone(QueueJoinActivity.this)) return;
                render(StoreDetail.from(snap));
            }

            @Override
            public void onFailure(String message) {
                if (Screens.isGone(QueueJoinActivity.this)) return;
                showLoadError(message, true);
            }
        });
    }

    private void render(@NonNull StoreDetail detail) {
        store = detail;
        nameView.setText(detail.name);
        introView.setText(detail.intro);
        introView.setVisibility(detail.intro == null ? View.GONE : View.VISIBLE);

        photos.removeAllViews();
        for (int i = 0; i < detail.photoUrls.size(); i++) {
            ImageView photo = (ImageView) getLayoutInflater().inflate(R.layout.item_join_photo, photos, false);
            photo.setContentDescription(getString(R.string.join_photo_description, detail.name, i + 1));
            Glide.with(this).load(detail.photoUrls.get(i)).centerCrop().into(photo);
            photos.addView(photo);
        }
        photosScroll.setVisibility(detail.photoUrls.isEmpty() ? View.GONE : View.VISIBLE);

        renderWait(detail);

        noticeText.setText(detail.notice);
        noticeCard.setVisibility(detail.notice == null ? View.GONE : View.VISIBLE);

        menuList.removeAllViews();
        for (StoreDetail.Menu menu : detail.menus) {
            menuList.addView(createMenuRow(menu));
        }
        menuSection.setVisibility(detail.menus.isEmpty() ? View.GONE : View.VISIBLE);

        String blocked = detail.blockReason();
        blockedView.setText(blocked);
        blockedView.setVisibility(blocked == null ? View.GONE : View.VISIBLE);
        submitButton.setEnabled(blocked == null);

        showState(content);
    }

    /** 대기 현황 태그 + 설명 (상태 태그 스타일과 같은 색 짝, 회색 태그만 요약 패널 위에서 보이게 한 단계 진하게) */
    private void renderWait(@NonNull StoreDetail detail) {
        String blocked = detail.blockReason();
        if ("paused".equals(detail.status)) {
            paintWaitTag(getString(R.string.join_tag_paused), R.color.surface_container_high, R.color.outline, R.color.on_surface_variant);
        } else if (blocked != null && !"open".equals(detail.status)) {
            paintWaitTag(getString(R.string.join_tag_closed), R.color.surface_container_high, R.color.outline, R.color.on_surface_variant);
        } else if (blocked != null) {
            paintWaitTag(getString(R.string.join_tag_full), R.color.accent_container, R.color.accent, R.color.on_accent_container);
        } else if (detail.waitingCount == 0) {
            paintWaitTag(getString(R.string.join_tag_now), R.color.success_container, R.color.success, R.color.on_success_container);
        } else {
            paintWaitTag(getString(R.string.join_tag_waiting, detail.waitingCount),
                    R.color.primary_container, R.color.primary, R.color.on_primary_container);
        }

        // 받지 않는 이유는 등록 버튼 위(join_blocked)에 따로 보여 주므로 여기서는 현재 대기만
        if (blocked != null) {
            waitText.setText(getString(R.string.join_wait_count, detail.waitingCount, detail.minutesPerTeam));
        } else if (detail.waitingCount == 0) {
            waitText.setText(R.string.join_wait_now);
        } else {
            waitText.setText(getString(R.string.join_wait_expected, detail.expectedWaitMinutes(), detail.minutesPerTeam));
        }
    }

    private void paintWaitTag(@NonNull String text, @ColorRes int background, @ColorRes int dot, @ColorRes int content) {
        waitTag.setText(text);
        waitTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        TextViewCompat.setCompoundDrawableTintList(waitTag, ColorStateList.valueOf(ContextCompat.getColor(this, dot)));
        waitTag.setTextColor(ContextCompat.getColor(this, content));
    }

    @NonNull
    private View createMenuRow(@NonNull StoreDetail.Menu menu) {
        View row = getLayoutInflater().inflate(R.layout.item_join_menu, menuList, false);
        ((TextView) row.findViewById(R.id.menu_row_name)).setText(menu.name);
        ((TextView) row.findViewById(R.id.menu_row_price)).setText(menu.price == null
                ? getString(R.string.menu_price_none)
                : getString(R.string.menu_price, NumberFormat.getNumberInstance(Locale.KOREA).format(menu.price)));
        row.findViewById(R.id.menu_row_badge).setVisibility(menu.representative ? View.VISIBLE : View.GONE);
        if (menu.photoUrl != null) {
            ImageView photo = row.findViewById(R.id.menu_row_photo);
            photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(menu.photoUrl).centerCrop().into(photo);
        }
        return row;
    }

    // ───────────────────────────── 대기 등록 ─────────────────────────────

    private void setPartySize(int size) {
        partySize = Math.max(QueueManager.MIN_PARTY_SIZE, Math.min(QueueManager.MAX_PARTY_SIZE, size));
        partyValue.setText(getString(R.string.join_party_value, partySize));
        partyMinus.setEnabled(!joining && partySize > QueueManager.MIN_PARTY_SIZE);
        partyPlus.setEnabled(!joining && partySize < QueueManager.MAX_PARTY_SIZE);
    }

    private void submit() {
        StoreDetail detail = store;
        if (joining || detail == null || storeId == null || !submitButton.isEnabled()) return;
        if (!consent.isChecked()) {
            Screens.showError(errorView, getString(R.string.join_consent_needed));
            return;
        }
        Screens.showError(errorView, null);
        setJoining(true);
        QueueManager.getInstance().joinQueue(storeId, partySize, Screens.text(phoneInput),
                new QueueManager.JoinCallback() {
                    @Override
                    public void onSuccess(QueueManager.Ticket ticket) {
                        if (Screens.isGone(QueueJoinActivity.this)) return;
                        startActivity(JoinDoneActivity.newIntent(QueueJoinActivity.this, detail.name, ticket.number,
                                ticket.teamsAhead, detail.minutesPerTeam, partySize, detail.entryLimitMinutes));
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (Screens.isGone(QueueJoinActivity.this)) return;
                        setJoining(false);
                        Screens.showError(errorView, message);
                    }
                });
    }

    private void setJoining(boolean joining) {
        this.joining = joining;
        blockBackWhileJoining.setEnabled(joining);
        submitButton.setEnabled(!joining);
        submitButton.setText(joining ? R.string.join_submitting : R.string.join_submit);
        phoneInput.setEnabled(!joining);
        consent.setEnabled(!joining);
        setPartySize(partySize);
    }

    // ───────────────────────────── 화면 상태 ─────────────────────────────

    private void showLoadError(@NonNull String message, boolean canRetry) {
        loadErrorText.setText(message);
        retryButton.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        showState(loadError);
    }

    /** 불러오는 중 / 매장 정보 / 오류 중 하나만. 매장 정보가 있을 때만 등록 버튼을 쓸 수 있음 */
    private void showState(@NonNull View visible) {
        progress.setVisibility(visible == progress ? View.VISIBLE : View.GONE);
        loadError.setVisibility(visible == loadError ? View.VISIBLE : View.GONE);
        content.setVisibility(visible == content ? View.VISIBLE : View.INVISIBLE);
        if (visible != content) {
            submitButton.setEnabled(false);
            blockedView.setVisibility(View.GONE);
            Screens.showError(errorView, null);
        }
    }
}
