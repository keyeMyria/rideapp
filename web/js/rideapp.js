var rideapp = (function($) {
    var contextPath;
    var sessionId;
    var garminUnlock;

    function getURI(path) {
        return contextPath + path + ";jsessionid=" + sessionId;
    }

    function init(initContextPath, initSessionId, initGarminUnlock) {
        contextPath = initContextPath;
        sessionId = initSessionId;
        garminUnlock = initGarminUnlock;

        initUploadFile();
        initGarmin();

        $.getJSON(getURI("/rest/friends"), function(data) {
            var ul = document.createElement("ul");
            $("#1").append(ul);
            for (var i = 0; i < data.length; i++) {
                var li = document.createElement("li");
                $(ul).append(li);
                $(li).text(data[i].name);
            }
        });
    }

    function initUploadFile() {
        $("#uploadFile").change(function() {
            $("#upload").attr("action",getURI("/upload"));
            $("#uploadSubmit").removeClass("hidden");
        });
        $("#uploadForm").submit(function() {
            $("#uploadBusy").removeClass("hidden");
            $("#uploadReady").addClass("hidden");
        });
        $("#uploadCancel").click(function() {
            $("#uploadFile").val(null);
            $("#uploadSubmit").addClass("hidden");
            $("#uploadBusy").addClass("hidden");
            $("#uploadReady").removeClass("hidden");
        });
        $("#uploadStatusButton").click(function() {
            $("#uploadStatus").addClass("hidden");
            $("#uploadReady").removeClass("hidden");
        });
    }

    function setUploadStatus(status) {
        if (status == "ok")
            $("#uploadStatusMessage").text("Upload succeeded: " + $("#uploadFile").val());
        else
            $("#uploadStatusMessage").text("There was an error uploading " + $("#uploadFile").val());
        $("#uploadSubmit").addClass("hidden");
        $("#uploadBusy").addClass("hidden");
        $("#uploadStatus").removeClass("hidden");
        $("#uploadFile").val(null);
    }

    var garminControl;

    function initGarmin() {
        garminControl = new Garmin.DeviceControl();
        if (!garminControl.isPluginInitialized())
            return;
        garminControl.register({
            onFinishFindDevices:garminFindDevice,
            onProgressReadFromDevice:garminReadProgress,
            onFinishReadFromDevice:garminReadFinished
        });
        $("#garminNoPlugin").addClass("hidden");
        $("#garmin").removeClass("hidden");
        $("#garminUpload").click(garminUpload);
        $("#garminStatusButton").click(function() {
            $("#garminStatusMessage").text("");
            $("#garminStatus").addClass("hidden");
            $("#garminReady").removeClass("hidden");
        });
    }

    function garminUpload() {
        $("#garminReady").addClass("hidden");
        $("#garminBusy").removeClass("hidden");
        $("#garminBusyStatus").text("Unlocking Garmin Communicator Plugin...");
        if (!garminControl.unlock(garminUnlock))
            return garminStatus("Failed to unlock Garmin Communicator Plugin");
        $("#garminBusyStatus").text("Finding device...");
        garminControl.findDevices();
    }

    function garminFindDevice(data) {
        var devices = data.controller.getDevices();
        if (devices.length == 0)
            return garminStatus("No devices connected");
        if (devices.length > 1)
            return garminStatus("Multiple devices connected");
        data.controller.setDeviceNumber(0);
        $("#garminBusyStatus").text("Reading...");
        data.controller.readFromDevice();
    }

    function garminReadProgress(data) {
        $("#garminBusyStatus").text("Reading..." + Math.min(100,data.progress.getPercentage()) + "%");
    }

    function garminReadFinished(data) {
        if (!data.success)
            return garminStatus("Read from device failed");
        $("#garminBusyStatus").text("Uploading...");
        $.ajax({
            type:"POST",
            url:getURI("/rest/upload"),
            data:data.controller.gpsDataString,
            contentType:"application/gpx",
            dataType:"text",
            success:function(status) {
                garminStatus("Upload done: " + status);
            },
            error:function(xhr,status) {
                if (xhr.status != 403)
                    return garminStatus("Upload failed: " + xhr.statusText);
                $("#iframe-hidden").queue("refreshSessionId", function(next) {
                    garminReadFinished(data);
                    next();
                });
                $("#iframe-hidden").attr("src",getURI("/refreshSession.jsp"));
            }
        });
    }

    function garminStatus(message) {
        $("#garminBusyStatus").text("");
        $("#garminBusy").addClass("hidden");
        $("#garminStatusMessage").text(message);
        $("#garminStatus").removeClass("hidden");
    }

    function refreshSessionId(newSessionId) {
        sessionId = newSessionId;
        $("#iframe-hidden").dequeue("refreshSessionId");
    }

    return {
        setUploadStatus:setUploadStatus,
        refreshSessionId:refreshSessionId,
        init:init
    };
})(jQuery)
