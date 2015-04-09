$(function() {
	initControls();
});

function initControls() {
	$("#authorize_panel").css({
		"width": "300px",
		"float": "left"
	});
	$("#content_panel").css({
		"margin-left": "300px"
	});
	refreshState(100);
	$("#login_button").button();
	initLoginForm();
	$("#logout_button").button().click(logoutClick);
}

function initLoginForm() {
	var frm = $('#login_form');
    frm.submit(function (ev) {
        $.ajax({
            type: frm.attr('method'),
            url: frm.attr('action'),
            data: frm.serialize(),
            success: function() {
            	refreshState(500);
            },
            error: function() {
            	refreshState(500);
            }
        });

        ev.preventDefault();
    });
}

function logoutClick() {
	$.ajax({
		url: "https://" + getBaseUrlNoProtocol() + "/secure/logout",
		success: function() {
			// reload page, start new session
			window.location = "https://" + getBaseUrlNoProtocol() + "/secure/main-form.html";
		}
	});
}

function refreshState(delay) {
	setTimeout(function() {
		$.getJSON("https://" + getBaseUrlNoProtocol() + "/unchecked/checkLoggedIn", afterCheckLoggedIn);
	}, delay);
}

function afterCheckLoggedIn(result) {
	console.log("afterCheckLoggedIn result:");
	console.log(result);
	if (result) {
		if (result.userLoggedIn) {
			$("#login_panel").hide();
			$("#logout_panel").show();
			$("#login_info_user").text("User name: [" + result.userName + "]");
			$("#login_form input[name='j_username']").val("");
			$("#login_form input[name='j_password']").val("");
		} else {
			$("#login_panel").show();
			$("#logout_panel").hide();
			$("#login_info_user").text("Not logged in");
			$("#login_form input[name='j_username']").val("");
			$("#login_form input[name='j_password']").val("");
		}
	}
}