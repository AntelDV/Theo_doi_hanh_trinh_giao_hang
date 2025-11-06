/*
 * File main.js đã được TỐI GIẢN HÓA
 * Đã loại bỏ logic tự quản lý 'modal-backdrop' (nguyên nhân gây kẹt)
 * Giờ đây, CSS sẽ tự xử lý việc ẩn/hiện backdrop khi class 'show' được thêm/xóa.
 */
document.addEventListener('DOMContentLoaded', function() {
    
    // 1. Dropdown Menu cho User Info (Logic này đã đúng)
    const userInfoDropdown = document.getElementById('userInfoDropdown');
    const userDropdownMenu = userInfoDropdown ? userInfoDropdown.querySelector('.dropdown-menu') : null;

    if (userInfoDropdown && userDropdownMenu) {
        // Lấy trigger là toàn bộ div user-info
        const trigger = userInfoDropdown; 

        trigger.addEventListener('click', function(event) {
            userDropdownMenu.classList.toggle('show');
            event.stopPropagation(); // Ngăn sự kiện click lan ra window
        });
    }

    // Đóng dropdown khi click ra ngoài (Logic này đã đúng)
    window.addEventListener('click', function(event) {
        if (userDropdownMenu && userDropdownMenu.classList.contains('show')) {
            // Kiểm tra xem click có nằm trong user-info không
            if (!userInfoDropdown.contains(event.target)) {
                userDropdownMenu.classList.remove('show');
            }
        }
    });

    // 2. Xử lý đóng Modal cơ bản (SỬA LẠI LOGIC)
    const closeButtons = document.querySelectorAll('[data-dismiss="modal"]');
    closeButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Tìm modal cha gần nhất và chỉ cần xóa class 'show'
            const modal = this.closest('.modal');
            if (modal) {
                modal.classList.remove('show');
                // (Đã xóa code cũ quản lý backdrop)
            }
        });
    });
});

/**
 * Hàm để mở modal (SỬA LẠI LOGIC)
 * @param {string} modalId - ID của modal cần mở
 */
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
         // (Đã xóa code cũ quản lý backdrop)
    }
}