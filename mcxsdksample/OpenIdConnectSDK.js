if (typeof console == "undefined" || typeof console.log == "undefined") var console = { log: function() {} };
/**
 * @constructor
 * @param display {?String} (page, popup, touch or wap)
 * @param ui_locales {?String}
 * @param claims_locales {?String}
 * @param id_token_hint {?String}
 * @param login_hint {?String}
 * @param dtbs {?String}
 */
function AuthorizationOptions(display, ui_locales, claims_locales, id_token_hint, login_hint, dtbs) {
	if(display)
		display = display.toLowerCase();
	if(display!='page' && display!='popup' && display!='touch' && display!='wap')
		throw (new callApiConnectException("AuthorizationOptions create error", "Bad format: display is not 'page', 'popup', 'touch' or 'wap'"));
 	if(!!display)
		this['display'] = display;
	if(!!ui_locales)
		this['ui_locales'] = ui_locales;
	if(!!claims_locales)
		this['claims_locales'] = claims_locales;
	if(!!id_token_hint)
		this['id_token_hint'] = id_token_hint;
	if(!!login_hint)
		this['login_hint'] = login_hint;
	if(!!dtbs)
		this['dtbs'] = dtbs;	
}
/**
 * AuthorizationOptions get
 * @param name
 * @returns {?String}
 */
AuthorizationOptions.prototype.get = function get(name) {
	return(this[name]);
};

AuthorizationOptions.prototype.set = function set(name, object) {
	this[name] = object;
};

/**
 * isAString
 * @access private
 * @param obj {Object | string}
 * @returns {Boolean} 
 */
function isAString(obj) {		
	return((typeof(obj)==='string')||(typeof(obj)==="String"));
}
/**
 * isANumber
 * @access private
 * @param obj {Object | number}
 * @returns {Boolean}
 */
function isANumber(obj) {		
	return((typeof(obj)==='number')||(typeof(obj)==="Number"));
}

function addParameter(root, name, value) {
    var resp=root;
    if (name && value && name!=null && value!=null) {
        if (resp.indexOf('?')>=0) {
            resp+='&';
        } else {
            resp+='?';
        }
        resp=resp+encodeURI(name)+'='+encodeURI(value);
    }
    return resp;
}

/**
 * Obtain Authorization Code
 * @param url {String}
 * @param client_id {String}
 * @param scope {String}
 * @param redirect_uri {String}
 * @param response_type {String}
 * @param state {String}
 * @param nonce {String}
 * @param prompt {String} (none, login, consent or select_account)
 * @param max_age {Number}
 * @param acer_values {String}
 * @param authorizationOptions {AuthorizationOptions}
 * @param callbackFunction {Function}
 */
function authorize(url, client_id, scope, redirect_uri, response_type, state, nonce, prompt, max_age, acr_values, authorizationOptions, callbackFunction) {
	var parameters = url;
	if(!!client_id)
		parameters=addParameter(parameters, 'client_id', client_id);
	if(!!scope)
		parameters=addParameter(parameters, 'scope', scope);
	if(!!redirect_uri)	
		parameters=addParameter(parameters, 'redirect_uri', redirect_uri);
    if (!response_type || response_type==null)
        response_type='code';
	parameters=addParameter(parameters, 'response_type', response_type);
	if(!!state)
		parameters=addParameter(parameters, 'state', state);
	if(!!nonce)
		parameters=addParameter(parameters, 'nonce', nonce);
    if (!prompt || prompt==null)
        prompt='none';
	prompt = prompt.toLowerCase();
	if(prompt!='none' && prompt!='login' && prompt!='consent' &&  prompt!='select_account')
		throw (new callApiConnectException("AuthorizationOptions request error", "Bad format: prompt is not 'login', 'none', 'consent' or 'select_account'"));
	parameters=addParameter(parameters, 'prompt', prompt);
	
	if(!!max_age)	
		parameters=addParameter(parameters, 'max_age', max_age);
	if(!!acr_values)
		parameters=addParameter(parameters, 'acr_values', acr_values);

    //authorizationOptions
	if(!!authorizationOptions){
		if(!!authorizationOptions.get('display'))
			parameters=addParameter(parameters, 'display', authorizationOptions.get('display'));
		if(!!authorizationOptions.get('ui_locales'))
			parameters=addParameter(parameters, 'ui_locales', authorizationOptions.get('ui_locales'));
		if(!!authorizationOptions.get('claims_locales'))
			parameters=addParameter(parameters, 'claims_locales', authorizationOptions.get('claims_locales'));
		if(!!authorizationOptions.get('id_token_hint'))
			parameters=addParameter(parameters, 'id_token_hint', authorizationOptions.get('id_token_hint'));
		if(!!authorizationOptions.get('login_hint'))
			parameters=addParameter(parameters, 'login_hint', authorizationOptions.get('login_hint'));
		if(!!authorizationOptions.get('dtbs'))
			parameters=addParameter(parameters, 'dtbs', authorizationOptions.get('dtbs'));
	}
	//HTTP request
	loginOpenId(parameters, function(data){
		callbackFunction(data);
	});
}

