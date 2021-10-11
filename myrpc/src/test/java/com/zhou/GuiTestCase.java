package com.zhou;

/**
 * 事件回调操作
 * 描述：模拟界面类 接收用户发起的事件 处理完成 侠士结果
 *需求：
 * 1. 下载完成后，需要显示信息
 * 2. 下载过程中，需要显示下载进度
 * */

//界面类
public class GuiTestCase implements INotifyCallBack {
    private DownLoad downLoad;

    public GuiTestCase(){
        this.downLoad = new DownLoad(this);
    }

    //下载文件
    public void downLoadFile(String file){
        System.out.println("begin start download file" + file);
        downLoad.start(file);
    }

    //显示下载过程的方法
    public void progress(String file,int progress){
        System.out.println("download file:" + file + " progress:" + progress + "%.");
    }

    //显示下载完成的方法
    public void result(String file){
        System.out.println("download file:" + file + " over.");
    }

    public static void main(String[] args){
        GuiTestCase gui = new GuiTestCase();
        gui.downLoadFile("我要学Java");
    }
}


//负责下载内容的类
class DownLoad{

    private GuiTestCase gui;
    //底层执行下载任务的方法

    public DownLoad(GuiTestCase gui){
        this.gui = gui;
    }
    public void start(String file){
        int count= 0;
        try{
            while(count <= 100){
                gui.progress(file,count);
                Thread.sleep(1000);
                count += 20;
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        gui.result(file);   //上报文件下载完成
    }
}

/*
 * 两个类之间互相依赖，这种代码是非常不好的，干什么是上面这个类里定义的，而什么时候做是下面这个类里面决定的
 * 而且不用接口相当于写死了，也不合适
 * 这种时候就知道应该用回调函数了
 *
 */

//回调函数非常简单,定义应该接口，面向接口编程
interface INotifyCallBack{
    void progress(String file,int progress);
    void result(String file);
}

class DownLoad2{

    //private GuiTestCase gui;
    private INotifyCallBack cb;
    //底层执行下载任务的方法

    public DownLoad2(GuiTestCase cb){
        this.cb = cb;
    }
    public void start(String file){
        int count= 0;
        try{
            while(count <= 100){
                cb.progress(file,count);
                Thread.sleep(1000);
                count += 20;
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        //这样就是只负责上报，具体做什么由重写的方法决定
        cb.result(file);   //上报文件下载完成
    }
}
