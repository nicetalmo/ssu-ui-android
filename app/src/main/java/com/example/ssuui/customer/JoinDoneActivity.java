package com.example.ssuui.customer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssuui.MainActivity;
import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.google.android.material.appbar.MaterialToolbar;

/** 대기 등록 완료 (손님). 발급된 대기 번호와 내 앞 대기·예상 시간·인원, 입장 제한시간을 보여 준다 */
public class JoinDoneActivity extends AppCompatActivity {

    private static final String EXTRA_STORE_NAME = "com.example.ssuui.customer.extra.STORE_NAME";
    private static final String EXTRA_NUMBER = "com.example.ssuui.customer.extra.NUMBER";
    private static final String EXTRA_TEAMS_AHEAD = "com.example.ssuui.customer.extra.TEAMS_AHEAD";
    private static final String EXTRA_MINUTES_PER_TEAM = "com.example.ssuui.customer.extra.MINUTES_PER_TEAM";
    private static final String EXTRA_PARTY_SIZE = "com.example.ssuui.customer.extra.PARTY_SIZE";
    private static final String EXTRA_ENTRY_LIMIT = "com.example.ssuui.customer.extra.ENTRY_LIMIT";

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String storeName, long number, long teamsAhead,
                            long minutesPerTeam, int partySize, long entryLimitMinutes) {
        return new Intent(context, JoinDoneActivity.class)
                .putExtra(EXTRA_STORE_NAME, storeName)
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_TEAMS_AHEAD, teamsAhead)
                .putExtra(EXTRA_MINUTES_PER_TEAM, minutesPerTeam)
                .putExtra(EXTRA_PARTY_SIZE, partySize)
                .putExtra(EXTRA_ENTRY_LIMIT, entryLimitMinutes);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.setContentView(this, R.layout.activity_join_done);

        MaterialToolbar toolbar = findViewById(R.id.done_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.done_confirm).setOnClickListener(v -> backToMain());

        Intent intent = getIntent();
        long teamsAhead = intent.getLongExtra(EXTRA_TEAMS_AHEAD, 0);
        long entryLimit = intent.getLongExtra(EXTRA_ENTRY_LIMIT, 0);

        ((TextView) findViewById(R.id.done_store)).setText(intent.getStringExtra(EXTRA_STORE_NAME));
        ((TextView) findViewById(R.id.done_number)).setText(String.valueOf(intent.getLongExtra(EXTRA_NUMBER, 0)));
        ((TextView) findViewById(R.id.done_ahead)).setText(getString(R.string.done_ahead_value, teamsAhead));
        ((TextView) findViewById(R.id.done_wait)).setText(teamsAhead == 0
                ? getString(R.string.done_wait_now)
                : getString(R.string.done_wait_value, teamsAhead * intent.getLongExtra(EXTRA_MINUTES_PER_TEAM, 0)));
        ((TextView) findViewById(R.id.done_party)).setText(
                getString(R.string.join_party_value, intent.getIntExtra(EXTRA_PARTY_SIZE, 0)));

        if (entryLimit > 0) {
            ((TextView) findViewById(R.id.done_entry_notice)).setText(getString(R.string.done_entry_notice, entryLimit));
        } else {
            findViewById(R.id.done_entry_card).setVisibility(View.GONE);
        }
    }

    /** 메인으로 (QR 링크로 바로 들어와 메인이 없던 경우에도 메인을 연다) */
    private void backToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
