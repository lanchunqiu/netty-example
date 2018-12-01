package com.lancq.chat.server;

import com.lancq.chat.protocol.IMDecoder;
import com.lancq.chat.protocol.IMEncoder;
import com.lancq.chat.server.handler.HttpHandler;
import com.lancq.chat.server.handler.SocketHandler;
import com.lancq.chat.server.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class ChatServer {
    private static Logger LOG = Logger.getLogger(ChatServer.class);

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

                            /** 解析自定义协议 */
                            client.pipeline().addLast(new IMDecoder());
                            client.pipeline().addLast(new IMEncoder());
                            client.pipeline().addLast(new SocketHandler());

                            /** 解析Http请求 */
                            client.pipeline().addLast(new HttpServerCodec());
                            //主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
                            client.pipeline().addLast(new HttpObjectAggregator(64*1024));
                            //主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的 ,加上这个handler我们就不用考虑这个问题了
                            client.pipeline().addLast(new ChunkedWriteHandler());
                            client.pipeline().addLast(new HttpHandler());

                            /** 解析WebSocket请求 */
                            client.pipeline().addLast(new WebSocketServerProtocolHandler("/im"));
                            client.pipeline().addLast(new WebSocketHandler());

                        }
                    })
                    //配置信息
                    .option(ChannelOption.SO_BACKLOG, 1024);//针对主线程的配置

            ChannelFuture f = server.bind(port).sync();
            System.out.println("HTTP服务已启动，监听端口:" + port);
            LOG.info("服务已启动,监听端口" + port);
            //开始接收客户请求
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new ChatServer().start(5555);

    }
}
