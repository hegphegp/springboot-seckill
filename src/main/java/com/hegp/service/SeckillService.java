package com.hegp.service;

import com.hegp.domain.Result;

public interface SeckillService {

    /** 查询秒杀售卖商品 */
    Long getSeckillCount(Long goodsId);

    /** 清空数据 */
    void cleanData(Long goodsId);

    /** 秒杀 一 */
    Result startSeckill(Long goodsId, Long userId);

    /**
     * 秒杀 二、程序锁, 锁相对于事务上移
     * @param goodsId
     * @param userId
     * @return
     */
    Result startSeckilLockError(long goodsId, long userId);

    /**
     * 秒杀 二、程序锁, 锁相对于事务下移
     * @param goodsId
     * @param userId
     * @return
     */
    Result startSeckilLock(long goodsId, long userId);

    /**
     * 秒杀 三、程序锁AOP
     * @param goodsId
     * @param userId
     * @return
     */
    Result startSeckilAopLock(long goodsId, long userId);

    Result reduceGoodsAndSaveWithTransactional(long goodsId, long userId);
}
