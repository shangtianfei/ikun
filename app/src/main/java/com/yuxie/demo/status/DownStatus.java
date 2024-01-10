package com.yuxie.demo.status;

public enum DownStatus {

    LOADING("加载中")
    , NODOWN("已存在，跳过下载")
    , OK("下载成功")
    , ERROR("下载失败");
    String msg;

    DownStatus(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}