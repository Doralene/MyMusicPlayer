package qian.com.mymusicplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import qian.com.mymusicplayer.bean.UpdateUI;

import static qian.com.mymusicplayer.MainActivity.TAG;

/**
 * Created by Administrator on 2018/5/22.
 * 后台播放
 *      基础版：四步走
 */

public class MusicService extends Service {

    private String url;
    private MediaPlayer mediaPlayer;
    boolean isStopThread;//是否停止线程

    //标记当前歌曲的序号
    private int i = 0;
    //歌曲路径
    private String[] musicPath = new String[]{
            Environment.getExternalStorageDirectory() + "/Sounds/We_Are_The_Brave.mp3",
            Environment.getExternalStorageDirectory() + "/Sounds/Honor.mp3",
            Environment.getExternalStorageDirectory() + "/Sounds/cave.mp3",
            Environment.getExternalStorageDirectory() + "/Sounds/Carve_Our_Names.mp3"

    };
    String music[]={"We_are_the_brave","Honor","我从工大走过（谢春花）","Carve_Our_Names"};
//    public MusicService() {
//        iniMediaPlayerFile(i);
//    }
    @Override
    public void onCreate() {
        super.onCreate();


        Log.d("音频","onCreate()");

        //【1】创建播放器
        mediaPlayer = new MediaPlayer();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("音频","onBind()");
        isStopThread=false;

        i=0;

        //【2】获取播放地址
        url = intent.getStringExtra("url");

        //【3】初始化播放器（含事件处理）
        try {
            mediaPlayer.reset();
//            mediaPlayer.setDataSource(url);
            mediaPlayer.setDataSource(musicPath[i]);
            //让MediaPlayer对象准备
            mediaPlayer.prepare();

            mediaPlayer.setLooping(true);//是否循环播放

           // mediaPlayer.prepareAsync();//网络视频，异步
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                     int duration=mp.getDuration();

//                    Message message=Message.obtain();
//                    message.what=1;
//                    message.arg1=duration;
//                    MainActivity.handler.sendMessage(message);

                    EventBus.getDefault().post(new UpdateUI(duration,1));
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

//                    MainActivity.handler.sendEmptyMessage(2);
                    EventBus.getDefault().post(new UpdateUI("",2));
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }


        return new MyBinder();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("音频","onUnbind()");

        //【4】关闭线程并释放资源
        isStopThread=true;//避免线程仍在走

        if (mediaPlayer.isPlaying())
            mediaPlayer.release();
        mediaPlayer=null;

        return super.onUnbind(intent);
    }

    public class MyBinder extends Binder implements MyOperation {

        @Override
        public void play() {//播放

            Log.d("执行方法","play()");
            mediaPlayer.start();
            updateSeekBar();
        }

        @Override
        public void pause() {//暂停
            Log.d("执行方法","pause()");
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }

        }

        @Override
        public void moveon() {//继续播放
            Log.d("执行方法","moveon()");
            mediaPlayer.start();

        }


        @Override
        public void rePlay() {//重新开始
            Log.d("执行方法","rePlay()");

            mediaPlayer.start();

        }

        public void change(){
            Log.d("执行方法","change()");
            mediaPlayer.reset();
            String name= (String) MainActivity.name.getText();
            switch (name){
                case "We_are_the_brave":
                    i=0;
                    break;
                case "Honor":
                    i=1;
                    break;
                case "我从工大走过（谢春花）":
                    i=2;
                    break;
                case "Carve_Our_Names":
                    i=3;
                    break;
            }
            try {
                mediaPlayer.setDataSource(musicPath[i]);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);//是否循环播放
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        int duration=mp.getDuration();

//                    Message message=Message.obtain();
//                    message.what=1;
//                    message.arg1=duration;
//                    MainActivity.handler.sendMessage(message);

                        EventBus.getDefault().post(new UpdateUI(duration,1));
                    }
                });
//                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                    @Override
//                    public void onCompletion(MediaPlayer mp) {
//
////                    MainActivity.handler.sendEmptyMessage(2);
//                        EventBus.getDefault().post(new UpdateUI("",2));
//                    }
//                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            isStopThread=true;//避免线程仍在走
            isStopThread=false;
            updateSeekBar();
        }



        /**
         * 更新进度条
         */
        private void updateSeekBar() {
            //开启线程发送数据
            new Thread() {
                @Override
                public void run() {

                    while(!isStopThread){

                        try {

                            int currentPosition = mediaPlayer.getCurrentPosition();

                            //方法3，使用EventBus实现

                            EventBus.getDefault().post(new UpdateUI(currentPosition,3));

                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    }

            }.start();
        }



        @Override
        public long getCurrentPosition() {
            return 0;
        }

        /**
         * 跳转到指定位置
         * @param position
         */
        @Override
        public void seekToPosition(int position) {
            mediaPlayer.seekTo(position);
        }

    }


}
