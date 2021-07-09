package com.shencoder.javacv_facedetectdemo;

/**
 * @author ShenBen
 * @date 2021/7/9 15:16
 * @email 714081644@qq.com
 */
public class RequestFaceBean {
    private String cmd;
    private String imagedata;

    public RequestFaceBean(String cmd, String imagedata) {
        this.cmd = cmd;
        this.imagedata = imagedata;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getImagedata() {
        return imagedata;
    }

    public void setImagedata(String imagedata) {
        this.imagedata = imagedata;
    }
}
