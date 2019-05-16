package qian.com.mymusicplayer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import qian.com.mymusicplayer.bean.UpdateUI;
import qian.com.mymusicplayer.receiver.MusicReceiver;
import qian.com.mymusicplayer.utils.AppUtils;
import qian.com.mymusicplayer.utils.TiUtils;


/**
 * 五步走
 * 【1】 绑定服务 ，并获取中间人对象（IBinder）
 * 【2】 通过中间人进行相关操作
 *      包括Notification（图标、下一次事件等）更新；Activity界面的更新
 * 【3】定时更新播放进度、监听准备成功以及播放完成后的操作
 *【4】动态注册广播，通知栏触发——PendingIntent
 *【5】根据广播内容，更新状态等
 *
 *【6】释放资源
 *  解除绑定、清除通知栏信息；注销广播接收者
 *
 * 注意
 *      清单文件中，添加网络权限，以及声明service
 *      同时设置android:launchMode="singleTask"，避免通知栏点击创建多个activity
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static Button button;
    private static Button pre;
    private static Button next;
    private static Button open;
    private static Button close;
    private static Button quit;
    static final   String TAG="音频";
    String url="https://mbrbimg.oss-cn-shanghai.aliyuncs.com/picturebook/audio/35_591d9f4cb39e2_08o.mp3";

    public static  MusicService.MyBinder myBinder;//中间人对象
    private MyConn myConn=new MyConn();

    private static int duration;//音频总长度
    private static int currentPosition;//当前进度

    private static int status; //0初始状态，1暂停，2播放，3播放完成
    private static SeekBar seekbar;
    private static TextView tv_current_time;
    private static TextView tv_time;
    public static Notification notification;
    private static NotificationManager notificationManager;
    private static Context mContext;
    private MusicReceiver receive;

    private TextView sp;
    private ListView listView;
    public static TextView name;
    static String PALYER_TAG;
    private int index=0;
    String music[]={"We_are_the_brave","Honor","我从工大走过（谢春花）","Carve_Our_Names"};
    public static final String PREFERENCE_NAME = "SaveSetting";
//    public static int MODE = Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE;
    boolean isWorked=false;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("00000000000");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);
        mContext = this;
        PALYER_TAG= AppUtils.getPackageName(this);

        initView();


        intent=new Intent(this,MusicService.class);
         intent.putExtra("url",url);

        //【1】绑定服务,并在MyConn中获取中间人对象（IBinder）


//判断权限够不够，不够就给
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        } else {
            //够了就设置路径等，准备播放

            bindService(intent, myConn,BIND_AUTO_CREATE);
            isWorked=true;
            loadSharedPreferences();
            saveSharedPreferences(0);
        }

        //【4】动态注册广播（具体操作由通知栏触发---PendingIntent）
        receive = new MusicReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PALYER_TAG);
        registerReceiver(receive, filter);

    }

    //获取到权限回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bindService(intent, myConn,BIND_AUTO_CREATE);
                } else {
                    Toast.makeText(this, "权限不够获取不到音乐，程序将退出", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }
    /**
     * 初始化控件
     */
    private void initView() {
        button = (Button) findViewById(R.id.btn);
        button.setOnClickListener(this);
        pre=(Button)findViewById(R.id.pre);
        next=(Button)findViewById(R.id.next);
        pre.setOnClickListener(this);
        next.setOnClickListener(this);
        sp=(TextView)findViewById(R.id.sp);
        open=(Button)findViewById(R.id.start);
        close=(Button)findViewById(R.id.close);
        open.setOnClickListener(this);
        close.setOnClickListener(this);
        quit=(Button)findViewById(R.id.quit);
        quit.setOnClickListener(this);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myBinder.seekToPosition(seekBar.getProgress());
                tv_current_time.setText(TiUtils.getTime(seekBar.getProgress()/1000));
            }
        });

        tv_current_time = (TextView) findViewById(R.id.tv_current_time);
        tv_time = (TextView) findViewById(R.id.tv_time);
        name=(TextView)findViewById(R.id.musicText);

        listView=(ListView)findViewById(R.id.list);
        listView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,getData()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position==index)
                    Toast.makeText(getApplicationContext(),"您选中的歌曲正在播放~",Toast.LENGTH_LONG).show();
                else{

                    index=position;
                    name.setText(music[index]);
                    myBinder.change();
                        status=2;

                }
               // Toast.makeText(getApplicationContext(),"您选中"+position,Toast.LENGTH_LONG).show();
            }
        });


    }

    List<String> getData(){
        List<String> data=new ArrayList<String>();
        data.add(music[0]);
        data.add(music[1]);
        data.add(music[2]);
        data.add(music[3]);

        return data;
    }
    /**
     * 初始化通知栏
     */
    private void initNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
