package com.example.ssuui.owner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import com.example.ssuui.AuthManager;
import com.example.ssuui.R;
import com.example.ssuui.StoreManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * 매장 QR 포스터 미리보기와 인쇄.
 * 매장 정보(상호명, 입장 제한시간)를 불러와 포스터를 그리고, 안드로이드 인쇄 화면으로 보낸다
 * (인쇄 화면에서 "PDF로 저장"도 고를 수 있음).
 */
public class QrPosterActivity extends AppCompatActivity {

    private static final String EXTRA_STORE_ID = "com.example.ssuui.owner.extra.STORE_ID";
    private static final int DEFAULT_ENTRY_LIMIT_MINUTES = 3;

    private String storeId;

    private View content;
    private View progress;
    private View errorGroup;
    private TextView errorText;
    private ImageView posterImage;
    private Button printButton;

    @Nullable private Bitmap poster;
    @Nullable private String storeName;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String storeId) {
        return new Intent(context, QrPosterActivity.class).putExtra(EXTRA_STORE_ID, storeId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storeId = getIntent().getStringExtra(EXTRA_STORE_ID);
        if (storeId == null) {
            finish();
            return;
        }
        OwnerScreen.setContentView(this, R.layout.activity_qr_poster);

        MaterialToolbar toolbar = findViewById(R.id.poster_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_logout) return false;
            AuthManager.getInstance().signOut();
            finish();
            return true;
        });

        content = findViewById(R.id.poster_content);
        progress = findViewById(R.id.poster_progress);
        errorGroup = findViewById(R.id.poster_error);
        errorText = findViewById(R.id.poster_error_text);
        posterImage = findViewById(R.id.poster_image);
        printButton = findViewById(R.id.poster_print);

        findViewById(R.id.poster_retry).setOnClickListener(v -> load());
        printButton.setOnClickListener(v -> print());

        load();
    }

    private void load() {
        showState(progress);
        StoreManager.getInstance().getStore(storeId, new StoreManager.StoreCallback() {
            @Override
            public void onSuccess(DocumentSnapshot store) {
                if (OwnerScreen.isGone(QrPosterActivity.this)) return;
                String name = store.getString("name");
                Long entryLimit = store.getLong("entryLimitMinutes");
                storeName = name != null ? name : "";
                poster = QrPoster.render(QrPosterActivity.this, storeId, storeName,
                        entryLimit != null ? entryLimit.intValue() : DEFAULT_ENTRY_LIMIT_MINUTES);
                posterImage.setImageBitmap(poster);
                posterImage.setContentDescription(getString(R.string.poster_image_description, storeName));
                showState(content);
            }

            @Override
            public void onFailure(String message) {
                if (OwnerScreen.isGone(QrPosterActivity.this)) return;
                errorText.setText(message);
                showState(errorGroup);
            }
        });
    }

    private void print() {
        if (poster == null) return;
        PrintHelper printHelper = new PrintHelper(this);
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        printHelper.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        printHelper.printBitmap(getString(R.string.poster_print_job, storeName), poster);
    }

    /** 불러오는 중 / 포스터 / 오류 중 하나만 보여 준다 */
    private void showState(@NonNull View visible) {
        progress.setVisibility(visible == progress ? View.VISIBLE : View.GONE);
        errorGroup.setVisibility(visible == errorGroup ? View.VISIBLE : View.GONE);
        content.setVisibility(visible == content ? View.VISIBLE : View.INVISIBLE);
        printButton.setEnabled(visible == content);
    }
}
