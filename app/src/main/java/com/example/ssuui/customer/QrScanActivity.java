package com.example.ssuui.customer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.ssuui.R;
import com.example.ssuui.Screens;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 매장 QR 스캔 (Figma: C2). 뒤 카메라로 QR 을 찾다가 마음대기 매장 QR 이면 대기 등록 화면으로 넘어간다.
 * 카메라 권한이 없으면 창 안에서 권한을 요청하고, 다시 묻지 않음으로 거절했으면 설정 화면을 연다.
 */
public class QrScanActivity extends AppCompatActivity implements StoreQrAnalyzer.Listener {

    private static final long MESSAGE_SHOW_MS = 2500;
    private static final long SCAN_LINE_MS = 1600;

    private PreviewView preview;
    private View scanWindow;
    private View scanLine;
    private View problem;
    private TextView problemText;
    private Button problemAction;
    private TextView message;
    private MaterialButton torch;

    private ObjectAnimator scanLineAnimator;
    private ExecutorService analysisExecutor;
    @Nullable private ImageAnalysis analysis;
    @Nullable private ProcessCameraProvider cameraProvider;
    @Nullable private Camera camera;
    private boolean cameraStarting;
    private boolean storeFound; // 매장 QR 을 찾은 뒤에 들어오는 결과는 무시

    private final Runnable hideMessage = () -> message.setVisibility(View.INVISIBLE);

