package com.example.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"com.example.user.mapper", "com.example.product.mapper"})
@SpringBootApplication(scanBasePackages = {"com.example.common", "com.example.user", "com.example.product", "com.example.seckill"})
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
