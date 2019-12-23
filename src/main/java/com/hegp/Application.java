package com.hegp;

import com.hegp.entity.Goods;
import com.hegp.entity.Record;
import com.hegp.queue.SeckillQueue;
import com.hegp.repository.GoodsRepository;
import com.hegp.service.SeckillService;
import com.hegp.utils.RedissLockUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Timestamp;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private SeckillService seckillService;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private RedissonClient redissonClient;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        RedissLockUtil.setRedissonClient(redissonClient);
        Goods goods = new Goods();
        goods.setId(10000L);
        goods.setName("小米手机");
        goods.setTotal(100);
        goods.setVersion(0);
        goods.setCreateTime(new Timestamp(System.currentTimeMillis()));
        goodsRepository.save(goods);

        while(true) {
            //进程内队列
            Record record = SeckillQueue.getMailQueue().consume();
            if(record!=null) {
                seckillService.startSeckill(record.getGoodsId(), record.getUserId());
            }
        }
    }
}
