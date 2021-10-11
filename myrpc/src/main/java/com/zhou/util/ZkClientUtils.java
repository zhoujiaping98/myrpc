package com.zhou.util;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//和zookeeper通信用的辅助工具类
//这个类就是实现zk客户端的功能嘛，可以连接服务器，创建节点，删除节点，读取节点内容
public class ZkClientUtils {
    private static String rootPath = "/myrpc";
    private ZkClient zkClient;
    private Map<String,String> ephemeralMap = new HashMap<>();

    //通过zk server字符串信息连接zkserver
    public ZkClientUtils(String serverList){
        //zk客户端和服务器通过会话连接，sessionTimeout是连接上之后多长时间没有交互就判为超时
        this.zkClient = new ZkClient(serverList,3000);
        //如果root节点不存在就创建
        if(!this.zkClient.exists(rootPath)){
            this.zkClient.createEphemeral(rootPath,null);
        }
    }

    //关闭和zkserver的连接
    public void close(){
        this.zkClient.close();
    }

    //zk上创建临时性节点，临时性节点就是连接断开后节点自动释放
    public void createEphemeral(String path,String data){
        path = rootPath + path;
        //znode节点不存在时才创建
        ephemeralMap.put(path, data);
        if(!this.zkClient.exists(path)){
            this.zkClient.createEphemeral(path,data);
        }
    }

    //zk上创建永久性节点
    public void createPersistent(String path,String data){
        path = rootPath + path;
        //znode节点不存在时才创建
        if(!this.zkClient.exists(path)){
            this.zkClient.createPersistent(path,data);
        }
    }

    //读取znode节点的值
    public String readData(String path){
        return this.zkClient.readData(rootPath+path,null);
    }

    //给zookeeper上指定的节点添加watcher监听
    public void addWatcher(String path) {
        this.zkClient.subscribeDataChanges(rootPath + path, new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            /**
             * 一定要设置znode节点监听，因为如果zkclient断掉，由于zkserver无法及时获知zkclient的关闭状态，
             * 所以zkserver会等待session tiomeout时间以后，会把zkclient创建的临时节点全部删除掉，但是
             * 如果在session tiomeout时间内，又启动了同样的zkclient，因为在timeout事件以内所以节点还存在就不能创建，
             * 但是等待session tiomeout时间超时以后，原先创建的临时节点都没了
             * @param path
             * @throws Exception
             */
            @Override
            public void handleDataDeleted(String path) throws Exception {
                System.out.println("watcher -> handleDataDeleted : " + path);
                // 把删除掉的znode临时性节点重新创建一下
                String str = ephemeralMap.get(path);   // /nprpc/UserServiceRpc/reg  get的是path啊得到的就是IP地址和端口啊
                if(str != null) {
                    zkClient.createEphemeral(path, str);
                }
            }
        });
    }

    public static String getRootPath(){
        return rootPath;
    }

    public static void setRootPath(String rootPath){
        ZkClientUtils.rootPath = rootPath;
    }

    public static void main(String[] args){
        //ZkClientUtils zk = new ZkClientUtils("127.0.0.1:2181");
        //zk.close();
    }
}
