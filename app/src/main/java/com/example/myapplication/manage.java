package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.MarkerOptions;
import com.example.myapplication.SQL_Lite.SensorData;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class manage extends AppCompatActivity {

    // 声明导航项的布局、图标和文字
    private LinearLayout managerLayout, controlLayout, analysisLayout, mynameLayout;
    private ImageView managerIcon, controlIcon, analysisIcon, mynameIcon;
    private TextView managerText, controlText, analysisText, mynameText;


   //上层交互
    private LinearLayout sensingLayout, deviceControlLayout, videoLayout, driverLayout;
    private ImageView sensingIcon, deviceControlIcon, videoIcon, driverIcon;
    private TextView sensingText, deviceControlText, videoText, driverText;

    //上层交互默认选中
    private int selectedDeviceIndex=0;

    // 记录当前选中的导航项索引
    private int selectedIndex = 0; // 默认选中第一个导航项



    //天气预报
    private TextView tvZuigaowen0, tvZuidiwen0, tvZuigaowen1, tvZuidiwen1;
    private ImageView ivWeather0, ivWeather1;

    private static final String TAG = "MainActivity";
    private static final String API_KEY = "kEW0dciQ5rc99ASB"; // 替换为您的彩云天气 API 密钥
    private static final String LOCATION = "113.131695,27.827433"; // 株洲市经纬度



    //语音播报
    private TextToSpeech tts;
    private TextView tvTemperature,tvHumidity,tvLight,tvSoilFertility;
    private Switch switchVoice;
    private SeekBar seekBarVolume;
    private TextView tvVolumePercent;

    //2d地图
    private MapView mapView;
    private AMap aMap;

    //株洲市经纬度
    private static final LatLng ZHUZHOU=new LatLng(27.8878,113.1851);



    //传感器交互
    private TextView wd,sd,gz,tf;
    private Handler handler;
    private Random random;
    private DecimalFormat decimalFormat; // 用于格式化带一位小数的数字
    // 随机值范围和更新间隔常量
    private final double MIN_TEMPERATURE = 26.0;
    private final double MAX_TEMPERATURE = 29.0;
    private final double MIN_HUMIDITY = 75.0;
    private final double MAX_HUMIDITY = 77.0;
    private final int MIN_LIGHT = 15000;
    private final int MAX_LIGHT = 23000;
    // 新增：用于 Analysis Activity 读取的纯数值 static 变量
    public static float currentTempValue = 26.0f; // 默认初始值
    public static float currentHumValue = 75.0f; // 默认初始值



    //数据库交互
    private ArrayList<SensorData> historicalData;
    private static final int MAX_HISTORY_SIZE = 10; // 最多存储10条历史数据




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage);

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
        sensingLayout = findViewById(R.id.sensingLayout);
        deviceControlLayout = findViewById(R.id.controlLayout);
        videoLayout = findViewById(R.id.VideoLayout);
        driverLayout = findViewById(R.id.driverLayout);

        // 初始化设备项图标
        sensingIcon = findViewById(R.id.sensingpage);
        deviceControlIcon = findViewById(R.id.controlpage);
        videoIcon = findViewById(R.id.videopage);
        driverIcon = findViewById(R.id.drivepage);

        // 初始化设备项文字
        sensingText = findViewById(R.id.sensingtext);
        deviceControlText = findViewById(R.id.controltext);
        videoText = findViewById(R.id.videotext);
        driverText = findViewById(R.id.drivetext);


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


        //传感器交互
        wd=findViewById(R.id.wendu);
        sd=findViewById(R.id.shidu);
        gz=findViewById(R.id.guangzhao);
        tf=findViewById(R.id.turang);
        handler=new Handler(Looper.getMainLooper());
        random=new Random();
        decimalFormat=new DecimalFormat("0.0");
        //初始化列表
        historicalData=new ArrayList<SensorData>();//初始化历史数据列表
        Test();








        //语音播报
        switchVoice=findViewById(R.id.switch_voicenoise);
        seekBarVolume=findViewById(R.id.seekbar_volume);
        tvVolumePercent=findViewById(R.id.tv_volume_percent);


        //初始化地图
        mapView=findViewById(R.id.mapView1);
        mapView.onCreate(savedInstanceState);
        aMap=mapView.getMap();

        //将地图中心设置为株洲,缩放等级12
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ZHUZHOU, 16));

        aMap.addMarker(new MarkerOptions()
                .position(ZHUZHOU) // 标记位置为株洲市坐标
                .title("株洲市位置") // 标记标题
                .snippet("纬度: " + ZHUZHOU.latitude + ", 经度: " + ZHUZHOU.longitude) // 标记描述
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))); // 设置标记为蓝色


        // 初始化 AudioManager
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 获取当前媒体音量和最大媒体音量
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // 将 SeekBar 的最大值设为 100，初始值按比例映射当前系统音量
        seekBarVolume.setMax(100);
        int progress = (int) ((currentVolume / (float) maxVolume) * 100);
        seekBarVolume.setProgress(progress);
        tvVolumePercent.setText(progress + "%");


        switchVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(manage.this,"语音播报已开启",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(manage.this,"语音播报已关闭",Toast.LENGTH_SHORT).show();
                }
            }
        });

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumePercent.setText(progress+"%");
                //更新音量百分比显示
                // 根据 SeekBar 的百分比计算系统音量
                int newVolume = (int) ((progress / 100.0f) * maxVolume);
                // 设置媒体音量，不产生声音反馈（flag = 0）
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //开始拖动时的操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //拖动结束时的操作
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manager), (v, insets) -> {
            return insets;
        });
        managerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(0);
            }
        });
        controlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(1);
                Intent intent=new Intent(manage.this,Control.class);
                startActivity(intent);
            }
        });
        analysisLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(2);
                Intent intent=new Intent(manage.this,Analysis.class);
                intent.putParcelableArrayListExtra("initial_history_data", historicalData); // <--- 注意：这里参数名建议和实时广播的不同
                startActivity(intent);
            }
        });
        handler.post(fetch());

        mynameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(3);
                Intent intent=new Intent(manage.this,Myname.class);
                startActivity(intent);
            }
        });


        //上层交互点击事件
        sensingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(0);
            }
        });

        deviceControlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(1);
                Intent intent=new Intent(manage.this,Control.class);
                startActivity(intent);
            }
        });

        videoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(2);
                Intent intent=new Intent(manage.this,Video.class);
                startActivity(intent);
            }
        });

        driverLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedDevice(3);
                Intent intent=new Intent(manage.this,drive_control.class);
                startActivity(intent);
            }
        });

        //默认选中第一个manager
        setSelectedTab(0);

        //上层交互默认选中
        setSelectedDevice(0);
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
//重置所有设备为未选中状态
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

                // 在主线程更新UI
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

    //传感器交互
    private void Test(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetch();
                Test();
            }
        },3000);
    }

    private Runnable fetch(){
        // 生成随机温度 (26.0 - 33.0)
        // random.nextDouble() 返回 [0.0, 1.0)
        double tempValue=MIN_TEMPERATURE+(MAX_TEMPERATURE-MIN_TEMPERATURE)*random.nextDouble();
        // 可选: 做一次边界限制确保严格在范围内 (虽然数学上一般不需要，但nextDouble的精度问题可能导致非常接近边界的值)
        tempValue=Math.min(MAX_TEMPERATURE,Math.max(MIN_TEMPERATURE,tempValue));
        if (wd!=null){
            wd.setText("温度"+decimalFormat.format(tempValue)+"℃");
        }
        manage.currentTempValue=(float) tempValue;

        double humValue=MIN_HUMIDITY+(MAX_HUMIDITY-MIN_HUMIDITY)*random.nextDouble();
        humValue=Math.min(MAX_HUMIDITY,Math.max(MIN_HUMIDITY,humValue));
        if (sd!=null){
            sd.setText("湿度："+decimalFormat.format(humValue)+"%");
        }
        manage.currentHumValue=(float) humValue;

        int lightValue=random.nextInt(MAX_LIGHT-MIN_LIGHT+1)+MIN_LIGHT;
        if (gz!=null){
            gz.setText("光照："+String.valueOf(lightValue)+"lux");
        }

        if (tf!=null){
            tf.setText("土壤肥力等级："+"-");
        }


        //将新的数据添加到历史列表中
        SensorData currentData=new SensorData((float) tempValue,(float) humValue);
        historicalData.add(currentData);
        // 如果历史数据量超过最大限制，移除最旧的一条
        if (historicalData.size()>MAX_HISTORY_SIZE){
            historicalData.remove(0);//移除列表头部最旧的数据
        }
        Intent dataUpdateIntent = new Intent("com.example.myapplication.SENSOR_DATA_REALTIME_UPDATE"); // 定义一个唯一的动作字符串
        dataUpdateIntent.putParcelableArrayListExtra("updated_history_data", historicalData); // 传递最新的整个列表
        LocalBroadcastManager.getInstance(this).sendBroadcast(dataUpdateIntent); // 发送本地广播

        Log.d(TAG, "Sent broadcast with " + historicalData.size() + " data points.");
        return null;
    }



    //地图生命周期管理
    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        if (handler!=null){
            handler.removeCallbacksAndMessages(null);//一键清除
            handler=null;//防止内存泄漏
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}