//        notification = new Notification();

//        notification.icon = R.mipmap.ic_launcher;//图标
        RemoteViews contentView = new RemoteViews(getPackageName(),
                R.layout.notification_control);
//        notification.contentView = contentView;

        contentView.setImageViewResource(R.id.iv_pic,R.drawable.pic);//图片展示

        contentView.setImageViewResource(R.id.iv_play,R.drawable.ting);//button显示为正在播放
        contentView.setTextViewText(R.id.title,"音乐播放器正在运行");

        Intent intentPause = new Intent(PALYER_TAG);
        intentPause.putExtra("status","pause");
        PendingIntent pIntentPause = PendingIntent.getBroadcast(this, 2, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.iv_play, pIntentPause);

        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mBuilder.setContent(contentView)
                .setSmallIcon(R.mipmap.ic_launcher)
        .setContentIntent(intent);//点击事件

        notification=mBuilder.build();
        notification.flags = notification.FLAG_NO_CLEAR;//设置通知点击或滑动时不被清除


        notificationManager.notify(PALYER_TAG,111, notification);//开启通知


    }

    /**
     * 更新状态栏
     * @param  type  图标样式：1是正在播放状态，2是停止状态； 3播放完成
     */
  public static   void updateNotification(int type){

      //【5】更新操作
      if (type==1){//播放中
          status =2;
          notification.contentView.setImageViewResource(R.id.iv_play,R.drawable.ting);
          Intent intentPlay = new Intent(PALYER_TAG);//下一次意图，并设置action标记为"play"，用于接收广播时过滤意图信息
          intentPlay.putExtra("status","pause");
          PendingIntent pIntentPlay = PendingIntent.getBroadcast(mContext, 2, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);
          notification.contentView.setOnClickPendingIntent(R.id.iv_play, pIntentPlay);//为控件注册事件

          button.setText("暂停");

      }else {//暂停或者播放完成

          notification.contentView.setImageViewResource(R.id.iv_play,R.drawable.play);

          Intent intentPause = new Intent(PALYER_TAG);

          if (type==2){//暂停
              status=1;
              button.setText("继续");
              intentPause.putExtra("status","continue");
          }else{//3播放完成
              button.setText("重新开始");
              intentPause.putExtra("status","replay");//下一步
          }



            PendingIntent pIntentPause = PendingIntent.getBroadcast(mContext, 2, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);
          notification.contentView.setOnClickPendingIntent(R.id.iv_play, pIntentPause);


      }


      notificationManager.notify(PALYER_TAG,111, notification);//开启通知

    }




    @Override
    public void onClick(View v) {
      switch (v.getId()){
          case R.id.btn:
              //【2】 通过中间人进行相关操作

              Log.d("当前状态","status = "+status);
              switch (status){
                  case 0://初始状态
                      //播放
                      myBinder.play();
                      status=2;
                      button.setText("暂停");

                      //初始化通知栏
                      initNotification();
                      break;

                  case 1://暂停
                      //继续播放
                      myBinder.moveon();
                      status=2;
                      button.setText("暂停");
                      updateNotification(1);
                      break;

                  case 2://播放中
                      //暂停
                      myBinder.pause();
                      status=1;
                      button.setText("继续播放");
                     updateNotification(2);
                      break;

                  case 3://播放完成
                      //重新开始
                      myBinder.rePlay();
                      status=2;
                      button.setText("暂停");
                 //     updateNotification(1);
                      break;
              }
              break;
          case R.id.pre:
                status=2;
                if(index==0)
                    index=3;
                else
                    index--;
                name.setText(music[index]);
                myBinder.change();
              break;
          case R.id.next:
              status=2;
              if(index==3)
                  index=0;
              else
                  index++;
              name.setText(music[index]);
              myBinder.change();
              break;
          case R.id.start:
              Log.d(TAG, "onClick: 开启服务");
              if(isWorked){
                  Toast.makeText(getApplicationContext(),"服务已开启",Toast.LENGTH_LONG).show();
              }
              else{
                  EventBus.getDefault().register(this);
                  mContext = this;
                  PALYER_TAG= AppUtils.getPackageName(this);
                  myConn=new MyConn();
                  intent=new Intent(this,MusicService.class);
                  bindService(intent, myConn,BIND_AUTO_CREATE);
                  isWorked=true;
                  status=2;
                  index=0;
                  name.setText(music[0]);
                  button.setText("暂停");
                  myBinder.play();
                  //初始化通知栏
                  initNotification();
                  Toast.makeText(getApplicationContext(),"服务已开启",Toast.LENGTH_LONG).show();
                  loadSharedPreferences();

                  receive = new MusicReceiver();
                  IntentFilter filter = new IntentFilter();
                  filter.addAction(PALYER_TAG);
                  registerReceiver(receive, filter);

              }
              break;
          case R.id.close:
              if(!isWorked){
                  Toast.makeText(getApplicationContext(),"服务已关闭",Toast.LENGTH_LONG).show();
              }
              else
              {
                  Log.d(TAG, "onClick: 关闭服务");
                  Toast.makeText(getApplicationContext(),"服务已关闭",Toast.LENGTH_LONG).show();
                  saveSharedPreferences(1);
                  button.setText("开始播放");
                  unbindService(myConn);
                  isWorked=false;
                  myConn=null;

                  if (notificationManager!=null)
                      notificationManager.cancel(PALYER_TAG,111);//通过tag和id,清除通知栏信息

                  EventBus.getDefault().unregister(this);
                  unregisterReceiver(receive);
              }


              break;
          case R.id.quit:
              this.onDestroy();
              this.finish();
      }

    }



    public class MyConn implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获取中间人对象
            myBinder = (MusicService.MyBinder) service;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)  //3.0之后，需要加注解
   public void onEventMainThread(UpdateUI updateUI){
       int flag = updateUI.getFlag();

        Log.d("音频状态",status+" ---- "+flag);
        //【3】设置进度
       if (flag==1){//准备完成，获取音频长度
           duration = (int) updateUI.getData();
           Log.d(TAG,"总长度"+duration);
           //设置总长度
           seekbar.setMax(duration);
           tv_time.setText(TiUtils.getTime(duration/1000));



       }else  if (flag==2){//播放完成
           Log.d(TAG,"播放完成～");

           //status=3;//已完成
            //重置信息
           seekbar.setProgress(0);
           tv_current_time.setText("00:00");
           button.setText("重新播放");

           updateNotification(3);

       }else if (flag==3){//更新进度
           if (status==3)//避免播放完成通知与线程通知冲突
               return;
           currentPosition = (int) updateUI.getData();
           Log.d(TAG,"当前进度"+currentPosition);

           //设置进度
           tv_current_time.setText(TiUtils.getTime(currentPosition/1000));
           seekbar.setProgress(currentPosition);
       }
   }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("销毁","onDestroy()");

        System.out.println("11111111111");
        saveSharedPreferences(1);
        //【6】解除绑定并注销
        unbindService(myConn);
        myConn=null;

        if (notificationManager!=null)
            notificationManager.cancel(PALYER_TAG,111);//通过tag和id,清除通知栏信息

        EventBus.getDefault().unregister(this);
        unregisterReceiver(receive);


    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();


//        finish();

    }
    private void loadSharedPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);


        String startTime= sharedPreferences.getString("StartTime","2019-01-01 00:00:00");
        String stopTime= sharedPreferences.getString("StopTime","2019-01-01 00:00:00");
        String musicName=sharedPreferences.getString("Music","We_are_the_brave");
        String currPosition=sharedPreferences.getString("CurrentPosition","00:00");

        String s="startTime:"+startTime+"\n"+
                "stopTime:"+stopTime+"\n"+
                "musicName:"+musicName+"\n"+
                "Position:"+currPosition;
        //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
        sp.setText(s);
    }

    private void saveSharedPreferences(int code){

        Date t = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=df.format(t);
        if(code==0){
            //表示打开
            SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("StartTime", time);
            editor.commit();
        }
        else{
            //表示关闭
            SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_APPEND);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("StopTime",time);
            editor.putString("Music",music[index]);
            editor.putString("CurrentPosition",TiUtils.getTime(currentPosition/1000));
            editor.commit();
        }

    }

}
