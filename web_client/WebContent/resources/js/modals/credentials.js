/* Block current page on button click */
$('#credentialsTestButton').on('click', function() {
    console.log("Credentials Test Clicked.");

    /*******Put your ajax call here**********/
    
    $.blockUI({ 
        baseZ: 2000,
        message: '<i class="icon-spinner4 spinner"></i>',
        overlayCSS: {
            backgroundColor: '#1b2024',
            opacity: 0.8,
            cursor: 'wait'
        },
        css: {
            border: 0,
            color: '#fff',
            padding: 0,
            backgroundColor: 'transparent'
        }
    });
});