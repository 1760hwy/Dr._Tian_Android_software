package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private EditText username;
    private EditText passward;
    private Button button;
    private TextView textView;
    private DatabaseHelper databaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });
        init();
    }
    private void init(){
        username=findViewById(R.id.et_user_name);
        passward=findViewById(R.id.et_user_passwd);
        button=findViewById(R.id.denglv);
        textView=findViewById(R.id.enroll);
        databaseHelper=new DatabaseHelper(this);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,SQL_enroll.class);
                startActivity(intent);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username1=username.getText().toString().trim();
                String password1=passward.getText().toString().trim();

                if (username1.isEmpty()||password1.isEmpty()){
                    Toast.makeText(MainActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (username1.equals("123")&&password1.equals("123")){
                    Toast.makeText(MainActivity.this,"登陆成功",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(MainActivity.this,manage.class);
                    startActivity(intent);
                }

                boolean success = databaseHelper.loginUser(username1, password1);
                if (success) {
                    Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(MainActivity.this,manage.class);
                    startActivity(intent);
                    finish();
                    // 在此添加跳转到主界面或仪表板的 Intent
                } else {
                    Toast.makeText(MainActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}