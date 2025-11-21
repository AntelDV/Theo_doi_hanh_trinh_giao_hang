package com.nhom12.doangiaohang.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    // Đặt tên file rõ ràng
    public static final String CONFIG_FILE = "db-config.properties";

    @Bean
    @Primary
    public DataSource dataSource() {
        File file = new File(CONFIG_FILE);
        System.out.println("==============================================================");
        System.out.println("[CHECK] Đang tìm file cấu hình tại: " + file.getAbsolutePath());

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                
                String url = props.getProperty("url");
                System.out.println("[OK] Tìm thấy file! Đang kết nối tới: " + url);
                System.out.println("==============================================================");
                
                return DataSourceBuilder.create()
                        .driverClassName("oracle.jdbc.OracleDriver")
                        .url(url)
                        .username(props.getProperty("username"))
                        .password(props.getProperty("password"))
                        .build();
            } catch (Exception e) {
                System.err.println("[ERROR] File lỗi, quay về H2: " + e.getMessage());
            }
        } else {
            System.out.println("[WARN] Không thấy file cấu hình. Chạy H2 (Chế độ Setup).");
            System.out.println("==============================================================");
        }

        // Chạy H2 nếu chưa cấu hình
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:setupdb;DB_CLOSE_DELAY=-1;MODE=Oracle")
                .username("sa")
                .password("")
                .build();
    }
}