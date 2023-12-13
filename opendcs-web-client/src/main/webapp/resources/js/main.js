document.addEventListener("DOMContentLoaded", function(event) {
	console.log("loaded main.js.");
	
	//This runs the logout functionality.  It is in main.js, which is loaded 
	//into any page.
	$("#logoutButton").on("click", function() {
        sessionStorage.removeItem("token");
        window.location = "login";
    });
	
	var token = sessionStorage.getItem("token");
	if (token != null)
	{
		$.ajaxSetup({
		    beforeSend: function(xhr) {
		    	xhr.setRequestHeader('Authorization', 
		    			`${token}`);
		    }
		});
	}
});