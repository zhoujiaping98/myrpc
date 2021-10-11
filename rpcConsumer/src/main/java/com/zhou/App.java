package com.zhou;
import com.zhou.consumer.RpcConsumer;
import com.zhou.controller.NrpcController;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        /*
        *   模拟rpc方法调用者 google grpc
        * */
        //RpcConsumer这个类是自己定义的
        //发送方所有的调用都是由这个stub来完成的
        UserServiceProto.UserServiceRpc.Stub stub = UserServiceProto.UserServiceRpc.newStub(new RpcConsumer("config.properties"));
        UserServiceProto.LoginRequest.Builder login_builder = UserServiceProto.LoginRequest.newBuilder();
        login_builder.setName("zhang san");
        login_builder.setPwd("888888");
        //作用就是可以将错误传达到rpc的调用方，用作当前主机一些状态信息的控制
        NrpcController con = new NrpcController();
        //第二个参数是我要调用方法需要给出的参数，第三个回调是调用完成后接收返回结果
        stub.login(con,login_builder.build(), response -> {
            //这里就是rpc方法调用完成以后的返回值
            if(con.failed()){   //rpc方法没有调用成功
                System.out.println(con.errorText());
            }else{
                System.out.println("receive rpc call response!");
                if(response.getErrno() == 0){//调用正常
                    System.out.println(response.getResult());
                }else{//调用出错
                    System.out.println(response.getErrinfo());
                }
            }
        });
    }
}
