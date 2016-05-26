<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%  String st = request.getParameter("state");
    String cd = request.getParameter("code"); %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Callback</title>
  <script language="JavaScript" src="DiscoverySDK.js" type="text/javascript"></script>
  <script language="JavaScript" src="LogoSDK.js" type="text/javascript"></script>
  <script language="JavaScript" src="Base64Utils.js" type="text/javascript"></script>
  <script language="JavaScript" src="OpenIdConnectSDK.js" type="text/javascript"></script>
  <script language="JavaScript" src="jwt-decode.js" type="text/javascript"></script>
  <script language="JavaScript" src="gibberish-aes-1.0.0.js" type="text/javascript"></script>
  <script src="//code.jquery.com/jquery-1.11.0.min.js"></script>
</head>
<body>
    
<div>Status : <p id="status"></p></div>

<script language="JavaScript">
    
    runAuthorization();


    function runAuthorization() {
        

        var state =  "<%=st%>";
        var code  = "<%=cd%>";
        var redirect_uri="http://104.197.223.199:8080/demo/callback.jsp";
        var client_id="XjMkcrFwcC7QRIRxMmwo89u9yhga";
        var client_secret="DOY01WdfDYyBdpJQlBqlPlFOoGMa";
        var token_endpoint="https://identity.qa.example.com:443/oauth2/token";

        document.getElementById("status").innerHTML =  "Authorized";        
        discoveryDetails=getCacheDiscoveryItem();
        
        tokenFromAuthorizationCode(token_endpoint, code, client_id, client_secret, redirect_uri,tokenReceived);
    }

    function tokenReceived(token) {
        if (console.log) console.log("token response="+JSON.stringify(token));
        if (!!token.refresh_token) $('#refresh_token').val(token.refresh_token);
        if (!!token.expires_in) $('#expires_in').val(token.expires_in);
        if (!!token.token_type) $('#token_type').val(token.token_type);

        /*
        window.onunload = refreshParent;
        function refreshParent() {
            window.opener.location.reload();
        }
        window.close();
        */

        if (!!token.access_token) {
            $('#access_token').val(token.access_token);
            discoveryDetails=getCacheDiscoveryItem();
            userinfoEndpoint=discoveryDetails.getResponse().getApiFunction('operatorid', 'userinfo');
            $('#status').val('Authorized + access token retrieved');
            if (userinfoEndpoint && userinfoEndpoint.trim().length>0) {
                userinfo(userinfoEndpoint, token.access_token, userinfoCallbackFunction);
            }
        }


        
    }

    function userinfoCallbackFunction(userinfo) {
        if (console.log) console.log("userinfo response="+JSON.stringify(userinfo));
        if (!!userinfo.email) $('#email').val(userinfo.email);
        if (userinfo && userinfo.email_verified!==null) $('#email_verified').val(userinfo.email_verified?'true':'false');
    }

  </script>



</body>
</html>


