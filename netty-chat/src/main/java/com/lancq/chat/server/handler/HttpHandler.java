package com.lancq.chat.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static Logger LOG = Logger.getLogger(HttpHandler.class);

    //获取class路径
    private URL baseURL = HttpHandler.class.getProtectionDomain().getCodeSource().getLocation();

    private final String WEB_ROOT = "webroot";

    private File getFileFromRoot(String fileName) throws URISyntaxException {
        String path = baseURL.toURI() + WEB_ROOT + "/" + fileName;
        path = !path.contains("file:") ? path : path.substring(5);
        path = path.replaceAll("//", "/");
        return new File(path);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.getUri();
        String html = uri.equals("/") ? "chat.html" : uri;
        RandomAccessFile file = null;
        try{
            file = new RandomAccessFile(getFileFromRoot(html),"r");
        } catch (Exception e){
            ctx.fireChannelRead(request.retain());
            return;
        }
        //System.out.println(file);

        String contextType = "text/html;";

        if(uri.endsWith(".css")){
            contextType = "text/css;";
        } else if(uri.endsWith(".js")){
            contextType = "text/javascript;";
        } else if(uri.toLowerCase().matches("(jgp|png|gif|ico)$")){
            String ext = uri.substring(uri.lastIndexOf("."));
            contextType = "image/" + ext + ";";
        }

        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contextType + "charset=UTF-8;");

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        if(keepAlive){
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);
        ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

        //清空缓冲区
        ChannelFuture f= ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if(!keepAlive){
            f.addListener(ChannelFutureListener.CLOSE);
        }

        file.close();

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel client = ctx.channel();
        System.out.println("Client:"+client.remoteAddress()+"异常");
        LOG.info("Client:"+client.remoteAddress()+"异常");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
