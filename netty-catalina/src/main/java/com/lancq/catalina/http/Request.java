package com.lancq.catalina.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class Request {
    private ChannelHandlerContext ctx;
    private HttpRequest r;
    public Request(ChannelHandlerContext ctx, HttpRequest r){
        this.ctx = ctx;
        this.r = r;
    }

    public String getUri(){
        return r.getUri();
    }

    public String getMethod(){
        return r.getMethod().name();
    }

    public Map<String, List<String>> getParameters(){
        QueryStringDecoder decoder = new QueryStringDecoder(r.getUri());
        return decoder.parameters();
    }

    public String getParameter(String name) {
        Map<String, List<String>> params = getParameters();
        List<String> param = params.get(name);
        if(null != param){
            return param.get(0);
        }else{
            return null;
        }
    }

}
