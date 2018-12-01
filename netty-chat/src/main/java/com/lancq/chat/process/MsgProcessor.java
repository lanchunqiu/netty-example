package com.lancq.chat.process;

import com.alibaba.fastjson.JSONObject;
import com.lancq.chat.protocol.IMDecoder;
import com.lancq.chat.protocol.IMEncoder;
import com.lancq.chat.protocol.IMMessage;
import com.lancq.chat.protocol.IMP;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/11
 **/
public class MsgProcessor {
    private final static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private IMDecoder decoder = new IMDecoder();
    private IMEncoder encoder = new IMEncoder();

    //定义一些扩展属性
    private final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    private final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    private final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    /**
     * 获取用户昵称
     * @param client
     * @return
     */
    public String getNickName(Channel client){
        return client.attr(NICK_NAME).get();
    }
    /**
     * 获取用户远程IP地址
     * @param client
     * @return
     */
    public String getAddress(Channel client){
        return client.remoteAddress().toString().replaceFirst("/","");
    }

    /**
     * 获取扩展属性
     * @param client
     * @return
     */
    public JSONObject getAttrs(Channel client){
        try{
            return client.attr(ATTRS).get();
        }catch(Exception e){
            return null;
        }
    }

    /**
     * 获取扩展属性
     * @param client
     * @return
     */
    private void setAttrs(Channel client, String key, Object value){
        try{
            JSONObject json = client.attr(ATTRS).get();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }catch(Exception e){
            JSONObject json = new JSONObject();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }
    }
    /**
     * 登出通知
     * @param client
     */
    public void logout(Channel client){
        //如果nickName为null，没有遵从聊天协议的连接，表示未非法登录
        if(getNickName(client) == null){ return; }
        for (Channel channel : onlineUsers) {
            IMMessage request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), getNickName(client) + "离开");
            String content = encoder.encode(request);
            channel.writeAndFlush(new TextWebSocketFrame(content));
        }
        onlineUsers.remove(client);
    }

    /**
     * 发送消息
     * @param client
     * @param msg
     */
    public void sendMsg(Channel client,IMMessage msg){
        sendMsg(client,encoder.encode(msg));
    }

    /**
     * 发送消息
     * @param client
     * @param msg
     */
    public void sendMsg(Channel client,String msg){
        IMMessage request = decoder.decode(msg);
        if(null == request){ return; }

        String addr = getAddress(client);
        String cmd = request.getCmd();
        String sender = request.getSender();
        //判断如果是登录动作，就往onlineUsers中加入一条信息
        if(IMP.LOGIN.getName().equals(cmd)){
            client.attr(NICK_NAME).getAndSet(sender);
            client.attr(IP_ADDR).getAndSet(addr);

            onlineUsers.add(client);

            for (Channel channel : onlineUsers) {
                if(channel != client){
                    request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), getNickName(client) + "加入");
                }else{
                    request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), "已与服务器建立连接！");
                }
                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else if(IMP.CHAT.getName().equals(cmd)){
            for (Channel channel : onlineUsers) {
                if (channel == client) {
                    request.setSender("你");
                }else{
                    request.setSender(getNickName(client));
                }
                request.setTime(sysTime());
                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else if(IMP.FLOWER.getName().equals(cmd)){
            JSONObject attrs = getAttrs(client);
            long currTime = sysTime();
            if(null != attrs){
                long lastTime = attrs.getLongValue("lastFlowerTime");
                //60秒之内不允许重复刷鲜花
                int secends = 10;
                long sub = currTime - lastTime;
                if(sub < 1000 * secends){
                    request.setSender("你");
                    request.setCmd(IMP.SYSTEM.getName());
                    request.setContent("您送鲜花太频繁," + (secends - Math.round(sub / 1000)) + "秒后再试");
                    String content = encoder.encode(request);
                    client.writeAndFlush(new TextWebSocketFrame(content));
                    return;
                }
            }

            //正常送花
            for (Channel channel : onlineUsers) {
                if (channel == client) {
                    request.setSender("你");
                    request.setContent("你给大家送了一波鲜花雨");
                    setAttrs(client, "lastFlowerTime", currTime);
                }else{
                    request.setSender(getNickName(client));
                    request.setContent(getNickName(client) + "送来一波鲜花雨");
                }
                request.setTime(sysTime());

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }
    }

    private long sysTime(){
        return System.currentTimeMillis();
    }
}
