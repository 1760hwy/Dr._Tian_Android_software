package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;



import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.pm.PackageManager; // 用于检查权限状态
import androidx.annotation.NonNull; // 用于标记参数或返回值不应为null
import androidx.core.app.ActivityCompat; // 用于请求权限和检查权限
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log; // 用于日志输出
import android.widget.Toast; // 用于显示提示信息
// (你已有的其他 import 语句)

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.AmapPageType;
import com.example.myapplication.Video;
import com.tplink.smbdeveplatsdk.SMBCloudSDKContext;

public class Control extends AppCompatActivity implements VideoPlaybackFragment.VideoPlaybackListener{

    // 声明导航项的布局、图标和文字
    private LinearLayout managerLayout, controlLayout, analysisLayout, mynameLayout;
    private ImageView managerIcon, controlIcon, analysisIcon, mynameIcon;
    private TextView managerText, controlText, analysisText, mynameText;



    //上层交互
    private LinearLayout sensingLayout, deviceControlLayout, videoLayout, driverLayout;
    private ImageView sensingIcon, deviceControlIcon, videoIcon, driverIcon;
    private TextView sensingText, deviceControlText, videoText, driverText;

    //上层交互默认选中
    private int selectedDeviceIndex=1;


    // 记录当前选中的导航项索引
    private int selectedIndex = 1; // 默认选中第二个导航项

    //导航栏
    private MapView mapView;//地图显示
    private AMap aMap;//地图控制
    private Button startNaviButton;//开始导航按钮
    private AMapLocationClient locationClient;//获取用户当前位置
    private AMapLocationClientOption locationOption;//配置定位参数
    private static final int PERMISSION_REQUEST_CODE=100;//权限请求唯一标识码
    private LatLng endPoint =new LatLng(27.8369,113.1384);//重点坐标

    // >>> 新增或确认存在：用于存储当前定位到的经纬度（作为类成员，全局可用）
    private LatLng currentLatLng;
    // >>> 新增或确认存在：用于存储当前位置的标记，方便后续移除或更新
    private Marker currentLocationMarker;




    //天气预报
    private TextView tvZuigaowen0, tvZuidiwen0, tvZuigaowen1, tvZuidiwen1;
    private ImageView ivWeather0, ivWeather1;

    private static final String TAG = "MainActivity";
    private static final String API_KEY = "kEW0dciQ5rc99ASB"; // 替换为您的彩云天气 API 密钥
    private static final String LOCATION = "113.131695,27.827433"; // 株洲市经纬度


    //摄像头
    private FrameLayout frameLayoutVideoContainer;
    private VideoPlaybackFragment videoFragmentInstance;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setupAmapPrivacy();


        setContentView(R.layout.activity_control);



        // 初始化导航项布局
        managerLayout = findViewById(R.id.manager);
        controlLayout = findViewById(R.id.control);
        analysisLayout = findViewById(R.id.analysis);
        mynameLayout = findViewById(R.id.myname);

        // 初始化图标
        managerIcon = findViewById(R.id.main_icon);
        controlIcon = findViewById(R.id.main_icon2);
        analysisIcon = findViewById(R.id.main_icon3);
        mynameIcon = findViewById(R.id.main_icon4);

        // 初始化文字
        managerText = findViewById(R.id.manager_text);
        controlText = findViewById(R.id.control_text);
        analysisText = findViewById(R.id.analysis_text);
        mynameText = findViewById(R.id.myname_text);




        //上层交互初始化
        // 初始化设备项布局
        sensingLayout = findViewById(R.id.sensingLayout1);
        deviceControlLayout = findViewById(R.id.controlLayout1);
        videoLayout = findViewById(R.id.VideoLayout1);
        driverLayout = findViewById(R.id.driverLayout1);

        // 初始化设备项图标
        sensingIcon = findViewById(R.id.sensingpage1);
        deviceControlIcon = findViewById(R.id.controlpage1);
        videoIcon = findViewById(R.id.videopage1);
        driverIcon = findViewById(R.id.drivepage1);

        // 初始化设备项文字
        sensingText = findViewById(R.id.sensingtext1);
        deviceControlText = findViewById(R.id.controltext1);
        videoText = findViewById(R.id.videotext1);
        driverText = findViewById(R.id.drivetext1);


