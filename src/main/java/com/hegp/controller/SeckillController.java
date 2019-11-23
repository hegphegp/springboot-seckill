package com.hegp.controller;

import com.hegp.domain.Result;
import com.hegp.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/seckill")
public class SeckillController {
    private final static Logger logger = LoggerFactory.getLogger(SeckillController.class);
    private int goodsTotal = 100;
    //创建线程池  调整队列数 拒绝服务
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(300,
            301,
            50, // executor最大保持 50 个线程在运行
            TimeUnit.SECONDS,
            new LinkedBlockingQueue(1000));

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/start")
    public Result start(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        for (int n = 1; n < 101; n++) {
            seckillService.cleanData(goodsId);
            int skillNum = 1000; // 抢购者远远大于商品数量
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            for(int i=0; i<skillNum; i++) {
                final long userId = i;
                Runnable task = () -> {
                    seckillService.startSeckill(goodsId, userId);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Result.ok();
    }

    public void checkSeckillCount(int n, Long goodsId) {
        Long seckillCount = seckillService.getSeckillCount(goodsId);
        StringBuffer sb = new StringBuffer("第" + n + "轮秒杀，一共秒杀出" + seckillCount + "件商品");
        if ((seckillCount-goodsTotal)>0) {
            sb.append(", 超卖" + (seckillCount - goodsTotal) + "件");
            logger.error(sb.toString());
        } else {
            logger.info(sb.toString());
        }
    }

    @GetMapping("/startLock-error")
    public Result startLockError(@RequestParam(required = false) Long goodsId) {
        return startLock(goodsId, true);
    }

    @GetMapping("/startLock")
    public Result startLock(@RequestParam(required = false) Long goodsId) {
        return startLock(goodsId, false);
    }

    public Result startLock(@RequestParam(required = false) Long goodsId, boolean error) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        for (int n = 1; n < 101; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            for(int i=0; i<skillNum; i++){
                final long userId = i;
                Runnable task = () -> {
                    if (error) {
                        seckillService.startSeckillLockError(goodsId, userId);
                    } else {
                        seckillService.startSeckilLock(goodsId, userId);
                    }
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return Result.ok();
    }

    @GetMapping("/startAopLock")
    public Result startAopLock(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        for (int n = 1; n < 101; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            for (int i = 0; i < 1000; i++) {
                final long userId = i;
                Runnable task = () -> {
                    seckillService.startSeckillAopLock(goodsId, userId);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Result.ok();
    }

    @GetMapping("/startDBPCC_ONE")
    public Result startDBPCC_ONE(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
        for (int n = 1; n < 1001; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            final long killId = goodsId;
            for(int i=0; i<1000; i++){
                final long userId = i;
                Runnable task = () -> {
                    Result result = seckillService.startSeckillDBPCC_ONE(killId, userId);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }

    @GetMapping("/startDBPCC_TWO")
    public Result startDPCC_TWO(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
        for (int n = 1; n < 1001; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            final long killId = goodsId;
            for(int i=0; i<1000; i++){
                final long userId = i;
                Runnable task = () -> {
                    Result result = seckillService.startSeckillDBPCC_TWO(killId, userId);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }


    @GetMapping("/startDBOCC")
    public Result startDBOCC(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
        for (int n = 1; n < 1001; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            final long killId = goodsId;
            for (int i = 0; i < 1000; i++) {
                final long userId = i;
                Runnable task = () -> {
                    //这里使用的乐观锁、可以自定义抢购数量、如果配置的抢购人数比较少、比如120:100(人数:商品) 会出现少买的情况
                    //用户同时进入会出现更新失败的情况
                    Result result = seckillService.startSeckillDBOCC(killId, userId, 1);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }

    @GetMapping("/startDBOCC-bySQL")
    public Result startDBOCCBySQL(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
        for (int n = 1; n < 1001; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            final long killId = goodsId;
            for (int i = 0; i < 1000; i++) {
                final long userId = i;
                Runnable task = () -> {
                    //这里使用的乐观锁、可以自定义抢购数量、如果配置的抢购人数比较少、比如120:100(人数:商品) 会出现少买的情况
                    //用户同时进入会出现更新失败的情况
                    Result result = seckillService.startSeckillDBOCCBySQL(killId, userId, 1);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }
}
