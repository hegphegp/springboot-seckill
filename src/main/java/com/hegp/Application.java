package com.hegp;

import com.hegp.entity.Goods;
import com.hegp.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Timestamp;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private GoodsRepository goodsRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Goods goods = new Goods();
        goods.setId(10000L);
        goods.setName("小米手机");
        goods.setTotal(100);
        goods.setVersion(10000);
        goods.setCreateTime(new Timestamp(System.currentTimeMillis()));
        goodsRepository.save(goods);
    }
}
