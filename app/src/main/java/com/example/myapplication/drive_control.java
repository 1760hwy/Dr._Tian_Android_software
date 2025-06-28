package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class drive_control extends AppCompatActivity {

    private Button Quit;
    private Button bt_send, bt_reception, bt_clear;
    private Button forward, back, left, right, stop;
    private TextView connect_state, rev_tv, command_show;

    // 蓝牙相关
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream; // 修改为 InputStream
    private BufferedReader bufferedReader; // 用于按行读取
    private Thread receiveThread; // 接收数据的线程

    // ESP32的蓝牙MAC地址 (你需要替换成你的ESP32的MAC地址)
    private static final String ESP32_MAC_ADDRESS = "20:00:00:00:38:78"; // 例如 "00:11:22:33:44:55"//20:00:00:00:38:78
    // 标准的SPP UUID
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Handler handler;

    // Handler消息类型常量
    private static final int CONNECT_SUCCESS = 1;
    private static final int CONNECT_FAIL = 2;
    private static final int RECEIVE_DATA = 3;
    private static final int SEND_SUCCESS = 4;
    private static final int SEND_FAIL = 5;
    private static final int BLUETOOTH_NOT_SUPPORTED = 6;
    private static final int BLUETOOTH_NOT_ENABLED = 7;
    private static final int DISCONNECTED = 8;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drive_control);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new androidx.core.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        init();
        initHandler();
        initBluetooth(); // 初始化蓝牙并尝试连接
    }

    private void init() {
        //初始化
        Quit = findViewById(R.id.quit);
        bt_send = findViewById(R.id.bt_send);
        bt_reception = findViewById(R.id.bt_reception);
        bt_clear = findViewById(R.id.bt_clear);
        forward = findViewById(R.id.forward);
        back = findViewById(R.id.back);
        left = findViewById(R.id.left);
        right = findViewById(R.id.right);
        stop = findViewById(R.id.stop);
        connect_state = findViewById(R.id.connect_state);
        rev_tv = findViewById(R.id.rev_tv);
        command_show = findViewById(R.id.command_show);

        //绑定控件
        Quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("forward");
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("back");
            }
        });
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("left");
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("right");
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("stop");
            }
        });
        // 设置其他按钮监听器
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 可根据需要实现发送功能，例如发送自定义指令
                // sendCommand("custom_command_from_bt_send");
                Toast.makeText(drive_control.this, "发送功能待实现 (请在代码中具体实现)", Toast.LENGTH_SHORT).show();
            }
        });
        bt_reception.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 接收是自动的，此按钮可以用于其他功能，例如重新连接或显示状态
                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    Toast.makeText(drive_control.this, "已连接，数据自动接收中", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(drive_control.this, "未连接，请尝试重新启动应用或检查蓝牙设置", Toast.LENGTH_SHORT).show();
                    // 可以尝试重新连接
                    // initBluetooth();
                }
            }
        });
        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rev_tv.setText("");
            }
        });
    }

    private void sendCommand(final String command) {
        // 在子线程中发送命令
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (outputStream != null && bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    try {
                        outputStream.write((command + "\n").getBytes()); // ESP32端需要能处理换行符作为结束标志
                        outputStream.flush();
                        Message msg = new Message();
                        msg.what = SEND_SUCCESS;
                        msg.obj = command;
                        handler.sendMessage(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.sendEmptyMessage(SEND_FAIL);
                    }
                } else {
                    handler.sendEmptyMessage(SEND_FAIL); // 或者提示未连接
                    if (bluetoothSocket == null || !bluetoothSocket.isConnected()){
                        handler.sendMessage(handler.obtainMessage(CONNECT_FAIL, "未连接到设备"));
                    }
                }
            }
        }).start();
    }

    private void initHandler() {
        //初始化Handler用来更新UI
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECT_SUCCESS:
                        connect_state.setText("已连接到: " + ESP32_MAC_ADDRESS);
                        Toast.makeText(drive_control.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                        break;
                    case CONNECT_FAIL:
                        String reason = (msg.obj != null) ? msg.obj.toString() : "连接失败";
                        connect_state.setText(reason);
                        Toast.makeText(drive_control.this, reason, Toast.LENGTH_SHORT).show();
                        break;
                    case RECEIVE_DATA:
                        String data = (String) msg.obj;
                        rev_tv.append(data + "\n");
                        break;
                    case SEND_SUCCESS:
                        command_show.setText("命令发送成功: " + msg.obj);
                        break;
                    case SEND_FAIL:
                        if (bluetoothSocket == null || !bluetoothSocket.isConnected()){
                            command_show.setText("命令发送失败: 未连接");
                            Toast.makeText(drive_control.this, "发送失败: 未连接", Toast.LENGTH_SHORT).show();
                        } else {
                            command_show.setText("命令发送失败");
                            Toast.makeText(drive_control.this, "发送失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case BLUETOOTH_NOT_SUPPORTED:
                        connect_state.setText("设备不支持蓝牙");
                        Toast.makeText(drive_control.this, "此设备不支持蓝牙", Toast.LENGTH_LONG).show();
                        break;
                    case BLUETOOTH_NOT_ENABLED:
                        connect_state.setText("蓝牙未启用");
                        Toast.makeText(drive_control.this, "请先开启蓝牙", Toast.LENGTH_LONG).show();
                        // 你可以在这里尝试发起一个Intent来请求用户开启蓝牙
                        // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); // 需要定义REQUEST_ENABLE_BT
                        break;
                    case DISCONNECTED:
                        connect_state.setText("连接已断开");
                        Toast.makeText(drive_control.this, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            handler.sendEmptyMessage(BLUETOOTH_NOT_SUPPORTED);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            handler.sendEmptyMessage(BLUETOOTH_NOT_ENABLED);
            // 考虑请求用户开启蓝牙，见 initHandler 中的注释
            return;
        }
        // 已经在 onCreate 中，开始连接
        connectToESP32();
    }


    private void connectToESP32() {
        connect_state.setText("正在连接中...");
        new Thread(new Runnable() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void run() {
                // 在尝试连接前，确保之前的socket和流已关闭（如果存在）
                closeBluetoothConnection();

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(ESP32_MAC_ADDRESS);
                try {
                    // 检查BLUETOOTH_CONNECT权限 (API 31+)
                    // 为了简化，这里假定权限已授予。在实际应用中，你应该在调用 connect 之前检查并请求此权限。
                    // if (ActivityCompat.checkSelfPermission(drive_control.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    //      handler.sendMessage(handler.obtainMessage(CONNECT_FAIL, "缺少BLUETOOTH_CONNECT权限"));
                    //     return;
                    // }
                    //bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(BT_MODULE_UUID); // 尝试这个
                    bluetoothSocket.connect(); // 这是一个阻塞调用

                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream)); // 用BufferedReader包装

                    handler.sendEmptyMessage(CONNECT_SUCCESS);

                    // 启动接收数据的线程
                    receiveThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String line;
                                // 当socket连接时，持续读取数据
                                while (bluetoothSocket != null && bluetoothSocket.isConnected() && (line = bufferedReader.readLine()) != null) {
                                    Message msg = new Message();
                                    msg.what = RECEIVE_DATA;
                                    msg.obj = line;
                                    handler.sendMessage(msg);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                // 连接断开或读取错误
                                if (bluetoothSocket != null && bluetoothSocket.isConnected()) { // 确保不是主动关闭导致的
                                    handler.sendEmptyMessage(DISCONNECTED);
                                }
                            } finally {
                                // 清理BufferedReader, InputStream 在 closeBluetoothConnection 中处理
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    bufferedReader = null;
                                }
                            }
                        }
                    });
                    receiveThread.start();

                } catch (IOException e) {
                    e.printStackTrace();
                    handler.sendMessage(handler.obtainMessage(CONNECT_FAIL, "连接失败: " + e.getMessage()));
                    closeBluetoothConnection(); // 连接失败时，确保关闭socket
                } catch (SecurityException se) {
                    // 通常是权限问题，比如 API 31+ 缺少 BLUETOOTH_CONNECT 权限
                    se.printStackTrace();
                    handler.sendMessage(handler.obtainMessage(CONNECT_FAIL, "连接失败: 权限问题"));
                    closeBluetoothConnection();
                }
            }
        }).start();
    }

    private void closeBluetoothConnection() {
        try {
            if (receiveThread != null) {
                receiveThread.interrupt(); // 尝试中断接收线程
                receiveThread = null;
            }
            if (bufferedReader != null) {
                bufferedReader.close();
                bufferedReader = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBluetoothConnection();
    }
}