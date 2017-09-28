package com.example.administrator.mylive.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.tencent.TIMConversationType;
import com.tencent.TIMElem;
import com.tencent.TIMElemType;
import com.tencent.TIMGroupSystemElem;
import com.tencent.TIMGroupSystemElemType;
import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
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

public class WatchLiveActivity extends AppCompatActivity implements View.OnClickListener, ILVLiveConfig.ILVLiveMsgListener, ILiveLoginManager.TILVBStatusListener {

    private EditText watch_edit;
    private AVRootView watch_rootview;
    private Button watch_looklive;
    private String strMsg = "";
    private TextView tv_msg;
    private ScrollView sv_scroll;
    private EditText watch_edit_text;
    private Button watch_btn_text;
    private DanmakuView danmu;
    private Button watch_btn_danmu;
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
        setContentView(R.layout.activity_watch_live);
        initView();
        //开启弹幕
        startDanmu();
    }

    private void initView() {
        watch_edit = (EditText) findViewById(R.id.watch_edit);
        watch_rootview = (AVRootView) findViewById(R.id.watch_rootview);
        watch_looklive = (Button) findViewById(R.id.watch_looklive);
        watch_looklive.setOnClickListener(this);
        ILVLiveManager.getInstance().setAvVideoView(watch_rootview);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);
        tv_msg = (TextView) findViewById(R.id.tv_msg);
        tv_msg.setOnClickListener(this);
        sv_scroll = (ScrollView) findViewById(R.id.sv_scroll);
        sv_scroll.setOnClickListener(this);
        watch_edit_text = (EditText) findViewById(R.id.watch_edit_text);
        watch_edit_text.setOnClickListener(this);
        watch_btn_text = (Button) findViewById(R.id.watch_btn_text);
        watch_btn_text.setOnClickListener(this);
        danmu = (DanmakuView) findViewById(R.id.danmu);
        danmu.setOnClickListener(this);
        watch_btn_danmu = (Button) findViewById(R.id.watch_btn_danmu);
        watch_btn_danmu.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.watch_looklive:
                String trim = watch_edit.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)) {
                    joinRoom(trim);
                } else {
                    Toast.makeText(this, "请输入正确的房间号", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.watch_btn_text:
                //发起评论
                String trim1 = watch_edit_text.getText().toString().trim();
                if (!TextUtils.isEmpty(trim1)) {

                    sendMsg(trim1);

                } else {
                    Toast.makeText(this, "发送内容不能为空", Toast.LENGTH_SHORT).show();
                }
                break;
            //发送弹幕信息
            case R.id.watch_btn_danmu:
                String trim2 = watch_edit_text.getText().toString().trim();
                addDanmaku(trim2,true);
                sendMsg(trim2);
                watch_edit_text.setText("");

                break;
        }
    }

    //发送消息
    private void sendMsg(final String trim1) {
        ILVText ilvText = new ILVText(ILVText.ILVTextType.eGroupMsg,
                ILiveRoomManager.getInstance().getIMGroupId(),
                trim1);
        ILVLiveManager.getInstance().sendText(ilvText, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                addMessage(ILiveLoginManager.getInstance().getMyUserId(), trim1);
                watch_edit_text.setText("");
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(watch_edit_text.getWindowToken(), 0);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                DlgMgr.showMsg(WatchLiveActivity.this, "sendText failed:" + module + "|" + errCode + "|" + errMsg);
            }
        });
    }

    private void joinRoom(String trim) {
        ILVLiveRoomOption ilvLiveRoomOption = new ILVLiveRoomOption("")
                .controlRole(Constants.ROLE_GUEST)
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .autoCamera(false)
                .autoMic(false);
        ILVLiveManager.getInstance().joinRoom(Integer.parseInt(trim),
                ilvLiveRoomOption, new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {
                        afterJoin();
                    }

                    @Override
                    public void onError(String module, final int errCode, final String errMsg) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WatchLiveActivity.this, "errCode:" + errCode + ",errMsg:" + errMsg, Toast.LENGTH_SHORT).show();
                                Log.e("onError观看直播失败", errCode + "," + errMsg);
                            }
                        });
                    }
                });

    }

    //观看视频
    private void afterJoin() {
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        watch_looklive.setVisibility(View.GONE);
        String s = watch_edit.getText().toString();
        watch_edit.setText("正在观看" + s + "房间主播....");
    }

    @Override
    public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
      //接收到消息发送弹幕或则显示评论
        String s = text.getText().toString();
        addMessage(SenderId, DemoFunc.getLimitString(text.getText(), Constants.MAX_SIZE));
        addDanmaku(s,false);
    }

    @Override
    public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {

    }

    @Override
    public void onNewOtherMsg(TIMMessage message) {
        if (message.getConversation() != null && message.getConversation().getPeer() != null) {
            if (message.getConversation().getType() == TIMConversationType.Group
                    && !ILiveRoomManager.getInstance().getIMGroupId().equals(message.getConversation().getPeer())) {
                return;
            }
        }

        for (int j = 0; j < message.getElementCount(); j++) {
            if (message.getElement(j) == null)
                continue;
            TIMElem elem = message.getElement(j);
            TIMElemType type = elem.getType();

            //系统消息
            if (type == TIMElemType.GroupSystem) {  // 群组解散消息
                if (TIMGroupSystemElemType.TIM_GROUP_SYSTEM_DELETE_GROUP_TYPE == ((TIMGroupSystemElem) elem).getSubtype()) {
                    DlgMgr.showMsg(WatchLiveActivity.this, getString(R.string.str_tips_discuss)).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finish();
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onForceOffline(int error, String message) {
        finish();
    }

    // 添加消息
    private void addMessage(String sender, String msg) {
        strMsg += "[" + sender + "]  " + msg + "\n";
        tv_msg.setText(strMsg);
        sv_scroll.fullScroll(View.FOCUS_DOWN);
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


    //弹幕
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
