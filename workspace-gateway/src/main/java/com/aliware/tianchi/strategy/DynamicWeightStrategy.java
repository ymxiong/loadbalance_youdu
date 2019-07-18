package com.aliware.tianchi.strategy;

import com.aliware.tianchi.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.*;

import static com.aliware.tianchi.strategy.AbstractStrategy.rand;

/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-07-15 16:46:35
 */
public class DynamicWeightStrategy implements UserLoadBalanceStrategy {

    private static DynamicWeightStrategy strategy = new DynamicWeightStrategy();

    public static DynamicWeightStrategy getInstance() {
        return strategy;
    }

    private static final int NUM_SMALL = 0;
    private static final int NUM_MEDIUM = 1;
    private static final int NUM_LARGE = 2;


    private static final int NUM_SMALL_OLD = 10;
    private static final int NUM_MEDIUM_OLD = 11;
    private static final int NUM_LARGE_OLD = 12;


    private static final int NUM_TOTAL = 20;
    private static final int NUM_TOTAL_OLD = 21;


    private static final int ACTIVE_SMALL = 30;

    private static final int ACTIVE_MEDIUM = 31;

    private static final int ACTIVE_LARGE = 32;

    private static final int ACTIVE_SMALL_OLD = 40;

    private static final int ACTIVE_MEDIUM_OLD = 41;

    private static final int ACTIVE_LARGE_OLD = 42;

    private static final int TOTAL_INIT_WEIGHT = 30000;

    private int smallWeight = TOTAL_INIT_WEIGHT / 3;

    private int mediumWeight = TOTAL_INIT_WEIGHT / 3;

    private int largeWeight = TOTAL_INIT_WEIGHT / 3;

    // 活跃门槛
    private static final int ALPHA_MAX = 20;

    private static final int ALPHA_LOW = 1;

    // 权重抢占参数
    private static final int GRAB_NUM = 20;

    // 权重调整滑动窗口时间 单位毫秒
    private static final int SLIDING_WINDOW_TIME = 10;

    private HashMap<Integer, Integer> numberMap = new HashMap<>();

    private static final boolean IS_DEBUG = Boolean.parseBoolean(System.getProperty("debug"));

