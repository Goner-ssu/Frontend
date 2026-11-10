package com.goner.app.friend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.InviteLink;
import com.goner.app.common.TopBar;
import com.goner.app.data.model.User;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.UserRepo;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// QR 스캔 (CameraX 미리보기 + ML Kit 로 QR 읽기). 읽으면 멈추고 친구 찾아서 시트로 보여줌
// QR 내용: 초대 링크(https://goner-ssu.web.app/i/{code}) 또는 코드 그대로 둘 다 됨
public class QrScanActivity extends AppCompatActivity {

    private final FriendRepo friendRepo = new FriendRepo();
    private final UserRepo userRepo = new UserRepo();
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();
    private BarcodeScanner scanner;
    private Camera camera;
    private boolean torchOn;
    private volatile boolean scanning = true; // 하나 읽으면 false (시트 닫으면 다시 true)

    private View layoutScan, layoutPermission, buttonFlash;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else showPermission(true);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        layoutScan = findViewById(R.id.layout_scan);
        layoutPermission = findViewById(R.id.layout_permission);
        buttonFlash = findViewById(R.id.button_flash);
        TopBar.bind(findViewById(R.id.top_bar), getString(R.string.qr_title), v -> finish());

        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        // 코드로 검색 = 친구 추가 화면(여기로 들어온 곳)으로 돌아가기
        findViewById(R.id.button_search_code).setOnClickListener(v -> finish());
        findViewById(R.id.button_search_direct).setOnClickListener(v -> finish());
        findViewById(R.id.button_settings).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null))));
        buttonFlash.setOnClickListener(v -> {
            if (camera == null) return;
            torchOn = !torchOn;
            camera.getCameraControl().enableTorch(torchOn);
        });

        if (hasCameraPermission()) startCamera();
        else permissionLauncher.launch(Manifest.permission.CAMERA);
    }

    // 설정에서 권한 켜고 돌아온 경우
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutPermission.getVisibility() == View.VISIBLE && hasCameraPermission()) startCamera();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermission(boolean show) {
        layoutPermission.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutScan.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void startCamera() {
        showPermission(false);
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(((PreviewView) findViewById(R.id.preview)).getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(analysisExecutor, this::analyze);

                provider.unbindAll();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                buttonFlash.setVisibility(camera.getCameraInfo().hasFlashUnit() ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 카메라 한 장면마다 불림 (백그라운드 스레드)
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyze(ImageProxy image) {
        if (!scanning || image.getImage() == null) {
            image.close();
            return;
        }
        InputImage input = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
        scanner.process(input)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode b : barcodes) {
                        String code = InviteLink.codeFrom(b.getRawValue());
                        if (code != null && scanning) {
                            scanning = false;
                            onCode(code);
                            break;
                        }
                    }
                })
                .addOnCompleteListener(t -> image.close());
    }

    private void onCode(String code) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
            scanning = true;
            return;
        }
        friendRepo.search(code, uid)
                .addOnSuccessListener(result -> {
                    if (result.state == FriendRepo.SearchResult.NOT_FOUND) {
                        Toast.makeText(this, R.string.add_not_found_title, Toast.LENGTH_SHORT).show();
                        scanning = true;
                        return;
                    }
                    showResultSheet(result, uid);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.add_search_failed, Toast.LENGTH_SHORT).show();
                    scanning = true;
                });
    }

    // 스캔 성공 시트 (Figma 376:378). 19번과 같은 예외: 내 코드 / 이미 친구 / 이미 요청 / 받은 요청은 수락
    private void showResultSheet(FriendRepo.SearchResult result, String uid) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.sheet_qr_result, null);
        User user = result.user;
        ((AvatarView) view.findViewById(R.id.avatar)).setUser(user.getUid(), user.getNickname(), user.getPhotoUrl());
        ((TextView) view.findViewById(R.id.text_name)).setText(result.state == FriendRepo.SearchResult.ME
                ? getString(R.string.add_me_name, user.getNickname()) : user.getNickname());
        ((TextView) view.findViewById(R.id.text_code)).setText(getString(R.string.create_friends_code, user.getFriendCode()));

        Button request = view.findViewById(R.id.button_request);
        setSheetButton(request, result.state);
        request.setOnClickListener(v -> {
            request.setEnabled(false);
            if (result.state == FriendRepo.SearchResult.RECEIVED) {
                friendRepo.accept(result.request)
                        .addOnSuccessListener(x -> {
                            setSheetButton(request, FriendRepo.SearchResult.FRIEND);
                            Toast.makeText(this, R.string.noti_accepted, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> actionFailed(request, result.state, e));
                return;
            }
            // 요청 보낼 때 내 정보 복사해야 해서 내 users 문서 먼저 읽음
            userRepo.getUser(uid)
                    .continueWithTask(task -> friendRepo.sendRequest(task.getResult(), user))
                    .addOnSuccessListener(r -> {
                        setSheetButton(request, FriendRepo.SearchResult.SENT);
                        Toast.makeText(this, R.string.add_sent, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> actionFailed(request, result.state, e));
        });
        view.findViewById(R.id.button_rescan).setOnClickListener(v -> sheet.dismiss());
        sheet.setOnDismissListener(d -> scanning = true);
        sheet.setContentView(view);
        sheet.show();
    }

    // 상태별 버튼 글자. 요청 보낼 수 있을 때(NONE)·수락(RECEIVED)만 눌림
    private void setSheetButton(Button button, int state) {
        switch (state) {
            case FriendRepo.SearchResult.ME:
                button.setText(R.string.add_my_code);
                break;
            case FriendRepo.SearchResult.FRIEND:
                button.setText(R.string.add_already_friend);
                break;
            case FriendRepo.SearchResult.SENT:
                button.setText(R.string.add_requested);
                break;
            case FriendRepo.SearchResult.RECEIVED:
                button.setText(R.string.noti_accept);
                break;
            default:
                button.setText(R.string.qr_send_request);
        }
        button.setEnabled(state == FriendRepo.SearchResult.NONE || state == FriendRepo.SearchResult.RECEIVED);
    }

    private void actionFailed(Button button, int state, Exception e) {
        setSheetButton(button, state);
        Toast.makeText(this, e.getMessage() == null ? getString(R.string.noti_action_failed) : e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        analysisExecutor.shutdown();
        scanner.close();
    }
}
