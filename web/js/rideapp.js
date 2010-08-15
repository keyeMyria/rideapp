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
    }

    var garminControl;

    function initGarmin() {
        garminControl = new Garmin.DeviceControl();
        if (!garminControl.isPluginInitialized())
            return;
        $("#nogarmin").addClass("hidden");
        $("#garmin").removeClass("hidden");
        $("#garminUpload").click(garminUpload);
    }

    function garminUpload() {
        $("#garminReady").addClass("hidden");
        $("#garminBusy").removeClass("hidden");
    }

    function setUploadStatus(status) {
        if (status != "ok")
            alert("There was an error uploading " + $("#uploadFile").val());
        $("#uploadFile").val(null);
        $("#uploadSubmit").addClass("hidden");
        $("#uploadBusy").addClass("hidden");
        $("#uploadReady").removeClass("hidden");
        $("#uploadFile").val(null);
        $("#upload").dequeue("update");
    }

    function refreshSessionId(newSessionId) {
        sessionId = newSessionId;
        $("#body").dequeue("refreshSessionId");
    }

    return {
        setUploadStatus:setUploadStatus,
        refreshSessionId:refreshSessionId,
        init:init
    };
})(jQuery)
