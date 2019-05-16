package qian.com.mymusicplayer.utils;

/**
 * Created by Administrator on 2018/5/23.
 *
 * 时间工具
 */

public class TiUtils {

    /**
     * 获取时间字符串
     * @param time   秒
     * @return
     */
    public static String getTime(int time){

        if (time<0)
            return "";

        if (time<60){//小于1min
            if (time<10){//避免单个显示 eg.00:3  >> 00:03
                return "00:0"+time;
            }

            return "00:"+time;
        }

        if (time<60*60){//小于1h
            int min=time/60;
            int sec=time%60;
            StringBuilder sb=new StringBuilder();

            if (min<10){
                sb.append("0"+min);
            }else {
                sb.append(""+min);
            }
            sb.append(":");

            if (sec<10){
                sb.append("0"+sec);
            }else {
                sb.append(""+sec);
            }

            return sb.toString();//time/60+":"+time%60
        }

        if (time<60*60*24){//小于1d
            int h=time/(60*60);
            int min=(time-time/(60*60))/60;
            int sec=time%60;
            StringBuilder sb=new StringBuilder();

            if (h<10){
                sb.append("0"+h);
            }else {
                sb.append(""+h);
            }
            sb.append(":");

            if (min<10){
                sb.append("0"+min);
            }else {
                sb.append(""+min);
            }
            sb.append(":");

            if (sec<10){
                sb.append("0"+sec);
            }else {
                sb.append(""+sec);
            }

            return sb.toString();

//            return  time/(60*60)+":"+(time-time/(60*60))/60+":"+time%60;
        }


        //超过一天
        return "";
    }
}
