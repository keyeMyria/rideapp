try {
    rideapp = rideapp;
} catch (e) {
    rideapp = {};
}
(function(rideapp) {
    var EARTH_RADIUS = 6366565;        

    function dist(pt1, pt2) {
        var dlat = (pt1.lat - pt2.lat)*Math.PI/180;
        var dlon = (pt1.lon - pt2.lon)*Math.PI/180;
        var sdlat2 = Math.sin(dlat/2);
        var sdlon2 = Math.sin(dlon/2);
        var clat1 = Math.cos(pt1.lat*Math.PI/180);
        var clat2 = Math.cos(pt2.lat*Math.PI/180);
        var a = sdlat2*sdlat2 + clat1*clat2*sdlon2*sdlon2;
        return 2*EARTH_RADIUS*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    function interpolateNearest(pt1, pt2, targetPt) {
        // ptNearest = { lat: pt1.lat*t + pt2.lat*(1-t),
        //               lon: pt1.lon*t + pt2.lon*(1-t) }
        // if 0 < t < 1, then the nearest point is between the two points
        // Applying these approximations:
        // cos(x) ~ cos(x + delta)
        // sin(x) ~ sin(x + delta)
        // cos(delta) ~ 1
        // sin(delta) ~ delta
        // and assuming no mistakes:
        var cos = Math.cos(targetPt.lat*Math.PI/180);
        var sin = Math.sin(targetPt.lat*Math.PI/180);
        var dlat = (pt1.lat - pt2.lat)*Math.PI/180;
        var dlon = (pt1.lon - pt2.lon)*Math.PI/180;
        var dlat0 = (targetPt.lat - pt2.lat)*Math.PI/180;
        var dlon0 = (targetPt.lon - pt2.lon)*Math.PI/180;
        var a = 4*cos*sin*dlon*dlon*dlat;
        var b = 2*dlat*dlat + 2*cos*sin*dlon0*dlon*dlat + 2*cos*cos*dlon*dlon;
        var c = 2*dlat0*dlat + cos*sin*dlon0*dlon0*dlat;
        var d = b*b - 4*a*c;
        if (d < 0)
            return -1;
        var e = Math.sqrt(d);
        var t = (-b+e)/(2*a);
        if (t < 0 || t > 1)
            t = (-b-e)/(2*a);
        return t;
    }

    function passesCheckPoint(pt, lastPt, checkPt, checkPointRadius) {
        var dlast = dist(checkPt, lastPt);
        var d = dist(checkPt, pt);
        if (dlast >= checkPointRadius && d < checkPointRadius)
            return true;
        if (dlast < checkPointRadius)
            return false;
        var t = interpolateNearest(pt, lastPt, checkPt);
        if (t <= 0 || t >= 1)
            return false;
        return dist(checkPt, { lat:pt.lat*t + lastPt.lat*(1-t), lon:pt.lon*t + lastPt.lon*(1-t) }) < checkPointRadius;
    }

    function integrateDist(track, firstIndex, lastIndex) {
        var d = 0.0;
        for (var i = firstIndex; i < lastIndex; i++)
            d += dist(track[i], track[i+1]);
        return d;
    }

    function findCourses(track, course, checkPointRadius) {
        var results = [];
        var pending = [];
        var lastPt;
        var ncheckPts = course.points.length + (course.loop ? 1 : 0);
        for (var i = 0; i < track.length; i++) {
            var pt = track[i];
            if (i == 0) {
                if (dist(course.points[0], pt) < checkPointRadius)
                    pending.push([0]);
            } else {
                for (var j = 0; j < pending.length; j++) {
                    var checkPt = course.points[pending[j].length % course.points.length];
                    if (passesCheckPoint(pt, lastPt, checkPt, checkPointRadius))
                        pending[j].push(i);
                }
                for (var j = pending.length - 1; j >= 0; j--)
                    if (pending[j].length >= ncheckPts)
                        results.push(pending.splice(j, 1)[0]);
                if (dist(course.points[0], lastPt) < checkPointRadius && dist(course.points[0], pt) >= checkPointRadius)
                    pending.push([i-1]);
            }
            lastPt = pt;
        }
        return results;
    }

    function parseTimestamp(iso8061) {
        var year = stripLeadingZero(iso8061.substring(0,4));
        var month = stripLeadingZero(iso8061.substring(5,7));
        var day = stripLeadingZero(iso8061.substring(8,10));
        var hour = stripLeadingZero(iso8061.substring(11,13));
        var minute = stripLeadingZero(iso8061.substring(14,16));
        var second = stripLeadingZero(iso8061.substring(17,19));
        var date = new Date();
        date.setUTCFullYear(year, month-1, day);
        date.setUTCHours(hour, minute, second, 0);
        return date;
    }

    function stripLeadingZero(digits) {
        if (digits.charAt(0) == "0") return parseInt(digits.substring(1));
        return parseInt(digits);
    }

    function formatMiles(meters) {
        return Math.round(meters/16.09344)/100 + "mi";
    }

    function formatSpeed(meters, millis) {
        return millis > 0 ? Math.round(meters/160.9344/millis*3600000)/10+"mph" : "";
    }

    function formatTime(date) {
        var h = date.getHours(), m = date.getMinutes(), s = date.getSeconds();
        return (h+":") + (m<10?"0":"") + m+":" + (s<10?"0":"") + s;
    }

    function formatDate(date) {
        return date.getFullYear() + "-" + (date.getMonth() - -1) + "-" + date.getDate();
    }

    function formatDuration(millis) {
        var h = Math.floor(millis/3600000), m = Math.floor((millis%3600000)/60000), s = Math.floor((millis%60000)/1000);
        return (h>0?h+":":"")+(m<10&&h>0?"0":"")+(m+":")+(s<10?"0":"")+s;
    }

    function encodePolyline(track, firstIndex, lastIndex, interval) {
        var lastPt = track[firstIndex];
        var encoded = encodePolylineNumber(lastPt.lat)+encodePolylineNumber(lastPt.lon);
        for (var i = firstIndex + interval; i < lastIndex; i += interval) {
            encoded += encodePolylineNumber(track[i].lat - lastPt.lat) + encodePolylineNumber(track[i].lon - lastPt.lon);
            lastPt = track[i];
        }
        return encoded;
    }

    function encodePolylineNumber(x) {
        var d = Math.round(x*100000) << 1;
        if (x < 0)
            d = ~d;
        var encoded = "";
        while (d != 0) {
            var c = d & 0x1f;
            d >>>= 5;
            if (d != 0)
                c |= 0x20;
            encoded += String.fromCharCode(c + 63);
        }
        return encoded;
    }

    rideapp.dist = dist;
    rideapp.integrateDist = integrateDist;
    rideapp.findCourses = findCourses;
    rideapp.parseTimestamp = parseTimestamp;
    rideapp.formatMiles = formatMiles;
    rideapp.formatSpeed = formatSpeed;
    rideapp.formatDate = formatDate;
    rideapp.formatTime = formatTime;
    rideapp.formatDuration = formatDuration;
    rideapp.encodePolyline = encodePolyline;
})(rideapp);
