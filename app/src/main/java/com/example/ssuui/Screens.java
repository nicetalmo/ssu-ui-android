package com.example.ssuui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

// 사장님·손님 화면들이 같이 쓰는 작은 도우미
public final class Screens {

    private Screens() {
    }

    /**
     * MainActivity 와 같은 방식(화면을 시스템 바 뒤까지)으로 layout 을 붙이고,
     * 상태 표시줄·내비게이션 바·키보드 높이만큼 화면 바깥쪽에 여백을 준다.
     */
    public static void setContentView(@NonNull AppCompatActivity activity, @LayoutRes int layout) {
        WindowCompat.enableEdgeToEdge(activity.getWindow());
        activity.setContentView(layout);
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }

    /** 비동기 응답이 왔을 때 화면이 아직 살아 있는지 */
    public static boolean isGone(@NonNull Activity activity) {
        return activity.isFinishing() || activity.isDestroyed();
    }

    /** 입력값 그대로 (비밀번호: 공백도 글자) */
    @NonNull
    public static String rawText(@NonNull EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    /** 앞뒤 공백을 뺀 입력값 */
    @NonNull
    public static String text(@NonNull EditText input) {
        return rawText(input).trim();
    }

    /** 앞뒤 공백을 뺀 입력값, 비어 있으면 null (선택 입력 칸) */
    @Nullable
    public static String textOrNull(@NonNull EditText input) {
        String text = text(input);
        return text.isEmpty() ? null : text;
    }

    /** 폼 아래 오류 문구(Widget.Maeum.ErrorText). null 이면 숨김 */
    public static void showError(@NonNull TextView errorView, @Nullable String message) {
        errorView.setText(message);
        errorView.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    /** 사진·메뉴 빼기처럼 되돌리기 번거로운 동작 전에 한 번 묻는다 */
    public static void confirmRemove(@NonNull Activity activity, @NonNull CharSequence title, @NonNull Runnable onRemove) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setPositiveButton(R.string.owner_remove, (dialog, which) -> onRemove.run())
                .setNegativeButton(R.string.owner_cancel, null)
                .show();
    }

    public static void confirmRemove(@NonNull Activity activity, @StringRes int title, @NonNull Runnable onRemove) {
        confirmRemove(activity, activity.getString(title), onRemove);
    }
}
