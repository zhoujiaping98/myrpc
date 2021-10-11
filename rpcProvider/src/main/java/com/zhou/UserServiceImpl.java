package com.zhou;


import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/*
* 原来是本地服务方法，现在要发布成rpc方法
* 要将远程服务变成本地服务，就要继承自己生成出来的rpc代理类
* */
public class UserServiceImpl extends UserServiceProto.UserServiceRpc{
    public boolean login(String name,String pwd){
        System.out.println("call UserServiceImpl->login");
        System.out.println("name:" + name);
        System.out.println("pwd:"+pwd);
        return true;
    }

    public boolean reg(String name,String pwd,int age,String sex,String phone){
        System.out.println("call UserServiceImpl->reg");
        System.out.println("name:" + name);
        System.out.println("pwd:" + pwd);
        System.out.println("age:" + age);
        System.out.println("sex:" + sex);
        System.out.println("phone:" + phone);

        return true;
    }

    //我是你的代理类我就有你同样的两个方法，然后你继承了你的代理类你就要实现这两个方法
    @Override
    //远程调用什么方法框架就会将字节流转换成需要转化的对象传到request里面就等于你参数有了，你这是provider端，参数都是框架给你的
    //将发来的请反序列化成对象，那这个对象是什么，不就是request
    public void login(RpcController controller, UserServiceProto.LoginRequest request, RpcCallback<UserServiceProto.Response> done) {
        //就是粉色框框的call
        // 1.从request里面读取到远程rpc调用请求的参数，已经通过反序列化好了，直接取就好了
        String name = request.getName();
        String pwd = request.getPwd();
        //就是粉色框框的work
        // 2.根据解析的参数做本地业务
        //代理方法调用本地业务真正实现功能
        boolean result = login(name,pwd);

        //就是粉色框框的return
        // 3.填写方法的响应值
        //返回值不是也需要塞到一个类里面，序列化，对面再反序列化吗
        UserServiceProto.Response.Builder response_builder = UserServiceProto.Response.newBuilder();
        response_builder.setErrno(0);
        response_builder.setErrinfo("");
        response_builder.setResult(result);

        // 4.把response对象给到myrpc框架，由框架负责处理发送rpc调用响应值
        //把对象给到框架，框架负责序列化然后发送出去
        //框架知道干什么，我知道什么时候干
        done.run(response_builder.build());
    }

    //reg的rpc代理方法
    @Override
    public void reg(RpcController controller, UserServiceProto.RegRequest request, RpcCallback<UserServiceProto.Response> done) {

    }
}
