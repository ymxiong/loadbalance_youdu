package com.aliware.tianchi;

import com.aliware.tianchi.strategy.AResStrategy;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;

import java.util.*;

import static com.aliware.tianchi.Constants.*;

/**
 * @author daofeng.xjf
 * <p>
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestClientFilter.class);

    //long startTime = 0;
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            //startTime = System.currentTimeMillis();
            URL url = invoker.getUrl();
            int port = url.getPort();
            if (port == 20880) {
                longAdderSmall.decrement();
//                LOGGER.info(new Date().getTime() + ":small:" + (com.aliware.tianchi.Constants.activeThreadCount.get("small") + ":" + com.aliware.tianchi.Constants.longAdderSmall.longValue()));
            } else if (port == 20870) {
                longAdderMedium.decrement();
//                LOGGER.info(new Date().getTime() + ":medium:" + com.aliware.tianchi.Constants.activeThreadCount.get("medium") + ":" + com.aliware.tianchi.Constants.longAdderMedium.longValue());
            } else {
                longAdderLarge.decrement();
//                LOGGER.info(new Date().getTime() + ":large:" + (com.aliware.tianchi.Constants.activeThreadCount.get("large") + ":" + com.aliware.tianchi.Constants.longAdderLarge.longValue()));
            }
            Result result = invoker.invoke(invocation);
            return result;
        } catch (Exception e) {
            throw e;
        }

    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        // long endTime = System.currentTimeMillis();
        //System.out.println( "request time : " +(endTime - startTime));
        URL url = invoker.getUrl();
        int port = url.getPort();
        if (port == 20880) {
            longAdderSmall.increment();
            smallTotalNum.increment();
        } else if (port == 20870) {
            longAdderMedium.increment();
            mediumTotalNum.increment();
        } else {
            longAdderLarge.increment();
            largeTotalNum.increment();
        }
        return result;
    }


    private static final boolean IS_DEBUG = Boolean.parseBoolean(System.getProperty("debug"));

    public TestClientFilter() {
        if (IS_DEBUG) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    System.err.println(new ReceiveItem("small left num: " + longAdderSmall.intValue() + " total: " + smallTotalNum.intValue(), new Date()));
                    System.err.println(new ReceiveItem("medium left num: " + longAdderMedium.intValue() + " total: " + mediumTotalNum.intValue(), new Date()));
                    System.err.println(new ReceiveItem("large left num: " + longAdderLarge.intValue() + " total: " + largeTotalNum.intValue(), new Date()));
                }
            }, 500, 500);
        }
    }

}
