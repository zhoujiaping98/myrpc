package com.zhou.provider;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.zhou.callback.INotifyProvider;
import com.zhou.util.ZkClientUtils;

import javax.management.Descriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/*
* rpc方法发布的站点，只需要一个站点就可以发布当前主机上所有的rpc方法，用单例模式设计RpcProvider
* */

//其实很好理解，netty接收到数据要向rpcprovider上报，就是netty知道什么时候干，你知道干什么，那我知道什么时候干了我就得调用你干什么的方法
// 但是这个干什么的方法不能直接定义在你里面，不然就是下层依赖上层了
// 所以其实就是把下层需要调用上层的方法不用写在上层里，写在接口里INotifyProvider，上层去实现这个接口，就这么简单
public class RpcProvider implements INotifyProvider {
    private static final String SERVER_IP = "ip";
    private static final String SERVER_PORT = "port";
    private static final String ZK_SERVER = "zookeeper";
    private String serverIp;
    private int serverPort;
    private String zkServer;
    private ThreadLocal<byte[]> responsebufLocal;

    //notify是在多线程环境中被调用的
    //接收RpServer网络模块上报的rpc调用相关信息参数，执行具体的rpc方法调用，把rpc方法调用完成以后的响应值返回
    //调用什么方法，参数都知道了，你就该干事情了
    @Override
    public byte[] notify(String serviceName, String methodName, byte[] args) {
        ServiceInfo si = serviceMap.get(serviceName);
        Service service = si.service;//获取服务对象
        Descriptors.MethodDescriptor method = si.methodMap.get(methodName);//获取服务方法
        //从args参数反序列化出method的方法参数 LoginRequet RegRequest
        //实际上就是创建了LoginRequet对象
        //获取login方法请求的原始类型service.getRequestPrototype(method)就是获取LoginRequet
        Message request = service.getRequestPrototype(method).toBuilder().build();
        try{
            request = request.getParserForType().parseFrom(args);
        }catch(InvalidProtocolBufferException e){
            e.printStackTrace();
        }

        //rpc对象：service
        //rpc对象的方法：method
        //rpc方法的参数：request
        service.callMethod(method,null,request,response -> {
            //你接受本地处理完之后的返回结果，再向netty返回，netty再向发送方返回
            //dorun就是执行这个
            responsebufLocal.set(response.toByteArray());
            //responsebufLocal只有一个，但是notify是在多线程环境中被调用的
        });
        return responsebufLocal.get();
    }


    private class ServiceInfo {
        public ServiceInfo(){
            this.service = null;
            this.methodMap = new HashMap<>();
        }

        Service service;
        Map<String, Descriptors.MethodDescriptor> methodMap;
    }

    //包含所有的rpc服务对象和服务方法
    //也就是说注册/发布就是往这个map表里面去写东西
    //string就是服务对象的名字，一个服务对象里面又包含了很多服务方法，所以ServiceInfo也是用一个Map来组织
    private Map<String,ServiceInfo> serviceMap;

    //启动rpc站点提供服务，rpc站点启动了工作在serverIp的serverPort端口号上
    public void start() {
        ZkClientUtils zk = new ZkClientUtils(zkServer);
        serviceMap.forEach((k,v)->{
            System.out.println(k);
            String path = "/" + k;  //把服务对象当成一个节点
            zk.createPersistent(path,null);
            v.methodMap.forEach((a,b)->{
                String createPath = path + "/" + a;
                //服务方法节点是服务对象节点的子节点 内容是ip和端口 远程调用ip都不知道怎么调用
                //注册服务和方法在哪一台主机上
                zk.createEphemeral(createPath,serverIp + ":" + serverPort);
                //给临时性节点添加监听器watcher
                zk.addWatcher(createPath);
                System.out.println("reg zk -> " + createPath);
            });
        });

        /*
        serviceMap.forEach((k,v)->{
            System.out.println(k);
            v.methodMap.forEach((a,b)-> System.out.println(a));
        });
        */
        //你不是注册的对象和方法都在Map上吗，你把Map上的东西都往zookeeper上注册
        //把service和method都往zookeeper上注册一下


        System.out.println("rpc server start at " + serverIp + ":" + serverPort);

        //启动rpcserver网络服务，就是搞出一个rpcserver对象来呗
        //RpcServer这个类是自己创建的
        RpcServer s = new RpcServer(this);
        s.start(serverIp,serverPort);
    }

    //注册rpc服务方法 只要支持rpc方法的类，都实现了com.google.protobuf.Service这个接口
    //参数写这个接口就是注册什么服务对象都可以
    //这个方法就是向Map里注册服务对象和服务方法
    public void registerRpcService(Service service){
        Descriptors.ServiceDescriptor sd = service.getDescriptorForType();
        //获取服务对象的名称
        String serviceName = sd.getName();
        ServiceInfo si = new ServiceInfo();
        si.service = service;   //si里面塞了服务对象和里面所有的服务方法
        //获取服务对象的所有服务方法列表
        List<Descriptors.MethodDescriptor> methodList = sd.getMethods();
        methodList.forEach(method -> {
            //获取服务方法名字
            String methodName = method.getName();
            si.methodMap.put(methodName,method);
        });
        serviceMap.put(serviceName,si);
    }

    private RpcProvider(){
        this.serviceMap = new HashMap<>();
        this.responsebufLocal = new ThreadLocal<>();
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getZkServer() {
        return zkServer;
    }

    public void setZkServer(String zkServer) {
        this.zkServer = zkServer;
    }

    //想要单例就搞了个这个出来，都用这个创建，封装RpcProvider对象创建的细节
    public static class Builder{
        private static RpcProvider INSTANCE = new RpcProvider();

        //从配置文件中读取rpc server的ip和port，给INSTANCE对象初始化数据
        //通过build创建一个rpcprovider对象
        public RpcProvider build(String configfile){

            Properties pro = new Properties();
            try {
                pro.load(Builder.class.getClassLoader().getResourceAsStream(configfile));
                INSTANCE.setServerIp(pro.getProperty(SERVER_IP));
                INSTANCE.setServerPort(Integer.parseInt(pro.getProperty(SERVER_PORT)));
                INSTANCE.setZkServer(pro.getProperty(ZK_SERVER));
                return INSTANCE;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //返回一个对象建造器
    public static Builder newBuilder(){
        return new Builder();
    }

}
