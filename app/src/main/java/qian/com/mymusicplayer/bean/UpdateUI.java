package qian.com.mymusicplayer.bean;

/**
 * Created by Administrator on 2018/5/24.
 */

public class UpdateUI {
    Object data;//携带数据
    int flag;//标志

    public UpdateUI(Object data,int flag) {
        this.data=data;
        this.flag=flag;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
}
