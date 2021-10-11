package com.zhou.consumer;

import com.google.protobuf.*;
import com.zhou.RpcMetaProto;
import com.zhou.util.ZkClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

public class RpcConsumer implements RpcChannel {

    private static final String ZK_SERVER = "zookeeper";
    private String zkServer;

    public RpcConsumer(String file){
        Properties pro = new Properties();
        try{
            pro.load(RpcConsumer.class.getClassLoader().getResourceAsStream(file));
            this.zkServer = pro.getProperty(ZK_SERVER);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /*
    * stub代理对象，需要接收一个实现了RpcChannel的对象，当用stub调用任意rpc方法的时候，全部都调用了当前这个RpcChannel的callMethod方法
    * 不管调用哪个最终调用的都是这个callMethod方法
    * */
    @Override
    public void callMethod(Descriptors.MethodDescriptor methodDescriptor, RpcController rpcController, Message message, Message message1, RpcCallback<Message> rpcCallback) {
        Descriptors.ServiceDescriptor sd = methodDescriptor.getService();
        String serviceName = sd.getName();
        String methodName = methodDescriptor.getName();


        //在zookeeper上查询serviceName-mothodName在哪个主机上ip和port

        String ip = "";
        int port = 0;
        ZkClientUtils zk = new ZkClientUtils(zkServer);

        //你服务不是在zookeeper上注册了吗，我通过服务对象名和服务方法名不就找到节点读取到节点里面的ip这些东西了
        String path = "/" + serviceName + "/" + methodName;
        String hostStr = zk.readData(path);
        zk.close();
        if(hostStr == null){
            rpcController.setFailed("read path:" + path + " data from zk is failed!");
            rpcCallback.run(message1);
            return;
        }else{
            String[] host = hostStr.split(":");
            ip = host[0];
            port = Integer.parseInt(host[1]);
        }

        //打包参数，递交网络发送
        //rpc调用参数格式：header_size + service_name + method_name + args
        //这个是框架双方规定好，你怎么发，我就怎么收

        //序列化头部信息
        RpcMetaProto.RpcMeta.Builder meta_builder = RpcMetaProto.RpcMeta.newBuilder();
        meta_builder.setServiceName(serviceName);
        meta_builder.setMethodName(methodName);
        byte[] metabuf = meta_builder.build().toByteArray();

        //参数
        byte[] argbuf = message.toByteArray();

        //组织rpc参数信息
        ByteBuf buf = Unpooled.buffer(4 + metabuf.length + argbuf.length);
        buf.writeInt(metabuf.length);
        buf.writeBytes(metabuf);
        buf.writeBytes(argbuf);

        //待发送的数据
        byte[] sendbuf = buf.array();

        //通过网络发送rpc调用请求信息
        Socket client = null;
        OutputStream out = null;
        InputStream in = null;

        try{
            client = new Socket();
            //之前是知道可以把ip和port写死
            //你要知道服务所在的主机才能创建连接发送数据，那怎么知道ip和port，通过服务对象和服务方法上zookeeper上面拿呀
            client.connect(new InetSocketAddress(ip,port));
            out = client.getOutputStream();
            in = client.getInputStream();

            //发送数据
            out.write(sendbuf);//写到TCP发送缓存区
            out.flush();//刷新一下发送出去

            //wait等待rpc调用响应
            ByteArrayOutputStream recvbuf = new ByteArrayOutputStream();
            byte[] rbuf = new byte[1024];
            int size = in.read(rbuf);

            //这里的size有可能是0，因为rpcprovider封装response响应参数的时候，如果响应参数的成员变量的值都是默认值，实际上rpcprovider递给response就是一个空数据
            if(size > 0){
                recvbuf.write(rbuf,0,size);
                //上报返回结果
                rpcCallback.run(message1.getParserForType().parseFrom(recvbuf.toByteArray()));
            }else{
                //这不都是调用回调函数，上报返回结果
                rpcCallback.run(message1);
            }
        }catch (IOException e){
            //这不都是调用回调函数，上报返回结果
            rpcController.setFailed("server connect error,check server!");
            rpcCallback.run(message1);
        }finally {

            try {
                if(out != null){
                    out.close();
                }

                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                if(client != null){
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
