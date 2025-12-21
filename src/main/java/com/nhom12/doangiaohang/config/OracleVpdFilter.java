package com.nhom12.doangiaohang.config;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager; 
import org.springframework.transaction.TransactionDefinition;    
import org.springframework.transaction.support.TransactionTemplate; 

import java.io.IOException;

@Component
public class OracleVpdFilter implements Filter {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @PersistenceContext private EntityManager entityManager;    
    @Autowired private PlatformTransactionManager transactionManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean contextSet = false;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
            
            if (tk != null) {
                String dbRole = "KHACHHANG";
                if (tk.getVaiTro().getIdVaiTro() == 1) dbRole = "QUANLY";
                else if (tk.getVaiTro().getIdVaiTro() == 2) dbRole = "SHIPPER";

                String finalDbRole = dbRole; 
                transactionTemplate.executeWithoutResult(status -> {
                    entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PKG_SECURITY.SET_APP_USER(:uid, :uname, :urole); END;")
                            .setParameter("uid", tk.getId())
                            .setParameter("uname", username)
                            .setParameter("urole", finalDbRole)
                            .executeUpdate();
                });
                
                contextSet = true;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            if (contextSet) {
                transactionTemplate.executeWithoutResult(status -> {
                    entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PKG_SECURITY.CLEAR_CONTEXT; END;").executeUpdate();
                });
            }
        }
    }
}