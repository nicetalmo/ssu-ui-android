package com.example.ssuui.owner;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.ssuui.AuthManager;
import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** 대기열 만들기 1 / 3 · 매장 정보 (상호명, 매장 소개, 매장 사진 최대 3장) */
public class StoreInfoActivity extends StoreStepActivity {

    private static final int MAX_PHOTOS = 3; // StoreManager 의 매장 사진 제한과 같음

    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextInputEditText introInput;
    private LinearLayout photoRow;
    private View addPhotoTile;
    private TextView photoCount;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null || draft.photoUris.size() >= MAX_PHOTOS) return;
                draft.photoUris.add(uri.toString());
                renderPhotos();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpStep(R.layout.activity_store_info, 1, R.string.info_heading, R.string.info_description);

        // 첫 단계에서는 다른 계정으로 바꿀 수 있게 로그아웃을 둔다
        MaterialToolbar toolbar = findViewById(R.id.step_toolbar);
        toolbar.inflateMenu(R.menu.menu_owner_account);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_logout) return false;
            AuthManager.getInstance().signOut();
            finish();
            return true;
        });

        nameLayout = findViewById(R.id.info_name_layout);
        nameInput = findViewById(R.id.info_name);
        introInput = findViewById(R.id.info_intro);
        photoRow = findViewById(R.id.info_photo_row);
        addPhotoTile = findViewById(R.id.info_photo_add);
        photoCount = findViewById(R.id.info_photo_count);

        nameInput.setText(draft.name);
        introInput.setText(draft.intro);

        addPhotoTile.setOnClickListener(v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        findViewById(R.id.info_next).setOnClickListener(v -> next());

        renderPhotos();
    }

    private void next() {
        saveInputs();
        if (draft.name == null) {
            nameLayout.setError(getString(R.string.info_name_error));
            nameInput.requestFocus();
            return;
        }
        nameLayout.setError(null);
        openNextStep(StoreMenuActivity.class);
    }

    @Override
    protected void saveInputs() {
        draft.name = Screens.textOrNull(nameInput);
        draft.intro = Screens.textOrNull(introInput);
    }

    @Override
    protected void onDraftReturned() {
        renderPhotos();
    }

    @Override
    protected void onStoreCreated(@NonNull String storeId) {
        startActivity(QrPosterActivity.newIntent(this, storeId));
        finish();
    }

    /** 고른 사진 칸을 "사진 추가" 칸 앞에 다시 채운다 */
    private void renderPhotos() {
        photoRow.removeViews(0, photoRow.getChildCount() - 1);
        for (int i = 0; i < draft.photoUris.size(); i++) {
            String uri = draft.photoUris.get(i);
            View tile = getLayoutInflater().inflate(R.layout.item_store_photo, photoRow, false);
            ImageView image = tile.findViewById(R.id.photo_image);
            Glide.with(this).load(Uri.parse(uri)).centerCrop().into(image);
            tile.setContentDescription(getString(R.string.info_photo_description, i + 1));
            tile.setOnClickListener(v -> Screens.confirmRemove(this, R.string.photo_remove_title, () -> {
                draft.photoUris.remove(uri);
                renderPhotos();
            }));
            photoRow.addView(tile, i);
        }
        addPhotoTile.setVisibility(draft.photoUris.size() < MAX_PHOTOS ? View.VISIBLE : View.GONE);
        photoCount.setText(getString(R.string.info_photo_count, draft.photoUris.size(), MAX_PHOTOS));
    }
}
