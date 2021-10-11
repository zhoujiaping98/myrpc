package com.zhou;

import com.zhou.provider.RpcProvider;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        /*
        * 启动一个可以提供rpc远程方法调用的server
        * 1. 需要一个rpcprovider（myrpc提供的）对象
        * 2. 向rpcprovider上面注册一个rpc方法 UserServiceImpl.Login  UserServiceImpl.Reg，你不注册框架怎么就知道要调用哪个方法
        * 3. 启动rpcprovider这个server站点 阻塞等待远程rpc方法调用请求
        * */

        //我有方法没有用啊，我得告诉框架我有这个方法，框架接收远程rpc请求后知道哦有这个方法，才会把数据反序列化以后
        //扔给我，我才能使用框架反序列化后的对象做本地业务，合成返回值再扔给框架，框架将响应发送回去
        //框架通过配置文件读取服务所在的ip地址和端口
        RpcProvider.Builder builder = RpcProvider.newBuilder();
        //框架知道你有这个方法了之后才会将字节流反序列化之后扔给你
        //配置文件里设置ip和端口好像是给rpcprovider设置的，框架通过配置文件读取它所在的主机的IP地址
        RpcProvider provider = builder.build("config.properties");


        //这一步就是像框架是发布服务对象（UserServiceImpl）和服务方法（login，reg）
        provider.registerRpcService(new UserServiceImpl());

        //启动rpc server站点，阻塞等到远程rpc调用请求
        provider.start();
    }
}
