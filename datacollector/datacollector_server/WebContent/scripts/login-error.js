$(function() {
	initControls();
});

function initControls() {
	var reloginBtn = $("#relogin_button").button();
	reloginBtn.click(reloginClick);
}

function reloginClick() {
	window.location = "https://" + getBaseUrlNoProtocol() + "/secure/main-form.html";
}