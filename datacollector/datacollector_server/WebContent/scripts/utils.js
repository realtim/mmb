function getBaseUrlNoProtocol() {
	var currUrl = window.location;
	var result = currUrl.host + "/" + currUrl.pathname.split('/')[1];
	return result;
}