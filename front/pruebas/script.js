window.toggleSidebar = function() {
    const sidebar = document.getElementById('fc-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    // Alternamos la clase 'active' para encender y apagar
    if (sidebar && overlay) {
        sidebar.classList.toggle('active');
        overlay.classList.toggle('active');
    }
};