    private final ActivityResultLauncher<String> requestCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    showPermissionProblem();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_qr_scan);

        // 어두운 화면이라 상태 표시줄·내비게이션 바 아이콘은 밝게
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(false);

        preview = findViewById(R.id.scan_preview);
        scanWindow = findViewById(R.id.scan_window);
        scanLine = findViewById(R.id.scan_line);
        problem = findViewById(R.id.scan_problem);
        problemText = findViewById(R.id.scan_problem_text);
        problemAction = findViewById(R.id.scan_problem_action);
        message = findViewById(R.id.scan_message);
        torch = findViewById(R.id.scan_torch);

        // 카메라 미리보기와 어두운 막은 화면 끝까지, 상단 바·안내·손전등만 시스템 바 안쪽으로
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan_overlay), (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ((ScanDimView) findViewById(R.id.scan_dim)).setWindow(scanWindow);

        findViewById(R.id.scan_close).setOnClickListener(v -> finish());
        // 버튼은 누르면 바로 켜짐 표시가 바뀌고, 실제 손전등 상태(TorchState)를 받아 다시 맞춘다
        torch.setOnClickListener(v -> {
            if (camera != null) camera.getCameraControl().enableTorch(torch.isChecked());
        });

        scanLineAnimator = ObjectAnimator.ofFloat(scanLine, View.TRANSLATION_Y, 0f, 0f);
        scanLineAnimator.setDuration(SCAN_LINE_MS);
        scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanLineAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        analysisExecutor = Executors.newSingleThreadExecutor();

        if (hasCameraPermission()) {
            startCamera();
        } else if (savedInstanceState == null) {
            requestCamera.launch(Manifest.permission.CAMERA);
        } else {
            // 화면이 다시 만들어진 경우: 떠 있던 권한 요청 결과는 그대로 requestCamera 로 온다
            showPermissionProblem();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 설정 화면에서 권한을 켜고 돌아온 경우
        if (camera == null && !cameraStarting && problem.getVisibility() == View.VISIBLE && hasCameraPermission()) {
            startCamera();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (scanLine.getVisibility() == View.VISIBLE) startScanLine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        scanLineAnimator.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        message.removeCallbacks(hideMessage);
        if (analysis != null) analysis.clearAnalyzer();
        analysisExecutor.shutdown();
    }

    // ───────────────────────────── 카메라 ─────────────────────────────

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        if (cameraStarting || camera != null) return;
        cameraStarting = true;
        problem.setVisibility(View.GONE);
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            cameraStarting = false;
            if (Screens.isGone(this)) return;
            try {
                ProcessCameraProvider provider = future.get();
                cameraProvider = provider;
                bindCamera(provider);
            } catch (ExecutionException e) {
                showProblem(R.string.scan_camera_error);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                showProblem(R.string.scan_camera_error);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@NonNull ProcessCameraProvider provider) {
        Preview previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 분석이 밀리면 지난 프레임은 버림
                .build();
        imageAnalysis.setAnalyzer(analysisExecutor, new StoreQrAnalyzer(this));
        analysis = imageAnalysis;

        Camera bound;
        try {
            provider.unbindAll();
            bound = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, imageAnalysis);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 뒤 카메라가 없거나 다른 앱이 쓰고 있을 때
            showProblem(R.string.scan_camera_error);
            return;
        }
        camera = bound;

        if (bound.getCameraInfo().hasFlashUnit()) {
            torch.setVisibility(View.VISIBLE);
            bound.getCameraInfo().getTorchState().observe(this,
                    state -> torch.setChecked(state != null && state == TorchState.ON));
        }
        showScanning();
    }

    // ───────────────────────────── QR 결과 (분석 스레드 → 화면) ─────────────────────────────

    @Override
    public void onStoreQr(@NonNull String storeId) {
        runOnUiThread(() -> openStore(storeId));
    }

    @Override
    public void onOtherQr() {
        runOnUiThread(() -> {
            if (storeFound || Screens.isGone(this)) return;
            message.setVisibility(View.VISIBLE);
            message.removeCallbacks(hideMessage);
            message.postDelayed(hideMessage, MESSAGE_SHOW_MS);
        });
    }

    private void openStore(@NonNull String storeId) {
        if (storeFound || Screens.isGone(this)) return;
        storeFound = true;
        if (cameraProvider != null) cameraProvider.unbindAll(); // 카메라·분석을 바로 멈춤
        scanWindow.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        startActivity(QueueJoinActivity.newIntent(this, storeId));
        finish();
    }

    // ───────────────────────────── 화면 상태 ─────────────────────────────

    private void showScanning() {
        problem.setVisibility(View.GONE);
        scanLine.setVisibility(View.VISIBLE);
        startScanLine();
    }

    private void showPermissionProblem() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showProblem(R.string.scan_permission_needed, R.string.scan_permission_allow,
                    v -> requestCamera.launch(Manifest.permission.CAMERA));
        } else {
            // "다시 묻지 않음": 권한 창이 더 뜨지 않으니 앱 설정 화면으로
            showProblem(R.string.scan_permission_settings, R.string.scan_open_settings,
                    v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null))));
        }
    }

    /** 창 안에 문제를 보여 준다 (해결 버튼 없음) */
    private void showProblem(@StringRes int text) {
        scanLineAnimator.cancel();
        scanLine.setVisibility(View.INVISIBLE);
        torch.setVisibility(View.GONE);
        message.removeCallbacks(hideMessage);
        message.setVisibility(View.INVISIBLE);
        problem.setVisibility(View.VISIBLE);
        problemText.setText(text);
        problemAction.setVisibility(View.GONE);
    }

    /** 창 안에 문제와 해결 버튼을 보여 준다 */
    private void showProblem(@StringRes int text, @StringRes int action, @NonNull View.OnClickListener onAction) {
        showProblem(text);
        problemAction.setVisibility(View.VISIBLE);
        problemAction.setText(action);
        problemAction.setOnClickListener(onAction);
    }

    private void startScanLine() {
        scanWindow.post(() -> {
            // 라인이 창 위쪽 여백(marginTop)에서 시작해 아래쪽 같은 여백까지
            float margin = scanLine.getTop() - scanWindow.getTop();
            scanLineAnimator.setFloatValues(0f, scanWindow.getHeight() - scanLine.getHeight() - 2 * margin);
            if (!scanLineAnimator.isStarted()) scanLineAnimator.start();
        });
    }
}
