package com.zhou.provider;

import com.zhou.RpcMetaProto;
import com.zhou.callback.INotifyProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RpcServer {

    private INotifyProvider notifyProvider;

    public RpcServer(INotifyProvider notify){
        this.notifyProvider = notify;
    }

    public void start(String ip,int port){
        //创建主事件循环，对应I/O线程，主要用来处理新用户的连接事件
        NioEventLoopGroup mainGroup = new NioEventLoopGroup(1);//1表示线程数量
        //创建worker工作线程事件循环，主要用来处理已连接用户的可读写事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(3);//3表示线程数量
        //线程的数量最好不要超过cpu的核数

        //netty网络服务的启动辅助类，里面可以设置很多东西
        ServerBootstrap b = new ServerBootstrap();
        b.group(mainGroup,workerGroup).channel(NioSctpServerChannel.class)//底层使用Java Nio Selector模型
                .option(ChannelOption.SO_BACKLOG,1024)  //设置TCP参数 1024是TCP三次握手成功后全连接队列的长度，超过的就被舍弃
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new ObjectEncoder());// 设置数据的编码和解码器 网络字节流《-》业务要处理的数据类型
                        channel.pipeline().addLast(new RpcServerChannel());// 设置事件回调处理器 RpcServerChannel类是下面自己定义的
                    }
                });//事件回调，把业务层的代码和网络层代码区分卡

        try{
            //阻塞，开启网络服务
            ChannelFuture f = b.bind(ip, port).sync();
            //关闭网络服务
            f.channel().closeFuture().sync();
        }catch (InterruptedException e){
            e.printStackTrace();
        }finally {
            //关闭资源
            workerGroup.shutdownGracefully();
            mainGroup.shutdownGracefully();
        }
    }

    //继承自netty的ChannelInboundHandlerAdapter，提供回调操作
    private class RpcServerChannel extends ChannelInboundHandlerAdapter{

        //处理接收到的事件
        //netty接收到数据以后就会调用这个方法
        //就是数据发送过来之后是由这个方法来进行处理
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            /*
            * ByteBuf<=netty 类似java nio的ByteBuff
            * request就是远端发送过来的rpc调用请求包含的所有信息参数
            *
            * header_size + UserServiceRpclogin + zhangsan123456(LoginRequest，zhangsan123456可以通过反序列化得到)
            * 发送方就按照这个格式发送数据20 + UserServiceRpclogin + 参数数据 20表示服务对象名和服务方法名加起来的长度
            *
            * UserServiceRpclogin也要通过proto反序列化来得到，所以有一个rpc_meta.proto
            * */
            ByteBuf request = (ByteBuf)msg;

            // 1.先读取头部信息的长度
            int header_size = request.readInt();
            // 2.读取头部信息（服务对象名称和服务方法名称）
            byte[] metabuf = new byte[header_size];
            request.readBytes(metabuf);
            // 3.反序列化生成RpcMeta
            //发送方通过RpcMeta序列化，你再通过RpcMeta反序列化得到服务对象名和服务方法名
            RpcMetaProto.RpcMeta rpcmeta = RpcMetaProto.RpcMeta.parseFrom(metabuf);
            String serviceName = rpcmeta.getServiceName();
            String methodName = rpcmeta.getMethodName();

            // 4.读取rpc方法的参数
            byte[] argbuf = new byte[request.readableBytes()];//request.readableBytes()把剩下的全部读完
            request.readBytes(argbuf);//读到argbuf里去了
            //参数都读到了之后就该去rpcprovider里调用了
            //就是该回调了嘛，我作为网络层知道什么时候接收到数据，rpcprovider知道接收到数据应该怎么做


            // 5.serviceName methodName argbuf
            //服务对象，方法名反序列化了，参数还没有反序列化
            byte[] response = notifyProvider.notify(serviceName,methodName,argbuf);

            // 6.把rpc方法调用的响应response通过网络发给rpc调用方
            ByteBuf buf = Unpooled.buffer(response.length);
            buf.writeBytes(response);
            ChannelFuture f = ctx.writeAndFlush(buf);
            // 7.模拟http响应完成后，直接关闭连接
            if(f.sync().isSuccess()){
                ctx.close();
            }
        }

        //连接异常处理
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
