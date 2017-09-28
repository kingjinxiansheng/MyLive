package com.example.administrator.mylive.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import static com.tencent.TIMElemType.Text;

//连麦activity
public class EventLiveActivity extends AppCompatActivity implements View.OnClickListener, ILVLiveConfig.ILVLiveMsgListener, ILiveLoginManager.TILVBStatusListener {

    private AVRootView event_rootview;
    private EditText event_edit;
    private Button event_btnroom;
    private String strMsg = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_live);
        initView();
    }

    private void initView() {
        event_rootview = (AVRootView) findViewById(R.id.event_rootview);
        event_edit = (EditText) findViewById(R.id.event_edit);
        event_btnroom = (Button) findViewById(R.id.event_btnroom);

        event_btnroom.setOnClickListener(this);

        ILVLiveManager.getInstance().setAvVideoView(event_rootview);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.event_btnroom:
                String trim = event_edit.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)){
                    joinRoom(trim);
                }

                break;
        }
    }

    //进入房间
    private void joinRoom(String trim) {
        ILVLiveRoomOption option = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .controlRole(Constants.ROLE_LIVEGUEST)
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .autoFocus(true);
        ILVLiveManager.getInstance().joinRoom(Integer.parseInt(trim),
                option, new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {
                        afterCreate();
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        DlgMgr.showMsg(EventLiveActivity.this, "create failed:"+module+"|"+errCode+"|"+errMsg);
                    }
                });
    }

    private void afterCreate() {
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        event_btnroom.setVisibility(View.GONE);
        String s = event_edit.getText().toString();
        event_edit.setText("与"+s+"房间主播连接中....");
    }


    @Override
    public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
        addMessage(SenderId, DemoFunc.getLimitString(text.getText(), Constants.MAX_SIZE));

    }

    private void addMessage(String senderId, String limitString) {
        strMsg += "["+senderId+"]  "+limitString+"\n";

    }

    @Override
    public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {

    }

    @Override
    public void onNewOtherMsg(TIMMessage message) {
        if (message.getConversation() != null && message.getConversation().getPeer() != null){
            if (message.getConversation().getType()== TIMConversationType.Group
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
                    DlgMgr.showMsg(EventLiveActivity.this, getString(R.string.str_tips_discuss)).setOnCancelListener(new DialogInterface.OnCancelListener() {
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
}
