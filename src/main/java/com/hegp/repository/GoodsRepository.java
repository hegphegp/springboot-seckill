package com.hegp.repository;

import com.hegp.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE shop_goods SET total=total-1 WHERE id=:id", nativeQuery = true)
    void goodsReduceOne(@Param("id") Long id);

}
