package com.lancq.catalina.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class Response {
    private ChannelHandlerContext ctx;
    private HttpRequest r;

    private static Map<Integer,HttpResponseStatus> statusMapping = new HashMap<Integer,HttpResponseStatus>();

    static{
        statusMapping.put(200, HttpResponseStatus.OK);
        statusMapping.put(404, HttpResponseStatus.NOT_FOUND);
    }
    public Response(ChannelHandlerContext ctx, HttpRequest r){
        this.ctx = ctx;
        this.r = r;
    }

    public void write(String out, int status) {
        if(out == null) return;
        try{
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    statusMapping.get(status),
                    Unpooled.wrappedBuffer(out.getBytes("UTF-8")));

            response.headers().set(CONTENT_TYPE, "text/json");
            response.headers().set(CONTENT_LENGTH,response.content().readableBytes());
            response.headers().set(EXPIRES, 0);

            if(HttpHeaders.isKeepAlive(r)){
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }
            ctx.write(response);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            ctx.flush();
        }

    }

}