    private DynamicWeightStrategy() {
        numberMap.put(NUM_SMALL, 0);
        numberMap.put(NUM_MEDIUM, 0);
        numberMap.put(NUM_LARGE, 0);
        numberMap.put(NUM_SMALL_OLD, 0);
        numberMap.put(NUM_MEDIUM_OLD, 0);
        numberMap.put(NUM_LARGE_OLD, 0);

        numberMap.put(NUM_TOTAL, 0);
        numberMap.put(NUM_TOTAL_OLD, 0);

        numberMap.put(ACTIVE_SMALL, 0);
        numberMap.put(ACTIVE_MEDIUM, 0);
        numberMap.put(ACTIVE_LARGE, 0);

        numberMap.put(ACTIVE_SMALL_OLD, 0);
        numberMap.put(ACTIVE_MEDIUM_OLD, 0);
        numberMap.put(ACTIVE_LARGE_OLD, 0);


        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                numberMap.put(NUM_TOTAL, numberMap.get(NUM_SMALL) + numberMap.get(NUM_MEDIUM) + numberMap.get(NUM_LARGE));

//                System.out.println(
//                        " NUM_SMALL: " + numberMap.get(NUM_SMALL) +
//                                " NUM_MEDIUM: " + numberMap.get(NUM_MEDIUM) +
//                                " NUM_LARGE: " + numberMap.get(NUM_LARGE) +
//                                " NUM_TOTAL: " + numberMap.get(NUM_TOTAL) +
//                                " D_SMALL: " + (numberMap.get(NUM_SMALL) - numberMap.get(NUM_SMALL_OLD)) +
//                                " D_MEDIUM: " + (numberMap.get(NUM_MEDIUM) - numberMap.get(NUM_MEDIUM_OLD)) +
//                                " D_LARGE: " + (numberMap.get(NUM_LARGE) - numberMap.get(NUM_LARGE_OLD)) +
//                                " D_TOTAL: " + (numberMap.get(NUM_TOTAL) - numberMap.get(NUM_TOTAL_OLD))
//                );
//                System.out.println("GRAB_NUM: " + GRAB_NUM + " SMALL_WEIGHT: " + smallWeight + " MEDIUM_WEIGHT: " + mediumWeight + " LARGE_WEIGHT: " + largeWeight);

                weightChange();

                numberMap.put(NUM_SMALL_OLD, numberMap.get(NUM_SMALL));
                numberMap.put(NUM_MEDIUM_OLD, numberMap.get(NUM_MEDIUM));
                numberMap.put(NUM_LARGE_OLD, numberMap.get(NUM_LARGE));
                numberMap.put(NUM_TOTAL_OLD, numberMap.get(NUM_TOTAL));

            }
        }, SLIDING_WINDOW_TIME, SLIDING_WINDOW_TIME);
    }

    @Override
    public int select(URL url, Invocation invocation) {
        int targetMachine = getTargetMachineB();
        numberMap.put(targetMachine, numberMap.get(targetMachine) + 1);
        return targetMachine;
    }

    private int getTargetMachineA() {
        Random rand = new Random();

        int targetMachine = 2;
        int activeSmall = Constants.longAdderSmall.intValue();
        int activeMedium = Constants.longAdderMedium.intValue();
        int activeLarge = Constants.longAdderLarge.intValue();

        int randNumber = rand.nextInt(activeSmall + activeMedium + activeLarge);
        if (randNumber < activeSmall) {
            targetMachine = 0;
        } else if (randNumber >= activeSmall && randNumber < activeSmall + activeMedium) {
            targetMachine = 1;
        }
        return targetMachine;
    }

    private int getTargetMachineB() {


        int smallWeightLocal = this.smallWeight;
        int mediumWeightLocal = this.mediumWeight;
        int largeWeightLocal = this.largeWeight;
        double smallRatio = smallWeightLocal / (double) largeWeightLocal;
        double mediumRatio = mediumWeightLocal / (double) largeWeightLocal;

        int leftSmall = Constants.longAdderSmall.intValue();
        int leftMedium = Constants.longAdderMedium.intValue();
        int leftLarge = Constants.longAdderLarge.intValue();

        // 低活跃门槛保护
        smallWeightLocal = (int) (smallWeightLocal * ratioA(leftSmall, ALPHA_MAX * smallRatio, ALPHA_LOW * smallRatio));
        mediumWeightLocal = (int) (mediumWeightLocal * ratioA(leftMedium, ALPHA_MAX * mediumRatio, ALPHA_LOW * mediumRatio));
        largeWeightLocal = (int) (largeWeightLocal * ratioA(leftLarge, ALPHA_MAX, ALPHA_LOW));

        double alpha = 3.5;
        double beta = 6.5;

        PriorityQueue<Double> queue = new PriorityQueue<>((o1, o2) -> o2.compareTo(o1));
        double k1 = Math.log(rand.nextDouble()) / (alpha * smallWeightLocal + beta * leftSmall);
        queue.offer(k1);
        double k2 = Math.log(rand.nextDouble()) / (alpha * mediumWeightLocal + beta * leftMedium);
        queue.offer(k2);
        double k3 = Math.log(rand.nextDouble()) / (alpha * largeWeightLocal + beta * leftLarge);
        queue.offer(k3);

        double result = queue.poll();

        if (result == k1) {
            return 0;
        }
        if (result == k2) {
            return 1;
        }
        return 2;


//        int targetMachine = 2;
//        int randNumber = rand.nextInt(smallWeightLocal + mediumWeightLocal + largeWeightLocal);
//        if (randNumber < smallWeightLocal) {
//            targetMachine = 0;
//        } else if (randNumber < smallWeightLocal + mediumWeightLocal) {
//            targetMachine = 1;
//        }
//        return targetMachine;
    }


    private void weightChange() {
        // 避免线程冲突
        int smallWeightLocal = this.smallWeight;
        int mediumWeightLocal = this.mediumWeight;
        int largeWeightLocal = this.largeWeight;

        // 抢占权重
        int grabTotal = 0;
        if (smallWeightLocal > GRAB_NUM) {
            smallWeightLocal = smallWeightLocal - GRAB_NUM;
            grabTotal += GRAB_NUM;
        }

        if (mediumWeightLocal > GRAB_NUM) {
            mediumWeightLocal = mediumWeightLocal - GRAB_NUM;
            grabTotal += GRAB_NUM;
        }

        if (largeWeightLocal > GRAB_NUM) {
            grabTotal += GRAB_NUM;
        }

//        double avgSmall = Constants.longAdderSmall.intValue();
//        double avgMedium = Constants.longAdderMedium.intValue();
//        double avgLarge = Constants.longAdderLarge.intValue();

        double alpha = 3.5;
        double beta = 6.5;

        double avgSmall = alpha * (numberMap.get(NUM_SMALL) - numberMap.get(NUM_SMALL_OLD)) + beta * Constants.longAdderSmall.intValue();
        double avgMedium = alpha * (numberMap.get(NUM_MEDIUM) - numberMap.get(NUM_MEDIUM_OLD)) + beta * Constants.longAdderMedium.intValue();
        double avgLarge = alpha * (numberMap.get(NUM_LARGE) - numberMap.get(NUM_LARGE_OLD)) + beta * Constants.longAdderLarge.intValue();


        // 分配权重
        double k1 = avgSmall * largeWeightLocal / smallWeightLocal;
        double k2 = avgMedium * largeWeightLocal / mediumWeightLocal;

        // 按照压比分配权重 最终双方逼近一个值
        double totalK = k1 + k2 + avgLarge;
        double smallRatio = k1 / totalK;
        double mediumRatio = k2 / totalK;
        smallWeightLocal = (int) (smallWeightLocal + grabTotal * smallRatio);
        mediumWeightLocal = (int) (mediumWeightLocal + grabTotal * mediumRatio);
        largeWeightLocal = TOTAL_INIT_WEIGHT - smallWeightLocal - mediumWeightLocal;

        // 写出权重，不管线程如何抢占，始终保持总值稳定
        if (smallWeightLocal != this.smallWeight) {
            this.write(smallWeightLocal, mediumWeightLocal, largeWeightLocal);
        }
//        System.out.println("grabTotal: " + grabTotal + " s: " + avgSmall + " m: " + avgMedium + " l: " + avgLarge + " sl: " + smallWeightLocal  + " ml: " + mediumWeightLocal  + " ll: " + largeWeightLocal);

    }


    private void write(int smallWeight, int mediumWeight, int largeWeight) {
        this.smallWeight = smallWeight;
        this.mediumWeight = mediumWeight;
        this.largeWeight = largeWeight;
    }

    private double ratioA(double activeNum, double max, double min) {
        if (activeNum > max) {
            return 1;
        } else if (activeNum < min) {
            return 0;
        }
        double d1 = activeNum - max;
        double d2 = max - min;
        return -d1 * d1 / d2 * d2 + 1;
    }

    private double ratioB(double activeNum, double max, double min) {
        if (activeNum > max) {
            return 1;
        } else if (activeNum < min) {
            return 0;
        }
        return (activeNum - max) / (max - min) + 1;
    }

}
