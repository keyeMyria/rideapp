var rideapp = (function($) {
    var contextPath;
    var sessionId;
    var garminUnlock;

    var info;
    var map;
    var tracks = {};
    var makeCourseMap;
    var makeCourseMarkers;
    var makeCourseMarkerIcons;

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
                    $("#iframe-hidden").attr("src",getURI("/refreshSession.jsp"));
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

        map = new google.maps.Map(document.getElementById("map"), { mapTypeId:google.maps.MapTypeId.ROADMAP, zoom:3, center:new google.maps.LatLng(39,-98) });
        getJSON("/rest/info", initInfo);
    }

    function initInfo(newInfo) {
        info = newInfo;
        setTracks();
        initRivals();
        setRivals();
        if (map.getZoom() == 3) {
            if (info.home) {
                map.setCenter(new google.maps.LatLng(info.home.lat, info.home.lon));
                map.setZoom(12);
            } else if (info.courses.length > 0) {
                map.setCenter(new google.maps.LatLng(info.courses[0].points[0].lat, info.courses[0].points[0].lon));
                map.setZoom(12);
            }
        }
        initCourses();
        setCourses();
    }

    function removeMapOverlays() {
        for (var i = 0; i < info.tracks.length; i++)
            tracks[info.tracks[i]].overlay.setMap(null);
    }

    function initUploadFile() {
        $("#uploadFile").change(function() {
            $("#iframe-hidden").queue("refreshSessionId", function(next) {
                $("#uploadForm").attr("action",getURI("/rest/upload"));
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
                $(td).addClass("chooseItem");
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
            $(td).addClass("chooseItemRight");
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
                        map.setCenter(pts[0]);
                        map.setZoom(12);
                    }
                };
            })(trackToFetch));
        }
    }

    var chooseRivalIndex = 0;
    var chooseRivalPrevIndex = 0;
    var chooseRivalBatchSize = 10;

    function initRivals() {
        $("#rivalAddRival").click(function() {
            $("#rivalAddRival").attr("disabled", "true");
            chooseRivalIndex = 0;
            if (info.friends)
                chooseRival();
            else
                getJSON("/rest/friends", function(friends) {
                    info.friends = friends;
                    chooseRival();
                }, function() {
                    $("#rivalBusy").hide();
                    $("#rivalAddRival").show();
                });
        });

        $("#chooseRivalNext").click(chooseRival);
        $("#chooseRivalPrev").click(function() {
            chooseRivalIndex = chooseRivalPrevIndex;
            chooseRival();
        });
        $("#chooseRivalCancel").click(function() {
            $("#chooseRival").hide();
            $("#rivalBusy").hide();
            $("#rivalAddRival").show();
            if (info.rivals.length < info.maxRivals)
                $("#rivalAddRival").removeAttr("disabled");
            else
                $("#rivalAddRival").attr("disabled", "true");
        });
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
                $(td).addClass("chooseItem");
                $(td).text(info.rivals[i].user.name);
                $(td).click(function() { alert("under construction"); });
                td = document.createElement("td");
                $(tr).append(td);
                $(td).addClass("chooseItemRight");
                var span = document.createElement("span");
                $(td).append(span);
                $(span).text("remove");
                $(span).click((function(index) {
                    return function() {
                        $("#rivalAddRival").hide();
                        $("#rivalBusy").show();
                        ajax("DELETE", "/rest/rival/"+info.rivals[index].user.id, function() {
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
    }

    function chooseRival() {
        function isRival(id) {
            for (var i = 0; i < info.rivals.length; i++)
                if (info.rivals[i].user.id == id)
                    return true;
            return false;
        }

        $("#chooseRival").show();
        $("#chooseRivalList").empty();
        var i;
        var count = 0;
        for (i = chooseRivalIndex; i < info.friends.length; i++) {
            if (isRival(info.friends[i].id))
                continue;
            if (count >= chooseRivalBatchSize)
                break;
            count++;
            var tr = document.createElement("tr");
            $("#chooseRivalList").append(tr);
            var td = document.createElement("td");
            $(tr).append(td);
            $(td).attr("colspan","3");
            $(td).addClass("chooseItem");
            $(td).text(info.friends[i].name);
            $(td).click((function(id) {
                return function() {
                    $("#chooseRival").hide();
                    $("#rivalAddRival").hide();
                    $("#rivalBusy").show();
                    ajax("POST", "/rest/rival/"+id, function(rivals) {
                        info.rivals = rivals;
                        setRivals();
                    }, setRivals);
                };
            })(info.friends[i].id));
        }
        if (chooseRivalIndex == 0) {
            $("#chooseRivalPrevDisabled").show();
            $("#chooseRivalPrev").hide();
        } else {
            $("#chooseRivalPrevDisabled").hide();
            $("#chooseRivalPrev").show();
        }
        if (i >= info.friends.length) {
            $("#chooseRivalNextDisabled").show();
            $("#chooseRivalNext").hide();
        } else {
            $("#chooseRivalNextDisabled").hide();
            $("#chooseRivalNext").show();
        }
        chooseRivalPrevIndex = Math.max(0, chooseRivalIndex - chooseRivalBatchSize);
        chooseRivalIndex = i;
    }

    function initCourses() {
        $("#courseAddCourse").click(function () { showMakeCourse(null); });
    }

    function setCourses() {
        $("#courseAddCourse").show();
        $("#courseBusy").hide();
        $("#courseList").empty();
        if (!info.courses || info.courses.length == 0) {
            $("#coursesNoCourses").show();
            $("#courses").hide();
            $("#courseAddCourse").removeAttr("disabled");
        } else {
            $("#coursesNoCourses").hide();
            $("#courses").show();
            for (var i = 0; i < info.courses.length; i++) {
                var tr = document.createElement("tr");
                $("#courseList").append(tr);
                var td = document.createElement("td");
                $(tr).append(td);
                $(td).addClass("chooseItem");
                $(td).text(info.courses[i].name);
                $(td).click((function(course) {
                    return function() {
                        showMakeCourse(course);
                    }
                })(info.courses[i]));
                td = document.createElement("td");
                $(tr).append(td);
                $(td).addClass("chooseItemRight");
                var span = document.createElement("span");
                $(td).append(span);
                $(span).text("remove");
                $(span).click((function(index) {
                    return function() {
                        $("#courseAddCourse").hide();
                        $("#courseBusy").show();
                        ajax("DELETE", "/rest/course/"+info.courses[index].id, function() {
                            info.courses.splice(index,1);
                            setCourses();
                        }, function() {
                            $("#courseAddCourse").show();
                            $("#courseBusy").hide();
                        });
                    };
                })(i));
            }
            if (info.courses.length < info.maxCourses)
                $("#courseAddCourse").removeAttr("disabled");
            else
                $("#courseAddCourse").attr("disabled", "true");
        }
    }

    function resetMakeCourseMarkers() {
        if (!makeCourseMarkers)
            return;
        for (var i = 0; i < makeCourseMarkers.length; i++) {
            makeCourseMarkers[i].setMap(null);
            makeCourseMarkers[i].setIcon(makeCourseMarkerIcons[i]);
            makeCourseMarkers[i].setTitle(i == 0 ? "Start" : "Checkpoint #" + (i - -1));
            makeCourseMarkers[i].index = i;
        }
    }

    function deleteMakeCourseMarker(marker) {
        makeCourseMarkers.splice(marker.index,1);
        makeCourseMarkers.push(marker);
        marker.setMap(null);
        for (var i = marker.index; i < makeCourseMarkers.length; i++) {
            makeCourseMarkers[i].setIcon(makeCourseMarkerIcons[i]);
            makeCourseMarkers[i].setTitle(i == 0 ? "Start" : "Checkpoint #" + (i - -1));
            makeCourseMarkers[i].index = i;
        }
    }

    function enableDisableAddCourse() {
        function disabled() {
            if (!$("#makeCourseName").val())
                return true;
            if (!makeCourseMarkers[0].getMap())
                return true;
            if (!makeCourseMarkers[1].getMap())
                return !$("#makeCourseLoop").attr("checked");
            return false;
        }
        if (disabled())
            $("#makeCourseAdd").attr("disabled", "true");
        else
            $("#makeCourseAdd").removeAttr("disabled");
    }

    var courseToEdit;

    function showMakeCourse(oldCourse) {
        courseToEdit = oldCourse;
        function makeCourseAdd() {
            var points = [];
            for (var i = 0; i < makeCourseMarkers.length; i++) {
                if (!makeCourseMarkers[i].getMap())
                    break;
                points.push({
                    lat:makeCourseMarkers[i].getPosition().lat(),
                    lon:makeCourseMarkers[i].getPosition().lng()
                });
            }
            points.toJSON = 0; // Hack because Prototype framework required by Garmin screws up JSON.stringify by adding Array.prototype.toJSON.

            resetMakeCourseMarkers();
            $("#makeCourse").hide();
            $("#courseAddCourse").hide();
            $("#courseBusy").show();
            ajax("POST", courseToEdit ? "/rest/course/" + courseToEdit.id : "/rest/course", (function(course) {
                return function(newCourse) {
                    if (course) {
                        for (var i = 0; i < info.courses.length; i++)
                            if (course.id == info.courses[i].id) {
                                info.courses.splice(i, 1);
                                break;
                            }
                    }
                    info.courses.push(newCourse);
                    setCourses();
                };
            })(courseToEdit), function() {
                $("#courseAddCourse").show();
                $("#courseBusy").hide();
            }, $.toJSON({
                name:$("#makeCourseName").val(),
                loop:!!$("#makeCourseLoop").attr("checked"),
                points:points
            }), "application/json");
        }

        resetMakeCourseMarkers();
        $("#makeCourseName").val("");
        $("#makeCourseLoop").removeAttr("checked");
        $("#makeCourse").show();
        if (!makeCourseMap) {
            makeCourseMap = new google.maps.Map(document.getElementById("makeCourseMap"), { mapTypeId:google.maps.MapTypeId.ROADMAP, zoom:3, center:new google.maps.LatLng(39,-98) });
            makeCourseMarkers = [];
            makeCourseMarkerIcons = [];
            for (var i = 0; i < Math.min(12, info.maxCoursePoints); i++) {
                var icon = new google.maps.MarkerImage(contextPath+"/img/"+(i- -1)+".gif", new google.maps.Size(24,24), new google.maps.Point(0,0), new google.maps.Point(12,12));
                makeCourseMarkerIcons.push(icon);
                var marker = new google.maps.Marker({
                    draggable:true,
                    flat:true,
                    icon:icon
                });
                marker.index = i;
                makeCourseMarkers.push(marker);
                google.maps.event.addListener(marker, "dblclick", (function(marker) {
                    return function(event) {
                        deleteMakeCourseMarker(marker);
                        enableDisableAddCourse();
                        return false;
                    };
                })(marker));
            }
            google.maps.event.addListener(makeCourseMap, "click", function(event) {
                for (var i = 0; i < makeCourseMarkers.length; i++) {
                    if (makeCourseMarkers[i].getMap())
                        continue;
                    makeCourseMarkers[i].setPosition(event.latLng);
                    makeCourseMarkers[i].setMap(makeCourseMap);
                    enableDisableAddCourse();
                    break;
                }
            });
            $("#makeCourseName").change(enableDisableAddCourse);
            $("#makeCourseLoop").change(enableDisableAddCourse);
            $("#makeCourseCancel").click(function() {
                resetMakeCourseMarkers();
                $("#makeCourse").hide();
            });
            $("#makeCourseAdd").click(function() { makeCourseAdd(); });
        }
        if (oldCourse) {
            $("#makeCourseName").val(oldCourse.name);
            if (oldCourse.loop)
                $("#makeCourseLoop").attr("checked", "true");
            else
                $("#makeCourseLoop").removeAttr("checked");
            var minLat = oldCourse.points[0].lat;
            var maxLat = oldCourse.points[0].lat;
            var minLon = oldCourse.points[0].lon;
            var maxLon = oldCourse.points[0].lon;
            for (var i = 0; i < Math.min(makeCourseMarkers.length, oldCourse.points.length); i++) {
                minLat = Math.min(minLat, oldCourse.points[i].lat);
                maxLat = Math.max(maxLat, oldCourse.points[i].lat);
                minLon = Math.min(minLon, oldCourse.points[i].lon);
                maxLon = Math.max(maxLon, oldCourse.points[i].lon);
                makeCourseMarkers[i].setPosition(new google.maps.LatLng(oldCourse.points[i].lat, oldCourse.points[i].lon));
                makeCourseMarkers[i].setMap(makeCourseMap);
            }
            makeCourseMap.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)));
            enableDisableAddCourse();
        } else {
            makeCourseMap.setCenter(map.getCenter());
            makeCourseMap.setZoom(14);
        }
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
