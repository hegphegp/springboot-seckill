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
    Result startSeckillLockError(long goodsId, long userId);

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
    Result startSeckillAopLock(long goodsId, long userId);

    /**
     * 秒杀 四、数据库悲观锁
     * @param goodsId
     * @param userId
     * @return
     */
    Result startSeckillDBPCC_ONE(long goodsId, long userId);

    /**
     * 秒杀 五、数据库悲观锁
     * @param goodsId
     * @param userId
     * @return
     */
    Result startSeckillDBPCC_TWO(long goodsId, long userId);

    /**
     * 秒杀 六、数据库乐观锁
     * @param goodsId
     * @param userId
     * @param number  购买的数量
     * @return
     */
    Result startSeckillDBOCC(long goodsId, long userId, long number);

    /**
     * 秒杀 六、数据库乐观锁
     * @param goodsId
     * @param userId
     * @param number  购买的数量
     * @return
     */
    Result startSeckillDBOCCBySQL(long goodsId, long userId, long number);

    Result reduceGoodsAndSaveWithTransactional(long goodsId, long userId);
}
