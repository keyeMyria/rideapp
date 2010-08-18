var rideapp = (function($) {
    var contextPath;
    var sessionId;
    var garminUnlock;

    var info;
    var map;
    var tracks = {};

    function getURI(path) {
        return contextPath + path + ";jsessionid=" + sessionId;
    }

    function ajax(method, path, success, error, data, contentType) {
        $.ajax({
            type:method,
            url:getURI(path),
            data:data,
            contentType:contentType,
            success:success,
            error:function(xhr,status) {
                if (xhr.status != 403) {
                    error(xhr,status);
                } else {
                    $("#iframe-hidden").queue("refreshSessionId", function(next) {
                        ajax(method, path, success, error, data, contentType);
                        next();
                    });
                }
            }
        });
    }

    function getJSON(path, success, error) {
        ajax("GET", path, success, error);
    }

    function formatTimestamp(timestamp) {
        return timestamp;
    }

    function init(initContextPath, initSessionId, initGarminUnlock) {
        contextPath = initContextPath;
        sessionId = initSessionId;
        garminUnlock = initGarminUnlock;

        initUploadFile();
        initGarmin();

        getJSON("/rest/info", function(newInfo) {
            info = newInfo;
            setTracks();
            setRivals();
            var options = { mapTypeId: google.maps.MapTypeId.ROADMAP };
            if (info.home) {
                options.zoom = 12;
                options.center = new google.maps.LatLng(info.home.lat, info.home.lon);
            } else {
                options.zoom = 3;
                options.center = new google.maps.LatLng(39,-98);
            }
            map = new google.maps.Map(document.getElementById("map"), options);
            $("#map").show();
        });
    }

    function removeMapOverlays() {
        for (var i = 0; i < info.tracks.length; i++)
            tracks[info.tracks[i]].overlay.setMap(null);
    }

    function initUploadFile() {
        $("#uploadFile").change(function() {
            $("#iframe-hidden").queue("refreshSessionId", function(next) {
                $("#upload").attr("action",getURI("/upload"));
                $("#uploadSubmit").show();
                next();
            });
            $("#iframe-hidden").attr("src",getURI("/refreshSession.jsp"));
        });
        $("#uploadForm").submit(function() {
            $("#uploadBusy").show();
            $("#uploadReady").hide();
        });
        $("#uploadCancel").click(function() {
            $("#uploadFile").val(null);
            $("#uploadSubmit").hide();
            $("#uploadBusy").hide();
            $("#uploadReady").show();
        });
        $("#uploadStatusButton").click(function() {
            $("#uploadStatus").hide();
            $("#uploadReady").show();
        });
    }

    function setUploadStatus(status) {
        if (status == "ok")
            $("#uploadStatusMessage").text("Upload succeeded: " + $("#uploadFile").val());
        else if (status == "tracktruncated")
            $("#uploadStatusMessage").text("Upload succeeded (truncated): " + $("#uploadFile").val());
        else
            $("#uploadStatusMessage").text("There was an error uploading " + $("#uploadFile").val());
        $("#uploadSubmit").hide();
        $("#uploadBusy").hide();
        $("#uploadStatus").show();
        $("#uploadFile").val(null);
        getJSON("/rest/tracks", function(tracks) {
            info.tracks = tracks;
            setTracks();
        });
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
        $("#garminNoPlugin").hide();
        $("#garmin").show();
        $("#garminUpload").click(garminUpload);
        $("#garminStatusButton").click(function() {
            $("#garminStatusMessage").text("");
            $("#garminStatus").hide();
            $("#garminReady").show();
        });
    }

    function garminUpload() {
        $("#garminReady").hide();
        $("#garminBusy").show();
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
        ajax("POST", "/rest/upload", function(status) {
            if (status == "ok")
                garminStatus("Upload succeeded");
            else if (status == "tracktruncated")
                garminStatus("Upload succeeded (truncated)");
            else
                garminStatus("Upload failed");
            getJSON("/rest/tracks", function(tracks) {
                info.tracks = tracks;
                setTracks();
            });
        }, function(xhr, status) {
            garminStatus("Upload failed: " + xhr.statusText);
        }, data.controller.gpsDataString, "application/gpx");
    }

    function garminStatus(message) {
        $("#garminBusyStatus").text("");
        $("#garminBusy").hide();
        $("#garminStatusMessage").text(message);
        $("#garminStatus").show();
    }

    function setTracks() {
        if (!info.tracks || info.tracks.length == 0) {
            $("#tracksNoTracks").show();
            $("#tracks").hide();
            return;
        }
        $("#tracksNoTracks").hide();
        $("#tracks").show();
        $("#trackList").empty();
        var trackToFetch = null;
        for (var i = 0; i < info.tracks.length; i++) {
            var tr = document.createElement("tr");
            $("#trackList").append(tr);
            var td = document.createElement("td");
            $(tr).append(td);
            if (tracks[info.tracks[i]]) {
                $(td).text(formatTimestamp(tracks[info.tracks[i]].pts[0].t));
                $(td).click((function(track) {
                    return function() {
                        removeMapOverlays();
                        track.overlay.setMap(map);
                        map.fitBounds(track.bounds);
                    };
                })(tracks[info.tracks[i]]));
            } else {
                var img = document.createElement("img");
                $(td).append(img);
                $(img).attr("src",contextPath+"/img/working.gif");
                if (!trackToFetch)
                    trackToFetch = info.tracks[i];
            }
            td = document.createElement("td");
            $(tr).append(td);
            $(td).addClass("chooseItem");
            var span = document.createElement("span");
            $(td).append(span);
            $(span).text("delete");
            $(span).click((function(index) {
                return function() {
                    ajax("DELETE", "/rest/track/"+info.tracks[index], function() {
                        tracks[info.tracks[index]].overlay.setMap(null);
                        tracks[info.tracks[index]] = null;
                        info.tracks.splice(index,1);
                        setTracks();
                    });
                };
            })(i));
        }
        if (trackToFetch) {
            getJSON("/rest/track/" + trackToFetch, (function(id) {
                return function(trackData) {
                    var pts = [];
                    var minLat, maxLat, minLon, maxLon;
                    minLat = trackData[0].lat;
                    maxLat = trackData[0].lat;
                    minLon = trackData[0].lon;
                    maxLon = trackData[0].lon;
                    for (var i = 0; i < trackData.length; i++) {
                        pts.push(new google.maps.LatLng(trackData[i].lat, trackData[i].lon));
                        minLat = Math.min(minLat, trackData[i].lat);
                        maxLat = Math.max(maxLat, trackData[i].lat);
                        minLon = Math.min(minLon, trackData[i].lon);
                        maxLon = Math.max(maxLon, trackData[i].lon);
                    }
                    tracks[id] = {
                        pts:trackData,
                        bounds:new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)),
                        overlay:new google.maps.Polyline({
                            path:pts,
                            strokeColor:"#FF00FF",
                            strokeOpacity:0.9,
                            strokeWeight:3
                        })
                    };
                    setTracks();
                    if (!info.home && map.getZoom() == 3) {
                        info.home = trackData[0];
                        map.setCenter(pts[0]);
                        map.setZoom(12);
                    }
                };
            })(trackToFetch));
        }
    }

    function setRivals() {
        $("#rivalAddRival").show();
        $("#rivalBusy").hide();
        $("#rivalList").empty();
        if (!info.rivals || info.rivals.length == 0) {
            $("#rivalsNoRivals").show();
            $("#rivals").hide();
            $("#rivalAddRival").removeAttr("disabled");
        } else {
            for (var i = 0; i < info.rivals.length; i++) {
                var tr = document.createElement("tr");
                $("#rivalList").append(tr);
                var td = document.createElement("td");
                $(tr).append(td);
                $(td).text(info.rivals[i].name);
                td = document.createElement("td");
                $(tr).append(td);
                $(td).addClass("chooseItem");
                var span = document.createElement("span");
                $(td).append(span);
                $(span).text("remove");
                $(span).click((function(index) {
                    return function() {
                        $("#rivalAddRival").hide();
                        $("#rivalBusy").show();
                        ajax("DELETE", "/rest/rival/"+info.rivals[index].id, function() {
                            info.rivals.splice(index,1);
                            setRivals();
                        }, function() {
                            $("#rivalAddRival").show();
                            $("#rivalBusy").hide();
                        });
                    };
                })(i));
            }
            $("#rivalsNoRivals").hide();
            $("#rivals").show();
            if (info.rivals.length < info.maxRivals)
                $("#rivalAddRival").removeAttr("disabled");
            else
                $("#rivalAddRival").attr("disabled", "true");
        }
        $("#rivalAddRival").click(function() {
            $("#rivalAddRival").hide();
            $("#rivalBusy").show();
            if (info.friends)
                chooseRival(0);
            else
                getJSON("/rest/friends", function(friends) {
                    info.friends = friends;
                    chooseRival(0);
                }, function() {
                    $("#rivalBusy").hide();
                    $("#rivalAddRival").show();
                });
        });
    }

    function chooseRival(index) {
        function isRival(id) {
            for (var i = 0; i < info.rivals.length; i++)
                if (info.rivals[i].id == id)
                    return true;
            return false;
        }

        var batchSize = 5;
        $("#chooseRival").show();
        $("#chooseRivalList").empty();
        for (var i = 0; i < batchSize && i + index < info.friends.length; i++) {
            if (isRival(info.friends[index + i].id))
                continue;
            var tr = document.createElement("tr");
            $("#chooseRivalList").append(tr);
            var td = document.createElement("td");
            $(tr).append(td);
            $(td).text(info.friends[index + i].name);
            td = document.createElement("td");
            $(tr).append(td);
            $(td).addClass("chooseItem");
            var span = document.createElement("span");
            $(td).append(span);
            $(span).text("add as rival");
            $(span).click((function(id) {
                return function() {
                    $("#chooseRival").hide();
                    ajax("POST", "/rest/rival/"+id, function(rivals) {
                        info.rivals = rivals;
                        setRivals();
                    }, setRivals);
                };
            })(info.friends[index + i].id));
        }
        if (index == 0) {
            $("#chooseRivalPrevDisabled").show();
            $("#chooseRivalPrev").hide();
        } else {
            $("#chooseRivalPrevDisabled").hide();
            $("#chooseRivalPrev").show();
            $("#chooseRivalPrev").click(function() {
                chooseRival(Math.max(0, index - batchSize));
            });
        }
        if (index + batchSize >= info.friends.length) {
            $("#chooseRivalNextDisabled").show();
            $("#chooseRivalNext").hide();
        } else {
            $("#chooseRivalNextDisabled").hide();
            $("#chooseRivalNext").show();
            $("#chooseRivalNext").click(function() {
                chooseRival(index + batchSize);
            });
        }
        $("#chooseRivalCancel").click(function() {
            $("#chooseRival").hide();
            $("#rivalBusy").hide();
            $("#rivalAddRival").show();
        });
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
