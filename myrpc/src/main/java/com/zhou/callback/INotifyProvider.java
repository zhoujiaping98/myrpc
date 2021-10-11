package com.zhou.callback;

public interface INotifyProvider {

    /*
    * 回调操作，RpcServer给RpcProvider上报接收到的rpc服务调用相关信息,然后把rpc调用完成后的数据响应返回
    * 把rpc调用完成后的数据响应返回
    * */
    byte[] notify(String serviceName,String methodName,byte[] args);
}
