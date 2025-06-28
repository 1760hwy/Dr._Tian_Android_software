package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;

import java.util.Arrays;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Myname extends AppCompatActivity {

    // 声明导航项的布局、图标和文字
    private LinearLayout managerLayout, controlLayout, analysisLayout, mynameLayout;
    private ImageView managerIcon, controlIcon, analysisIcon, mynameIcon;
    private TextView managerText, controlText, analysisText, mynameText;

    // 记录当前选中的导航项索引
    private int selectedIndex = 3; // 默认选中第四个导航项

    private LinearLayout linearLayoutmanage;
    private LinearLayout linearLayoutAbout;


    //轮播接口
    private Banner<String, BannerImageAdapter<String>> banner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_myname);

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


        //轮播
        banner = findViewById(R.id.main_banner);

        List<String> urls = Arrays.asList(
                "https://img95.699pic.com/photo/40190/7258.jpg_wh300.jpg!/fh/300/quality/90",
                "https://th.bing.com/th/id/R.989387d65c5ca7b1ea81f73edba028f8?rik=9S348ICa4tLdDg&riu=http%3a%2f%2fp4.itc.cn%2fq_70%2fimages03%2f20201105%2f570c82faf67041e3972911c422cd401e.jpeg&ehk=BEEAiFvXl5cqp3iVe0%2bMuzDmIngKW1xjQMG1JKZuziE%3d&risl=&pid=ImgRaw&r=0",
                "https://th.bing.com/th/id/R.19f6e864bd21ba0f1d57f694ac876834?rik=wGiFvaXCYi8wBQ&riu=http%3a%2f%2fimg.qjsmartech.com%2fTopic%2fImages%2f2021-05%2f2021052511012620028.jpg&ehk=51CryF6envUNnF2TSUBCzwZH8RjjTnHzsAFiWD1fwmM%3d&risl=&pid=ImgRaw&r=0",
                "https://www.citictel-cpc.com/editor/image/20190926/20190926092451_8418.png",
                "https://pic1.zhimg.com/v2-1053a80f911246dccf4e738e93e8dc18_r.jpg"
        );
        banner.setAdapter(new BannerImageAdapter<String>(urls) {
                    @Override
                    public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                        ImageView iv = holder.imageView;
                        iv.setImageBitmap(null);             // 重置
                        iv.setBackgroundColor(0xFFCCCCCC);   // 占位背景

                        // 调用我们写的原生加载器
                        SimpleImageLoader.load(data, new SimpleImageLoader.Callback() {
                            @Override
                            public void onLoaded(Bitmap bmp) {
                                if (bmp != null) {
                                    holder.imageView.setImageBitmap(bmp);
                                }
                            }
                        });
                    }
                })
                .setLoopTime(2000)  // 轮播间隔；也可通过 XML 属性 app:banner_loop_time 设置
                .start();



        linearLayoutmanage=findViewById(R.id.item_manage_platform);
        linearLayoutAbout=findViewById(R.id.item_about_platform);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myname), (v, insets) -> {
            return insets;
        });
        managerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(0);
                Intent intent=new Intent(Myname.this,manage.class);
                startActivity(intent);
            }
        });
        controlLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(1);
                Intent intent=new Intent(Myname.this,Control.class);
                startActivity(intent);
            }
        });
        analysisLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(2);
                Intent intent=new Intent(Myname.this,Analysis.class);
                startActivity(intent);
            }
        });
        mynameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedTab(3);
            }
        });
        linearLayoutmanage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Myname.this,manage.class);
                startActivity(intent);
            }
        });

        linearLayoutAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Myname.this,About_the_platform.class);
                startActivity(intent);
            }
        });



        setSelectedTab(3);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (banner != null) banner.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (banner != null) banner.stop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();//释放内存
    }
}