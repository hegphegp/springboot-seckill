package com.hegp.service.impl;

import com.hegp.annotation.Servicelock;
import com.hegp.controller.SeckillController;
import com.hegp.domain.Result;
import com.hegp.entity.Goods;
import com.hegp.entity.Record;
import com.hegp.repository.GoodsRepository;
import com.hegp.repository.RecordRepository;
import com.hegp.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SeckillServiceImpl implements SeckillService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillController.class);
    private Lock lock = new ReentrantLock(true);//互斥锁 参数默认false，不公平锁
    private int goodsTotal = 100;
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
        entityManager.createNativeQuery("UPDATE shop_goods SET total=100, version=0").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM shop_record").executeUpdate();
    }

    @Override
    @Transactional
    public Result startSeckill(Long goodsId, Long userId) {
        //校验库存
        Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
        Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
        if(total>0) {
            return reduceGoodsAndSaveRecord(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    public Result reduceGoodsAndSaveRecord(long goodsId, long userId) {
        //扣库存
        entityManager.createNativeQuery("UPDATE shop_goods SET total=total-1 WHERE id="+goodsId).executeUpdate();
        //创建订单
        saveRecord(goodsId, userId);
        return Result.ok("成功抢到商品");
    }

    public void saveRecord(long goodsId, long userId) {
        //创建订单
        Record record = new Record();
        record.setGoodsId(goodsId);
        record.setUserId(userId);
        record.setCreateTime(new Timestamp(System.currentTimeMillis()));
        record.setState(1);
        recordRepository.save(record);
    }

    @Override
    @Transactional
    public Result startSeckillLockError(long goodsId, long userId) {
        try {
            lock.lock();
            Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
            Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
            if(total>0) {
                return reduceGoodsAndSaveRecord(goodsId, userId);
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
    public Result startSeckillAopLock(long goodsId, long userId) {
        //来自码云码友<马丁的早晨>的建议 使用AOP + 锁实现
        return startSeckill(goodsId, userId);
    }

    @Override
//	@ServiceLimit(limitType= ServiceLimit.LimitType.IP)
    @Transactional
    public Result startSeckillDBPCC_ONE(long goodsId, long userId) {
        //单用户抢购一件商品或者多件都没有问题
        Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId+" FOR UPDATE ");
        Integer total =  Integer.parseInt(countQuery.getSingleResult().toString());
        if(total>0){
            return reduceGoodsAndSaveRecord(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    @Override
    @Transactional
    public Result startSeckillDBPCC_TWO(long goodsId, long userId) {
        //单用户抢购一件商品没有问题、但是抢购多件商品不建议这种写法
        Integer count = entityManager.createNativeQuery("UPDATE shop_goods SET total=total-1 WHERE id="+goodsId+" AND total>0").executeUpdate();//UPDATE锁表
        if(count>0) {
            return reduceGoodsAndSaveRecord(goodsId, userId);
        } else {
            return Result.error("抢购失败");
        }
    }

    // 多次测试发现，好像startSeckillDBOCC的方案比startSeckillDBOCCBySQL方案快几秒，不知道是不是goodsRepository.getOne(goodsId)有缓存
    @Override
    @Transactional
    public Result startSeckillDBOCC(long goodsId, long userId, long number) {
        Goods goods = goodsRepository.getOne(goodsId);
        if (goods==null) {
            return Result.error("系统数据库有问题，goodsId为10000的商品在数据库是必须存在的");
        }
        if(goods.getTotal()>=number) {//剩余的数量应该要大于等于秒杀的数量
            //乐观锁
            String sql = "UPDATE shop_goods SET total=total-"+number+", version=version+1 WHERE id="+goodsId+" AND version="+goods.getVersion();
            Integer count = entityManager.createNativeQuery(sql).executeUpdate();//UPDATE锁表
            if(count>0) {
                //创建订单
                saveRecord(goodsId, userId);
                return Result.ok("成功抢到商品");
            } else {
                return Result.error("抢购失败");
            }
        } else {
            return Result.error("抢购失败");
        }
    }

    @Override
    @Transactional
    public Result startSeckillDBOCCBySQL(long goodsId, long userId, long number) {
        List<Object[]> list= entityManager.createNativeQuery("SELECT total, version FROM shop_goods WHERE id="+goodsId).getResultList();
        if (list==null || list.size()==0) {
            return Result.error("系统数据库有问题，goodsId为10000的商品在数据库是必须存在的");
        }
        Integer total = (Integer)list.get(0)[0];
        Integer version = (Integer)list.get(0)[1];
        if(total>=number) {//剩余的数量应该要大于等于秒杀的数量
            //乐观锁
            String sql = "UPDATE shop_goods SET total=total-"+number+", version=version+1 WHERE id="+goodsId+" AND version="+version;
            Integer count = entityManager.createNativeQuery(sql).executeUpdate();//UPDATE锁表
            if(count>0) {
                //创建订单
                saveRecord(goodsId, userId);
                return Result.ok("成功抢到商品");
            } else {
                return Result.error("抢购失败");
            }
        } else {
            return Result.error("抢购失败");
        }
    }

    @Override
    @Transactional
    public Result reduceGoodsAndSaveWithTransactional(long goodsId, long userId) {
        return reduceGoodsAndSaveRecord(goodsId, userId);
    }

    @Override
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
}
