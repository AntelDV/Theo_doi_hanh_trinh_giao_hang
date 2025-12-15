document.addEventListener('DOMContentLoaded', function() {
    
    // Dropdown User
    const userInfo = document.getElementById('userInfoDropdown');
    if (userInfo) {
        userInfo.addEventListener('click', function(e) {
            e.stopPropagation();
            const menu = this.querySelector('.dropdown-menu');
            if (menu) menu.classList.toggle('show');
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function() {
        const menus = document.querySelectorAll('.dropdown-menu.show');
        menus.forEach(m => m.classList.remove('show'));
    });

    // Xử lý đóng Modal (Nút X hoặc nút Hủy)
    const closeButtons = document.querySelectorAll('.close, .btn-secondary[data-dismiss="modal"]');
    closeButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            if (modal) closeModal(modal.id);
        });
    });

    // Đóng modal khi click ra ngoài vùng content
    window.addEventListener('click', function(e) {
        if (e.target.classList.contains('modal')) {
            closeModal(e.target.id);
        }
    });
});

// Hàm mở Modal (Gọi từ HTML onclick="openModal('id')")
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
    } else {
        console.error('Không tìm thấy modal có ID:', modalId);
    }
}

// Hàm đóng Modal
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
}