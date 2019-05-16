package qian.com.mymusicplayer.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by Administrator on 2018/6/6.
 */

public class AppUtils {
    /**
     *   包名
     */
    public static String getPackageName(Context context) {
        Log.d("包名",getPackageInfo(context).packageName+"");
        return getPackageInfo(context).packageName;
    }


    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;
        try { PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pi;
    }
}
