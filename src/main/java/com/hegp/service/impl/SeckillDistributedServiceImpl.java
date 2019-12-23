package com.hegp.service.impl;

import com.hegp.domain.Result;
import com.hegp.service.SeckillDistributedService;
import com.hegp.service.SeckillService;
import com.hegp.utils.RedissLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillDistributedServiceImpl implements SeckillDistributedService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private SeckillService seckillService;

    @Override
    public Result startSeckilRedisLock(Long goodsId, Long userId) {
        boolean res=false;
        try {
            res = RedissLockUtil.tryLock(goodsId+"", TimeUnit.SECONDS, 1, 20);
            if(res) {
                Query countQuery = entityManager.createNativeQuery("SELECT total FROM shop_goods WHERE id="+goodsId);
                Integer total = Integer.parseInt(countQuery.getSingleResult().toString());
                if(total>0) {
                    return seckillService.reduceGoodsAndSaveWithTransactional(goodsId, userId);
                }
                return Result.ok();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            if(res) {//释放锁
                RedissLockUtil.unlock(goodsId+"");
            }
        }
        return Result.error();
    }
}
