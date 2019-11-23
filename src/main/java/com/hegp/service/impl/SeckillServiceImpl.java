package com.hegp.service.impl;

import com.hegp.annotation.Servicelock;
import com.hegp.domain.Result;
import com.hegp.entity.Goods;
import com.hegp.entity.Record;
import com.hegp.repository.GoodsRepository;
import com.hegp.repository.RecordRepository;
import com.hegp.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SeckillServiceImpl implements SeckillService {
    private Lock lock = new ReentrantLock(true);//互斥锁 参数默认false，不公平锁

    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private RecordRepository recordRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private SeckillService seckillService;

    @Override
    public Long getSeckillCount(Long goodsId) {
        return recordRepository.count();
    }

    @Override
    @Transactional
    public void cleanData(Long goodsId) {
        entityManager.createNativeQuery("UPDATE shop_goods SET total=100;").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM shop_record").executeUpdate();
    }

    @Override
    @Transactional
    public Result startSeckill(Long goodsId, Long userId) {
        //校验库存
        Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
        Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
        if(total>0) {
            return reduceGoodsAndSave(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    public Result reduceGoodsAndSave(long goodsId, long userId) {
        //扣库存
        entityManager.createNativeQuery("UPDATE shop_goods SET total=total-1 WHERE id="+goodsId).executeUpdate();
        //创建订单
        Record record = new Record();
        record.setGoodsId(goodsId);
        record.setUserId(userId);
        record.setCreateTime(new Timestamp(System.currentTimeMillis()));
        record.setState(1);
        recordRepository.save(record);
        return Result.ok("成功抢到商品");
    }

    @Override
    @Transactional
    public Result startSeckilLockError(long goodsId, long userId) {
        try {
            lock.lock();
            Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
            Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
            if(total>0) {
                return reduceGoodsAndSave(goodsId, userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return Result.ok();
    }

    @Override
    public Result startSeckilLock(long goodsId, long userId) {
        try {
            lock.lock();
            Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
            Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
            if(total>0) {
                return seckillService.reduceGoodsAndSaveWithTransactional(goodsId, userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return Result.ok();
    }

    @Override
    @Servicelock
    @Transactional
    public Result startSeckilAopLock(long goodsId, long userId) {
        //来自码云码友<马丁的早晨>的建议 使用AOP + 锁实现
        return startSeckill(goodsId, userId);
    }

    @Override
//	@ServiceLimit(limitType= ServiceLimit.LimitType.IP)
    @Transactional
    public Result startSeckilDBPCC_ONE(long goodsId, long userId) {
        //单用户抢购一件商品或者多件都没有问题
        Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId+" FOR UPDATE ");
        Integer total =  Integer.parseInt(countQuery.getSingleResult().toString());
        if(total>0){
            return reduceGoodsAndSave(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    @Override
    @Transactional
    public Result startSeckilDBPCC_TWO(long goodsId, long userId) {
        //单用户抢购一件商品没有问题、但是抢购多件商品不建议这种写法
        Integer total = entityManager.createNativeQuery("UPDATE shop_goods SET total=total-1 WHERE id="+goodsId+" AND total>0").executeUpdate();//UPDATE锁表
        if(total>0) {
            return reduceGoodsAndSave(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    @Override
    @Transactional
    public Result reduceGoodsAndSaveWithTransactional(long goodsId, long userId) {
        return reduceGoodsAndSave(goodsId, userId);
    }
}
