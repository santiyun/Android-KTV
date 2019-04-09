package com.ttt.ktv.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ttt.ktv.LocalConfig;
import com.ttt.ktv.LocalConstans;
import com.ttt.ktv.R;
import com.ttt.ktv.bean.JniObjs;
import com.ttt.ktv.callback.MyTTTRtcEngineEventHandler;
import com.ttt.ktv.utils.DensityUtils;
import com.ttt.ktv.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ttt.ijk.media.exo.widget.media.IRenderView;
import ttt.ijk.media.exo.widget.media.IjkVideoView;
import ttt.ijk.media.player.IMediaPlayer;


public class MainActivity extends BaseActivity {

    public static final String NET_MUSIC_URL = "http://39.107.116.40/res/tpl/default/file/guoke.mp4";
    private TTTRtcEngine mTTTEngine;
    private SeekBar mVideoControlLocalVolume, mVideoControlMusicVolume;
    private TextView mAudioState, mVideoState, mAdjMusicNum, mAdjLocalNum, mAnchorTV, mChangeRoleTV;
    private ImageView mVideoControl, mMicControl;
    private ViewGroup mMainVideoPlayerly;
    private boolean mIsPlaying, mIsMuteLocalAudio, mIsPause;
    private boolean mIsFilePath = true;
    private IjkVideoView mIjkVideoView;
    private String mPlayPath;
    private String mAuidoMixFilePath;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private AlertDialog.Builder mErrorExitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 将音乐文件从 asset 中拷贝到手机存储卡上
        copyFileFromAsset();
        // 如果是主播身份，设置默认的MV播放路径
        if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
            mPlayPath = mAuidoMixFilePath;
        }
        // 获取 TTTRtcEngine 引擎对象
        mTTTEngine = TTTRtcEngine.getInstance();
        // 初始化控件
        initView();
        initData();
        initListener();
        // 初始化回调
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        LocalConfig.mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitRoom();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        // 布局调整代码
        ViewGroup topLayout = findViewById(R.id.relativelayout1);
        int statusBarHeight = DensityUtils.getStatusBarHeight(this);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) topLayout.getLayoutParams();
        layoutParams.topMargin = statusBarHeight;
        topLayout.setLayoutParams(layoutParams);

        // 创建控件
        mMicControl = findViewById(R.id.main_mic_control);
        mVideoControl = findViewById(R.id.main_video_control);
        mAdjLocalNum = findViewById(R.id.video_control_local_volume_num);
        mAdjMusicNum = findViewById(R.id.video_control_music_volume_num);
        mAudioState = findViewById(R.id.main_audio_state);
        mVideoState = findViewById(R.id.main_video_state);
        mVideoControlLocalVolume = findViewById(R.id.video_control_local_volume);
        mVideoControlMusicVolume = findViewById(R.id.video_control_music_volume);
        mMainVideoPlayerly = findViewById(R.id.main_video_playerly);
        mChangeRoleTV = findViewById(R.id.main_change_role);

        // 界面布局调整代码
        mAnchorTV = findViewById(R.id.main_anchor_id);
        if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
            String strAnchor = getString(R.string.ttt_role_anchor);
            mAnchorTV.setText(strAnchor + "ID: " + LocalConfig.mUid);
        }
        TextView mRoomID = findViewById(R.id.main_channel_id);
        String strChannel = getString(R.string.ttt_prefix_channel_name);
        mRoomID.setText(strChannel + ": " + LocalConfig.mRoomID);
        TextView mRoleTV = findViewById(R.id.main_role);
        String strRole = getString(R.string.ttt_role);
        if (Constants.CLIENT_ROLE_AUDIENCE == LocalConfig.mRole) {
            String strAudidence = getString(R.string.ttt_role_audience);
            mRoleTV.setText(strRole + ": " + strAudidence);
        } else if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
            String strAuthor = getString(R.string.ttt_role_anchor);
            mRoleTV.setText(strRole + ": " + strAuthor);
        }
        TextView mLocalUserID = findViewById(R.id.main_local_user_id);
        mLocalUserID.setText(String.valueOf(getString(R.string.ttt_ktv_local_user_prefix) + LocalConfig.mUid));
        int max = mVideoControlLocalVolume.getMax();
        mAdjLocalNum.setText(String.valueOf(max));
        mVideoControlLocalVolume.setProgress(max);
        int max1 = mVideoControlMusicVolume.getMax();
        mAdjMusicNum.setText(String.valueOf(max1));
        mVideoControlMusicVolume.setProgress(max1);

        // 如果是主播身份，创建 ijk 视频播放控件，并添加到布局中。
        if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
            mIjkVideoView = mTTTEngine.CreateIjkRendererView(this);
            ViewGroup parent = (ViewGroup) mIjkVideoView.getParent();
            if (parent != null) {
                parent.removeAllViews();
            }
            mMainVideoPlayerly.addView(mIjkVideoView, 0);
        }
    }

    private void initData() {
        ViewGroup controlly = findViewById(R.id.main_controlly);
        if (Constants.CLIENT_ROLE_AUDIENCE == LocalConfig.mRole) {
            controlly.setVisibility(View.GONE);
            mMicControl.setVisibility(View.INVISIBLE);
            mVideoControl.setVisibility(View.INVISIBLE);
        } else {
            mIjkVideoView.controlKtvMode(true);
            mChangeRoleTV.setVisibility(View.INVISIBLE);
        }
    }

    private void initListener() {
        // 开始/停止播放MV的操作。
        mVideoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsPlaying) {
                    startPlayVideo();
                } else {
                    if (mIsPause) {
                        resumePlayVideo();
                    } else {
                        pausePlayVideo();
                    }
                }
            }
        });

        // 切换MV操作
        findViewById(R.id.main_change_song).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsFilePath) {
                    mPlayPath = NET_MUSIC_URL;
                } else {
                    mPlayPath = mAuidoMixFilePath;
                }
                mIsFilePath = !mIsFilePath;
                stopPlayVideo();
                startPlayVideo();
            }
        });

        // 静音/取消静音的操作。
        mMicControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsMuteLocalAudio) {
                    mTTTEngine.muteLocalAudioStream(false); // 本地音频流继续发送
                    mMicControl.setImageResource(R.drawable.ic_microphone_normal);
                } else {
                    mTTTEngine.muteLocalAudioStream(true); // 本地音频流停止发送
                    mMicControl.setImageResource(R.drawable.ic_microphone_disable);
                }
                mIsMuteLocalAudio = !mIsMuteLocalAudio;
            }
        });

        // 退房间的操作。
        findViewById(R.id.main_exit_room).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitRoom();
            }
        });

        // 调节伴奏的音量大小。
        mVideoControlMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // 调节 ijk 本地播放的音量大小，调节范围是0~1。此调节仅是改变本地的效果，不影响远端。
                float result = i / 100f;
                mIjkVideoView.setVolume(result);
                // 调节远端听到的音量大小，调节范围是0~100。此调节仅是改变远端的效果，不影响本地。
                mTTTEngine.adjustAudioMixingVolume(i);
                mAdjMusicNum.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 调节人声的音量大小。
        mVideoControlLocalVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // 调节范围是0~100
                mTTTEngine.adjustAudioMixingSoloVolume(i);
                mAdjLocalNum.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 上麦和下麦操作，观众可以通过上麦操作变为副播角色，可以发言说话；也可以通过下麦操作变为观众，只能听和看，不能说话。
        mChangeRoleTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Constants.CLIENT_ROLE_AUDIENCE == LocalConfig.mRole) {
                    mChangeRoleTV.setText(R.string.ttt_ktv_audience);
                    mMicControl.setVisibility(View.VISIBLE);
                    mTTTEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER); // 变为副播
                    LocalConfig.mRole = Constants.CLIENT_ROLE_BROADCASTER;
                } else if (Constants.CLIENT_ROLE_BROADCASTER == LocalConfig.mRole) {
                    mChangeRoleTV.setText(R.string.ttt_ktv_broadcast);
                    mMicControl.setVisibility(View.INVISIBLE);
                    mTTTEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE); // 变为观众
                    LocalConfig.mRole = Constants.CLIENT_ROLE_AUDIENCE;
                }
            }
        });

        if (mIjkVideoView != null) {
            // 聆听MV播放完毕的事件。当一首歌播放完毕，会回调该函数。
            mIjkVideoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    stopPlayVideo();
                }
            });
        }
    }

    /**
     * 开始播放视频
     */
    private synchronized void startPlayVideo() {
        if (!mIsPlaying) {
            Uri uri = Uri.parse(mPlayPath);
            mIjkVideoView.setAspectRatio(IRenderView.AR_16_9_FIT_PARENT);
            mTTTEngine.startIjkPlayer(uri.toString(), true);
            mIsPlaying = true;
            mVideoControl.setImageResource(R.drawable.btn_player_pause);
        }
    }

    /**
     * 停止播放视频
     */
    private synchronized void stopPlayVideo() {
        if (mIsPlaying) {
            mIsPlaying = false;
            mTTTEngine.stopIjkPlayer();
            mVideoControl.setImageResource(R.drawable.btn_player_play);
            mIsPause = false;
        }
    }

    /**
     * 暂停视频播放
     */
    private synchronized void pausePlayVideo() {
        if (mIsPlaying && !mIsPause) {
            mIjkVideoView.pause();
            mVideoControl.setImageResource(R.drawable.btn_player_play);
            mIsPause = true;
        }
    }

    /**
     * 继续视频播放
     */
    private synchronized void resumePlayVideo() {
        if (mIsPlaying && mIsPause) {
            mIjkVideoView.start();
            mVideoControl.setImageResource(R.drawable.btn_player_pause);
            mIsPause = false;
        }
    }

    /**
     * 退出当前频道并停止视频的播放
     */
    private void exitRoom() {
        mTTTEngine.leaveChannel();
        mTTTEngine.stopIjkPlayer();

        Intent i = new Intent(mContext, SplashActivity.class);
        startActivity(i);
        finish();
    }

    private void copyFileFromAsset() {
        File externalFilesDir = getFilesDir();
        if (externalFilesDir == null) {
            return;
        }

        mAuidoMixFilePath = externalFilesDir.getAbsolutePath() + "/1080.mp4";
        File temp = new File(mAuidoMixFilePath);
        if (temp.exists()) {
            return;
        }

        AssetManager mAssetManager = getResources().getAssets();
        FileOutputStream mFileOutputStream = null;
        InputStream mInputStream = null;
        try {
            mInputStream = mAssetManager.open("1080.mp4");
            mFileOutputStream = new FileOutputStream(mAuidoMixFilePath);
            byte[] buf = new byte[1024];
            while (mInputStream.read(buf) != -1) {
                mFileOutputStream.write(buf, 0, buf.length);
                mFileOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mFileOutputStream != null) {
                try {
                    mFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 显示和创建频道出现异常错误的对话框
     *
     * @param message 错误信息
     */
    public void showErrorExitDialog(String message) {
        if (mErrorExitDialog == null) {
            mErrorExitDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.ttt_error_exit_dialog_title))//设置对话框标题
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ttt_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            exitRoom();
                        }
                    });
        }
        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }

    }

    /**
     * 接收 SDK 发送的回调信息
     */
    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MyTTTRtcEngineEventHandler.TAG.equals(intent.getAction())) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                MyLog.d("UI onReceive callBack... mJniType : " + mJniObjs.mJniType);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_USER_KICK: // 频道出现异常，需要退出
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST: // 网络异常，需要退出
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN: // 其他用户加入该房间
                        long uid = mJniObjs.mUid;
                        long mIdentity = mJniObjs.mIdentity;
                        // 如果是主播身份的用户，打开他的视频。
                        if (Constants.CLIENT_ROLE_ANCHOR == mIdentity) {
                            // 界面上显示他的ID
                            String strAnchor = getString(R.string.ttt_role_anchor);
                            mAnchorTV.setText(strAnchor + ": " + uid);
                            // 创建视频控件
                            SurfaceView surfaceView = mTTTEngine.CreateRendererView(mContext);
                            // 设置视频属性
                            mTTTEngine.setupRemoteVideo(new VideoCanvas(uid, Constants.RENDER_MODE_FIT,surfaceView));
                            // 添加到布局文件中
                            mMainVideoPlayerly.addView(surfaceView, 0);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
                            String localAudioString = getResources().getString(R.string.ttt_audio_upspeed);
                            String localAudioResult = String.format(localAudioString, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                            mAudioState.setText(localAudioResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        if (Constants.CLIENT_ROLE_ANCHOR == LocalConfig.mRole) {
                            String localVideoString = getResources().getString(R.string.ttt_video_upspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                            mVideoState.setText(localVideoResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        if (Constants.CLIENT_ROLE_AUDIENCE == LocalConfig.mRole) {
                            String localVideoString = getResources().getString(R.string.ttt_audio_downspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                            mAudioState.setText(localVideoResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        if (Constants.CLIENT_ROLE_AUDIENCE == LocalConfig.mRole) {
                            String localVideoString = getResources().getString(R.string.ttt_video_downspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                            mVideoState.setText(localVideoResult);
                        }
                        break;
                }
            }
        }
    }
}
