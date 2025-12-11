$(document).ready(function() {
    const orgSelect = $('#select_organization');
    $.ajax({
        url: `${globalThis.API_URL}/organizations`,
        type: "GET",
        dataType: "json",
        success: function (data) {
            data.forEach(function (org) {
                $('<option>')
                    .val(org.name)
                    .text(org.name)
                    .appendTo(orgSelect);
            });
            orgSelect.select2({
                placeholder: 'Select an organization',
                allowClear: true,
                minimumResultsForSearch: 0,
                dropdownParent: orgSelect.closest('.dropdown-menu'),
                width: '100%'
            });
            orgSelect.on('select2:select', function (e) {
                const selectedData = e.params.data;
                localStorage.setItem('organizationId', selectedData.id);
                globalThis.location.reload();
            });
        }
    });
    $('.submenu-menu').on('mousedown click', function(e) {
        e.stopPropagation();
    });
    $('.dropdown-submenu > .dropdown-toggle').on('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        const submenu = $(this).next('.submenu-menu');
        $('.submenu-menu.show').not(submenu).removeClass('show');
        submenu.toggleClass('show');
    });
    $('.dropdown').on('hide.bs.dropdown', function () {
        $(this).find('.submenu-menu.show').removeClass('show');
    });
});
