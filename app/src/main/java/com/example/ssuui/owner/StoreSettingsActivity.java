package com.example.ssuui.owner;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.example.ssuui.StoreManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 대기열 만들기 3 / 3 · 운영 설정 (1팀당 소요시간, 최대 대기 팀 수, 입장 제한시간, 공지사항).
 * "대기열 만들기"를 누르면 StoreManager.createStore 로 사진 업로드와 매장 저장을 하고,
 * 성공하면 storeId 를 앞 단계로 돌려 QR 포스터 화면으로 이어 간다.
 */
public class StoreSettingsActivity extends StoreStepActivity {

    // 처음 들어왔을 때 채워 두는 값 (포스터 기본 문구 "3분 안에 와 주세요"와 같은 입장 제한시간)
    private static final int DEFAULT_MINUTES_PER_TEAM = 5;
    private static final int DEFAULT_MAX_TEAMS = 30;
    private static final int DEFAULT_ENTRY_LIMIT_MINUTES = 3;

    // StoreManager 의 검사 범위와 같음
    private static final int MAX_MINUTES_PER_TEAM = 120;
    private static final int MAX_TEAMS = 500;

    private TextInputLayout minutesLayout;
    private TextInputEditText minutesInput;
    private TextInputLayout maxTeamsLayout;
    private TextInputEditText maxTeamsInput;
    private ChipGroup entryGroup;
    private TextInputEditText noticeInput;
    private TextView errorView;
    private LinearProgressIndicator progress;
    private Button submitButton;

    private boolean creating;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpStep(R.layout.activity_store_settings, 3, R.string.settings_heading, R.string.settings_description);

        minutesLayout = findViewById(R.id.settings_minutes_layout);
        minutesInput = findViewById(R.id.settings_minutes);
        maxTeamsLayout = findViewById(R.id.settings_max_teams_layout);
        maxTeamsInput = findViewById(R.id.settings_max_teams);
        entryGroup = findViewById(R.id.settings_entry_group);
        noticeInput = findViewById(R.id.settings_notice);
        errorView = findViewById(R.id.settings_error);
        progress = findViewById(R.id.settings_progress);
        submitButton = findViewById(R.id.settings_submit);

        minutesInput.setText(String.valueOf(draft.minutesPerTeam > 0 ? draft.minutesPerTeam : DEFAULT_MINUTES_PER_TEAM));
        maxTeamsInput.setText(String.valueOf(draft.maxTeams > 0 ? draft.maxTeams : DEFAULT_MAX_TEAMS));
        checkEntryLimit(draft.entryLimitMinutes > 0 ? draft.entryLimitMinutes : DEFAULT_ENTRY_LIMIT_MINUTES);
        noticeInput.setText(draft.notice);

        submitButton.setOnClickListener(v -> create());
    }

    @Override
    protected void saveInputs() {
        draft.minutesPerTeam = number(minutesInput);
        draft.maxTeams = number(maxTeamsInput);
        View checked = entryGroup.findViewById(entryGroup.getCheckedChipId());
        draft.entryLimitMinutes = checked == null ? 0 : Integer.parseInt((String) checked.getTag());
        draft.notice = Screens.textOrNull(noticeInput);
    }

    @Override
    protected boolean isBusy() {
        return creating;
    }

    private void create() {
        if (creating) return;
        saveInputs();
        if (!checkInputs()) return;

        setCreating(true);
        Screens.showError(errorView, null);
        StoreManager.getInstance().createStore(draft, new StoreManager.CreateCallback() {
            @Override
            public void onSuccess(String storeId) {
                if (Screens.isGone(StoreSettingsActivity.this)) return;
                finishWithStore(storeId);
            }

            @Override
            public void onFailure(String message) {
                if (Screens.isGone(StoreSettingsActivity.this)) return;
                setCreating(false);
                Screens.showError(errorView, message);
            }
        });
    }

    /** 이 단계 칸들은 칸 아래에 바로 오류를 보여 준다. 나머지는 createStore 가 돌려주는 문구로 */
    private boolean checkInputs() {
        boolean minutesOk = draft.minutesPerTeam >= 1 && draft.minutesPerTeam <= MAX_MINUTES_PER_TEAM;
        boolean teamsOk = draft.maxTeams >= 1 && draft.maxTeams <= MAX_TEAMS;
        minutesLayout.setError(minutesOk ? null : getString(R.string.settings_minutes_error));
        maxTeamsLayout.setError(teamsOk ? null : getString(R.string.settings_max_teams_error));
        if (!minutesOk) {
            minutesInput.requestFocus();
        } else if (!teamsOk) {
            maxTeamsInput.requestFocus();
        }
        return minutesOk && teamsOk;
    }

    private void checkEntryLimit(int minutes) {
        View chip = entryGroup.findViewWithTag(String.valueOf(minutes));
        entryGroup.check(chip != null ? chip.getId() : R.id.settings_entry_3);
    }

    private void setCreating(boolean creating) {
        this.creating = creating;
        progress.setVisibility(creating ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!creating);
        submitButton.setText(creating ? R.string.settings_submitting : R.string.settings_submit);
        minutesInput.setEnabled(!creating);
        maxTeamsInput.setEnabled(!creating);
        noticeInput.setEnabled(!creating);
        for (int i = 0; i < entryGroup.getChildCount(); i++) {
            entryGroup.getChildAt(i).setEnabled(!creating);
        }
    }

    /** 숫자 칸 값, 비어 있으면 0 (숫자 키패드 + 최대 3자리라 그 밖의 글자는 들어오지 않음) */
    private static int number(TextInputEditText input) {
        String text = Screens.text(input);
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }
}
