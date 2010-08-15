<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>rideapp</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/rideapp.css"></link>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js" type="text/javascript"></script>
<script type="text/javascript">jQuery.noConflict()</script>

<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/prototype/prototype.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/util/Util-Broadcaster.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/util/Util-BrowserDetect.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/util/Util-DateTimeFormat.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/util/Util-PluginDetect.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/util/Util-XmlConverter.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/device/GarminObjectGenerator.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/device/GarminPluginUtils.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/device/GarminDevice.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/device/GarminDevicePlugin.js"></script>
<script type="text/javascript" src="http://developer.garmin.com/web/communicator-api-1.7/garmin/device/GarminDeviceControl.js"></script> 

<script src="${pageContext.request.contextPath}/js/rideapp.js" type="text/javascript"></script>

</head>
<body id="body" onload="rideapp.init('${pageContext.request.contextPath}','${sessionId}', ${garminUnlock})">
<iframe id="iframe-hidden" class="iframe-hidden"></iframe>

<span style="float:right">
  <span id="upload" class="widget">
    <form id="uploadForm" target="iframe-hidden"
          action="${pageContext.request.contextPath}/rest/upload;jsessionid=${sessionId}"
          method="POST" enctype="multipart/form-data">Upload gpx file:
      <span id="uploadBusy" class="hidden">
        <img src="${pageContext.request.contextPath}/img/working.gif"/>
        uploading file...
      </span>
      <span id="uploadReady">
        <input id="uploadFile" type="file" name="file"/>
        <span id="uploadSubmit" class="hidden">
          <input type="submit" value="Upload"/>
          <input type="button" id="uploadCancel" value="Cancel"/>
        </span>
      </span>
    </form>
  </span>
  <br/>
  <span class="widget">
    <span id="nogarmin">
      Download and install the <a href="http://www.garmin.com/products/communicator/">Garmin Communicator Plugin<a>
    </span>
    <span id="garmin" class="hidden">
      <span id="garminReady">
        Upload track from your Garmin:
        <input id="garminUpload" type="button" value="Upload"/>
      </span>
      <span id="garminBusy" class="hidden">
        <img src="${pageContext.request.contextPath}/img/working.gif"/>
        <span id="garminBusyStatus"></span>
      </span>
      <div class="fineprint">
        <a href="http://www.garmin.com/products/communicator/">Garmin Communicator Plugin<a> provided by <a href="http://www.garmin.com/">Garmin</a>.
      </div>
    </span>
  </span>
</span>

<div id="1"></div>
sessionId=${sessionId}
user.id=${sessionScope.user.id}
user.name=${sessionScope.user.name}
</body>
</html>