function loginOpenId(url,callbackFunction){
	var ancho = 400;
	var alto = 400;
	var posicion_x; 
	var posicion_y; 
	posicion_x=(screen.width/2)-(ancho/2); 
	posicion_y=(screen.height/2)-(alto/2); 
	var aux = window.open(url, "loginOpenId", "width="+ancho+",height="+alto+",menubar=0,toolbar=0,directories=0,scrollbars=no,resizable=no,left="+posicion_x+",top="+posicion_y+"");
	var eventListener = function(m){
		console.log("RECEIVED: "+m.data);
		window.clearInterval(interval);
		removeEventListener("message", eventListener);
		var result = -1;
		if(m.data.indexOf("?")>=0){
			var urlToParse = m.data.substr(m.data.indexOf("?")+1, m.data.length);
			result = getJsonFromUrl(urlToParse);
		}
		aux.close();		
		callbackFunction(result);
	};
	addEventListener("message", eventListener,false);
	var interval = setInterval(function(){
		if(!aux.closed){
			aux.postMessage(window.location.href, "*");
		}else{
			window.clearInterval(interval);
			callbackFunction(-1);
		}
	}, 1000);	
}
function callApiConnectException(name, description) {
	this.name = name;
	this.description = description;
}

function parseResponseData(){
	var params = {};
	var href = location.href;
    // parse out the initial scheme prefix
    if (href.indexOf("://")>=0) {
        href=href.substr(href.indexOf("://")+3);
    }
    // strip out everything up to the start of the query params
    if (href.indexOf('?')>=0) {
        href=href.substr(href.indexOf("?")+1);
    }
    // parse into key value pairs
	regex = /([^&=]+)=([^&]*)/g;
	while (url = regex.exec(href)) {
	  params[decodeURIComponent(url[1])] = decodeURIComponent(url[2]);
	}
	return(params);	
}

/**
 * tokenFromAuthorizationCode
 * Request access token from authorization code
 * @param url {String} 
 * @param code {String} The authorisation code received from the authorisation server, from the authorisation request
 * @param client_id {String}
 * @param client_secret {String}
 * @param redirect_uri {String} The redirect_uri value MUST match the one sent in the authorisation request
 * @param callbackFunction {Function} 
 */
