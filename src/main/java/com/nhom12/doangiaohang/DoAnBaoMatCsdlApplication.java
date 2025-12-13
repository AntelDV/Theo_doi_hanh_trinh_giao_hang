package com.nhom12.doangiaohang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; 

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.nhom12.doangiaohang.repository") 
public class DoAnBaoMatCsdlApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoAnBaoMatCsdlApplication.class, args);
    }
}










