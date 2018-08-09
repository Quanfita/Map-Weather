package com.example.baidu.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.BaiduMap.*;
import com.baidu.mapapi.map.*;

import com.example.baidu.map.service.AutoUpdateService;
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

public class MainActivity extends ActionBarActivity {
    private MapView myMapView = null;//地图控件
    private BaiduMap myBaiduMap;//百度地图对象
    private LocationClient mylocationClient;//定位服务客户对象
    private MylocationListener mylistener;//重写的监听类
    private Context context;

    private double myLatitude;//纬度，用于存储自己所在位置的纬度
    private double myLongitude;//经度，用于存储自己所在位置的经度
    private float myCurrentX;

    private BitmapDescriptor myIconLocation1;//图标1，当前位置的箭头图标

    private MyOrientationListener myOrientationListener;//方向感应器类对象

    private MyLocationConfiguration.LocationMode locationMode;//定位图层显示方式

    private LinearLayout myLinearLayout2; //地址搜索区域2
    private BitmapDescriptor bitmap;
    String text, name, temp;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x123) {
                Toast.makeText(MainActivity.this, name + ":" + text + "," + temp + "°C", Toast.LENGTH_LONG).show();
                //textView.setText(name+":"+text+","+temp+"°C");
                //tv.setText(name+":"+text+","+temp+"°C");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        this.context = this;
        initView();
        initLocation();

    }

    private void initView() {
        myMapView = (MapView) findViewById(R.id.baiduMapView);

        myBaiduMap = myMapView.getMap();
        //根据给定增量缩放地图级别
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(18.0f);
        myBaiduMap.setMapStatus(msu);
        UiSettings myUiSettings = myBaiduMap.getUiSettings();
        //实例化UiSettings类对象
        myUiSettings.setCompassEnabled(true);
    }

    private void initLocation() {
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mylocationClient = new LocationClient(this);
        mylistener = new MylocationListener();

        //注册监听器
        mylocationClient.registerLocationListener(mylistener);
        //配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption mOption = new LocationClientOption();
        //设置坐标类型
        mOption.setCoorType("bd09ll");
        //设置是否需要地址信息，默认为无地址
        mOption.setIsNeedAddress(true);
        //设置是否打开gps进行定位
        mOption.setOpenGps(true);
        //设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效
        int span = 1000;
        mOption.setScanSpan(span);
        //设置 LocationClientOption
        mylocationClient.setLocOption(mOption);

        //初始化图标,BitmapDescriptorFactory是bitmap 描述信息工厂类.
        myIconLocation1 = BitmapDescriptorFactory.fromResource(R.drawable.location_marker);
//        myIconLocation2 = BitmapDescriptorFactory.fromResource(R.drawable.icon_target);

        //配置定位图层显示方式,三个参数的构造器
        MyLocationConfiguration configuration
                = new MyLocationConfiguration(locationMode, true, myIconLocation1);
        //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
        myBaiduMap.setMyLocationConfigeration(configuration);

        myOrientationListener = new MyOrientationListener(context);
        //通过接口回调来实现实时方向的改变
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                myCurrentX = x;
            }
        });


        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_mark);
        myBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                // TODO Auto-generated method stub
                final String POIName = mapPoi.getName();//POI点名称
                final LatLng POIPosition = mapPoi.getPosition();//POI点坐标

                final Handler handle = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 0x111) {
                            TextView tvw = new TextView(getApplicationContext());
                            //textView.setText(name+":"+text+","+temp+"°C");
                            tvw.setText(POIName + "\n" + text + "," + temp + "°C");
                            tvw.setBackgroundResource(R.drawable.text_view);
                            InfoWindow mInfoWindow = new InfoWindow(tvw, POIPosition, -47);
                            myBaiduMap.showInfoWindow(mInfoWindow);
                        }
                    }
                };
                //添加图层显示POI点
                myBaiduMap.clear();
                myBaiduMap.addOverlay(
                        new MarkerOptions()
                                .position(POIPosition)                                     //坐标位置
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_mark))
                                .title(POIName)                                         //标题

                );

                //将该POI点设置为地图中心
                myBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(POIPosition));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //通过API获取天气信息
                        try {
                            URL url = new URL(String.format("https://api.seniverse.com/v3/weather/now.json?key=82uave76xyhey7qm&location=%s:%s&language=zh-Hans&unit=c", POIPosition.latitude, POIPosition.longitude));
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
                            Log.v("d", json.toString());
                            JSONArray result = json.getJSONArray("results");
                            JSONObject location = result.getJSONObject(0).getJSONObject("location");

                            name = location.getString("name");//地区名称
                            location = result.getJSONObject(0).getJSONObject("now");
                            text = location.getString("text");//天气信息
                            temp = location.getString("temperature");//温度

                            Log.v("d", name);
                            Log.v("d", text);
                            Log.v("d", temp);

                            //tv.setText(name+' '+text+','+temp);
                            //handler.sendEmptyMessage(0x123);
                            handle.sendEmptyMessage(0x111);
                        } catch (
                                MalformedURLException e)

                        {
                            e.printStackTrace();
                        } catch (
                                IOException e)

                        {
                            e.printStackTrace();
                        } catch (
                                JSONException e)

                        {
                            e.printStackTrace();
                        }
                    }
                }).start();

                return true;

            }

            //此方法就是点击地图监听
            @Override
            public void onMapClick(LatLng latLng) {
                //获取经纬度
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;
                System.out.println("latitude=" + latitude + ",longitude=" + longitude);

            }
        });

    }

    /*
     *创建菜单操作
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
             *第一个功能，返回自己所在的位置，箭头表示
             */
            case R.id.menu_item_mylocation://返回当前位置
                //textView.setVisibility(View.INVISIBLE);
                getLocationByLL(myLatitude, myLongitude);
                break;


            case R.id.menu_item_llsearch:
                Intent intent = new Intent(this, WeatherActivity.class);
                intent.putExtra("string",name);
                startActivity(intent);
                //finish();
                break;
            /*
             *第三个功能，根据地址名前往所在的位置
             */

            case R.id.menu_item_sitesearch://根据地址搜索
                myLinearLayout2 = (LinearLayout) findViewById(R.id.linearLayout2);
                //显示地址搜索区域2
                myLinearLayout2.setVisibility(View.VISIBLE);
                final EditText myEditText_site = (EditText) findViewById(R.id.editText_site);
                Button button_site = (Button) findViewById(R.id.button_sitesearch);

                //textView.setVisibility(View.VISIBLE);
                button_site.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String site_str = myEditText_site.getText().toString();

                        //隐藏前面地址输入区域
                        myLinearLayout2.setVisibility(View.GONE);
                        //隐藏输入法键盘
                        InputMethodManager imm = (InputMethodManager) getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final AddressToLatitudeLongitude at = new AddressToLatitudeLongitude(site_str);
                                at.getLatAndLngByAddress();
                                getLocationByLL(at.getLatitude(), at.getLongitude());
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Looper.prepare();
                                        try {
                                            URL url = new URL(String.format("http://api.yytianqi.com/alarminfo?city=%s,%s&key=t9r8sd7gkt8fu1rj", at.getLatitude(), at.getLongitude()));
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
                                            Log.v("d", json.toString());
                                            JSONObject data = json.getJSONObject("data");
                                            data.getString("cityName");
                                            JSONArray list = data.getJSONArray("list");
                                            for (int i = 0; i < list.length(); i++) {
                                                String type = list.getJSONObject(i).getString("type");
                                                String level = list.getJSONObject(i).getString("level");
                                                String content = list.getJSONObject(i).getString("content");
                                                new AlertDialog.Builder(MainActivity.this)
                                                        .setTitle(type + level + "预警！")
                                                        .setMessage(content)
                                                        .setPositiveButton("确定", null)
                                                        .show();
                                                Log.v("d", type);
                                                Log.v("d", level);
                                                Log.v("d", content);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        Looper.loop();
                                    }
                                }).start();

                                Looper.prepare();
                                try {
                                    URL url = new URL(String.format("https://api.seniverse.com/v3/weather/now.json?key=82uave76xyhey7qm&location=%s:%s&language=zh-Hans&unit=c", at.getLatitude(), at.getLongitude()));
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
                                    Log.v("d", json.toString());
                                    JSONArray result = json.getJSONArray("results");
                                    JSONObject location = result.getJSONObject(0).getJSONObject("location");

                                    name = location.getString("name");
                                    location = result.getJSONObject(0).getJSONObject("now");
                                    text = location.getString("text");
                                    temp = location.getString("temperature");

                                    Log.v("d", name);
                                    Log.v("d", text);
                                    Log.v("d", temp);

                                    handler.sendEmptyMessage(0x123);
                                } catch (
                                        MalformedURLException e)

                                {
                                    e.printStackTrace();
                                } catch (
                                        IOException e)

                                {
                                    e.printStackTrace();
                                } catch (
                                        JSONException e)

                                {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        Looper.loop();

                    }
                });
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    /*
     *根据经纬度前往
     */
    public void getLocationByLL(double la, double lg) {
        //地理坐标的数据结构
        LatLng latLng = new LatLng(la, lg);
        //描述地图状态将要发生的变化,通过当前经纬度来使地图显示到该位置
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        myBaiduMap.setMapStatus(msu);
    }

    /*
     *定位请求回调接口
     */
    public class MylocationListener implements BDLocationListener {
        //定位请求回调接口
        private boolean isFirstIn = true;

        //定位请求回调函数,这里面会得到定位信息
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            //BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
            //MyLocationData 定位数据,定位数据建造器
            /*
             * 可以通过BDLocation配置如下参数
             * 1.accuracy 定位精度
             * 2.latitude 百度纬度坐标
             * 3.longitude 百度经度坐标
             * 4.satellitesNum GPS定位时卫星数目 getSatelliteNumber() gps定位结果时，获取gps锁定用的卫星数
             * 5.speed GPS定位时速度 getSpeed()获取速度，仅gps定位结果时有速度信息，单位公里/小时，默认值0.0f
             * 6.direction GPS定位时方向角度
             * */
            myLatitude = bdLocation.getLatitude();
            myLongitude = bdLocation.getLongitude();

            //打印出当前位置
            Log.i("TAG", "location.getAddrStr()=" + bdLocation.getAddrStr());
            //打印出当前城市
            Log.i("TAG", "location.getCity()=" + bdLocation.getCity());
            MyLocationData data = new MyLocationData.Builder()
                    .direction(myCurrentX)//设定图标方向
                    .accuracy(bdLocation.getRadius())//getRadius 获取定位精度,默认值0.0f
                    .latitude(myLatitude)//百度纬度坐标
                    .longitude(myLongitude)//百度经度坐标
                    .build();
            //设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
            myBaiduMap.setMyLocationData(data);

            //判断是否为第一次定位,是的话需要定位到用户当前位置
            if (isFirstIn) {
                //根据当前所在位置经纬度前往
                getLocationByLL(myLatitude, myLongitude);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        try {

                            URL url = new URL(String.format("http://api.yytianqi.com/alarminfo?city=%s,%s&key=n9qanosdrmg0f37d", myLatitude, myLongitude));
                            Log.v("d", url.toString());
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
                            Log.v("d", json.toString());
                            JSONObject data = json.getJSONObject("data");
                            data.getString("cityName");
                            JSONArray list = data.getJSONArray("list");
                            for (int i = 0; i < list.length(); i++) {
                                String type = list.getJSONObject(i).getString("type");
                                String level = list.getJSONObject(i).getString("level");
                                String content = list.getJSONObject(i).getString("content");
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(type + level + "预警！")
                                        .setMessage(content)
                                        .setPositiveButton("确定", null)
                                        .show();
                                Log.v("d", type);
                                Log.v("d", level);
                                Log.v("d", content);
                            }
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
                isFirstIn = false;
            }
        }

        private void initLocation() {
            LocationClientOption option = new LocationClientOption();
            //就是这个方法设置为true，才能获取当前的位置信息
            option.setIsNeedAddress(true);
            option.setOpenGps(true);
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
            );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            option.setCoorType("gcj02");//可选，默认gcj02，设置返回的定位结果坐标系
            //int span = 1000;
            //option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mylocationClient.setLocOption(option);
        }
    }

    /*
     *定位服务的生命周期，达到节省
     */
    @Override
    protected void onStart() {
        super.onStart();
        //开启定位，显示位置图标
        myBaiduMap.setMyLocationEnabled(true);
        if (!mylocationClient.isStarted()) {
            mylocationClient.start();
        }
        myOrientationListener.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        myBaiduMap.setMyLocationEnabled(false);
        mylocationClient.stop();
        myOrientationListener.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMapView.onDestroy();
    }
}