package com.lancq.catalina.servlet;

import com.lancq.catalina.http.Request;
import com.lancq.catalina.http.Response;
import com.lancq.catalina.http.Servlet;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public class MyServlet extends Servlet {
    @Override
    public void doGet(Request request, Response response) {
        doPost(request, response);
    }

    @Override
    public void doPost(Request request, Response response) {
        String param = "name";
        String value = request.getParameter("name");
        response.write(param + " : " + value, 200);
    }
}
