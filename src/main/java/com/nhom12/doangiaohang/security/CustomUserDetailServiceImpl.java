package com.nhom12.doangiaohang.security;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
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

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @PersistenceContext private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        
        // GỌI THỦ TỤC DB ĐỂ KIỂM TRA TRẠNG THÁI KHÓA (Logic DB)
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("CSDL_NHOM12.SP_CHECK_LOGIN");
            query.registerStoredProcedureParameter("p_username", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_result", String.class, ParameterMode.OUT);
            
            query.setParameter("p_username", tenDangNhap);
            query.execute();
            
            String result = (String) query.getOutputParameterValue("p_result");
            
            if ("LOCKED".equals(result)) {
                throw new UsernameNotFoundException("Tài khoản đã bị khóa do nhập sai nhiều lần.");
            }
            // Nếu "FAIL" (Không tồn tại), ta vẫn để code dưới chạy để Spring xử lý 
        } catch (Exception e) {
            System.err.println("Lỗi gọi SP_CHECK_LOGIN: " + e.getMessage());
        }

        // Tải thông tin user để Spring Security kiểm tra mật khẩu
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Sai thông tin đăng nhập."));

        // Mapping Role
        String roleSecurity = "ROLE_USER"; 
        int roleId = taiKhoan.getVaiTro().getIdVaiTro();
        if (roleId == 1) roleSecurity = "ROLE_QUANLY";
        else if (roleId == 2) roleSecurity = "ROLE_SHIPPER";
        else if (roleId == 3) roleSecurity = "ROLE_KHACHHANG";

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(roleSecurity)); 

        return new User(taiKhoan.getTenDangNhap(), taiKhoan.getMatKhau(), authorities);
    }
}