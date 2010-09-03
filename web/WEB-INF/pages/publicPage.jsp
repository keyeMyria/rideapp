<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>rideapp</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/rideapp.css"/>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js" type="text/javascript"></script>
<script type="text/javascript">jQuery.noConflict()</script>

<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>

<script type="text/javascript" src="http://connect.facebook.net/en_US/all.js"></script>

<script src="${pageContext.request.contextPath}/js/rideapp-util.js" type="text/javascript"></script>
<script src="${pageContext.request.contextPath}/js/rideapp.js" type="text/javascript"></script>

<script type="text/javascript">rideapp.initPublicPage2=function(){rideapp.initPublicPage("${pageContext.request.contextPath}", ${restapiInfo}, ${tracks});}</script>
</head>
<body id="body" onload="rideapp.initPublicPage2()">

<h1><a id="publicUser" target="_top"></a></h1>

<div id="map"></div>
<table id="mainContentTable" cellspacing="0">
  <tbody id="mainContent"></tbody>
</table>

<div id="fb-root" style="display:none;"></div>

</body>
</html>
