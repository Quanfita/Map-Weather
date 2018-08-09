package com.example.baidu.map;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ScrollingView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.example.baidu.map.gson.Basic;
import com.example.baidu.map.gson.Forecast;
import com.example.baidu.map.gson.Weather;
import com.example.baidu.map.service.AutoUpdateService;
import com.example.baidu.map.util.HttpUtil;
import com.example.baidu.map.util.Utility;
import com.example.baidu.map.AddressToLatitudeLongitude;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;
    private Button navButton;
    private Button mapButton;
    private Button seaButton;

    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;
    private  String CityName;
    private boolean First = true;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        
        weatherLayout =(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
        forecastLayout=(LinearLayout)findViewById(R.id.forecast_layout);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);

        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        navButton=(Button)findViewById(R.id.nav_button);
        mapButton=(Button)findViewById(R.id.map_button);
        seaButton=(Button)findViewById(R.id.sea_button);

        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        Intent intent=new Intent(this, AutoUpdateService.class);
        startService(intent);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        final String weatherString=prefs.getString("weather",null);
        String bingPic=prefs.getString("bing_pic",null);

        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }
        //获取要获得天气的地点
        final String WeatherId=getIntent().getStringExtra("string");
        if(WeatherId != null){
            if(weatherString!=null) {
                final Weather weather = Utility.handleWeatherResponse(weatherString);
                mWeatherId = weather.basic.weatherId;
                if(First == true){
                    CityName = WeatherId;
                    First = false;
                }
                else{
                    CityName = weather.basic.cityName;
                }
                Log.v("d","Cityname="+CityName);
                showWeatherInfo(weather);
            }
        }
        else {
            requestWeather("CN101090101");
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                requestWeather(WeatherId);
            }
        });
        //选择区域
        navButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        //回主页
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        //查找
        seaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        try{
                            URL url;
                            if(mWeatherId == null) {
                                //final Weather weather = Utility.handleWeatherResponse(weatherString);
                                AddressToLatitudeLongitude at = new AddressToLatitudeLongitude(WeatherId);
                                at.getLatAndLngByAddress();
                                url=new URL(String.format("http://api.yytianqi.com/weatherhours?city=%s,%s&key=n9qanosdrmg0f37d",(double)Math.round(at.getLatitude()*100-1)/100,(double)Math.round(at.getLongitude()*100)/100));
                            }
                            else  {
                                //final Weather weather = Utility.handleWeatherResponse(weatherString);
                                //CityName = weather.basic.cityName;
                                Log.v("d","CityName = "+CityName);
                                AddressToLatitudeLongitude at = new AddressToLatitudeLongitude(CityName);
                                requestWeather(CityName);
                                at.getLatAndLngByAddress();
                                url=new URL(String.format("http://api.yytianqi.com/weatherhours?city=%s,%s&key=n9qanosdrmg0f37d",(double)Math.round(at.getLatitude()*100-1)/100,(double)Math.round(at.getLongitude()*100)/100));
                            }

                            Log.v("d",url.toString());
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.setConnectTimeout(8000);
                            connection.setReadTimeout(8000);
                            InputStream in = connection.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                            StringBuilder response = new StringBuilder();
                            String line;
                            //下面对获取到的输入流进行读取
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            JSONObject json = new JSONObject(response.toString());
                            Log.v("d",json.toString());
                            JSONObject data = json.getJSONObject("data");
                            final String city = data.getString("cityName");
                            final JSONArray list = data.getJSONArray("list");
                            final int time=getTimefromJson(data.getString("sj"));
                            Node [] node = new Node[24];
                            for(int i = 0;i<list.length();i++) {
                                String tq = list.getJSONObject(i).getString("tq");
                                String qw = list.getJSONObject(i).getString("qw");
                                String sd = list.getJSONObject(i).getString("sd");
                                Log.v("d",tq);
                                Log.v("d",qw);
                                Log.v("d",sd);
                                node[(time+i)%24] = new Node();
                                node[(time+i)%24].tq = tq;
                                node[(time+i)%24].qw = qw;
                                node[(time+i)%24].sd = sd;
                            }
                            final Node [] tenode = node;
                            //弹窗显示查询信息
                            new AlertDialog.Builder(WeatherActivity.this)
                                    .setTitle("查询类型")
                                    .setItems(new String[]{"按时刻查询", "按时段查询"}, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            switch (i){
                                                case 0:
                                                    new AlertDialog.Builder(WeatherActivity.this)
                                                            .setTitle("请选择要查询的时刻")
                                                            .setItems(new String[]{"0:00", "1:00", "2:00", "3:00", "4:00", "5:00", "6:00", "7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"}, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    try {
                                                                        new AlertDialog.Builder(WeatherActivity.this)
                                                                                .setTitle(city+list.getJSONObject((24-time+i)%24).getString("sj")+"天气")
                                                                                .setMessage("天气："+tenode[i].tq+"\n温度："+tenode[i].qw+"\n湿度："+tenode[i].sd+"%")
                                                                                .setPositiveButton("确定",null)
                                                                                .show();
                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            })
                                                            .show();
                                                    break;
                                                case 1:
                                                    final EditText edt = new EditText(WeatherActivity.this);
                                                    edt.setMinLines(3);
                                                    new AlertDialog.Builder(WeatherActivity.this)
                                                            .setTitle("请输入要查询的时间段")
                                                            .setIcon(android.R.drawable.ic_dialog_info)
                                                            .setView(edt)
                                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface arg0, int arg1) {
                                                                    String temp = edt.getText().toString();
                                                                    int index = temp.indexOf('-');
                                                                    int front = Integer.parseInt(temp.substring(0,index));
                                                                    int eval = Integer.parseInt(temp.substring(index+1,temp.length()));
                                                                    int sdt = 0,qwt = 0;
                                                                    for(int i = front;i<eval;i++){
                                                                        sdt += Integer.parseInt(tenode[i].sd);
                                                                        qwt += Integer.parseInt(tenode[i].qw);
                                                                    }
                                                                    new AlertDialog.Builder(WeatherActivity.this)
                                                                            .setTitle(city+edt.getText().toString()+"时天气")
                                                                            .setMessage("天气："+tenode[(index+eval)/2].tq+"\n温度："+qwt/(eval-front)+"\n湿度："+sdt/(eval-front)+"%")
                                                                            .show();

                                                                }
                                                            })
                                                            .setNegativeButton("取消", null)
                                                            .show();
                                                    break;
                                                    default:
                                                        break;
                                            }
                                        }
                                    })
                                    .setPositiveButton("确定", null)
                                    .show();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (ProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Looper.loop();
                    }
                }).start();

            }
        });

    }
    class Node{
        String tq;
        String qw;
        String sd;
    }
    public int getTimefromJson(String string){
        String str = new String(string.substring(11,13));
        int a = 0;
        try {
            a = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return a;
    }

    public void requestWeather(final String weatherId){
        //接收天气信息更新
        mWeatherId=weatherId;
        String weatherUrl="http://guolin.tech/api/weather?cityid="+mWeatherId+"&key=fb0e22d7b17f4bd0947c2e0c0045093d";
        //Log.v("d",mWeatherId);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取网络天气信息失败!",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                try {
                    CityName = weather.basic.cityName;
                }
                catch (NullPointerException e){
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败!",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    public void showWeatherInfo(Weather weather){
        //显示天气信息
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"℃";
        String weatherInfo=weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime+"更新");
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast: weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText=(TextView)view.findViewById(R.id.date_text);
            TextView infoText=(TextView)view.findViewById(R.id.info_text);
            TextView maxText=(TextView)view.findViewById(R.id.max_text);
            TextView minText=(TextView)view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.max);
            forecastLayout.addView(view);
        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort="舒适度："+weather.suggestion.comfort.info;
        String carWash="洗车指数："+weather.suggestion.carWash.info;
        String sport="运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    private void loadBingPic(){
        //获取图片
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
