package com.zhou;

import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void test(){
        //生成一个建造者，建造者负责对象的建造
        //LoginRequest是TestProto内部的类
        TestProto.LoginRequest.Builder login_builder = TestProto.LoginRequest.newBuilder();
        login_builder.setName("zhang san");
        login_builder.setPwd("123456");

        //通过建造者产生一个loginRequest对象
        TestProto.LoginRequest request = login_builder.build();
        System.out.println(request.getName());
        System.out.println(request.getPwd());

        //把loginRequest对象序列化成字节流，通过网络发送出去，此处的sendbuf就可以通过网络发送出去了
        byte[] sendbuf = request.toByteArray();

        //protobuf从byte数组字节流反序列化生成loginRequest对象
        try{
            TestProto.LoginRequest r = TestProto.LoginRequest.parseFrom(sendbuf);
            System.out.println(r.getName());
            System.out.println(r.getPwd());
        }catch(InvalidProtocolBufferException e){
            e.printStackTrace();
        }
    }
}
