//package com.liuhao.myapplication;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.support.v4.app.ActivityCompat;
//import android.text.TextUtils;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup.LayoutParams;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.RadioButton;
//import android.widget.TextView;
//
//import com.netease.neliveplayer.demo.R;
//import com.netease.neliveplayer.demo.activity.advance.ShortVideoTextureActivity;
//import com.netease.neliveplayer.demo.util.HttpPostUtils;
//import com.netease.neliveplayer.playerkit.sdk.PlayerManager;
//import com.netease.neliveplayer.playerkit.sdk.model.SDKInfo;
//import com.netease.neliveplayer.playerkit.sdk.model.SDKOptions;
//import com.netease.neliveplayer.proxy.config.NEPlayerConfig;
//import com.netease.neliveplayer.sdk.NELivePlayer;
//import com.netease.neliveplayer.sdk.model.NEDynamicLoadingConfig;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//
//public class MainActivity extends BaseActivity {
//
//    public final static String TAG = MainActivity.class.getSimpleName();
//
//    private static final int REQUEST_CODE = 100;
//
//    private TextView mMediaOption;    //显示播放选项
//
//    private Button mBtnLiveStream;
//
//    private Button mBtnVideoOnDemand;
//
//    private Button mBtnShortVideo;
//
//    private Button mBtnMultiVideo;
//
//    private ImageView mMediaTypeSelected;
//
//    //private ImageView mMediaTypeUnselected;
//    private EditText mEditURL; //用于输入网络流地址
//
//    private Button mQRScan; // 二维码扫描
//
//    private Button mBtnPlay, mManualPlay;   //开始播放
//
//    private RadioButton mHardware; //硬件解码
//
//    private RadioButton mSoftware; //软件解码
//
//    private TextView mHardwareReminder; //硬件解码提示语
//
//    private String decodeType = "software";  //解码类型，默认软件解码
//
//    private String mediaType = "livestream"; //媒体类型，默认网络直播
//
//    private SDKOptions config;
//
//    private int tabWidth;
//
//    /**
//     * 6.0权限处理
//     **/
//    private boolean bPermission = false;
//
//    private final int WRITE_PERMISSION_REQ_CODE = 100;
//
//    private boolean checkPublishPermission() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            List<String> permissions = new ArrayList<>();
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(MainActivity.this,
//                                                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(MainActivity.this,
//                                                                                        Manifest.permission.CAMERA)) {
//                permissions.add(Manifest.permission.CAMERA);
//            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(MainActivity.this,
//                                                                                        Manifest.permission.READ_PHONE_STATE)) {
//                permissions.add(Manifest.permission.READ_PHONE_STATE);
//            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(MainActivity.this,
//                                                                                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(MainActivity.this,
//                                                                                        Manifest.permission.ACCESS_FINE_LOCATION)) {
//                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
//            }
//            if (permissions.size() != 0) {
//                ActivityCompat.requestPermissions(MainActivity.this, (String[]) permissions.toArray(new String[0]),
//                                                  WRITE_PERMISSION_REQ_CODE);
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case WRITE_PERMISSION_REQ_CODE:
//                initPlayer();
//                for (int ret : grantResults) {
//                    if (ret != PackageManager.PERMISSION_GRANTED) {
//                        return;
//                    }
//                }
//                bPermission = true;
//                break;
//            default:
//                break;
//        }
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        /**   6.0权限申请     **/
//        bPermission = checkPublishPermission();
//        setContentView(R.layout.activity_main);
//        // 尽量在请求权限后再初始化
//        if (bPermission) {
//            initPlayer();
//        }
//        mMediaOption = findViewById(R.id.mediaOption);
//        mMediaOption.setGravity(Gravity.CENTER);
//        findViewById(R.id.player_about).setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                SettingActivity.start(MainActivity.this);
//            }
//        });
//        mBtnLiveStream = findViewById(R.id.livestreamBtn);
//        mBtnVideoOnDemand = findViewById(R.id.videoOnDemandBtn);
//        mBtnShortVideo = findViewById(R.id.shortVideoBtn);
//        mBtnMultiVideo = findViewById(R.id.multiVideoBtn);
//        mMediaTypeSelected = findViewById(R.id.mediaTypeSelected);
//        mEditURL = findViewById(R.id.netVideoUrl);
//        mQRScan = findViewById(R.id.btnScan);
//        mBtnPlay = findViewById(R.id.play_button);
//        mManualPlay = findViewById(R.id.manual_play);
//        mHardware = findViewById(R.id.hardware);
//        mSoftware = findViewById(R.id.software);
//        mSoftware.setButtonDrawable(R.drawable.decode_type_selected);
//        mHardware.setButtonDrawable(R.drawable.decode_type_unselected);
//        mHardwareReminder = findViewById(R.id.hardware_reminder);
//        DisplayMetrics dm = new DisplayMetrics(); //获取屏幕分辨率
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        final int screenW = dm.widthPixels;
//        LayoutParams params = mMediaTypeSelected.getLayoutParams();
//        tabWidth = screenW / 4;
//        params.width = tabWidth;
//        mMediaTypeSelected.setLayoutParams(params);
//        mBtnLiveStream.setOnClickListener(onClickListener);
//        mBtnVideoOnDemand.setOnClickListener(onClickListener);
//        mBtnShortVideo.setOnClickListener(onClickListener);
//        mBtnMultiVideo.setOnClickListener(onClickListener);
//        mHardware.setOnClickListener(onClickListener);
//        mSoftware.setOnClickListener(onClickListener);
//        mBtnPlay.setOnClickListener(onClickListener);
//        mQRScan.setOnClickListener(onClickListener);
//        mManualPlay.setOnClickListener(onClickListener);
//    }
//
//    private OnClickListener onClickListener = new OnClickListener() {
//
//        @SuppressLint("NewApi")
//        @Override
//        public void onClick(View v) {
//            switch (v.getId()) {
//                case R.id.livestreamBtn:
//                    mBtnLiveStream.setEnabled(false);
//                    mBtnVideoOnDemand.setEnabled(true);
//                    mBtnShortVideo.setEnabled(true);
//                    mBtnMultiVideo.setEnabled(true);
//                    mMediaTypeSelected.setX((float) 0.0);
//                    mEditURL.setVisibility(View.VISIBLE);
//                    mEditURL.setHint("请输入直播流地址：URL");
//                    mQRScan.setVisibility(View.VISIBLE);
//                    mHardware.setVisibility(View.VISIBLE);
//                    mSoftware.setVisibility(View.VISIBLE);
//                    mHardwareReminder.setVisibility(View.VISIBLE);
//                    mBtnPlay.setVisibility(View.VISIBLE);
//                    mManualPlay.setVisibility(View.VISIBLE);
//                    mediaType = "livestream";
//                    break;
//                case R.id.videoOnDemandBtn:
//                    mBtnLiveStream.setEnabled(true);
//                    mBtnVideoOnDemand.setEnabled(false);
//                    mBtnShortVideo.setEnabled(true);
//                    mBtnMultiVideo.setEnabled(true);
//                    mMediaTypeSelected.setX((float) tabWidth);
//                    mEditURL.setVisibility(View.VISIBLE);
//                    mEditURL.setHint("请输入点播流地址：URL");
//                    mQRScan.setVisibility(View.VISIBLE);
//                    mHardware.setVisibility(View.VISIBLE);
//                    mSoftware.setVisibility(View.VISIBLE);
//                    mHardwareReminder.setVisibility(View.VISIBLE);
//                    mBtnPlay.setVisibility(View.VISIBLE);
//                    mManualPlay.setVisibility(View.VISIBLE);
//                    mediaType = "videoondemand";
//                    break;
//                case R.id.shortVideoBtn:
//                    mBtnLiveStream.setEnabled(true);
//                    mBtnVideoOnDemand.setEnabled(true);
//                    mBtnShortVideo.setEnabled(false);
//                    mBtnMultiVideo.setEnabled(true);
//                    mMediaTypeSelected.setX((float) tabWidth * 2);
//                    mEditURL.setVisibility(View.INVISIBLE);
//                    mQRScan.setVisibility(View.INVISIBLE);
//                    mHardware.setVisibility(View.INVISIBLE);
//                    mSoftware.setVisibility(View.INVISIBLE);
//                    mHardwareReminder.setVisibility(View.INVISIBLE);
//                    mBtnPlay.setVisibility(View.VISIBLE);
//                    mManualPlay.setVisibility(View.GONE);
//                    mediaType = "shortvideo";
//                    break;
//                case R.id.multiVideoBtn:
//                    mBtnLiveStream.setEnabled(true);
//                    mBtnVideoOnDemand.setEnabled(true);
//                    mBtnShortVideo.setEnabled(true);
//                    mBtnMultiVideo.setEnabled(false);
//                    mMediaTypeSelected.setX((float) tabWidth * 3);
//                    mEditURL.setVisibility(View.VISIBLE);
//                    mEditURL.setHint("请输入点播流地址：URL");
//                    mQRScan.setVisibility(View.VISIBLE);
//                    mHardware.setVisibility(View.VISIBLE);
//                    mSoftware.setVisibility(View.VISIBLE);
//                    mHardwareReminder.setVisibility(View.VISIBLE);
//                    mBtnPlay.setVisibility(View.VISIBLE);
//                    mManualPlay.setVisibility(View.GONE);
//                    mediaType = "multivideo";
//                    break;
//                case R.id.btnScan:
//                    Intent intent2 = new Intent(MainActivity.this, QRCodeScanActivity.class);
//                    startActivityForResult(intent2, REQUEST_CODE);
//                    break;
//                case R.id.hardware:
//                    mSoftware.setButtonDrawable(R.drawable.decode_type_unselected);
//                    mHardware.setButtonDrawable(R.drawable.decode_type_selected);
//                    decodeType = "hardware";
//                    break;
//                case R.id.software:
//                    mSoftware.setButtonDrawable(R.drawable.decode_type_selected);
//                    mHardware.setButtonDrawable(R.drawable.decode_type_unselected);
//                    decodeType = "software";
//                    break;
//                case R.id.play_button:
//                    String url = mEditURL.getText().toString();
//                    Log.d(TAG, "url = " + url);
//                    Log.d(TAG, "decode_type = " + decodeType);
//                    if (TextUtils.isEmpty(url) && (TextUtils.equals("livestream", mediaType) || TextUtils.equals(
//                            "videoondemand", mediaType) || TextUtils.equals("multivideo", mediaType))) {
//                        AlertDialogBuild(0);
//                        break;
//                    }
//                    if (!bPermission) {
//                        showToast("app所需要的权限未满足");
//                    }
//                    if (config != null && config.dynamicLoadingConfig != null &&
//                        config.dynamicLoadingConfig.isDynamicLoading && !NELivePlayer.isDynamicLoadReady()) {
//                        showToast("请等待加载so文件");
//                        return;
//                    }
//                    //把多个参数传给Activity
//                    Intent intent;
//                    if (mediaType != null && mediaType.equals("livestream")) {
//                        intent = new Intent(MainActivity.this, LiveActivity.class);
//                    } else if (mediaType.equals("videoondemand")) {
//                        intent = new Intent(MainActivity.this, VodActivity.class);
//                    } else if (mediaType.equals("multivideo")) {
//                        intent = new Intent(MainActivity.this, MultiPlayerActivity.class);
//                    } else {
//                        //如果使用surface，打开ShortVideoSurfaceActivity示例
//                        //                            intent = new Intent(MainActivity.this, ShortVideoSurfaceActivity.class);
//                        //如果使用Texture，打开ShortVideoTextureActivity示例
//                        intent = new Intent(MainActivity.this, ShortVideoTextureActivity.class);
//                    }
//                    intent.putExtra("media_type", mediaType);
//                    intent.putExtra("decode_type", decodeType);
//                    intent.putExtra("videoPath", url);
//                    startActivity(intent);
//                    break;
//                case R.id.manual_play:
//                    if (!bPermission) {
//                        showToast("app所需要的权限未满足");
//                        return;
//                    }
//                    if (config != null && config.dynamicLoadingConfig != null &&
//                        config.dynamicLoadingConfig.isDynamicLoading && !NELivePlayer.isDynamicLoadReady()) {
//                        showToast("请等待加载so文件");
//                        return;
//                    }
//                    String url1 = mEditURL.getText().toString();
//                    if (TextUtils.isEmpty(url1)) {
//                        showToast("地址不能为空");
//                        return;
//                    }
//                    Intent intent1;
//                    if (mediaType.equals("livestream")) {
//                        intent1 = new Intent(MainActivity.this, ManualLiveActivity.class);
//                    } else {
//                        intent1 = new Intent(MainActivity.this, ManualVodActivity.class);
//                    }
//                    intent1.putExtra("media_type", mediaType);
//                    intent1.putExtra("decode_type", decodeType);
//                    intent1.putExtra("videoPath", url1);
//                    startActivity(intent1);
//                    break;
//            }
//        }
//    };
//
//    private void initPlayer() {
//        config = new SDKOptions();
//        //是否开启动态加载功能，默认关闭
//        //        config.dynamicLoadingConfig = new NEDynamicLoadingConfig();
//        //        config.dynamicLoadingConfig.isDynamicLoading = true;
//        //        config.dynamicLoadingConfig.isArmeabiv7a = true;
//        //        config.dynamicLoadingConfig.armeabiv7aUrl = "your_url";
//        //        config.dynamicLoadingConfig.onDynamicLoadingListener = mOnDynamicLoadingListener;
//        // SDK将内部的网络请求以回调的方式开给上层，如果需要上层自己进行网络请求请实现config.dataUploadListener，
//        // 如果上层不需要自己进行网络请求而是让SDK进行网络请求，这里就不需要操作config.dataUploadListener
//        config.dataUploadListener = mOnDataUploadListener;
//        //是否支持H265解码回调
//        config.supportDecodeListener = mOnSupportDecodeListener;
//        //这里可以绑定客户的账号系统或device_id，方便出问题时双方联调
//        //        config.thirdUserId = "your_id";
//        config.privateConfig = new NEPlayerConfig();
//        PlayerManager.init(this, config);
//        SDKInfo sdkInfo = PlayerManager.getSDKInfo(this);
//        Log.i(TAG, "NESDKInfo:version" + sdkInfo.version + ",deviceId:" + sdkInfo.deviceId);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (data == null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
//            return;
//        }
//        String result = data.getExtras().getString("result");
//        if (mEditURL != null) {
//            mEditURL.setText(result);
//        }
//    }
//
//    public void AlertDialogBuild(int flag) //创建对话框
//    {
//        AlertDialog alertDialog;
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//        alertDialogBuilder.setTitle("注意");
//        if (flag == 0) {
//            if (mediaType.equals("livestream")) {
//                alertDialogBuilder.setMessage("请输入直播流地址");
//            } else if (mediaType.equals("videoondemand")) {
//                alertDialogBuilder.setMessage("请输入点播流地址");
//            }
//        } else if (flag == 1) {
//            if (mediaType.equals("livestream")) {
//                alertDialogBuilder.setMessage("请输入正确的直播流地址");
//            } else if (mediaType.equals("videoondemand")) {
//                alertDialogBuilder.setMessage("请输入正确的点播流地址");
//            }
//        }
//        alertDialogBuilder.setCancelable(false).setPositiveButton("确定", new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                ;
//            }
//        });
//        alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
//    }
//
//    @Override
//    public void onPause() {
//        Log.d(TAG, "on pause");
//        super.onPause();
//    }
//
//    @Override
//    public void onDestroy() {
//        Log.d(TAG, "on destroy");
//        super.onDestroy();
//    }
//
//    @Override
//    public void onRestart() {
//        Log.d(TAG, "on restart");
//        super.onRestart();
//    }
//
//    @Override
//    public void onResume() {
//        Log.d(TAG, "on resmue");
//        super.onResume();
//    }
//
//    @Override
//    public void onStart() {
//        Log.d(TAG, "on start");
//        super.onStart();
//    }
//
//
//    private NELivePlayer.OnDynamicLoadingListener mOnDynamicLoadingListener = new NELivePlayer.OnDynamicLoadingListener() {
//
//        @Override
//        public void onDynamicLoading(NEDynamicLoadingConfig.ArchitectureType type, boolean isCompleted) {
//            Log.d(TAG, "type:" + type + "，isCompleted:" + isCompleted);
//        }
//    };
//
//    private NELivePlayer.OnSupportDecodeListener mOnSupportDecodeListener = new NELivePlayer.OnSupportDecodeListener() {
//
//        @Override
//        public void onSupportDecode(boolean isSupport) {
//            Log.d(TAG, "是否支持H265硬件解码 onSupportDecode isSupport:" + isSupport);
//            //如果支持H265硬件解码，那么可以使用H265的视频源进行播放
//        }
//    };
//
//
//    private NELivePlayer.OnDataUploadListener mOnDataUploadListener = new NELivePlayer.OnDataUploadListener() {
//
//        @Override
//        public boolean onDataUpload(String url, String data) {
//            Log.d(TAG, "onDataUpload url:" + url + ", data:" + data);
//            sendData(url, data);
//            return true;
//        }
//
//        @Override
//        public boolean onDocumentUpload(String url, Map<String, String> params, Map<String, String> filepaths) {
//            Log.d(TAG, "onDataUpload url:" + url + ", params:" + params + ",filepaths:" + filepaths);
//            return (new HttpPostUtils(url, params, filepaths).connPost());
//        }
//    };
//
//    private boolean sendData(final String urlStr, final String content) {
//        int response = 0;
//        try {
//            URL url = new URL(urlStr);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setConnectTimeout(5000);
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//            OutputStream outputStream = conn.getOutputStream();
//            outputStream.write(content.getBytes());
//            response = conn.getResponseCode();
//            if (response == HttpURLConnection.HTTP_OK) {
//                Log.i(TAG, " sendData finished,data:" + content);
//
//            } else {
//                Log.i(TAG, " sendData, response: " + response);
//
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "sendData, recv code is error: " + e.getMessage());
//
//        } catch (Exception e) {
//            Log.e(TAG, "sendData, recv code is error2: " + e.getMessage());
//
//        }
//        return (response == HttpURLConnection.HTTP_OK);
//    }
//
//}
