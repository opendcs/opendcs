// resources/js/layout.js
(function () {
    function setNavbarOffset() {
        var nav = document.querySelector('nav.navbar.fixed-top');
        if (!nav) return;

        // offsetHeight includes borders, but add a safety pixel for sub-pixel rounding at zoom
        var rect = nav.getBoundingClientRect();
        document.documentElement.style.setProperty('--navbar-height', rect.height + 'px');
        document.body.classList.add('navbar-top');
    }

    function initDropdowns() {
        if (!(window.bootstrap && window.bootstrap.Dropdown)) {
            console.warn('Bootstrap Dropdown not found. Ensure bootstrap.bundle.min.js is loaded before app scripts.');
            return;
        }

        // Create instances so programmatic control works later,
        document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(function (el) {
            bootstrap.Dropdown.getOrCreateInstance(el, {
                autoClose: true,
                popperConfig: { strategy: 'fixed' }
            });
        });

        const orgId = localStorage.getItem('organizationId');   // or from server via data-* attr
        if (orgId) {
            document.getElementById('organization_id').textContent = orgId;
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        setNavbarOffset();
        initDropdowns();

        // Recompute on resize & after fonts load
        window.addEventListener('resize', setNavbarOffset);
        if (document.fonts && document.fonts.ready) document.fonts.ready.then(setNavbarOffset);
    });
})();
