package com.hegp.service;

import com.hegp.domain.Result;

public interface SeckillDistributedService {

	/**
	 * 秒杀 一  单个商品
	 * @param goodsId 秒杀商品ID
	 * @param userId 用户ID
	 * @return
	 */
	Result startSeckilRedisLock(Long goodsId, Long userId);

//	/**
//	 * 秒杀 一  单个商品
//	 * @param seckillId 秒杀商品ID
//	 * @param userId 用户ID
//	 * @return
//	 */
//	Result startSeckilZksLock(long seckillId,long userId);
//
//	/**
//	 * 秒杀 二 多个商品
//	 * @param seckillId 秒杀商品ID
//	 * @param userId 用户ID
//	 * @param number 秒杀商品数量
//	 * @return
//	 */
//	Result startSeckilLock(long seckillId,long userId,long number);
	
}
