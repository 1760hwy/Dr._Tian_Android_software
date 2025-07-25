package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.SQL_Lite.SensorData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Native;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;

import javax.net.ssl.SNIHostName;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DataAnalysis extends AppCompatActivity {

    private Button Backbutton;

    //折线图
    private LineChart lineChart;
    //柱状图
    private BarChart barChart;

    //AI人机交互
    private TextView tvAnswer;
    private EditText etQuestion;
    private Button btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;

    private Handler handler;

    // --- 核心：实时数据传输所需成员 ---
    private BroadcastReceiver sensorDataReceiver;//声名一个广播接收器
    private ArrayList<SensorData> currentHistoricalData=new ArrayList<>(); // 用于存储从广播接收到的最新历史数据
    private static final String TAG="DataAnalysis_log";//过滤日志

    // 添加频率控制相关变量
    private long lastAnalysisTime = 0; // 上次分析时间
    private static final long ANALYSIS_INTERVAL = 30000; // 30秒间隔（可调整）
    private int dataUpdateCount = 0; // 数据更新计数器
    private static final int UPDATE_THRESHOLD = 5; // 每5次更新才分析一次（可调整）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_data_analysis);

        //初始化
        Backbutton = findViewById(R.id.Backbutton);

        lineChart = findViewById(R.id.lineChart1);
        setupLineChart();
        setChartData();
        barChart = findViewById(R.id.barChart);
        setupBarChart();
        setBarChartData();

        //ai交互
        // 初始化控件
        etQuestion = findViewById(R.id.etQuestion);
        btnSend = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrolView);
        tvAnswer=findViewById(R.id.tvAnswer);
        handler=new Handler(Looper.getMainLooper());
        //数据库
        handler.post(SQ_Lite());

        etQuestion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question=etQuestion.getText().toString().trim();
                if (!question.isEmpty()){
                    // 显示用户消息
                    addMessageToChat("用户: " + question, true);
                    etQuestion.setText(""); // 清空输入框

                    NativeChatBot.queryAPI(question,new NativeChatBot.ChatCallback(){
                        @Override
                        public void onSuccess(final String answer){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addMessageToChat("AI: " + answer, false);
                                }
                            });
                        }
                        @Override
                        public void onError(final String error){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DataAnalysis.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }else{
                    Toast.makeText(DataAnalysis.this,"问题不能为空哦`-`",Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });
        Backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //返回上一层(关闭当前Activity)
                finish();
            }
        });
    }

    //折线图图表数据
    private void setupLineChart() {
        //设置图表描述
        Description description = new Description();
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
        Legend legend = lineChart.getLegend();
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

    private void setChartData() {
        // 示例数据
        String[] times = {"10:00", "11:00", "12:00", "13:00", "14:00"};
        float[] temperatures = {27.4f, 27.2f, 28.8f, 27.3f, 27.8f};
        float[] humidities = {77.3f, 78.2f, 77.3f, 78.4f, 76.3f};

        List<Entry> temperatureEntries = new ArrayList<>();
        List<Entry> humidityEntries = new ArrayList<>();

        for (int i = 0; i < times.length; i++) {
            temperatureEntries.add(new Entry(i, temperatures[i]));
            humidityEntries.add(new Entry(i, humidities[i]));
        }

        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "温度 (℃)");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setCircleColor(Color.RED);
        temperatureDataSet.setLineWidth(2f);
        temperatureDataSet.setCircleRadius(4f);
        temperatureDataSet.setDrawValues(false);

        LineDataSet humidityDataSet = new LineDataSet(humidityEntries, "湿度 (%)");
        humidityDataSet.setColor(Color.BLUE);
        humidityDataSet.setCircleColor(Color.BLUE);
        humidityDataSet.setLineWidth(2f);
        humidityDataSet.setCircleRadius(4f);
        humidityDataSet.setDrawValues(false);

        LineData lineData = new LineData(temperatureDataSet, humidityDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // 刷新图表
    }

    // 设置柱状图的样式
    private void setupBarChart() {
        Description description = new Description();
        description.setText("温度与湿度柱状图");
        barChart.setDescription(description);

        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setPinchZoom(true);
        barChart.setBackgroundColor(Color.WHITE);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextColor(Color.BLACK);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.BLACK);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    // 设置柱状图的数据
    private void setBarChartData() {
        String[] times = {"10:00", "11:00", "12:00", "13:00", "14:00"};
        float[] temperatures = {27.4f, 27.2f, 28.8f, 27.3f, 27.8f};
        float[] humidities = {77.3f, 78.2f, 77.3f, 78.4f, 76.3f};

        List<BarEntry> temperatureEntries = new ArrayList<>();
        List<BarEntry> humidityEntries = new ArrayList<>();

        for (int i = 0; i < times.length; i++) {
            temperatureEntries.add(new BarEntry(i, temperatures[i]));
            humidityEntries.add(new BarEntry(i, humidities[i]));
        }

        BarDataSet temperatureDataSet = new BarDataSet(temperatureEntries, "温度 (℃)");
        temperatureDataSet.setColor(Color.RED);

        BarDataSet humidityDataSet = new BarDataSet(humidityEntries, "湿度 (%)");
        humidityDataSet.setColor(Color.BLUE);

        BarData data = new BarData(temperatureDataSet, humidityDataSet);
        data.setBarWidth(0.4f); // 设置柱状图的宽度
        barChart.setData(data);
        barChart.groupBars(0f, 0.1f, 0.05f); // 设置组间距和柱间距
        barChart.invalidate(); // 刷新图表
    }

    //数据传输 - 修改后添加频率控制
    private Runnable SQ_Lite(){
        try {
            ArrayList<SensorData> historicalData = getIntent().getParcelableArrayListExtra("initial_history_data");
            if (historicalData != null && !historicalData.isEmpty()) {
                Log.d(TAG, "Received initial historicalData with size: " + historicalData.size());
                currentHistoricalData.addAll(historicalData);
                updateChartData(currentHistoricalData);

                // 初始分析只执行一次
                generateInitialAnalysis();
            } else {
                updateChartData(new ArrayList<>());
                addMessageToChat("🤖 等待传感器数据中...", false);
            }
        }catch (Exception e){
            Log.e(TAG, "Error processing initial data.", e);
        }

        // 注册广播接收器，添加频率控制
        sensorDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.myapplication.SENSOR_DATA_REALTIME_UPDATE".equals(intent.getAction())) {
                    Log.d(TAG, "Received sensor data update broadcast.");

                    ArrayList<SensorData> updatedData = intent.getParcelableArrayListExtra("updated_history_data");

                    if (updatedData != null) {
                        currentHistoricalData.clear();
                        currentHistoricalData.addAll(updatedData);
                        updateChartData(currentHistoricalData);

                        // 增加更新计数
                        dataUpdateCount++;

                        // 频率控制：时间间隔 OR 更新次数达到阈值
                        long currentTime = System.currentTimeMillis();
                        boolean timeIntervalReached = (currentTime - lastAnalysisTime) >= ANALYSIS_INTERVAL;
                        boolean updateThresholdReached = dataUpdateCount >= UPDATE_THRESHOLD;

                        if (timeIntervalReached || updateThresholdReached) {
                            generateRealtimeAnalysis();
                            lastAnalysisTime = currentTime;
                            dataUpdateCount = 0; // 重置计数器
                            Log.d(TAG, "AI analysis triggered. Reason: " +
                                    (timeIntervalReached ? "Time interval" : "Update threshold"));
                        } else {
                            Log.d(TAG, "AI analysis skipped. Updates: " + dataUpdateCount +
                                    ", Time since last: " + (currentTime - lastAnalysisTime) + "ms");
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.example.myapplication.SENSOR_DATA_REALTIME_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, filter);
        Log.d(TAG, "BroadcastReceiver registered.");
        return null;
    };

    //AI人机交互
    private static class NativeChatBot{
        public static void queryAPI(String question,ChatCallback callBack){
            new AsyncTask<String,Void,String>(){
                private Exception exception;

                protected String doInBackground(String...params){
                    String apiUrl="https://api.moonshot.cn/v1/chat/completions";
                    String apiKey="sk-L2omJCkUz1zaJ0O7gXZoP5ZXq2c45FpNJj56DHnqpyR364UJ";

                    try{
                        URL url=new URL(apiUrl);
                        HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type","application/json");
                        conn.setRequestProperty("Authorization","Bearer "+apiKey);
                        conn.setConnectTimeout(60000);
                        conn.setReadTimeout(60000);
                        conn.setDoOutput(true);

                        JSONObject jsonParam=new JSONObject();
                        jsonParam.put("model","moonshot-v1-128k");
                        jsonParam.put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",params[0])));

                        OutputStream os=conn.getOutputStream();
                        byte[] input=jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input,0,input.length);
                        os.close();

                        int responseCode=conn.getResponseCode();
                        if (responseCode==200){
                            InputStream is=conn.getInputStream();
                            BufferedReader reader=new BufferedReader(new InputStreamReader(is));
                            StringBuilder response=new StringBuilder();

                            String line;
                            while((line=reader.readLine())!=null){
                                response.append(line);
                            }
                            reader.close();
                            return response.toString();
                        }else {
                            throw new IOException("HTTP error code"+responseCode);
                        }
                    }catch (Exception e){
                        this.exception=e;
                        return null;
                    }
                }
                protected void onPostExecute(String result){
                    if (result!=null){
                        try {
                            JSONObject json=new JSONObject(result);
                            String answer=json.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            callBack.onSuccess(answer);
                        } catch (JSONException e) {
                            callBack.onError("解析错误"+e.getMessage());
                        }
                    }else {
                        callBack.onError("请求失败"+(exception!=null?exception.getMessage():"未知错误"));
                    }
                }
            }.execute(question);
        }
        public interface ChatCallback{
            void onSuccess(String answer);
            void onError(String error);
        }
    }

    // 修改初始分析方法
    private void generateInitialAnalysis() {
        // 检查是否有实际数据
        if (currentHistoricalData == null || currentHistoricalData.isEmpty()) {
            addMessageToChat("🤖 暂无传感器数据，无法进行分析。\n", false);
            return;
        }

        // 使用实际接收到的传感器数据
        StringBuilder dataDescription = new StringBuilder("以下是最近的温度和湿度数据：\n");

        // 获取最近的数据点（最多5个）
        int dataSize = currentHistoricalData.size();
        int startIndex = Math.max(0, dataSize - 5);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (int i = startIndex; i < dataSize; i++) {
            SensorData data = currentHistoricalData.get(i);
            String formattedTime = sdf.format(new Date(data.getTimestamp()));

            dataDescription.append("时间: ").append(formattedTime)
                    .append(", 温度: ").append(data.getTemperature()).append("℃")
                    .append(", 湿度: ").append(data.getHumidity()).append("%\n");
        }

        dataDescription.append("我是一个农业工作者,现在请你对上面的相关数据进行分析,以便于我开展相关的农业工作");

        // 显示"分析中"状态
        addMessageToChat("🤖 正在进行初始数据分析...\n", false);

        // 调用 AI API 并显示结果
        NativeChatBot.queryAPI(dataDescription.toString(), new NativeChatBot.ChatCallback() {
            @Override
            public void onSuccess(final String answer) {
                runOnUiThread(() -> addMessageToChat("📋 初始分析报告: " + answer, false));
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(() -> {
                    addMessageToChat("❌ 初始分析失败: \n" + error, false);
                    Toast.makeText(DataAnalysis.this, "分析失败: \n" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // 修改实时分析方法，添加更详细的日志和优化
    private void generateRealtimeAnalysis() {
        // 检查是否有足够的数据进行分析
        if (currentHistoricalData == null || currentHistoricalData.size() < 3) {
            Log.d(TAG, "Insufficient data for analysis. Size: " +
                    (currentHistoricalData != null ? currentHistoricalData.size() : 0));
            return;
        }

        Log.d(TAG, "Starting realtime AI analysis...");

        // 获取最新的3-5个数据点进行趋势分析
        int dataSize = currentHistoricalData.size();
        int startIndex = Math.max(0, dataSize - 5);

        StringBuilder dataDescription = new StringBuilder("数据更新 - ");

        // 添加时间戳，让用户知道这是什么时候的分析
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        dataDescription.append(timeFormat.format(new Date())).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = startIndex; i < dataSize; i++) {
            SensorData data = currentHistoricalData.get(i);
            String formattedTime = sdf.format(new Date(data.getTimestamp()));

            dataDescription.append("时间: ").append(formattedTime)
                    .append(", 温度: ").append(data.getTemperature()).append("℃")
                    .append(", 湿度: ").append(data.getHumidity()).append("%\n");
        }

        dataDescription.append("请简要分析这些数据的变化趋势，并给出农业生产建议。");

        // 显示"分析中"状态
        TextView analysisStatus = new TextView(this);
        analysisStatus.setText("🤖 AI正在分析最新数据...\n");
        analysisStatus.setPadding(8, 8, 8, 8);
        analysisStatus.setTextColor(Color.GRAY);
        analysisStatus.setTextSize(12);
        analysisStatus.setGravity(Gravity.START);
        analysisStatus.setBackgroundColor(Color.TRANSPARENT);
        chatContainer.addView(analysisStatus);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        // 调用 AI API
        NativeChatBot.queryAPI(dataDescription.toString(), new NativeChatBot.ChatCallback() {
            @Override
            public void onSuccess(final String answer) {
                runOnUiThread(() -> {
                    // 移除"分析中"状态
                    chatContainer.removeView(analysisStatus);

                    // 添加分析结果，带时间戳
                    String timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    addMessageToChat("📊 [" + timestamp + "] " + answer, false);

                    Log.d(TAG, "AI analysis completed successfully");
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(() -> {
                    // 移除"分析中"状态
                    chatContainer.removeView(analysisStatus);

                    addMessageToChat("❌ 分析失败: " + error, false);
                    Log.e(TAG, "AI analysis failed: " + error);
                });
            }
        });
    }

    // 添加手动触发分析的方法（可选）
    public void triggerManualAnalysis() {
        if (currentHistoricalData != null && !currentHistoricalData.isEmpty()) {
            generateRealtimeAnalysis();
            lastAnalysisTime = System.currentTimeMillis();
            dataUpdateCount = 0;
        } else {
            Toast.makeText(this, "暂无数据可分析", Toast.LENGTH_SHORT).show();
        }
    }

    // 优化后的添加消息方法，避免重复消息
    private void addMessageToChat(String message, boolean isUser) {
        // 检查是否是重复的AI分析消息（简单检查）
        if (!isUser && message.contains("📊")) {
            // 移除旧的分析消息，保持界面整洁
            for (int i = chatContainer.getChildCount() - 1; i >= 0; i--) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    String text = textView.getText().toString();
                    if (text.contains("📊") && !text.equals(message)) {
                        // 只保留最近2条分析消息
                        int analysisCount = 0;
                        for (int j = chatContainer.getChildCount() - 1; j >= 0; j--) {
                            View otherChild = chatContainer.getChildAt(j);
                            if (otherChild instanceof TextView) {
                                TextView otherTextView = (TextView) otherChild;
                                if (otherTextView.getText().toString().contains("📊")) {
                                    analysisCount++;
                                    if (analysisCount > 2) { // 保留最近2条，删除更早的
                                        chatContainer.removeView(otherChild);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(8, 8, 8, 8);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);

        if (isUser) {
            textView.setGravity(Gravity.END);
            textView.setBackgroundColor(Color.LTGRAY);
        } else {
            textView.setGravity(Gravity.START);
            textView.setBackgroundColor(Color.WHITE);
        }

        chatContainer.addView(textView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }


    // 在 updateChartData 方法中添加 BarChart 的更新逻辑
    private void updateChartData(List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            // 清空 LineChart
            lineChart.clear();
            lineChart.invalidate();

            // 清空 BarChart
            barChart.clear();
            barChart.invalidate();
            return;
        }

        final int MAX_DATA_POINTS = 5;

        // LineChart 数据准备
        List<Entry> temperatureEntries = new ArrayList<>();
        List<Entry> humidityEntries = new ArrayList<>();

        // BarChart 数据准备
        List<BarEntry> temperatureBarEntries = new ArrayList<>();
        List<BarEntry> humidityBarEntries = new ArrayList<>();

        final List<String> timestamps = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        int startIndex = Math.max(0, dataList.size() - MAX_DATA_POINTS);

        // 同时为 LineChart 和 BarChart 准备数据
        for (int i = startIndex; i < dataList.size(); i++) {
            SensorData data = dataList.get(i);
            int chartIndex = i - startIndex;

            // LineChart 数据
            temperatureEntries.add(new Entry(chartIndex, data.getTemperature()));
            humidityEntries.add(new Entry(chartIndex, data.getHumidity()));

            // BarChart 数据
            temperatureBarEntries.add(new BarEntry(chartIndex, data.getTemperature()));
            humidityBarEntries.add(new BarEntry(chartIndex, data.getHumidity()));

            long timestampMillis = data.getTimestamp();
            String formattedTime = sdf.format(new Date(timestampMillis));
            timestamps.add(formattedTime);
        }

        // 更新 LineChart
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "温度 (℃)");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setCircleColor(Color.RED);
        temperatureDataSet.setDrawValues(false);

        LineDataSet humidityDataSet = new LineDataSet(humidityEntries, "湿度 (%)");
        humidityDataSet.setColor(Color.BLUE);
        humidityDataSet.setCircleColor(Color.BLUE);
        humidityDataSet.setDrawValues(false);

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

        LineData lineData = new LineData(temperatureDataSet, humidityDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        // 更新 BarChart
        BarDataSet temperatureBarDataSet = new BarDataSet(temperatureBarEntries, "温度 (℃)");
        temperatureBarDataSet.setColor(Color.RED);

        BarDataSet humidityBarDataSet = new BarDataSet(humidityBarEntries, "湿度 (%)");
        humidityBarDataSet.setColor(Color.BLUE);

        // 设置 BarChart 的 X 轴标签
        XAxis barXAxis = barChart.getXAxis();
        barXAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) {
                    return timestamps.get(index);
                }
                return "";
            }
        });

        BarData barData = new BarData(temperatureBarDataSet, humidityBarDataSet);
        barData.setBarWidth(0.4f); // 设置柱状图的宽度
        barChart.setData(barData);
        barChart.groupBars(0f, 0.1f, 0.05f); // 设置组间距和柱间距
        barChart.invalidate(); // 刷新图表
    }
}