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
                        seckillService.startSeckilLockError(goodsId, userId);
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
                    seckillService.startSeckilAopLock(goodsId, userId);
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
}
