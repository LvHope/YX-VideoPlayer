package com.liuhao.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liuhao.myapplication.receiver.Observer;
import com.liuhao.myapplication.receiver.PhoneCallStateObserver;
import com.liuhao.myapplication.services.PlayerService;
import com.netease.neliveplayer.playerkit.common.log.LogUtil;
import com.netease.neliveplayer.playerkit.sdk.PlayerManager;
import com.netease.neliveplayer.playerkit.sdk.VodPlayer;
import com.netease.neliveplayer.playerkit.sdk.VodPlayerObserver;
import com.netease.neliveplayer.playerkit.sdk.constant.CauseCode;
import com.netease.neliveplayer.playerkit.sdk.model.MediaInfo;
import com.netease.neliveplayer.playerkit.sdk.model.StateInfo;
import com.netease.neliveplayer.playerkit.sdk.model.VideoBufferStrategy;
import com.netease.neliveplayer.playerkit.sdk.model.VideoOptions;
import com.netease.neliveplayer.playerkit.sdk.model.VideoScaleMode;
import com.netease.neliveplayer.playerkit.sdk.view.AdvanceSurfaceView;
import com.netease.neliveplayer.playerkit.sdk.view.AdvanceTextureView;
import com.netease.neliveplayer.sdk.NELivePlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class VodActivity extends Activity {

    public final static String TAG = VodActivity.class.getSimpleName();

    private static final int SHOW_PROGRESS = 0x01;


    private ImageButton mPlayBack;

    private TextView mFileName; //文件名称

    private ImageView mAudioRemind; //播音频文件时提示

    private View mBuffer; //用于指示缓冲状态

    private RelativeLayout mPlayToolbar;

    private ImageView mPauseButton;

    private ImageView mSetPlayerScaleButton;

    private ImageView mSnapshotButton;

    private ImageView mMuteButton;

    private SeekBar mProgressBar;


    private TextView mEndTime;

    private TextView mCurrentTime;

    private TextView mSwitchDefinition;

    private TextView mPlayBackSpeed;

    private TextView mSwitchUrl;

    private TextView mSubtitle;

    private AdvanceSurfaceView surfaceView;

    private AdvanceTextureView textureView;

    protected VodPlayer player;

    private MediaInfo mediaInfo;

    private String mDecodeType;//解码类型，硬解或软解

    private String mMediaType; //媒体类型

    private boolean mHardware = true;

    private Uri mUri;

    private String mTitle;

    private boolean mBackPressed;

    private boolean mPaused = false;

    private boolean isMute = false;

    private boolean mIsFullScreen = false;

    protected boolean isPauseInBackgroud;

    private String mVideoPath;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            long position;
            switch (msg.what) {
                case SHOW_PROGRESS:
                    position = setProgress();
                    msg = obtainMessage(SHOW_PROGRESS);
                    sendMessageDelayed(msg, 1000 - (position % 1000));
                    break;
            }
        }
    };


    private long setProgress() {
        if (player == null) {
            return 0;
        }
        int position = (int) player.getCurrentPosition();
        int duration = (int) player.getDuration();
        if (mProgressBar != null) {
            if (duration > 0) {
                LogUtil.i(TAG, "setProgress,position:" + position + "duration:" + duration);
                long pos = 100L * position / duration;
                mProgressBar.setProgress((int) pos);
            }
        }
        if (mEndTime != null && duration > 0) {
            mEndTime.setText(stringForTime(duration));
        } else {
            mEndTime.setText("--:--:--");
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }
        return position;
    }


    private View.OnClickListener mPlayBackOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Log.i(TAG, "player_exit");
            mBackPressed = true;
            finish();
            releasePlayer();
        }
    };

    private SeekBar.OnSeekBarChangeListener mProgressSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeMessages(SHOW_PROGRESS);

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            player.seekTo(player.getDuration() * seekBar.getProgress() / 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(getLayoutId());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //保持屏幕常亮
        PhoneCallStateObserver.getInstance().observeLocalPhoneObserver(localPhoneObserver, true);
        parseIntent();
        findViews();
        initPlayer();
    }

    protected int getLayoutId() {
        return R.layout.activity_vod;
    }

    private void parseIntent() {
        //接收MainActivity传过来的参数
        mMediaType = getIntent().getStringExtra("media_type");
        mDecodeType = getIntent().getStringExtra("decode_type");
        mVideoPath = getIntent().getStringExtra("videoPath");
        mUri = Uri.parse(mVideoPath);
        if (mMediaType != null && mMediaType.equals("localaudio")) { //本地音频文件采用软件解码
            mDecodeType = "software";
        }
        if (mDecodeType != null && mDecodeType.equals("hardware")) {
            mHardware = true;
        } else {
            mHardware = false;
        }

    }

    protected void findViews() {
        //这里支持使用SurfaceView和TextureView
        //surfaceView = findViewById(R.id.live_surface);
        textureView = findViewById(R.id.live_texture);
        mPlayToolbar = (RelativeLayout) findViewById(R.id.play_toolbar);
        mPlayBack = (ImageButton) findViewById(R.id.player_exit);//退出播放
        mPlayBack.getBackground().setAlpha(0);
        mFileName = (TextView) findViewById(R.id.file_name);
        if (mUri != null) { //获取文件名，不包括地址
            List<String> paths = mUri.getPathSegments();
            String name = paths == null || paths.isEmpty() ? "null" : paths.get(paths.size() - 1);
            setFileName(name);
        }
        mAudioRemind = (ImageView) findViewById(R.id.audio_remind);
        if (mMediaType != null && mMediaType.equals("localaudio")) {
            mAudioRemind.setVisibility(View.VISIBLE);
        } else {
            mAudioRemind.setVisibility(View.INVISIBLE);
        }
        mBuffer = findViewById(R.id.buffering_prompt);
        mPauseButton = (ImageView) findViewById(R.id.mediacontroller_play_pause); //播放暂停按钮
        mPauseButton.setImageResource(R.mipmap.nemediacontroller_play);
        mPauseButton.setOnClickListener(mPauseListener);
        mPlayBack.setOnClickListener(mPlayBackOnClickListener); //监听退出播放的事件响应
        mSetPlayerScaleButton = (ImageView) findViewById(R.id.video_player_scale);  //画面显示模式按钮
        mSnapshotButton = (ImageView) findViewById(R.id.snapShot);  //截图按钮
        mMuteButton = (ImageView) findViewById(R.id.video_player_mute);  //静音按钮
        mMuteButton.setOnClickListener(mMuteListener);
        mProgressBar = (SeekBar) findViewById(R.id.mediacontroller_seekbar);  //进度条
        mProgressBar.setOnSeekBarChangeListener(mProgressSeekListener);
        mEndTime = (TextView) findViewById(R.id.mediacontroller_time_total); //总时长
        mEndTime.setText("--:--:--");
        mCurrentTime = (TextView) findViewById(R.id.mediacontroller_time_current); //当前播放位置
        mCurrentTime.setText("--:--:--");
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        mSnapshotButton = (ImageView) findViewById(R.id.snapShot);  //截图按钮
        mSnapshotButton.setOnClickListener(mSnapShotListener);
        mSetPlayerScaleButton = (ImageView) findViewById(R.id.video_player_scale);  //画面显示模式按钮
        mSetPlayerScaleButton.setOnClickListener(mSetPlayerScaleListener);


    }


    private View.OnClickListener mPauseListener = new View.OnClickListener() {

        public void onClick(View v) {
            if (player.isPlaying()) {
                mPauseButton.setImageResource(R.mipmap.nemediacontroller_pause);
                showToast("暂停播放");
                player.pause();
                mPaused = true;
            } else {
                mPauseButton.setImageResource(R.mipmap.nemediacontroller_play);
                showToast("继续播放");
                player.start();
                mPaused = false;
            }
        }
    };

    private void showToast(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener mMuteListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!isMute) {
                mMuteButton.setImageResource(R.mipmap.nemediacontroller_mute01);
                player.setMute(true);
                isMute = true;
            } else {
                mMuteButton.setImageResource(R.mipmap.nemediacontroller_mute02);
                player.setMute(false);
                isMute = false;
            }
        }
    };


    private void initPlayer() {
        VideoOptions options = new VideoOptions();
        options.hardwareDecode = mHardware;
        /**
         * isPlayLongTimeBackground 控制退到后台或者锁屏时是否继续播放，开发者可根据实际情况灵活开发,我们的示例逻辑如下：
         * 使用软件解码：
         * isPlayLongTimeBackground 为 false 时，直播进入后台停止播放，进入前台重新拉流播放
         * isPlayLongTimeBackground 为 true 时，直播进入后台不做处理，继续播放,
         *
         * 使用硬件解码：
         * 直播进入后台停止播放，进入前台重新拉流播放
         */
        options.isPlayLongTimeBackground = !isPauseInBackgroud;
        options.bufferStrategy = VideoBufferStrategy.ANTI_JITTER;
        player = PlayerManager.buildVodPlayer(this, mVideoPath, options);
        intentToStartBackgroundPlay();
        start();
        if (surfaceView == null) {
            player.setupRenderView(textureView, VideoScaleMode.FIT);
        } else {
            player.setupRenderView(surfaceView, VideoScaleMode.FIT);
        }

    }

    public void setFileName(String name) { //设置文件名并显示出来
        mTitle = name;
        if (mFileName != null) {
            mFileName.setText(mTitle);
        }
        mFileName.setGravity(Gravity.CENTER);
    }

    private void start() {
        player.registerPlayerObserver(playerObserver, true);
        player.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (player != null && !mPaused) {
            player.onActivityResume(false);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        enterBackgroundPlay();
        if (player != null) {
            player.onActivityStop(false);
        }

    }


    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        mBackPressed = true;
        finish();
        releasePlayer();
        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        releasePlayer();

    }

    private void releasePlayer() {
        if (player == null) {
            return;
        }
        LogUtil.i(TAG, "releasePlayer");
        player.registerPlayerObserver(playerObserver, false);
        PhoneCallStateObserver.getInstance().observeLocalPhoneObserver(localPhoneObserver, false);
        player.setupRenderView(null, VideoScaleMode.NONE);
        textureView.releaseSurface();
        textureView = null;
        player.stop();
        player = null;
        intentToStopBackgroundPlay();
        mHandler.removeCallbacksAndMessages(null);

    }

    private VodPlayerObserver playerObserver = new VodPlayerObserver() {

        @Override
        public void onCurrentPlayProgress(long currentPosition, long duration, float percent, long cachedPosition) {
        }

        @Override
        public void onSeekCompleted() {
            LogUtil.i(TAG, "onSeekCompleted");
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);

        }

        @Override
        public void onCompletion() {
        }

        @Override
        public void onAudioVideoUnsync() {
            showToast("音视频不同步");
        }

        @Override
        public void onNetStateBad() {
        }

        @Override
        public void onDecryption(int ret) {
        }

        @Override
        public void onPreparing() {
        }

        @Override
        public void onPrepared(MediaInfo info) {
            mediaInfo = info;
        }

        @Override
        public void onError(int code, int extra) {
            mBuffer.setVisibility(View.INVISIBLE);
            if (code == CauseCode.CODE_VIDEO_PARSER_ERROR) {
                showToast("视频解析出错");
            } else {
                AlertDialog.Builder build = new AlertDialog.Builder(VodActivity.this);
                build.setTitle("播放错误").setMessage("错误码：" + code).setPositiveButton("确定", null).setCancelable(false)
                     .show();
            }

        }

        @Override
        public void onFirstVideoRendered() {
            showToast("视频第一帧已解析");

        }

        @Override
        public void onFirstAudioRendered() {
            //            showToast("音频第一帧已解析");
        }

        @Override
        public void onBufferingStart() {
            mBuffer.setVisibility(View.VISIBLE);
        }

        @Override
        public void onBufferingEnd() {
            mBuffer.setVisibility(View.GONE);
        }

        @Override
        public void onBuffering(int percent) {
            LogUtil.d(TAG, "缓冲中..." + percent + "%");
            mProgressBar.setSecondaryProgress(percent);
        }

        @Override
        public void onVideoDecoderOpen(int value) {
            showToast("使用解码类型：" + (value == 1 ? "硬件解码" : "软解解码"));
        }

        @Override
        public void onStateChanged(StateInfo stateInfo) {
        }


        @Override
        public void onHttpResponseInfo(int code, String header) {
            Log.i(TAG, "onHttpResponseInfo,code:" + code + " header:" + header);
        }
    };

    private static String stringForTime(long position) {
        int totalSeconds = (int) ((position / 1000.0) + 0.5);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds).toString();
    }

    public void getSnapshot() {
        if (mediaInfo == null) {
            Log.d(TAG, "mediaInfo is null,截图不成功");
            showToast("截图不成功");
        } else if (mediaInfo.getVideoDecoderMode().equals("MediaCodec")) {
            Log.d(TAG, "hardware decoder unsupport snapshot");
            showToast("截图不支持硬件解码");
        } else {
            Bitmap bitmap = player.getSnapshot();
            String picName = "/sdcard/NESnapshot" + System.currentTimeMillis() + ".jpg";
            File f = new File(picName);
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(f);
                if (picName.substring(picName.lastIndexOf(".") + 1, picName.length()).equals("jpg")) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                } else if (picName.substring(picName.lastIndexOf(".") + 1, picName.length()).equals("png")) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                }
                fOut.flush();
                fOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            showToast("截图成功");
        }
    }

    private View.OnClickListener mSnapShotListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mMediaType.equals("localaudio") || mHardware) {
                if (mMediaType.equals("localaudio")) {
                    showToast("音频播放不支持截图！");
                } else if (mHardware) {
                    showToast("硬件解码不支持截图！");
                }
                return;
            }
            getSnapshot();
        }
    };

    private View.OnClickListener mSetPlayerScaleListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            player.setupRenderView(null, VideoScaleMode.NONE);
            if (mIsFullScreen) {
                mSetPlayerScaleButton.setImageResource(R.mipmap.nemediacontroller_scale01);
                mIsFullScreen = false;
                player.setupRenderView(textureView, VideoScaleMode.FIT);

            } else {
                mSetPlayerScaleButton.setImageResource(R.mipmap.nemediacontroller_scale02);
                mIsFullScreen = true;
                player.setupRenderView(textureView, VideoScaleMode.FULL);
            }
        }
    };


    /**
     * 时间戳回调
     */
    private NELivePlayer.OnCurrentSyncTimestampListener mOnCurrentSyncTimestampListener = new NELivePlayer.OnCurrentSyncTimestampListener() {

        @Override
        public void onCurrentSyncTimestamp(long timestamp) {
            Log.v(TAG, "OnCurrentSyncTimestampListener,onCurrentSyncTimestamp:" + timestamp);

        }
    };

    private NELivePlayer.OnCurrentSyncContentListener mOnCurrentSyncContentListener = new NELivePlayer.OnCurrentSyncContentListener() {

        @Override
        public void onCurrentSyncContent(List<String> content) {
            StringBuffer sb = new StringBuffer();
            for (String str : content) {
                sb.append(str + "\r\n");
            }
            showToast("onCurrentSyncContent,收到同步信息:" + sb.toString());
            Log.v(TAG, "onCurrentSyncContent,收到同步信息:" + sb.toString());
        }
    };

    /**
     * 处理service后台播放逻辑
     */
    private void intentToStartBackgroundPlay() {
        if (!mHardware && !isPauseInBackgroud) {
            PlayerService.intentToStart(this);
        }
    }

    private void intentToStopBackgroundPlay() {
        if (!mHardware && !isPauseInBackgroud) {
            PlayerService.intentToStop(this);
            player = null;
        }
    }


    private void enterBackgroundPlay() {
        if (!mHardware && !isPauseInBackgroud) {
            PlayerService.setMediaPlayer(player);
        }
    }

    private void stopBackgroundPlay() {
        if (!mHardware && !isPauseInBackgroud) {
            PlayerService.setMediaPlayer(null);
        }
    }

    //处理与电话逻辑
    private Observer<Integer> localPhoneObserver = new Observer<Integer>() {

        @Override
        public void onEvent(Integer phoneState) {
            if (phoneState == TelephonyManager.CALL_STATE_IDLE) {
                player.start();
            } else if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
                player.stop();
            } else {
                Log.i(TAG, "localPhoneObserver onEvent " + phoneState);
            }

        }
    };
}
