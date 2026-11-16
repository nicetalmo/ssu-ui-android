package com.example.ssuui.owner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.bumptech.glide.Glide;
import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.example.ssuui.StoreDraft;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 메뉴 하나 추가·수정 (대기열 만들기 2 / 3 에서 연다).
 * 저장하면 RESULT_OK 와 함께 메뉴와 목록 위치(새 메뉴는 NEW_MENU)를 돌려준다.
 */
public class MenuEditActivity extends AppCompatActivity {

    static final int NEW_MENU = -1;

    private static final String EXTRA_MENU = "com.example.ssuui.owner.extra.MENU";
    private static final String EXTRA_INDEX = "com.example.ssuui.owner.extra.MENU_INDEX";
    private static final String STATE_PHOTO = "photo";

    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextInputEditText priceInput;
    private MaterialSwitch representativeSwitch;
    private View photoAddTile;
    private View photoTile;
    private ImageView photoImage;

    private int index;
    @Nullable private String photoUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                photoUri = uri.toString();
                renderPhoto();
            });

    @NonNull
    static Intent newIntent(@NonNull Context context, @Nullable StoreDraft.Menu menu, int index) {
        Intent intent = new Intent(context, MenuEditActivity.class).putExtra(EXTRA_INDEX, index);
        if (menu != null) intent.putExtra(EXTRA_MENU, menu);
        return intent;
    }

    @Nullable
    static StoreDraft.Menu menuFrom(@NonNull Intent result) {
        return IntentCompat.getSerializableExtra(result, EXTRA_MENU, StoreDraft.Menu.class);
    }

    static int indexFrom(@NonNull Intent result) {
        return result.getIntExtra(EXTRA_INDEX, NEW_MENU);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.setContentView(this, R.layout.activity_menu_edit);

        index = getIntent().getIntExtra(EXTRA_INDEX, NEW_MENU);
        boolean editing = index != NEW_MENU;

        MaterialToolbar toolbar = findViewById(R.id.menu_edit_toolbar);
        toolbar.setTitle(editing ? R.string.menu_edit_title_edit : R.string.menu_edit_title_add);
        toolbar.setNavigationOnClickListener(v -> finish());

        nameLayout = findViewById(R.id.menu_edit_name_layout);
        nameInput = findViewById(R.id.menu_edit_name);
        priceInput = findViewById(R.id.menu_edit_price);
        representativeSwitch = findViewById(R.id.menu_edit_representative);
        photoAddTile = findViewById(R.id.menu_edit_photo_add);
        photoTile = findViewById(R.id.menu_edit_photo);
        photoImage = photoTile.findViewById(R.id.photo_image);

        Button save = findViewById(R.id.menu_edit_save);
        save.setText(editing ? R.string.menu_edit_save_edit : R.string.menu_edit_save_add);
        save.setOnClickListener(v -> save());

        photoAddTile.setOnClickListener(v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        photoTile.setContentDescription(getString(R.string.menu_edit_photo_description));
        photoTile.setOnClickListener(v -> Screens.confirmRemove(this, R.string.photo_remove_title, () -> {
            photoUri = null;
            renderPhoto();
        }));

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getString(STATE_PHOTO);
        } else {
            StoreDraft.Menu menu = IntentCompat.getSerializableExtra(getIntent(), EXTRA_MENU, StoreDraft.Menu.class);
            if (menu != null) {
                nameInput.setText(menu.name);
                priceInput.setText(menu.price == null ? null : String.valueOf(menu.price));
                representativeSwitch.setChecked(menu.representative);
                photoUri = menu.photoUri;
            }
        }
        renderPhoto();
    }

    private void save() {
        String name = Screens.text(nameInput);
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.menu_edit_name_error));
            nameInput.requestFocus();
            return;
        }
        nameLayout.setError(null);

        // 숫자 키패드 + 최대 7자리라 int 범위 안의 0 이상 정수만 들어옴
        String price = Screens.text(priceInput);
        StoreDraft.Menu menu = new StoreDraft.Menu(name,
                price.isEmpty() ? null : Integer.valueOf(price),
                photoUri,
                representativeSwitch.isChecked());

        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_MENU, menu)
                .putExtra(EXTRA_INDEX, index));
        finish();
    }

    private void renderPhoto() {
        photoAddTile.setVisibility(photoUri == null ? View.VISIBLE : View.GONE);
        photoTile.setVisibility(photoUri == null ? View.GONE : View.VISIBLE);
        if (photoUri != null) {
            Glide.with(this).load(Uri.parse(photoUri)).centerCrop().into(photoImage);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PHOTO, photoUri);
    }
}
