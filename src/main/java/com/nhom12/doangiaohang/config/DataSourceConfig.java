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

    public static final String CONFIG_FILE = "db-config.properties";

    @Bean
    @Primary
    public DataSource dataSource() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);

        if (file.exists()) {
            // Nếu tìm thấy file cấu hình, đọc và kết nối Oracle
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                System.out.println(">> [INFO] Đang kết nối CSDL từ file cấu hình: " + props.getProperty("url"));
                
                return DataSourceBuilder.create()
                        .driverClassName("oracle.jdbc.OracleDriver")
                        .url(props.getProperty("url"))
                        .username(props.getProperty("username"))
                        .password(props.getProperty("password"))
                        .build();
            } catch (Exception e) {
                System.err.println(">> [ERROR] Lỗi đọc file cấu hình. Chuyển sang H2.");
            }
        } else {
            System.out.println(">> [WARN] Không tìm thấy file db-config.properties. Chạy chế độ Setup (H2 Database).");
        }

        // Mặc định: Chạy H2 In-Memory để ứng dụng khởi động được (để vào trang Setup)
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:setupdb;DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .build();
    }
}