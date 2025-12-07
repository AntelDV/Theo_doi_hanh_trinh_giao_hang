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

@Service
public class CustomUserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không chính xác."));

        if (!taiKhoan.isTrangThai()) {
            throw new UsernameNotFoundException("Tài khoản này đã bị khóa.");
        }


        String roleSecurity = "ROLE_USER"; 
        int roleId = taiKhoan.getVaiTro().getIdVaiTro();
        
        if (roleId == 1) {
            roleSecurity = "ROLE_QUANLY";
        } else if (roleId == 2) {
            roleSecurity = "ROLE_SHIPPER";
        } else if (roleId == 3) {
            roleSecurity = "ROLE_KHACHHANG";
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(roleSecurity)); 

        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                authorities
        );
    }
}