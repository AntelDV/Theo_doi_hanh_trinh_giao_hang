document.addEventListener('DOMContentLoaded', function() {
    
    // 1. Dropdown Menu cho User Info
    const userInfoDropdown = document.getElementById('userInfoDropdown');
    const userDropdownMenu = userInfoDropdown ? userInfoDropdown.querySelector('.dropdown-menu') : null;

    if (userInfoDropdown && userDropdownMenu) {
        // Lấy trigger là toàn bộ div user-info hoặc chỉ span username tùy thiết kế
        const trigger = userInfoDropdown; // Hoặc userInfoDropdown.querySelector('.username-trigger'); nếu bạn tạo riêng

        trigger.addEventListener('click', function(event) {
            userDropdownMenu.classList.toggle('show');
            event.stopPropagation(); // Ngăn sự kiện click lan ra window
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function(event) {
        if (userDropdownMenu && userDropdownMenu.classList.contains('show')) {
            // Kiểm tra xem click có nằm trong user-info không
            if (!userInfoDropdown.contains(event.target)) {
                userDropdownMenu.classList.remove('show');
            }
        }
    });

    // 2. Xử lý đóng Modal cơ bản (Thêm sau nếu cần)
    const closeButtons = document.querySelectorAll('[data-dismiss="modal"]');
    closeButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Tìm modal cha gần nhất và ẩn nó
            const modal = this.closest('.modal');
            if (modal) {
                modal.classList.remove('show');
                // Optional: Ẩn cả backdrop nếu có
                const backdrop = document.querySelector('.modal-backdrop');
                if (backdrop) backdrop.style.display = 'none';
            }
        });
    });

    // Optional: Đóng modal khi click vào backdrop (Thêm sau nếu cần)
    // const backdrops = document.querySelectorAll('.modal-backdrop');
    // backdrops.forEach(backdrop => {
    //     backdrop.addEventListener('click', function() {
    //         this.style.display = 'none';
    //         const modal = document.querySelector('.modal.show'); // Tìm modal đang hiển thị
    //         if (modal) modal.classList.remove('show');
    //     });
    // });
});

// Hàm để mở modal 
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
        // Optional: Hiển thị backdrop nếu có
        const backdrop = document.querySelector('.modal-backdrop'); // Giả sử chỉ có 1 backdrop
        if (backdrop) backdrop.style.display = 'block';
    }
}