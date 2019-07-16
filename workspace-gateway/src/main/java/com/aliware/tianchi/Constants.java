package com.aliware.tianchi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class Constants {
//    public static final ThreadLocal<Integer>  threadLocal = new ThreadLocal();
/*    public static  Integer threadSmall = 0;
    public static  Integer threadMedium = 0;
    public static  Integer threadLarge = 0;*/

    public static Map<String, Integer> activeThreadCount = new HashMap<>();
    public static LongAdder longAdderLarge = new LongAdder();
    public static LongAdder longAdderMedium = new LongAdder();
    public static LongAdder longAdderSmall = new LongAdder();

    static {
        longAdderLarge.add(650);
        longAdderMedium.add(450);
        longAdderSmall.add(200);
        activeThreadCount.put("small", 200);
        activeThreadCount.put("medium", 450);
        activeThreadCount.put("large", 650);

        // 区间活跃总量统计 用于计算窗口值
        activeThreadCount.put("small_period", 0);
        activeThreadCount.put("small_period_num", 1);
        activeThreadCount.put("medium_period", 0);
        activeThreadCount.put("medium_period_num", 0);
        activeThreadCount.put("large_period", 0);
        activeThreadCount.put("large_period_num", 0);
    }

}
