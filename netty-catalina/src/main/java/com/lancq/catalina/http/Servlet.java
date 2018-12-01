package com.lancq.catalina.http;

/**
 * @Author lancq
 * @Description
 * @Date 2018/11/10
 **/
public abstract class Servlet {
    public abstract void doGet(Request request, Response response);
    public abstract void doPost(Request request, Response response);
}