        //天气预报
        // 初始化视图
        tvZuigaowen0 = findViewById(R.id.zuigaowen0);
        tvZuidiwen0 = findViewById(R.id.zuidiwen0);
        tvZuigaowen1 = findViewById(R.id.zuigaowen1);
        tvZuidiwen1 = findViewById(R.id.zuidiwen1);
        ivWeather0 = findViewById(R.id.weatherIcon0);
        ivWeather1 = findViewById(R.id.weatherIcon1);

        //获取天气数据
        fetchWeatherData();


        //导航
        initNavi(savedInstanceState);






        //摄像头
        frameLayoutVideoContainer=findViewById(R.id.video_navigation);




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.control), (v, insets) -> {
            return insets;
        });
        managerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(0);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                Intent intent=new Intent(Control.this,manage.class);
                startActivity(intent);
            }
        });
        controlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(1);
            }
        });
        analysisLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(2);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                Intent intent=new Intent(Control.this,Analysis.class);
                startActivity(intent);
            }
        });
        mynameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(3);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                Intent intent=new Intent(Control.this,Myname.class);
                startActivity(intent);
            }
        });



        //上层交互点击事件
        sensingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(0);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                Intent intent=new Intent(Control.this,manage.class);
                startActivity(intent);
            }
        });

        deviceControlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(1);
            }
        });

        videoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Control.this,Video.class);
                startActivity(intent);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                setSelectedDevice(2);
            }
        });

        driverLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(3);
                hideVideoFragmentIfShown(); // 如果切换，隐藏视频
                Intent intent=new Intent(Control.this,drive_control.class);
                startActivity(intent);
            }
        });


        setSelectedTab(1);

        //上层交互默认选中
        setSelectedDevice(1);
        if (selectedDeviceIndex==1){
            loadVideoFragment();
        }else {
            hideVideoFragmentIfShown(); //确保一开始是隐藏的如果不是默认视频
        }
    }
    private void setSelectedTab(int index){
        //重置所有的导航项状态
        resetTabs();

        //更新选中的索引
        selectedIndex=index;

        //使用if_else更新相关的选中状态
        if (index==0){
            managerIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            managerText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index==1) {
            controlIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            controlText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index==2) {
            analysisIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            analysisText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index==3) {
            mynameIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            mynameText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    private void resetTabs(){
        //清除图标颜色过滤器,恢复图标默认颜色
        managerIcon.clearColorFilter();
        controlIcon.clearColorFilter();
        analysisIcon.clearColorFilter();
        mynameIcon.clearColorFilter();

        //设置文字颜色为默认黑色
        managerText.setTextColor(getResources().getColor(android.R.color.black));
        controlText.setTextColor(getResources().getColor(android.R.color.black));
        analysisText.setTextColor(getResources().getColor(R.color.black));
        mynameText.setTextColor(getResources().getColor(R.color.black));
    }

    private void setSelectedDevice(int index){
        // 重置所有设备项状态
        resetDevices();

        // 更新选中的索引
        selectedDeviceIndex = index;

        // 使用 if-else 更新相关的选中状态
        if (index == 0) {
            sensingIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            sensingText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index == 1) {
            deviceControlIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            deviceControlText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index == 2) {
            videoIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            videoText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (index == 3) {
            driverIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            driverText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void resetDevices(){
        // 清除图标颜色过滤器，恢复默认颜色
        sensingIcon.clearColorFilter();
        deviceControlIcon.clearColorFilter();
        videoIcon.clearColorFilter();
        driverIcon.clearColorFilter();

        // 设置文字颜色为默认黑色
        sensingText.setTextColor(getResources().getColor(android.R.color.black));
        deviceControlText.setTextColor(getResources().getColor(android.R.color.black));
        videoText.setTextColor(getResources().getColor(android.R.color.black));
        driverText.setTextColor(getResources().getColor(android.R.color.black));
    }





    private void fetchWeatherData() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    URL url = new URL("https://api.caiyunapp.com/v2.5/" + API_KEY + "/" + LOCATION + "/weather.json?lang=zh_CN&unit=metric");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        return response.toString();
                    } else {
                        Log.e("Weather", "HTTP error code: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e("Weather", "Error fetching weather data", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String response) {
                if (response != null) {
                    parseWeatherData(response);
                }
            }
        }.execute();
    }


    private void parseWeatherData(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject result = json.getJSONObject("result");
            JSONObject daily = result.getJSONObject("daily");

            JSONArray temperatureArray = daily.getJSONArray("temperature");
            JSONArray skyconArray = daily.getJSONArray("skycon");

            if (temperatureArray.length() >= 3 && skyconArray.length() >= 3) {
                // 明天：index 1
                JSONObject tempTomorrow = temperatureArray.getJSONObject(1);
                double maxTomorrow = tempTomorrow.getDouble("max");
                double minTomorrow = tempTomorrow.getDouble("min");
                JSONObject skyconTomorrow = skyconArray.getJSONObject(1);
                String skyconValueTomorrow = skyconTomorrow.getString("value");

                // 后天：index 2
                JSONObject tempDayAfter = temperatureArray.getJSONObject(2);
                double maxDayAfter = tempDayAfter.getDouble("max");
                double minDayAfter = tempDayAfter.getDouble("min");
                JSONObject skyconDayAfter = skyconArray.getJSONObject(2);
                String skyconValueDayAfter = skyconDayAfter.getString("value");

                // 在主线程更新UIa
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvZuigaowen0.setText("最高温:" + (int) maxTomorrow + "℃");
                        tvZuidiwen0.setText("最低温:" + (int) minTomorrow + "℃");
                        ivWeather0.setImageResource(getWeatherIcon(skyconValueTomorrow));

                        tvZuigaowen1.setText("最高温:" + (int) maxDayAfter + "℃");
                        tvZuidiwen1.setText("最低温:" + (int) minDayAfter + "℃");
                        ivWeather1.setImageResource(getWeatherIcon(skyconValueDayAfter));
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Weather", "Error parsing weather data", e);
        }
    }

    private void setupAmapPrivacy() {
        AMapLocationClient.updatePrivacyShow(this,true,true);
        AMapLocationClient.updatePrivacyAgree(this,true);
    }


    private void loadVideoFragment() {
        if (frameLayoutVideoContainer == null) {
            Log.e("ControlActivity", "video_navigation FrameLayout is null!");
            Toast.makeText(this, "视频容器未准备好", Toast.LENGTH_SHORT).show();
            return;
        }
        frameLayoutVideoContainer.setVisibility(View.VISIBLE); // 确保容器可见

        if (videoFragmentInstance == null) {
            videoFragmentInstance = VideoPlaybackFragment.newInstance();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (!videoFragmentInstance.isAdded()) {
            fragmentTransaction.replace(R.id.video_navigation, videoFragmentInstance, "VIDEO_PLAYBACK_FRAGMENT");
        } else if (!videoFragmentInstance.isVisible()) {
            fragmentTransaction.show(videoFragmentInstance);
        }
        // fragmentTransaction.addToBackStack(null); // 通常对于这种嵌入式的不需要
        fragmentTransaction.commitAllowingStateLoss();
        Log.d("ControlActivity", "已加载/显示视频 Fragment");
    }
    private void hideVideoFragmentIfShown() {
        if (videoFragmentInstance != null && videoFragmentInstance.isAdded() && videoFragmentInstance.isVisible()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.hide(videoFragmentInstance);
            fragmentTransaction.commitAllowingStateLoss();
            Log.d("ControlActivity", "已隐藏视频 Fragment");
            // 或者让容器消失
            // if (frameLayoutVideoContainer != null) {
            //    frameLayoutVideoContainer.setVisibility(View.GONE);
            // }
        }
    }

    private int getWeatherIcon(String skycon) {
        if (skycon.equals("CLEAR_DAY") || skycon.equals("CLEAR_NIGHT")) {
            return R.drawable.sum; // 晴天（白天或夜晚）
        } else if (skycon.equals("PARTLY_CLOUDY_DAY") || skycon.equals("PARTLY_CLOUDY_NIGHT")) {
            return R.drawable.cloud; // 多云
        } else if (skycon.equals("CLOUDY")) {
            return R.drawable.cloudy; // 阴天
        } else if (skycon.equals("LIGHT_RAIN") || skycon.equals("MODERATE_RAIN") || skycon.equals("HEAVY_RAIN")) {
            return R.drawable.rain; // 雨
        } else {
            Log.w("WeatherIcon", "Unknown skycon: " + skycon); // 调试未知值
            return R.drawable.cloud; // 默认多云
        }
    }

    //导航
    private void initNavi(Bundle savedInsstanceState){
        mapView=findViewById(R.id.mapView);
        mapView.onCreate(savedInsstanceState);
        aMap=mapView.getMap();

        startNaviButton=findViewById(R.id.btnStartCruise);
        startNaviButton.setOnClickListener(v -> startNavigation());

        if (aMap==null){
            Log.e("MapError","Failed to initialize Amap");
            Toast.makeText(this,"地图初始化失败，请监测配置或重试",Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkConnected()){
            Toast.makeText(this,"请检查网络连接",Toast.LENGTH_SHORT).show();
            return;
        }

        requestPermissions();
    }

    private boolean isNetworkConnected(){
        ConnectivityManager cm=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm!=null){
            NetworkInfo activeNetwork=cm.getActiveNetworkInfo();
            return activeNetwork !=null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void requestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            setupMapAndMarkers();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "所有必需权限已获取", Toast.LENGTH_SHORT).show();
                setupMapAndMarkers();
            } else {
                Toast.makeText(this, "定位权限被拒绝，导航无法启动", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupMapAndMarkers() {
        if (aMap != null) {
            aMap.addMarker(new MarkerOptions().position(endPoint).title("终点").snippet("株洲市农业科学研究院"));
        } else {
            Toast.makeText(this, "地图对象未准备好", Toast.LENGTH_SHORT).show();
            Log.e("MapError", "aMap object is null in setupMapAndMarkers()");
        }
    }

    private void startNavigation() {
        if (!isNetworkConnected()) {
            Toast.makeText(this, "请检查网络连接", Toast.LENGTH_LONG).show();
            return;
        }
        initLocation();
    }

    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(this);
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(true); // 单次定位
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(locationListener);
            locationClient.startLocation();
        } catch (Exception e) {
            Log.e("LocationError", "Failed to initialize location client: " + e.getMessage());
            Toast.makeText(this, "定位初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (location != null) {
                if (location.getErrorCode() == 0) {
                    com.amap.api.maps.model.LatLng currentLocation = new com.amap.api.maps.model.LatLng(location.getLatitude(), location.getLongitude());
                    aMap.addMarker(new MarkerOptions().position(currentLocation).title("当前位置"));
                    startNavigationFromCurrentLocation(currentLocation);
                } else {
                    Toast.makeText(Control.this, "定位失败，错误码：" + location.getErrorCode(), Toast.LENGTH_SHORT).show();
                    Log.e("LocationError", "Location failed with error code: " + location.getErrorCode());
                }
            } else {
                Toast.makeText(Control.this, "定位失败，位置信息为空", Toast.LENGTH_SHORT).show();
            }
            if (locationClient != null) {
                locationClient.stopLocation();
            }
        }
    };


    private void startNavigationFromCurrentLocation(com.amap.api.maps.model.LatLng currentLocation) {
        Poi start = new Poi("当前位置", currentLocation, "");
        Poi end = new Poi("株洲市农业科学研究院", endPoint, "");
        AmapNaviParams naviParams = new AmapNaviParams(start, null, end, AmapNaviType.DRIVER, AmapPageType.ROUTE);
        try {
            AmapNaviPage.getInstance().showRouteActivity(this, naviParams, null);
        } catch (Exception e) {
            Log.e("NavigationError", "Failed to start navigation: " + e.getMessage());
            Toast.makeText(this, "导航启动失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onStatusUpdate(@NonNull String statusMessage) {
        // 在 Control Activity 中通过 Toast 显示状态
        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show();
        Log.i("ControlActivity_Video", "状态更新: " + statusMessage);
    }

    @Override
    public void onError(@NonNull String errorMessage) {
        // 在 Control Activity 中通过 Toast 显示错误
        Toast.makeText(this, "视频错误: " + errorMessage, Toast.LENGTH_LONG).show();
        Log.e("ControlActivity_Video", "错误: " + errorMessage);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}