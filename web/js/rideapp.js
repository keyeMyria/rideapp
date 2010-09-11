try {
    rideapp = rideapp;
} catch (e) {
    rideapp = {};
}
(function($) {
    var contextPath;
    var sessionId;
    var garminUnlock;

    var resizer = function () { if (FB && FB.Canvas) FB.Canvas.setSize(); };

    var info;
    var map;
    var tracks = {};
    var markers;
    var markerIcons;
    var polyline;
    var makeCourseMap;
    var makeCourseMarkers;

    function getURI(path) {
        return contextPath + path + ";jsessionid=" + sessionId;
    }

    function ajax(method, path, success, error, data, contentType, params) {
        $.ajax({
            type:method,
            url:getURI(path) + (params ? "?" + $.param(params) : ""),
            data:data,
            contentType:contentType,
            success:success,
            error:function(xhr,status) {
                if (xhr.status != 403) {
                    error(xhr,status);
                } else {
                    $("#iframe-hidden").queue("refreshSessionId", function(next) {
                        ajax(method, path, success, error, data, contentType, params);
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

    function initUserPage(initContextPath, initSessionId, initGarminUnlock) {
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
        initMap();
        initRivals();
        setRivals();
        initCourses();
        setCourses();
        setTracks();
        setMainContent();
    }

    function initMap() {
        if (map.getZoom() == 3) {
            if (info.userInfo.home) {
                map.setCenter(new google.maps.LatLng(info.userInfo.home.lat, info.userInfo.home.lon));
                map.setZoom(12);
            } else if (info.courses.length > 0) {
                map.setCenter(new google.maps.LatLng(info.courses[0].points[0].lat, info.courses[0].points[0].lon));
                map.setZoom(12);
            }
        }
        markerIcons = [];
        markers = [];
        for (var i = 0; i < Math.min(12, info.maxCoursePoints); i++) {
            var icon = new google.maps.MarkerImage(contextPath+"/img/"+(i- -1)+".gif", new google.maps.Size(24,24), new google.maps.Point(0,0), new google.maps.Point(12,12));
            markerIcons.push(icon);
            var marker = new google.maps.Marker({
                flat:true,
                icon:icon,
                title:(i == 0 ? "Start" : "Checkpoint #" + (i - -1))
            });
            markers.push(marker);
        }
        polyline = new google.maps.Polyline({
            strokeColor:"#FF00FF",
            strokeOpacity:0.9,
            strokeWeight:3
        });
    }

    function setOverlays(course, pts, startIndex, endIndex) {
        for (var i = 0; i < markers.length; i++) {
            if (course && i < course.points.length) {
                markers[i].setMap(map);
                markers[i].setPosition(new google.maps.LatLng(course.points[i].lat, course.points[i].lon));
            } else {
                markers[i].setMap(null);
            }
        }
        if (!pts) {
            polyline.setMap(null);
        } else {
            var path = [];
            for (var i = startIndex; i < endIndex; i++)
                path.push(new google.maps.LatLng(pts[i].lat, pts[i].lon));
            polyline.setPath(path);
            polyline.setMap(map);
        }
    }

    function fitMapToPoints(pts, startIndex, endIndex) {
        var minLat = pts[startIndex].lat;
        var maxLat = pts[startIndex].lat;
        var minLon = pts[startIndex].lon;
        var maxLon = pts[startIndex].lon;
        for (var i = startIndex; i < endIndex; i++) {
            minLat = Math.min(minLat, pts[i].lat);
            maxLat = Math.max(maxLat, pts[i].lat);
            minLon = Math.min(minLon, pts[i].lon);
            maxLon = Math.max(maxLon, pts[i].lon);
        }
        map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)));
    }

    function fitMapToCourse(course) {
        var minLat = course.points[0].lat;
        var maxLat = course.points[0].lat;
        var minLon = course.points[0].lon;
        var maxLon = course.points[0].lon;
        for (var i = 0; i < course.points.length; i++) {
            minLat = Math.min(minLat, course.points[i].lat);
            maxLat = Math.max(maxLat, course.points[i].lat);
            minLon = Math.min(minLon, course.points[i].lon);
            maxLon = Math.max(maxLon, course.points[i].lon);
        }
        map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)));
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
            setMainContent();
        });
    }

    var garminControl;

    function initGarmin() {
        try {
            garminControl = new Garmin.DeviceControl();
            if (!garminControl.isPluginInitialized())
                return;
        } catch (e) {
            return;
        }
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
        var lastTime = null;
        for (var i = 0; i < info.tracks.length; i++) {
            var pts = tracks[info.tracks[i]].pts;
            if (pts && pts.length > 0 && (lastTime == null || lastTime < pts[pts.length-1].t))
                lastTime = pts[pts.length-1].t;
        }
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
                setMainContent();
            });
        }, function(xhr, status) {
            garminStatus("Upload failed: " + xhr.statusText);
        }, data.controller.gpsDataString, "application/gpx", lastTime ? {lastTime:lastTime} : null);
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
        for (var i = 0; i < info.tracks.length; i++) {
            var tr = document.createElement("tr");
            $("#trackList").append(tr);
            var td = document.createElement("td");
            $(tr).append(td);
            if (tracks[info.tracks[i]]) {
                $(td).addClass("chooseItem");
                var date = rideapp.parseTimestamp(tracks[info.tracks[i]].pts[0].t);
                $(td).text(rideapp.formatDate(date));
                $(td).attr("title", date.toString());
                $(td).click((function(track) {
                    return function() {
                        setOverlays(null, track.pts, 0, track.pts.length);
                        fitMapToPoints(track.pts, 0, track.pts.length);
                    };
                })(tracks[info.tracks[i]]));
            } else {
                var img = document.createElement("img");
                $(td).append(img);
                $(img).attr("src",contextPath+"/img/working.gif");
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
                        tracks[info.tracks[index]] = null;
                        info.tracks.splice(index,1);
                        setTracks();
                        setMainContent();
                    });
                };
            })(i));
        }
    }

    function fetchTracks() {
        var trackToFetch = null;
        var fetchUri;
        var yourTracksDone = true;
        for (var i = 0; i < info.tracks.length; i++) {
            var id = info.tracks[i];
            if (!tracks[id]) {
                trackToFetch = id;
                fetchUri = "/rest/track/" + id;
                yourTracksDone = false;
                break;
            }
        }
        if (yourTracksDone) {
            for (var i = 0; i < info.rivals.length; i++) {
                for (var j = Math.max(0, info.rivals[i].tracks.length - info.maxTracks); j < info.rivals[i].tracks.length; j++) {
                    var id = info.rivals[i].user.id + "/track/" + info.rivals[i].tracks[j];
                    if (!tracks[id]) {
                        trackToFetch = id;
                        fetchUri = "/rest/rival/" + id;
                        break;
                    }
                }
                if (trackToFetch)
                    break;
            }
        }
        if (!trackToFetch)
            return;

        getJSON(fetchUri, (function(id, yourTracksDone) {
            return function(trackData) {
                tracks[id] = { pts:trackData };
                if (!yourTracksDone)
                    setTracks();
                setMainContent();
                if (!info.home && map.getZoom() == 3) {
                    map.setCenter(new google.maps.LatLng(trackData[0].lat, trackData[0].lon));
                    map.setZoom(12);
                }
            };
        })(trackToFetch, yourTracksDone));
    }

    function setMainContent() {
        $("#mainContent").empty();
        for (var i = 0; i < info.courses.length; i++)
            formatCourse($("#mainContent"), info.courses[i]);
        for (var i = 0; i < info.rivals.length; i++)
            for (var j = 0; j < info.rivals[i].courses.length; j++)
                formatCourse($("#mainContent"), info.rivals[i].courses[j], info.rivals[i].user);
        fetchTracks();
        resizer();
    }

    function formatCourse(tbody, course, user) {
        var tr = document.createElement("tr");
        tbody.append(tr);
        $(tr).addClass("courseTitle");
        var td = document.createElement("th");
        $(tr).append(td);
        $(td).text(course.name);
        $(td).click(function() { setOverlays(course); fitMapToCourse(course); });
        if (!user) {
            $(td).attr("colspan","7");
        } else {
            $(td).attr("colspan","4");
            td = document.createElement("td");
            $(tr).append(td);
            $(td).attr("colspan","3");
            $(td).addClass("courseOwner");
            var span = document.createElement("span");
            $(td).append(span);
            $(span).addClass("user");
            $(span).text(user.name);
            $(span).attr("title","Go to rival");
            $(span).click(function() {
                document.location = contextPath+"/user/"+user.id;
            });
            var button = document.createElement("input");
            $(button).attr("type", "button");
            $(button).val("Copy course");
            $(td).append(button);
            if (info.courses.length >= info.maxCourses) {
                $(button).attr("disabled", "true");
            } else {
                $(button).click(function() {
                    $("#courseAddCourse").hide();
                    $("#courseBusy").show();
                    course.points.toJSON = 0; // Hack because Prototype framework required by Garmin screws up JSON.stringify by adding Array.prototype.toJSON.
                    ajax("POST", "/rest/course", function(newCourse) {
                        info.courses.push(newCourse);
                        setCourses();
                        setMainContent();
                    }, function() {
                        $("#courseAddCourse").show();
                        $("#courseBusy").hide();
                    }, $.toJSON(course), "application/json");
                });
            }
        }

        for (var i = 0; i < info.tracks.length; i++)
            formatCourseData(tbody, course, tracks[info.tracks[i]]);
        for (var i = 0; i < info.rivals.length; i++) {
            var rival = info.rivals[i];
            for (var j = 0; j < rival.tracks.length; j++)
                formatCourseData(tbody, course, tracks[rival.user.id + "/track/" + rival.tracks[j]], rival.user);
        }
    }

    function formatCourseData(tbody, course, track, user) {
        if (!track)
            return;
        var id = user ? user.id + "/track/" + course.id : course.id;
        var data = track[id];
        if (!data)
            data = track[id] = rideapp.findCourses(track.pts, course, info.userInfo.checkpointRadius);

        function makeMapSegment(startIndex, endIndex) {
            return function() {
                setOverlays(course, track.pts, startIndex, endIndex+1);
                fitMapToPoints(track.pts, startIndex, endIndex+1);
            };
        }

        for (var i = 0; i < data.length; i++) {
            var tstart = rideapp.parseTimestamp(track.pts[data[i][0]].t);
            var tlast = tstart;
            var totalDist = 0;
            var trhead = document.createElement("tr");
            var rowElts = [];
            tbody.append(trhead);
            $(trhead).addClass("courseTime");
            var lastRow;
            for (var j = 0; j < data[i].length; j++) {
                var t = rideapp.parseTimestamp(track.pts[data[i][j]].t);
                var dist = j == 0 ? 0 : rideapp.integrateDist(track.pts, data[i][j-1], data[i][j]);
                totalDist += dist;
                var tr = document.createElement("tr");
                tbody.append(tr);
                $(tr).hide();
                $(tr).addClass("checkpoint");
                $(tr).addClass(j%2 ? "even" : "odd");
                rowElts.push(tr);

                var clickLeft = (function(courseIndex, startIndex, endIndex) {
                    return function() {
                        if (courseIndex > 0)
                            setOverlays(course, track.pts, startIndex, endIndex);
                        else
                            setOverlays(course);
                        map.setCenter(new google.maps.LatLng(course.points[courseIndex%course.points.length].lat, course.points[courseIndex%course.points.length].lon));
                        map.setZoom(15);
                    };
                })(j, j > 0 ? data[i][j-1] : 0, data[i][j]);
                var clickRight = makeMapSegment(data[i][j-1], data[i][j]);

                var td = document.createElement("td");
                $(tr).append(td);
                $(td).click(clickLeft);
                td = document.createElement("td");
                $(tr).append(td);
                $(td).text(rideapp.formatDuration(t-tstart));
                $(td).click(clickLeft);
                td = document.createElement("td");
                $(tr).append(td);
                $(td).text(rideapp.formatMiles(totalDist));
                $(td).click(clickLeft);
                td = document.createElement("td");
                $(tr).append(td);
                $(td).text(rideapp.formatSpeed(totalDist,t-tstart));
                $(td).click(clickLeft);
                if (j > 0) {
                    td = document.createElement("td");
                    $(lastRow).append(td);
                    $(td).text(rideapp.formatDuration(t-tlast));
                    $(td).click(clickRight);
                    td = document.createElement("td");
                    $(lastRow).append(td);
                    $(td).text(rideapp.formatMiles(dist));
                    $(td).click(clickRight);
                    td = document.createElement("td");
                    $(lastRow).append(td);
                    $(td).text(rideapp.formatSpeed(dist,t-tlast));
                    $(td).click(clickRight);
                }
                lastRow = tr;
                tlast = t;
            }
            td = document.createElement("td");
            $(lastRow).append(td);
            $(td).attr("colspan", "3");

            td = document.createElement("td");
            $(trhead).append(td);
            $(td).text("+");
            var onclick = (function(rowElts,td) {
                return function() {
                    if ($(td).text() == "+") {
                        $(td).text("-");
                        for (var k = 0; k < rowElts.length; k++)
                            $(rowElts[k]).show();
                    } else {
                        $(td).text("+");
                        for (var k = 0; k < rowElts.length; k++)
                            $(rowElts[k]).hide();
                    }
                    resizer();
                };
            })(rowElts,td)
            $(td).click(onclick);

            td = document.createElement("td");
            $(trhead).append(td);
            $(td).attr("colspan", "3");
            var span = document.createElement("span");
            $(span).text(rideapp.formatDate(tstart) + " ");
            $(td).append(span);
            $(td).attr("title", tstart.toString());
            var span = document.createElement("span");
            $(span).addClass("user");
            if (user) {
                $(span).text(user.name);
                $(span).attr("title","Go to rival");
                $(span).click(function() {
                    document.location = contextPath+"/user/"+user.id;
                });
            } else {
                $(span).text(info.userInfo.user.name);
            }
            $(td).append(span);
            $(td).click(onclick);

            onclick = makeMapSegment(data[i][0], data[i][data[i].length-1]);
            td = document.createElement("td");
            $(trhead).append(td);
            $(td).text(rideapp.formatDuration(t-tstart));
            $(td).click(onclick);
            td = document.createElement("td");
            $(trhead).append(td);
            $(td).text(rideapp.formatMiles(totalDist));
            $(td).click(onclick);
            td = document.createElement("td");
            $(trhead).append(td);
            $(td).text(rideapp.formatSpeed(totalDist, t-tstart));
            $(td).click(onclick);
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
                $(td).text(info.rivals[i].user.name);
                if (info.rivals[i].userInfo) {
                    $(td).addClass("chooseItem");
                    $(td).click((function(rivalId) {
                        return function() {
                            document.location = contextPath+"/user/"+rivalId;
                        };
                    })(info.rivals[i].user.id));
                }
                td = document.createElement("td");
                $(tr).append(td);
                $(td).addClass("chooseItemRight");
                var span = document.createElement("span");
                $(td).append(span);
                $(span).text("delete");
                $(span).click((function(index) {
                    return function() {
                        $("#rivalAddRival").hide();
                        $("#rivalBusy").show();
                        ajax("DELETE", "/rest/rival/"+info.rivals[index].user.id, function() {
                            info.rivals.splice(index,1);
                            setRivals();
                            setMainContent();
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
                        setMainContent();
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
                $(span).text("delete");
                $(span).click((function(index) {
                    return function() {
                        $("#courseAddCourse").hide();
                        $("#courseBusy").show();
                        ajax("DELETE", "/rest/course/"+info.courses[index].id, function() {
                            info.courses.splice(index,1);
                            setCourses();
                            setMainContent();
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
            makeCourseMarkers[i].setIcon(markerIcons[i]);
            makeCourseMarkers[i].setTitle(i == 0 ? "Start" : "Checkpoint #" + (i - -1));
            makeCourseMarkers[i].index = i;
        }
    }

    function deleteMakeCourseMarker(marker) {
        makeCourseMarkers.splice(marker.index,1);
        makeCourseMarkers.push(marker);
        marker.setMap(null);
        for (var i = marker.index; i < makeCourseMarkers.length; i++) {
            makeCourseMarkers[i].setIcon(markerIcons[i]);
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
            var name = $("#makeCourseName").val();
            if (name.length > info.maxNameLength)
                name = name.substring(0, info.maxNameLength);

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
                    setMainContent();
                };
            })(courseToEdit), function() {
                $("#courseAddCourse").show();
                $("#courseBusy").hide();
            }, $.toJSON({
                name:name,
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
            for (var i = 0; i < Math.min(12, info.maxCoursePoints); i++) {
                var marker = new google.maps.Marker({
                    draggable:true,
                    flat:true,
                    icon:markerIcons[i]
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
        } else {
            google.maps.event.trigger(makeCourseMap, "resize");
        }
        if (oldCourse) {
            $("#makeCourseAdd").val("Save course");
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
        } else {
            $("#makeCourseAdd").val("Add course");
            makeCourseMap.setCenter(map.getCenter());
            makeCourseMap.setZoom(14);
        }
        enableDisableAddCourse();
    }

    function initPublicPage(initContextPath, initInfo, initTracks) {
        contextPath = initContextPath;
        info = initInfo;
        tracks = initTracks;
        info.rivals = [];

        $("#publicUser").text(initInfo.userInfo.user.name);
        $("#publicUser").attr("href", initInfo.userInfo.user.link);
        $("#mainContent").empty();
        map = new google.maps.Map(document.getElementById("map"), { mapTypeId:google.maps.MapTypeId.ROADMAP, zoom:3, center:new google.maps.LatLng(39,-98) });
        initMap();
        for (var i = 0; i < info.courses.length; i++)
            formatCourse($("#mainContent"), info.courses[i]);
    }

    function refreshSessionId(newSessionId) {
        sessionId = newSessionId;
        $("#iframe-hidden").dequeue("refreshSessionId");
    }

    rideapp.setUploadStatus = setUploadStatus;
    rideapp.refreshSessionId = refreshSessionId;
    rideapp.initUserPage = initUserPage;
    rideapp.initPublicPage = initPublicPage;
})(jQuery)
