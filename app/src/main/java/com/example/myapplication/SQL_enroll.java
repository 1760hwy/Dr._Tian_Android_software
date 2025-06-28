package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SQL_enroll extends AppCompatActivity {

    private EditText etUsername,etPassword;
    private Button btnRegister;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sql_enroll);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });

        etUsername=findViewById(R.id.et_user_name);
        etPassword=findViewById(R.id.et_user_passwd);
        btnRegister=findViewById(R.id.denglv);
        databaseHelper=new DatabaseHelper(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // 检查输入是否为空
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SQL_enroll.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 检查用户名是否已存在
                if (databaseHelper.checkUsernameExists(username)) {
                    Toast.makeText(SQL_enroll.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                } else {
                    // 注册用户
                    boolean success = databaseHelper.registerUser(username, password);
                    if (success) {
                        Toast.makeText(SQL_enroll.this, "注册成功", Toast.LENGTH_SHORT).show();
                        finish(); // 返回登录界面
                    } else {
                        Toast.makeText(SQL_enroll.this, "注册失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}