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

<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>

<script src="${pageContext.request.contextPath}/js/rideapp.js" type="text/javascript"></script>

</head>
<body id="body" onload="rideapp.init('${pageContext.request.contextPath}','${sessionId}', ${garminUnlock})">
<iframe id="iframe-hidden" style="position:absolute; top:0px; left:0px; width: 0px; height: 0px; border: none;"></iframe>

<span style="float:right">
  <span class="widget" title="Record">
    <form id="uploadForm" target="iframe-hidden"
          action="${pageContext.request.contextPath}/rest/upload;jsessionid=${sessionId}"
          method="POST" enctype="multipart/form-data">Upload gpx file:
      <span id="uploadBusy" style="display:none;">
        <img src="${pageContext.request.contextPath}/img/working.gif"/>
        uploading file...
      </span>
      <span id="uploadStatus" style="display:none;">
        <span id="uploadStatusMessage"></span>
        <input id="uploadStatusButton" type="button" value="Continue"/>
      </span>
      <span id="uploadReady">
        <input id="uploadFile" type="file" name="file"/>
        <span id="uploadSubmit" style="display:none;">
          <input type="submit" value="Upload"/>
          <input type="button" id="uploadCancel" value="Cancel"/>
        </span>
      </span>
    </form>
  </span>
  <br clear="right"/>
  <span class="widget" title="Record">
    <span id="garminNoPlugin">
      Download and install the <a href="http://www.garmin.com/products/communicator/">Garmin Communicator Plugin</a> to upload tracks from your Garmin device.
    </span>
    <span id="garmin" style="display:none;">
      <span id="garminReady">
        Upload track from your Garmin device:
        <input id="garminUpload" type="button" value="Upload"/>
      </span>
      <span id="garminBusy" style="display:none;">
        <img src="${pageContext.request.contextPath}/img/working.gif"/>
        <span id="garminBusyStatus"></span>
      </span>
      <span id="garminStatus" style="display:none;">
        <span id="garminStatusMessage"></span>
        <input id="garminStatusButton" type="button" value="Continue"/>
      </span>
      <br/>
      <div class="fineprint">
        <a href="http://www.garmin.com/products/communicator/">Garmin Communicator Plugin</a> provided by <a href="http://www.garmin.com/">Garmin</a>.
      </div>
    </span>
  </span>
  <br clear="right"/>
  <span class="widget" title="105">
    <span id="tracksNoTracks">You have no uploaded tracks.</span>
    <span id="tracks" style="display:none;">
      <table style="width:100%;" cellspacing="0">
        <tbody><tr><td colspan="2">Your tracks:</td></tr></tbody>
        <tbody id="trackList" class="chooseList"></tbody>
      </table>
    </span>
  </span>
  <br clear="right"/>
  <span class="widget" title="105">
    <span id="coursesNoCourses">You have no courses.<br/></span>
    <span id="courses" style="display:none;">
      <table style="width:100%;" cellspacing="0">
        <tbody><tr><td colspan="2">Your courses:</td></tr></tbody>
        <tbody id="courseList" class="chooseList"></tbody>
      </table>
    </span>
    <input id="courseAddCourse" type="button" value="Add course" disabled="true"/>
    <img id="courseBusy" style="display:none;" src="${pageContext.request.contextPath}/img/working.gif"/>
  </span>
  <br clear="right"/>
  <span class="widget" title="Rival">
    <span id="rivalsNoRivals">You have no rivals.<br/></span>
    <span id="rivals" style="display:none;">
      <table style="width:100%;" cellspacing="0">
        <tbody><tr><td colspan="2">Your rivals:</td></tr></tbody>
        <tbody id="rivalList" class="chooseList"></tbody>
      </table>
    </span>
    <input id="rivalAddRival" type="button" value="Add rival" disabled="true"/>
    <img id="rivalBusy" style="display:none;" src="${pageContext.request.contextPath}/img/working.gif"/>
  </span>
</span>

<div id="map"></div>

<table id="chooseRival" style="display:none;" cellspacing="0" title="Rival">
  <tbody id="chooseRivalList" class="chooseList"></tbody>
  <tbody>
    <tr>
      <td>
        <span id="chooseRivalPrevDisabled">&lt;-prev</span>
        <a id="chooseRivalPrev" href="javascript:;">&lt;-prev</a>
      </td>
      <td align="center">
        <a id="chooseRivalCancel" href="javascript:;">cancel</a>
      </td>
      <td align="right">
        <span id="chooseRivalNextDisabled">next-&gt;</span>
        <a id="chooseRivalNext" href="javascript:;">next-&gt;</a>
      </td>
    </tr>
  </tbody>
</table>

<div id="makeCourse" style="display:none;">
  <div title="105">
    Make a course:
    <div class="instructions">Click on the map to add a checkpoint.  The
    first checkpoint is the start point.  Double click on a checkpoint
    to delete it.  Drag the checkpoint to move it.</div>
    Name: <input id="makeCourseName" type="text"/>
    Loop: <input id="makeCourseLoop" type="checkbox"/>
    <input id="makeCourseAdd" type="button" value="Add course" disabled="true"/>
    <input id="makeCourseCancel" type="button" value="Cancel"/>
  </div>
  <div id="makeCourseMap"></div>
</div>

</body>
</html>
