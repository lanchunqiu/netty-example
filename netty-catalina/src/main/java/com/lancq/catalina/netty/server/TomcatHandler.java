package com.lancq.catalina.netty.server;

import com.lancq.catalina.http.Request;
import com.lancq.catalina.http.Response;
import com.lancq.catalina.http.Servlet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

import javax.core.common.config.CustomConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class TomcatHandler extends ChannelInboundHandlerAdapter {

    private static final Map<Pattern,Class<?>> servletMapping = new HashMap<Pattern,Class<?>>();
    static{
        //加载servlet映射配置
        CustomConfig.load("web.properties");

        for (String key : CustomConfig.getKeys()) {
            if(key.startsWith("servlet")){
                String name = key.replaceFirst("servlet.", "");
                if(name.indexOf(".") != -1){
                    name = name.substring(0,name.indexOf("."));
                }else{
                    continue;
                }
                String pattern = CustomConfig.getString("servlet." + name + ".urlPattern");
                pattern = pattern.replaceAll("\\*", ".*");
                String className = CustomConfig.getString("servlet." + name + ".className");
                if(!servletMapping.containsKey(pattern)){
                    try {
                        servletMapping.put(Pattern.compile(pattern), Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpRequest){
            HttpRequest r = (HttpRequest) msg;
            Request request = new Request(ctx, r);
            Response response = new Response(ctx, r);
            String uri = request.getUri();
            String method = request.getMethod();

            System.out.println(String.format("Uri:%s method %s", uri, method));
            boolean hasPattern = false;
            for (Map.Entry<Pattern, Class<?>> entry : servletMapping.entrySet()) {
                Pattern key = entry.getKey();
                if(key.matcher(uri).matches()){
                    Servlet servlet = (Servlet) entry.getValue().newInstance();
                    if("GET".equalsIgnoreCase(method)){
                        servlet.doGet(request, response);
                    } else {
                        servlet.doPost(request, response);
                    }
                    hasPattern = true;
                }
            }
            if(!hasPattern){
                String out = String.format("404 NotFound URL%s for method %s", uri,method);
                response.write(out,404);
                return;
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
