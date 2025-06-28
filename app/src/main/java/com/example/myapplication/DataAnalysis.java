package com.example.myapplication;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        //自动生成AI分析
        generateInitialAnalysis();

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
                    NativeChatBot.queryAPI(question,new NativeChatBot.ChatCallback(){
                        @Override

                        public void onSuccess(final String answer){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvAnswer.setText(answer);
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
    private void generateInitialAnalysis() {
        // 获取图表数据
        String[] times = {"10:00", "11:00", "12:00", "13:00", "14:00"};
        float[] temperatures = {26f, 27f, 28f, 29f, 32f};
        float[] humidities = {45f, 47f, 48f, 49f, 56f};

        // 格式化为字符串
        StringBuilder dataDescription = new StringBuilder("以下是最近5个时间点的温度和湿度数据：\n");
        for (int i = 0; i < times.length; i++) {
            dataDescription.append("时间: ").append(times[i])
                    .append(", 温度: ").append(temperatures[i]).append("℃")
                    .append(", 湿度: ").append(humidities[i]).append("%\n");
        }
        dataDescription.append("我是一个农业工作者,现在请你对上面的相关数据进行分析,以便于我开展相关的农业工作");

        // 调用 AI API 并显示结果
        NativeChatBot.queryAPI(dataDescription.toString(), new NativeChatBot.ChatCallback() {
            @Override
            public void onSuccess(final String answer) {
                runOnUiThread(() -> addMessageToChat("AI: " + answer, false));
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(() -> Toast.makeText(DataAnalysis.this, "分析失败: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
    // 动态添加消息到聊天容器
    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(8, 8, 8, 8);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);

        if (isUser) {
            textView.setGravity(Gravity.END); // 用户消息靠右
            textView.setBackgroundColor(Color.LTGRAY); // 灰色背景
        } else {
            textView.setGravity(Gravity.START); // AI 消息靠左
            textView.setBackgroundColor(Color.WHITE); // 白色背景
        }

        chatContainer.addView(textView);
        // 自动滚动到最新消息
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}