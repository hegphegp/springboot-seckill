package com.hegp.controller;

import com.hegp.domain.Result;
import com.hegp.entity.Record;
import com.hegp.queue.SeckillQueue;
import com.hegp.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 每秒下单成功量万级以上的并发系统，都应该用redis去扛并发，不应该用数据库，redis的性能比数据库高出上千倍
 */
@RestController
@RequestMapping("/v1/seckill")
public class SeckillController {
    private final static Logger logger = LoggerFactory.getLogger(SeckillController.class);
    //创建线程池  调整队列数 拒绝服务
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(300,
            301,
            50, // executor最大保持 50 个线程在运行
            TimeUnit.SECONDS,
            new LinkedBlockingQueue(1000));

    @Autowired
    private SeckillService seckillService;

    // 方法一，是错误的，不加锁
    @GetMapping("/start")
    public Result start(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        int skillNum = 1000; // 抢购者远远大于商品数量
        for (int n = 1; n < 101; n++) {
            seckillService.cleanData(goodsId);
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
                seckillService.checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Result.ok();
    }



    // 方法二，用Lock加锁，但是用法错误，应该是事务结束后再释放锁
    @GetMapping("/startLock-error")
    public Result startLockError(@RequestParam(required = false) Long goodsId) {
        return startLock(goodsId, true);
    }

    // 方法三，用Lock加锁，用法正确，事务结束后才释放锁
    @GetMapping("/startLock")
    public Result startLock(@RequestParam(required = false) Long goodsId) {
        return startLock(goodsId, false);
    }

    public Result startLock(@RequestParam(required = false) Long goodsId, boolean error) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
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
                seckillService.checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }

    // 用注解修饰方法，然后用切面拦截，用一把锁限制，一次只能一个人执行
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
                seckillService.checkSeckillCount(n, goodsId);
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
                seckillService.checkSeckillCount(n, goodsId);
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
                seckillService.checkSeckillCount(n, goodsId);
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
                seckillService.checkSeckillCount(n, goodsId);
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
                seckillService.checkSeckillCount(n, goodsId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时===>>>"+(end-start));
        return Result.ok();
    }

    @GetMapping("/startQueue")
    public Result startQueue(@RequestParam(required = false) Long goodsId) {
        if (goodsId==null || 10000!=goodsId) {
            return Result.error("商品ID错误");
        }
        long start = System.currentTimeMillis();
        int skillNum = 1000;
        for (int n = 1; n < 1001; n++) {
            seckillService.cleanData(goodsId);
            final CountDownLatch latch = new CountDownLatch(skillNum);//N个购买者
            for (int i = 0; i < 1000; i++) {
                final long userId = i;
                Runnable task = () -> {
                    Record record = new Record();
                    record.setGoodsId(goodsId);
                    record.setUserId(userId);
                    try {
                        Boolean flag = SeckillQueue.getMailQueue().produce(record);
//                        if(flag){
//                            LOGGER.info("用户:{}{}",kill.getUserId(),"秒杀成功");
//                        } else {
//                            LOGGER.info("用户:{}{}",userId,"秒杀失败");
//                        }
                        latch.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