function tokenFromAuthorizationCode(url, code, client_id, client_secret, redirect_uri, callbackFunction){
	data = 'grant_type=authorization_code';
	if(!!code)
        data = data+'&code='+encodeURI(code);
	if(!!redirect_uri)
        data = data+'&redirect_uri='+encodeURI(redirect_uri);
	//HTTP request
	var xhr = new XMLHttpRequest();
    
    creds=((client_id && client_id!=null)?client_id:'')+':'+((client_secret && client_secret!=null)?client_secret:'');
    authorization='Basic '+Base64.encode(creds);
    
	xhr.open('POST', url, true);
	xhr.onreadystatechange = function() {
		if (xhr.readyState === 4) { //DONE
			var result = xhr.responseText;
			if(xhr.status===200){
				result = JSON.parse(result);
			}else{
				console.log(xhr.status+" --- "+xhr.responseText);
				if(xhr.responseText!='')
					result = JSON.parse(xhr.responseText);
				else
					result = { "error":"0" , "error_description":"no description received" };
			}

			var isMobile = false; //initiate as false
			// device detection
			if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|ipad|iris|kindle|Android|Silk|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(navigator.userAgent) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(navigator.userAgent.substr(0,4))) isMobile = true;


            var decoded = jwt_decode(result.id_token);
    		var pcr = decoded.sub;
    		var pcr_encoded = Base64.encode(pcr);
    		var pcrCookie = getCookie("pcr_encoded");

    		console.log("pcr : "+pcr);
    		console.log("isMobile : "+ isMobile);    		
			console.log("pcr_encoded : "+pcr_encoded);			

			if (pcrCookie == null && isMobile == true) {
		       document.cookie = "pcr_encoded="+pcr_encoded;
		    }		    			
			callbackFunction(result);			
		}
	};
	xhr.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	xhr.setRequestHeader('Accept','application/json');
	xhr.setRequestHeader("Authorization", authorization);
	xhr.send(data);	
}

function getCookie(name) {
	 var value = "; " + document.cookie;
	 var parts = value.split("; " + name + "=");
	 if (parts.length == 2) return parts.pop().split(";").shift();
}

function tokenResponse (access_token, token_type, id_token, expires_in, refresh_token){
	this[access_token] = access_token;
	this[token_type] = token_type;
	this[id_token] = id_token;
	this[expires_in] = expires_in;
	this[refresh_token] = refresh_token;
}

tokenResponse.prototype.get = function (name){
	return(this[name]);
};

function generateTokenResponse (json){
	var token = new tokenResponse (json.access_token, json.token_type, json.id_token, json.expires_in, json.refresh_token);
	return (token);
}

/**
 * Checks if the access token is still valid based on current time and expires_in value.
 * @param expires_in
 * @returns {Boolean}
 */
function isAccessTokenValid (expires_in){
	var d = new Date();
	return( d.getTime() <= expires_in );
}

/**
 * Refreshing a token
 * @param url {String}
 * @param refresh_token {String}
 * @param scope {String}
 * @param client_id {String}
 * @param client_secret {String}
 * @param callbackFunction {Function}
 */
function refreshToken(url, refresh_token, scope, client_id, client_secret, callbackFunction) {
    data = 'grant_type=refresh_token';
    if(!!refresh_token)
        data = data+'&refresh_token='+encodeURI(refresh_token);
    if(!!scope)
        data = data+'&scope='+encodeURI(scope);

    creds=((client_id && client_id!=null)?client_id:'')+':'+((client_secret && client_secret!=null)?client_secret:'');
    authorization='Basic '+Base64.encode(creds);
    
	//HTTP request
	var xhr = new XMLHttpRequest();
	xhr.open('POST', url, true);
	xhr.onreadystatechange = function() {
		if (xhr.readyState === 4) { //DONE
			var result = xhr.responseText;
			if(xhr.status===200){
				result = JSON.parse(result);
			}else{
				console.log(xhr.status+" --- "+result);
				if(xhr.responseText!='')
					result = JSON.parse(xhr.responseText);
				else
					result = { "error":"0" , "error_description":"no description received" };
			}
			callbackFunction(result);
		}
	};
	xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	xhr.setRequestHeader('Accept','application/json');
    xhr.setRequestHeader("Authorization", authorization);
	xhr.send(data);
}

/**
 * It request revocation of an access token at any time. 
 * @param url {String}
 * @param access_token {String}
 * @param client_id {String}
 * @param client_secret {?String}
 */
