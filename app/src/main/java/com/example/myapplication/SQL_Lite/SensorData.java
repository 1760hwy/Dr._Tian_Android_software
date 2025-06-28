package com.example.myapplication.SQL_Lite;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.myapplication.SimpleImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SensorData implements Parcelable {
    private float temperature;
    private float humidity;
    private long timestamp; // 记录数据生成的时间戳

    public SensorData(float temperature, float humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = System.currentTimeMillis(); // 构造时记录当前时间
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // 重写 toString 方法，方便显示
    @Override
    public String toString() {
        // 格式化时间戳为可读的字符串
        // HH:mm:ss 表示时:分:秒，mm/dd 表示月/日，您可以根据需要调整格式
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = sdf.format(new Date(timestamp));
        return String.format(Locale.getDefault(), "[%s] 温度: %.1f°C, 湿度: %.1f%%",
                timeStr, temperature, humidity);
    }

    // --- Parcelable Implementation ---

    // 构造函数，用于从Parcel中恢复对象
    protected SensorData(Parcel in) {
        temperature = in.readFloat();
        humidity = in.readFloat();
        timestamp = in.readLong();
    }

    // CREATOR 对象，用于反序列化
    // 这是实现 Parcelable 接口必须有的一个静态 final 字段
    public static final Creator<SensorData> CREATOR = new Creator<SensorData>() {
        @Override
        public SensorData createFromParcel(Parcel in) {
            return new SensorData(in);
        }

        @Override
        public SensorData[] newArray(int size) {
            return new SensorData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // 大多数情况下返回0即可，除非你的Parcelable包含了FileDescriptor这样的特殊数据
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // 将对象数据写入Parcel
        dest.writeFloat(temperature);
        dest.writeFloat(humidity);
        dest.writeLong(timestamp);
    }
}