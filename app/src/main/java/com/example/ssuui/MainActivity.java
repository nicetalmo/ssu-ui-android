package com.example.ssuui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ssuui.map.MapActivity;
import com.example.ssuui.owner.OwnerLoginActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.main_open_map).setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));
        // 사장님 화면 시작: 로그인 (이미 로그인했으면 로그인 화면이 바로 내 매장으로 넘겨 줌)
        findViewById(R.id.main_owner_start).setOnClickListener(v ->
                startActivity(new Intent(this, OwnerLoginActivity.class)));
    }
}

