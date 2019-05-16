package qian.com.mymusicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import qian.com.mymusicplayer.MainActivity;
import qian.com.mymusicplayer.utils.AppUtils;

import static qian.com.mymusicplayer.MainActivity.myBinder;

/**
 * Created by Administrator on 2018/5/24.
 */

public class MusicReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();//获取action标记，用户区分点击事件
        String status = intent.getStringExtra("status");

        Log.d("音频接收器","action =   "+action+"   status ="+status);
        if (myBinder==null)
            return;

        if (!AppUtils.getPackageName(context).equals(action)){
            return;
        }

        if ("pause".equals(status)) {
            myBinder.pause();
            MainActivity.updateNotification(2);
        } else if ("continue".equals(status)) {
            myBinder.moveon();
            MainActivity.updateNotification(1);

        }else if ("replay".equals(status)) {
            myBinder.rePlay();
            MainActivity.updateNotification(1);

        }
    }
}
