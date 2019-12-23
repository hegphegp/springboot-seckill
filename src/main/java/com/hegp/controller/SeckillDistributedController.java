package com.hegp.controller;

import com.hegp.domain.Result;
import com.hegp.service.SeckillDistributedService;
import com.hegp.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/seckillDistributed")
public class SeckillDistributedController {
    //创建线程池  调整队列数 拒绝服务
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(300,
            301,
            50, // executor最大保持 50 个线程在运行
            TimeUnit.SECONDS,
            new LinkedBlockingQueue(1000));

    @Autowired
    private SeckillService seckillService;
    @Autowired
    private SeckillDistributedService seckillDistributedService;

    /** 秒杀一(Rediss分布式锁)*/
    @GetMapping("/startRedisLock")
    public Result startRedisLock(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }

        long start = System.currentTimeMillis();
        for (int n = 1; n < 1001; n++) {
            int skillNum = 1000;
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            final long killId = goodsId;
            for(int i=0;i<1000;i++){
                final long userId = i;
                Runnable task = () -> {
                    seckillDistributedService.startSeckilRedisLock(killId, userId);
                    latch.countDown();
                };
                executor.execute(task);
            }
            try {
                latch.await();// 等待所有人任务结束
                seckillService.checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }
}
