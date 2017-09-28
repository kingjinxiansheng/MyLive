package com.example.administrator.mylive.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.administrator.mylive.R;
import com.example.administrator.mylive.view.LineMenuView;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private LineMenuView lmv_host;
    private LineMenuView lmv_guest;
    private LineMenuView event_live;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
    }

    private void initView() {
        lmv_host = (LineMenuView) findViewById(R.id.lmv_host);
        lmv_guest = (LineMenuView) findViewById(R.id.lmv_guest);
        lmv_host.setTitle("发起直播");
        lmv_guest.setTitle("观看直播");
        lmv_host.setOnClickListener(this);
        lmv_guest.setOnClickListener(this);
        event_live = (LineMenuView) findViewById(R.id.event_live);
        event_live.setTitle("连麦");
        event_live.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lmv_host:
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.lmv_guest:
                Intent intent1 = new Intent(HomeActivity.this, WatchLiveActivity.class);
                startActivity(intent1);
                break;
            case R.id.event_live:
                Intent intent2 = new Intent(HomeActivity.this, EventLiveActivity.class);
                startActivity(intent2);
                break;
        }
    }
}
