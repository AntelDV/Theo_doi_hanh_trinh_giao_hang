package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Override
    @Transactional(readOnly = true) // Đảm bảo việc đọc dữ liệu nhất quán
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        
        // 1. Tìm tài khoản trong CSDL
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với tên đăng nhập: " + tenDangNhap));

        // 2. Kiểm tra tài khoản có bị khóa hay không
        if (!taiKhoan.isTrangThai()) {
            throw new UsernameNotFoundException("Tài khoản này đã bị khóa.");
        }

        // 3. Lấy thông tin vai trò
        Set<GrantedAuthority> authorities = new HashSet<>();
        String vaiTro = taiKhoan.getVaiTro().getTenVaiTro();
        
        // Quan trọng: Spring Security yêu cầu tên vai trò phải có tiền tố "ROLE_"
        // CSDL của chúng ta là "Quản lý", "Shipper", "Khách hàng"
        // Chúng ta sẽ tự động thêm tiền tố
        String roleName;
        if (vaiTro.equals("Quản lý")) {
            roleName = "ROLE_QUANLY";
        } else if (vaiTro.equals("Shipper")) {
            roleName = "ROLE_SHIPPER";
        } else if (vaiTro.equals("Khách hàng")) {
            roleName = "ROLE_KHACHHANG";
        } else {
            roleName = "ROLE_USER"; // Vai trò mặc định nếu có lỗi
        }
        
        authorities.add(new SimpleGrantedAuthority(roleName));

        // 4. Tạo đối tượng UserDetails mà Spring Security có thể hiểu
        return new User(
            taiKhoan.getTenDangNhap(),
            taiKhoan.getMatKhau(),
            authorities // Danh sách các quyền
        );
    }
}