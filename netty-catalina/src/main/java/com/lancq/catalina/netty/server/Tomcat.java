package com.lancq.catalina.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.io.IOException;


/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class Tomcat {
    public void start( int port) throws IOException, InterruptedException {
        //boss线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //worker线程
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            //Netty服务
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    //主线程处理类
                    .channel(NioServerSocketChannel.class)
                    //子线程处理类
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel client) throws Exception {
                            //业务逻辑链路，编码器
                            client.pipeline().addLast(new HttpResponseEncoder());
                            //解码器
                            client.pipeline().addLast(new HttpRequestDecoder());

                            client.pipeline().addLast(new TomcatHandler());
                        }
                    })
                    //配置信息
                    .option(ChannelOption.SO_BACKLOG, 128)//针对主线程的配置
                    .childOption(ChannelOption.SO_KEEPALIVE, true);//针对子线程的配置

            ChannelFuture f = server.bind(port).sync();
            System.out.println("HTTP服务已启动，监听端口:" + port);

            //开始接收客户请求
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Tomcat().start(5555);
    }
}
