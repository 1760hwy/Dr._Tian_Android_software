package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.SQL_Lite.SensorData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

public class Analysis extends AppCompatActivity {

    // 声明导航项的布局、图标和文字
    private LinearLayout managerLayout, controlLayout, analysisLayout, mynameLayout;
    private ImageView managerIcon, controlIcon, analysisIcon, mynameIcon;
    private TextView managerText, controlText, analysisText, mynameText;

    private Handler handler;


    private Button buttonAI;

    //折线图
    private LineChart lineChart;


    // 记录当前选中的导航项索引
    private int selectedIndex = 2; // 默认选中第三个导航项


    private TextView historyTextView;

    // --- 核心：实时数据传输所需成员 ---
    private BroadcastReceiver sensorDataReceiver; // 声明一个广播接收器
    private ArrayList<SensorData> currentHistoricalData=new ArrayList<>(); // 用于存储从广播接收到的最新历史数据
    private static final String TAG="Analysis_log";//过滤日志

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analysis);

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


        buttonAI=findViewById(R.id.analysisbutton);

        handler=new Handler(Looper.getMainLooper());

        //数据库
        handler.post(SQL_Lite());



        //折线图
        lineChart=findViewById(R.id.lineChart);
        setupLineChart();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analysis), (v, insets) -> {
            return insets;
        });
        managerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(0);
                Intent intent=new Intent(Analysis.this,manage.class);
                startActivity(intent);
            }
        });
        controlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(1);
                Intent intent=new Intent(Analysis.this,Control.class);
                startActivity(intent);
            }
        });
        analysisLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(2);
            }
        });
        mynameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(3);
                Intent intent=new Intent(Analysis.this,Myname.class);
                startActivity(intent);
            }
        });

        buttonAI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Analysis.this,"开始AI数据分析,请耐心等待",Toast.LENGTH_LONG).show();
                Intent intent=new Intent(Analysis.this,DataAnalysis.class);
                startActivity(intent);
            }
        });


        setSelectedTab(2);
    }

    private Runnable SQL_Lite() {
        historyTextView = findViewById(R.id.historyTextView);

        try {
            // 获取初始历史数据
            ArrayList<SensorData> historicalData = getIntent().getParcelableArrayListExtra("initial_history_data");

            if (historicalData != null && !historicalData.isEmpty()) {
                Log.d(TAG, "Received initial historicalData with size: " + historicalData.size());
                currentHistoricalData.addAll(historicalData); // 存储初始数据
                updateHistoryDisplay(currentHistoricalData);
                updateChartData(currentHistoricalData); // <-- 关键：使用初始数据更新图表
            } else {
                updateChartData(new ArrayList<>()); // <-- 关键：如果没有数据，则清空图表
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing initial data.", e);
            historyTextView.setText("加载历史数据时发生错误。");
        }

        // 注册广播接收器，用于实时接收数据更新
        sensorDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.myapplication.SENSOR_DATA_REALTIME_UPDATE".equals(intent.getAction())) {
                    Log.d(TAG, "Received sensor data update broadcast.");

                    ArrayList<SensorData> updatedData = intent.getParcelableArrayListExtra("updated_history_data");

                    if (updatedData != null) {
                        currentHistoricalData.clear(); // 清空旧数据
                        currentHistoricalData.addAll(updatedData); // 添加新数据
                        updateHistoryDisplay(currentHistoricalData); // 更新 TextView
                        updateChartData(currentHistoricalData); // <-- 关键：实时更新图表
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.myapplication.SENSOR_DATA_REALTIME_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, filter);
        Log.d(TAG, "BroadcastReceiver registered.");
        return null;
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
    //折线图图表数据
    private void setupLineChart(){
        //设置图表描述
        Description description=new Description();
        description.setText("温度与湿度变化图");
        lineChart.setDescription(description);

        //启动触摸手势
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        //配置图例
        Legend legend=lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);

        // 配置 X 轴
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // 设置最小间隔，防止缩放后标签重叠

        // 配置左侧 Y 轴
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);

        // 禁用右侧 Y 轴
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    // --- 新增：用于更新 TextView 的方法 ---
    private void updateHistoryDisplay(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            historyTextView.setText("当前暂无历史数据。");
            return;
        }

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("历史温湿度数据：\n\n");
        for (SensorData data : dataList) {
            historyBuilder.append(data.toString()).append("\n"); // 使用 SensorData 的 toString 方法
        }
        historyTextView.setText(historyBuilder.toString());
    }

    /**
     * 根据传入的传感器数据列表更新折线图，始终只显示最新的5条数据。
     * @param dataList 传感器数据列表
     */
    /**
     * 根据传入的传感器数据列表更新折线图，始终只显示最新的5条数据。
     * @param dataList 传感器数据列表
     */
    // 请用这个新版本完整替换你现有的 updateChartData 方法
    private void updateChartData(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        final int MAX_DATA_POINTS = 5;

        List<Entry> temperatureEntries = new ArrayList<>();
        List<Entry> humidityEntries = new ArrayList<>();
        final List<String> timestamps = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        int startIndex = Math.max(0, dataList.size() - MAX_DATA_POINTS);

        for (int i = startIndex; i < dataList.size(); i++) {
            SensorData data = dataList.get(i);
            int chartIndex = i - startIndex;
            temperatureEntries.add(new Entry(chartIndex, data.getTemperature()));
            humidityEntries.add(new Entry(chartIndex, data.getHumidity()));

            long timestampMillis = data.getTimestamp();
            String formattedTime = sdf.format(new Date(timestampMillis));
            timestamps.add(formattedTime);
        }

        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "温度 (℃)");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setCircleColor(Color.RED);
        temperatureDataSet.setDrawValues(false);

        LineDataSet humidityDataSet = new LineDataSet(humidityEntries, "湿度 (%)");
        humidityDataSet.setColor(Color.BLUE);
        humidityDataSet.setCircleColor(Color.BLUE);
        humidityDataSet.setDrawValues(false);

        // ★★★ 核心修正部分 ★★★
        // 使用 IAxisValueFormatter 兼容旧版本
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) {
                    return timestamps.get(index);
                }
                return "";
            }
        });
        // ★★★ 修正结束 ★★★

        LineData lineData = new LineData(temperatureDataSet, humidityDataSet);
        lineChart.setData(lineData);

        lineChart.invalidate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- 关键：在 Activity 销毁时，解除注册广播接收器，防止内存泄漏 ---
        if (sensorDataReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver);
            Log.d(TAG, "BroadcastReceiver unregistered.");
        }
        if (handler!=null){
            handler.removeCallbacksAndMessages(null);//一键清除
            handler=null;//防止内存泄漏
        }
    }
}