function revokeToken(url, access_token, client_id, client_secret) {
    var parameters = url;
    if(!!client_id)
        parameters=addParameter(parameters, 'token', token);

    creds=((client_id && client_id!=null)?client_id:'')+':'+((client_secret && client_secret!=null)?client_secret:'');
    authorization='Basic '+Base64.encode(creds);

	//HTTP request
	var xhr = new XMLHttpRequest();
	xhr.open('GET', parameters, true);
	xhr.onreadystatechange = function() {
		if (xhr.readyState === 4) { //DONE
			var result = xhr.responseText;
			if(xhr.status===200){
				result = JSON.parse(result);
			}else{
				console.log(xhr.status+" --- "+result);
				if(xhr.responseText!='')
					result = JSON.parse(xhr.responseText);
				else
					result = { "error":"0" , "error_description":"no description received" };
			}
			callbackFunction(result);
		}
	};
	xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	xhr.setRequestHeader('Accept','application/json');
    xhr.setRequestHeader("Authorization", authorization);
	xhr.send(parameters);
}

/**
 * It handles the revokation process and if successful marking the old access token and any associated refresh token as revoked.
 * @param url {String}
 * @param client_id {String}
 * @param client_secret {String}
 */
//function revokeToken(client_id, client_secret) {
//	var parameters = "";
//	parameters += "&scope=revoke";
//	if(!!client_id)
//		parameters += "&client_id=" + client_id;
//	if(!!client_secret)
//		parameters += "&client_secret=" + client_secret;
//	parameters += "&grant_type=access_token";
//	//Remove first & 
//	if(parameters!="")
//		parameters = parameters.substr(1,parameters.length);
//	//HTTP request
//	var xhr = new XMLHttpRequest();
//	xhr.open('POST', url, true);
//	xhr.onreadystatechange = function() {
//		if (xhr.readyState === 4) { //DONE
//			var result = xhr.responseText;
//			if(xhr.status===200){
//				result = JSON.parse(result);
//			}else{
//				console.log(xhr.status+" --- "+result);
//				if(xhr.responseText!='')
//					result = JSON.parse(xhr.responseText);
//				else
//					result = { "error":"0" , "error_description":"no description received" };
//			}
//			callbackFunction(result);
//		}
//	};
//	xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
//	xhr.setRequestHeader('Accept','application/json');
//	xhr.send(parameters);
//}

/**
 * Obtain userinfo using access token
 * @param url {String}
 * @param access_token {String}
 * @param callbackFunction {Function}
 */
function userinfo(url, access_token, callbackFunction){
	var parameters = "";
	var xhr = new XMLHttpRequest();
	xhr.open('GET', url+parameters, true);
	xhr.onreadystatechange = function() {
		if (xhr.readyState === 4) { //DONE
			var result = xhr.responseText;
			if(xhr.status===200){
                result = JSON.parse(xhr.responseText);
			}else{
				console.log(xhr.status+" --- "+xhr.responseText);
				if(xhr.responseText!='')
					result = JSON.parse(xhr.responseText);
				else
					result = { "error":"0" , "error_description":"no description received" };
			}
			callbackFunction(result);
		}
	};
	xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	xhr.setRequestHeader('Accept','application/json');
	xhr.setRequestHeader('Authorization',"Bearer "+access_token);
	xhr.send();
}

function getJsonFromUrl(query) {
	  var data = query.split("&");
	  var result = {};
	  for(var i=0; i<data.length; i++) {
	    var item = data[i].split("=");
	    result[item[0]] = item[1];
	  }
	  return result;
}

/**
 * helperRedirectOpenIdAuthorize
 * get from redirected url and sent to SDK
 */
function helperRedirectOpenIdAuthorize() {
	if(!!window.opener && !window.opener.closed){
		addEventListener("message",function(m){
			console.log("_______ "+window.location.href);
			console.log("_______ "+m.data);
    		window.opener.postMessage(window.location.href, m.data);
		},false);
	}
}