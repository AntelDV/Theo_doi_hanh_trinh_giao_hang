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

    // Tên file chứa thông tin kết nối (sẽ nằm cùng thư mục với file chạy)
    public static final String CONFIG_FILE = "db-config.properties";

    @Bean
    @Primary
    public DataSource dataSource() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);

        // Trường hợp 1: Đã có file cấu hình (Đã Setup xong) -> Kết nối Oracle thật
        if (file.exists()) {
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
                System.err.println(">> [ERROR] Lỗi đọc file cấu hình. Chuyển sang chế độ Setup (H2).");
            }
        } else {
            System.out.println(">> [WARN] Không tìm thấy file " + CONFIG_FILE + ". Chuyển sang chế độ Setup (H2).");
        }

        // Trường hợp 2: Chưa có file cấu hình -> Chạy Database ảo (H2) để hiện trang Setup
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:setupdb;DB_CLOSE_DELAY=-1;MODE=Oracle") 
                .username("sa")
                .password("")
                .build();
    }
}