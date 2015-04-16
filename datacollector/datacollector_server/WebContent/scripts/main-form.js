$(function() {
	initControls();
});

function initControls() {	
	// init Login panel
	$("#login_button").button();
	initLoginForm();
	$("#logout_button").button().click(logoutClick);
	// init Dictionaries upload
	$("#upload_dicts_button").button();
	initUploadForm($("#upload_dicts_form"), $("#upload_dicts_file"));
	
	// Refresh state
	disableControls();
	refreshState(100);
}

function disableControls() {
	$("#upload_dicts_file").prop("disabled", true);
	$("#upload_dicts_button").button("option", "disabled", true);
}

function enableControls() {
	$("#upload_dicts_file").prop("disabled", false);
	$("#upload_dicts_button").button("option", "disabled", false);
}

function refreshState(delay) {
	setTimeout(function() {
		$.getJSON("https://" + getBaseUrlNoProtocol() + "/unchecked/checkLoggedIn", afterCheckLoggedIn);
	}, delay);
}

function afterCheckLoggedIn(result) {
	// console.log("afterCheckLoggedIn result:");
	// console.log(result);
	if (result) {
		if (result.userLoggedIn) {
			$("#login_panel").hide();
			$("#logout_panel").show();
			$("#login_info_user").text("В системе [" + result.userName + "]");
			$("#login_form input[name='j_username']").val("");
			$("#login_form input[name='j_password']").val("");
			enableControls();
		} else {
			$("#login_panel").show();
			$("#logout_panel").hide();
			$("#login_info_user").text("Войдите в систему");
			$("#login_form input[name='j_username']").val("");
			$("#login_form input[name='j_password']").val("");
			disableControls();
		}
	}
}

function initLoginForm() {
	var frm = $("#login_form");
    frm.submit(function (ev) {
        $.ajax({
            type: frm.attr("method"),
            url: frm.attr("action"),
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

function initUploadForm(uploadForm, uploadFileInput) {
	uploadForm.submit(function (ev) {
		$("#status_label").text("начинается загрузка файла");
		opt = {
			type: uploadForm.attr("method"),
            url: uploadForm.attr("action"),
            success: function(response) {
            	uploadFileInput.val("");
            	$("#status_label").text(response);
            },
            error: function(error) {
            	uploadFileInput.val("");
            	$("#status_label").text(error);
            }
		}
        uploadForm.ajaxSubmit(opt);

        ev.preventDefault();
    });
}