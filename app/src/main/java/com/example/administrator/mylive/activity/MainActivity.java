package com.example.administrator.mylive.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.mylive.R;
import com.example.administrator.mylive.model.Constants;
import com.example.administrator.mylive.model.DemoFunc;
import com.example.administrator.mylive.model.DlgMgr;
import com.example.administrator.mylive.model.MessageObservable;
import com.example.administrator.mylive.model.StatusObservable;
import com.example.administrator.mylive.model.UserInfo;
import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveConstants;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;
import com.tencent.livesdk.ILVText;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;


//直播页面
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ILVLiveConfig.ILVLiveMsgListener, ILiveLoginManager.TILVBStatusListener {

    private AVRootView arvRoot;
    private Button startlive;
    private AVRootView arv_root;
    private EditText roomnum;
    private String strMsg = "";
    private TextView tv_msg;
    private ScrollView sv_scroll;
    private DanmakuView danmu;

    //弹幕
    private BaseDanmakuParser parser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };

    private boolean showDanmaku;
    private DanmakuContext danmakuContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //开启弹幕
        startDanmu();
        ILVLiveManager.getInstance().setAvVideoView(arvRoot);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);

        arvRoot.setAutoOrientation(false);
        // 打开摄像头预览
        arvRoot.setSubCreatedListener(new AVRootView.onSubViewCreatedListener() {
            @Override
            public void onSubViewCreated() {
                ILiveRoomManager.getInstance().enableCamera(ILiveConstants.FRONT_CAMERA, true);
            }
        });
    }

    private void initView() {
        arvRoot = (AVRootView) findViewById(R.id.arv_root);
        startlive = (Button) findViewById(R.id.startlive);

        startlive.setOnClickListener(this);
        arv_root = (AVRootView) findViewById(R.id.arv_root);
        arv_root.setOnClickListener(this);
        roomnum = (EditText) findViewById(R.id.roomnum);
        tv_msg = (TextView) findViewById(R.id.tv_msg);
        tv_msg.setOnClickListener(this);
        sv_scroll = (ScrollView) findViewById(R.id.sv_scroll);
        sv_scroll.setOnClickListener(this);
        danmu = (DanmakuView) findViewById(R.id.danmu);
        danmu.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startlive:
                String trim = roomnum.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)) {
                    createRoom(trim);
                } else {
                    Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    // 添加消息
    private void addMessage(String sender, String msg) {
        strMsg += "[" + sender + "]  " + msg + "\n";
        tv_msg.setText(strMsg);
        sv_scroll.fullScroll(View.FOCUS_DOWN);
    }

    private void createRoom(String trim) {

        ILVLiveRoomOption option = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .autoCamera(ILiveConstants.NONE_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId())
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.ROLE_MASTER)
                .autoFocus(true);
        ILVLiveManager.getInstance().createRoom(Integer.parseInt(trim),
                option, new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {
                        afterCreate();
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        if (module.equals(ILiveConstants.Module_IMSDK) && 10021 == errCode) {
                            // 被占用，改加入
                            Toast.makeText(MainActivity.this, "房间存在", Toast.LENGTH_SHORT).show();
                        } else {
//                            DlgMgr.showMsg(getContenxt(), "create failed:" + module + "|" + errCode + "|" + errMsg);
                            Toast.makeText(MainActivity.this, "提示", Toast.LENGTH_SHORT).show();
                            Log.e("error", errCode + "," + errMsg);
                        }
                    }
                });
    }

    private void afterCreate() {

        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        roomnum.setEnabled(false);
        Toast.makeText(this, "开始直播", Toast.LENGTH_SHORT).show();
        //开始直播会后隐藏对应的按钮
        startlive.setVisibility(View.GONE);

    }

    @Override
    public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
        String text1 = text.getText();
        Log.e("收到消息",text1 );
        //收到消息让消息显示在评论
        addMessage(SenderId, DemoFunc.getLimitString(text.getText(), Constants.MAX_SIZE));
        //收到消息发送弹幕
        addDanmaku(text1,false);
    }

    @Override
    public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {
        switch (cmd.getCmd()) {
            case ILVLiveConstants.ILVLIVE_CMD_LINKROOM_REQ:     // 跨房邀请
                linkRoomReq(id);
                break;
        }
    }

    @Override
    public void onNewOtherMsg(TIMMessage message) {

    }

    @Override
    public void onForceOffline(int error, String message) {
        finish();
    }

    private void linkRoomReq(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.live_title_link);
        builder.setMessage("[" + id + "]" + getString(R.string.link_req_tips));
        builder.setNegativeButton(R.string.str_btn_refuse, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                refuseLink(id);
            }
        });
        builder.setPositiveButton(R.string.str_btn_agree, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                acceptLink(id);
            }
        });
        DlgMgr.showAlertDlg(this, builder);
    }

    // 拒绝跨房连麦
    private void refuseLink(String id) {
        ILVLiveManager.getInstance().refuseLinkRoom(id, null);
    }

    // 同意跨房连麦
    private void acceptLink(String id) {
        ILVLiveManager.getInstance().acceptLinkRoom(id, null);
    }


    @Override
    protected void onPause() {
        super.onPause();
        ILVLiveManager.getInstance().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILVLiveManager.getInstance().onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ILVLiveManager.getInstance().onDestory();
    }


    //开启弹幕
    private void startDanmu() {
        danmu.enableDanmakuDrawingCache(true);
        danmu.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                showDanmaku = true;
                danmu.start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        danmakuContext = DanmakuContext.create();
        danmu.prepare(parser, danmakuContext);
    }
    private void addDanmaku(String content, boolean b) {
        BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        danmaku.text = content;
        danmaku.padding = 5;
        danmaku.textSize = sp2px(20);
        danmaku.textColor = Color.WHITE;
        danmaku.setTime(danmu.getCurrentTime());
        if (b) {
            danmaku.borderColor = Color.GREEN;
        }
        danmu.addDanmaku(danmaku);
    }
    //
    private float sp2px(int i) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (i * fontScale + 0.5f);
    }

}
