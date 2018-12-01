package com.lancq.catalina.servlet;

import com.alibaba.fastjson.JSON;
import com.lancq.catalina.http.Request;
import com.lancq.catalina.http.Response;
import com.lancq.catalina.http.Servlet;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class MyServlet2 extends Servlet {
    @Override
    public void doGet(Request request, Response response) {
        doPost(request, response);
    }

    @Override
    public void doPost(Request request, Response response) {
        String str = JSON.toJSONString(request.getParameters(),true);
        response.write(str,200);
    }
}
