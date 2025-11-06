package com.nhom12.doangiaohang.security;

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

import java.util.HashSet;
import java.util.Set;

/**
 * Dịch vụ này CỰC KỲ QUAN TRỌNG.
 * Spring Security sử dụng nó để TÌM và XÁC THỰC người dùng khi bạn bấm nút "Đăng nhập".
 */
@Service
public class CustomUserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    /**
     * Hàm này được Spring Security tự động gọi khi xử lý form đăng nhập.
     *
     * @param tenDangNhap Tên đăng nhập người dùng gõ vào form.
     * @return một đối tượng UserDetails mà Spring Security có thể hiểu.
     */
    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        
        // 1. Tìm tài khoản trong CSDL
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không chính xác."));

        // 2. Kiểm tra tài khoản có bị khóa không
        if (!taiKhoan.isTrangThai()) {
            throw new UsernameNotFoundException("Tài khoản này đã bị khóa.");
        }

        // 3. Xử lý LỖI LOGIC VAI TRÒ
        String vaiTroName = taiKhoan.getVaiTro().getTenVaiTro();
        
        // SỬA LỖI: Tên vai trò (authority) BẮT BUỘC phải bắt đầu bằng 'ROLE_'
        // vì chúng ta đang dùng hasRole() trong WebSecurityConfig.
        String roleSecurity;

        switch (vaiTroName) {
            case "Quản lý":
                roleSecurity = "ROLE_QUANLY"; // Phải khớp với hasRole("QUANLY")
                break;
            case "Shipper":
                roleSecurity = "ROLE_SHIPPER"; // Phải khớp với hasRole("SHIPPER")
                break;
            case "Khách hàng":
                roleSecurity = "ROLE_KHACHHANG"; // Phải khớp với hasRole("KHACHHANG")
                break;
            default:
                roleSecurity = "ROLE_USER"; 
        }

        // 4. Cấp quyền (Role) cho tài khoản
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(roleSecurity)); // Thêm quyền (ví dụ: "ROLE_KHACHHANG")

        // 5. Trả về đối tượng User (của Spring Security)
        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                authorities
        );
    }
}