package cn.richzjc.easyokhttp;


import java.io.IOException;

public interface Interceptor2 {

    Response2 doNext(Chain2 chain2) throws IOException;

}
