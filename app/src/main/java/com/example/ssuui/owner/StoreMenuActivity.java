package com.example.ssuui.owner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.example.ssuui.StoreDraft;

import java.text.NumberFormat;
import java.util.Locale;

/** 대기열 만들기 2 / 3 · 메뉴 (선택). 메뉴 추가·수정은 MenuEditActivity 에서 */
public class StoreMenuActivity extends StoreStepActivity {

    private LinearLayout list;
    private View emptyView;

    private final ActivityResultLauncher<Intent> editMenu = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null) return;
                StoreDraft.Menu menu = MenuEditActivity.menuFrom(data);
                if (menu == null) return;
                putMenu(MenuEditActivity.indexFrom(data), menu);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpStep(R.layout.activity_store_menu, 2, R.string.menu_heading, R.string.menu_description);

        list = findViewById(R.id.menu_list);
        emptyView = findViewById(R.id.menu_empty);

        findViewById(R.id.menu_add).setOnClickListener(v ->
                editMenu.launch(MenuEditActivity.newIntent(this, null, MenuEditActivity.NEW_MENU)));
        findViewById(R.id.menu_next).setOnClickListener(v -> openNextStep(StoreSettingsActivity.class));

        render();
    }

    @Override
    protected void saveInputs() {
        // 메뉴 목록은 고칠 때마다 draft.menus 에 바로 반영됨
    }

    @Override
    protected void onDraftReturned() {
        render();
    }

    /** 새 메뉴는 끝에 붙이고, 고친 메뉴는 제자리에. 대표 메뉴는 하나만 남긴다 */
    private void putMenu(int index, @NonNull StoreDraft.Menu menu) {
        if (menu.representative) {
            for (StoreDraft.Menu other : draft.menus) other.representative = false;
        }
        if (index >= 0 && index < draft.menus.size()) {
            draft.menus.set(index, menu);
        } else {
            draft.menus.add(menu);
        }
        render();
    }

    private void render() {
        list.removeAllViews();
        for (int i = 0; i < draft.menus.size(); i++) {
            list.addView(createRow(i, draft.menus.get(i)));
        }
        emptyView.setVisibility(draft.menus.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private View createRow(int index, @NonNull StoreDraft.Menu menu) {
        View row = getLayoutInflater().inflate(R.layout.item_store_menu, list, false);
        ((TextView) row.findViewById(R.id.menu_row_name)).setText(menu.name);
        ((TextView) row.findViewById(R.id.menu_row_price)).setText(menu.price == null
                ? getString(R.string.menu_price_none)
                : getString(R.string.menu_price, NumberFormat.getNumberInstance(Locale.KOREA).format(menu.price)));
        row.findViewById(R.id.menu_row_badge).setVisibility(menu.representative ? View.VISIBLE : View.GONE);

        ImageView photo = row.findViewById(R.id.menu_row_photo);
        if (menu.photoUri != null) {
            photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(Uri.parse(menu.photoUri)).centerCrop().into(photo);
        }

        View remove = row.findViewById(R.id.menu_row_remove);
        remove.setContentDescription(getString(R.string.menu_remove, menu.name));
        remove.setOnClickListener(v -> Screens.confirmRemove(this,
                getString(R.string.menu_remove_title, menu.name), () -> {
                    draft.menus.remove(menu);
                    render();
                }));

        row.setOnClickListener(v -> editMenu.launch(MenuEditActivity.newIntent(this, menu, index)));
        return row;
    }
}
