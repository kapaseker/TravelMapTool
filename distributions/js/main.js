window.commonMargin = "10px";
window.contentPadding = "20px";
window.map = new AMap.Map('container', {
    resizeEnable: true,
    rotateEnable: true,
    pitchEnable: true,
    rotation: 0,
    pitch: 0, // 地图俯仰角度，有效范围 0 度- 83 度
    viewMode: '3D', // 默认使用 2D 模式，如果希望使用带有俯仰角的 3D 模式，请设置 viewMode: '3D',
});

window.changeZoomCenter = function (zoom, center) {
    map.setZoomAndCenter(zoom, center, true);
}

window.getMapZoom = function () {
    return map.getZoom();
}

window.getMapCenter = function () {
    var cent = map.getCenter();
    return [cent.lng, cent.lat];
}

window.ruler = new AMap.RangingTool(map);
ruler.turnOff();

window.rulerTurn = false;

window.turnRuler = function (open) {

    window.rulerTurn = open;

    if (open) {
        ruler.turnOn();
    } else {
        ruler.turnOff();
    }
};

AMap.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.Driving', 'AMap.ControlBar', 'AMap.CustomLayer'], function () {

    var zoomControl = new AMap.ToolBar({
        position: {
            bottom: contentPadding,
            right: contentPadding,
        }
    });

    map.addControl(zoomControl);

    var compass = new AMap.ControlBar({
        position: {
            bottom: contentPadding,
            left: contentPadding,
        }
    });
    map.addControl(compass);

    var scale = new AMap.Scale({
        position: {
            bottom: contentPadding,
            right: '70px',
        }
    });
    map.addControl(scale);

    window.driveTool = new AMap.Driving({
        // 驾车路线规划策略，AMap.DrivingPolicy.LEAST_TIME是最快捷模式
        policy: AMap.DrivingPolicy.LEAST_TIME,
        map: map,
        hideMarkers: true,
        showTraffic: true,
        autoFitView: false,
    });
});

AMapUI.loadUI(['control/BasicControl'], function (BasicControl) {

    var layerSwitch = new BasicControl.LayerSwitcher({
        position: {
            right: contentPadding,
            bottom: '100px',
        }
    });

    map.addControl(layerSwitch);
});

window.removeItemInArray = function (array, item) {
    var index = array.indexOf(item);
    if (index !== -1) {
        array.splice(index, 1);
    }
}

window.showTip = function (message) {
    UIkit.notification({
        message: message,
        status: 'primary',
        pos: 'top-center',
        timeout: 2000
    });
}

window.copyToClipboard = function (textToCopy) {
    // navigator clipboard api needs a secure context (https)
    if (navigator.clipboard && window.isSecureContext) {
        // navigator clipboard api method'
        return navigator.clipboard.writeText(textToCopy);
    } else {
        // text area method
        var textArea = document.createElement("textarea");
        textArea.value = textToCopy;
        // make the textarea out of viewport
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        return new Promise(function (res, rej) {
            document.execCommand('copy') ? res() : rej();
            textArea.remove();
        });
    }
}

window.startMarker = null;
window.endMarker = null;
window.viaMarkers = Array();

window.addStartMarker = function (lng, lat) {

    var marker = new AMap.Marker({
        icon: "./res/mk_start.png",
        position: [lng, lat],
        anchor: "center",
        draggable: true,
    });

    marker.on("dragend", function (event) {
        tryCalcRoute();
    });

    window.map.add(marker);

    if (window.startMarker) {
        window.map.remove(window.startMarker);
    }

    window.startMarker = marker;

    hideContextMenu();

    tryCalcRoute();
}

window.addEndMarker = function (lng, lat) {

    // 构造点标记
    var marker = new AMap.Marker({
        icon: "./res/mk_end.png",
        position: [lng, lat],
        anchor: "center",
        draggable: true,
    });

    marker.on("dragend", function (event) {
        tryCalcRoute();
    });

    window.map.add(marker);

    if (window.endMarker) {
        window.map.remove(window.endMarker);
    }

    window.endMarker = marker;

    hideContextMenu();

    tryCalcRoute();
}

window.addViaMarker = function (lng, lat) {

    var marker = new AMap.Marker({
        icon: "./res/mk_via.png",
        position: [lng, lat],
        anchor: "center",
        draggable: true,
    });

    marker.on("dblclick", window.removeViaPoint);

    marker.on("dragend", function (event) {
        tryCalcRoute();
    });

    window.map.add(marker);

    window.viaMarkers.push(marker);

    hideContextMenu();

    tryCalcRoute();
}

window.removeViaPoint = function (ev) {
    removeItemInArray(viaMarkers, ev.target);
    removeMapElement(ev.target);
    tryCalcRoute();
}

window.removeMapElementArray = function (anys) {
    anys.forEach(function (ele) {
        removeMapElement(ele);
    })
}

window.removeMapElement = function (ele) {
    if (ele) {
        map.remove(ele);
    }
}

window.exportRoute = function () {
    return window.currentRoutePath;
}

/**
 * 尝试算路
 */
window.tryCalcRoute = function () {
    if (startMarker && endMarker && driveTool) {

        loading(true);
        window.driveTool.search(startMarker.getPosition(), endMarker.getPosition(), {
            waypoints: viaMarkers.map(function (item) {
                return item.getPosition();
            })
        }, function (status, result) {

            loading(false);
            var straight_length = AMap.GeometryUtil.distance(startMarker.getPosition(), endMarker.getPosition());

            if (status == 'complete' && result.routes && result.routes.length) {
                var routeResult = result.routes[0];
                parseRouteToPath(routeResult);
                loadRouteResult(true, routeResult.distance, straight_length, routeResult.time, routeResult.tolls);
            } else {
                loadRouteResult(false, result);
            }
        });
    }
}

window.currentRoutePath = []

function parseRouteToPath(route) {
    var path = [];

    for (var i = 0, l = route.steps.length; i < l; i++) {
        var step = route.steps[i];

        for (var j = 0, n = step.path.length; j < n; j++) {
            var lng = step.path[j].lng;
            var lat = step.path[j].lat;
            path.push([lng, lat]);
        }
    }

    window.currentRoutePath = path;
}

window.cleanMapRoute = function () {

    removeMapElement(startMarker);
    removeMapElement(endMarker);
    startMarker = null;
    endMarker = null;

    viaMarkers.forEach(function (item) {
        removeMapElement(item);
    });
    viaMarkers = [];

    driveTool.clear();
    removeRouteResult();
    hideContextMenu();
    window.currentRoutePath = []
}

window.lngLatMarkList = Array();

window.addPositionMarker = function (lng, lat) {

    var marker = new AMap.Marker({
        icon: "./res/mk_label.png",
        position: [lng, lat],
        anchor: "bottom-left",
        cursor: "default",
    });

    marker.on("dblclick", function (ev) { removeLngLatMarker(ev.target) });
    marker.on("rightclick", function (ev) {
        showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.target.getPosition().lng, ev.target.getPosition().lat);
    });

    map.add(marker);

    var text = new AMap.Text({
        position: [lng, lat],
        anchor: 'middle-left',
        offset: [42, -21],
        text: lng + ',' + lat,
    });

    text.on("click", function (ev) {
        var promise = copyToClipboard(ev.target.getText());

        promise.then(function () {
            showTip("已复制经纬度！");
        });

        promise.catch(function () {
            alert("无法写入剪贴板，请联系开发者解决。");
        });
    });

    map.add(text);

    window.lngLatMarkList.push([marker, text]);

    hideContextMenu();
};

window.cleanAllLngLatMarker = function () {
    window.lngLatMarkList.forEach(function (element) {
        removeMapElementArray(element);
    });
    window.lngLatMarkList = [];
    hideContextMenu();
}

window.removeLngLatMarker = function (mark) {

    var mayFind = window.lngLatMarkList.find(function (element) {
        return element[0] == mark
    });

    if (mayFind) {
        removeMapElementArray(mayFind);
        removeItemInArray(lngLatMarkList, mayFind);
    }
}

window.searchTool = new AMap.PlaceSearch({
    pageSize: 10, // 单页显示结果条数
    pageIndex: 1, // 页码
    map: map, // 展现结果的地图实例
    panel: "search-panel", // 结果列表将在此容器中进行展示。
    autoFitView: true // 是否自动调整地图视野使绘制的 Marker点都处于视口的可见范围
});

window.searchNearby = function (keyword, radius) {
    if (radius <= 0) {
        window.searchTool.search(keyword);
    } else {
        window.searchTool.searchNearBy(keyword, map.getCenter(), radius * 1000);
    }
}

window.clearSearch = function () {
    window.searchTool.clear();
}

window.buildMarkerInfo = function (imageIcon, lng, lat, content, backgroundColor) {

    var icon = {
        // 图标类型，现阶段只支持 image 类型
        type: "image",
        // 图片 url
        image: imageIcon,
        // 图片尺寸
        size: [18, 18],
        // 图片相对 position 的锚点，默认为 bottom-center
        anchor: "center",
    };

    var text = {
        // 要展示的文字内容
        content: content ? content : "",
        // 文字方向，有 icon 时为围绕文字的方向，没有 icon 时，则为相对 position 的位置
        direction: "top",
        // 在 direction 基础上的偏移量
        offset: [- (content ? content : "").length / 2, -0],
        opacity: 0.3,
        // 文字样式
        style: {
            fontWeight: "normal",
            // 字体大小
            fontSize: 12,
            // 字体颜色
            fillColor: "#ffffff",
            //
            backgroundColor: backgroundColor,
            fold: true,
            padding: "2, 5",
        }
    };

    return {
        // 此属性非绘制文字内容，仅最为标识使用
        dotName: "标注",
        position: [lng, lat],
        zooms: [3, 20],
        zIndex: 10,
        opacity: 1,
        fold: true,
        icon: icon,
        // 将第二步创建的 text 对象传给 text 属性
        text: text,
    };
}

window.map.on("rightclick", function (ev) {
    showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.lnglat.lng, ev.lnglat.lat);
});

window.map.on("click", function (ev) {
    hideContextMenu();
});

window.addColorPicker = function (id, inputId, color) {

    var input = document.createElement('INPUT');
    input.id = inputId;
    initJsColor(input, color);
    document.getElementById(id).appendChild(input);
}

window.initJsColor = function (element, color) {
    var picker = new JSColor(element, {});
    picker.fromString(color)
}

/**
 * 制作扇形绘制的多边形
 * @param positon 原点
 * @param radius 绘制半径
 * @param angle 角度
 * @returns 返回扇形绘制的点
 */
function makePolygon(positon, radius, angle) {
    return [positon, makeSectorPosition(positon, radius, angle, -15), makeSectorPosition(positon, radius, angle, 0), makeSectorPosition(positon, radius, angle, 15), positon];
}

/**
 * 制作线绘制
 * 
 * @param positon 原点
 * @param radius 绘制半径
 * @param angle 角度
 * @returns 
 */
function makeAngleLine(positon, radius, angle) {
    return [positon, makeSectorPosition(positon, radius, angle, 0)]
}

/**
 * 绘制扇形位置
 * @param origin 原点
 * @param radius 绘制半径
 * @param angle 角度
 * @param span 角度的跨度
 * @returns 
 */
function makeSectorPosition(origin, radius, angle, span) {
    const position = [0, 0];
    position[0] = origin[0] + radius * Math.sin((angle + span) * Math.PI / 180) * 180 / (Math.PI * 6371229 * Math.cos(origin[1] * Math.PI / 180));
    position[1] = origin[1] + radius * Math.cos((angle + span) * Math.PI / 180) / (Math.PI * 6371229 / 180);
    return position;
}

window.drawMapLineElements = function (markerArray, color, move) {

    let lineDots = [];

    markerArray = JSON.parse(markerArray);

    markerArray.forEach(function (item) {
        lineDots.push([item.lng, item.lat]);
    });

    var polyline = new AMap.Polyline({
        path: lineDots,            // 设置线覆盖物路径
        strokeColor: color,   // 线颜色
        strokeOpacity: 0.8,         // 线透明度
        strokeWeight: 4,          // 线宽
        strokeStyle: 'solid',     // 线样式
    });

    map.add(polyline);

    if (move && lineDots.length > 0) {
        map.setZoomAndCenter(13, [lineDots[0][0], lineDots[0][1]]);
    }

    return polyline;
}

window.drawMapDotElements = function (markerArray, radius, speed, color, icon, move) {

    let dotArray = [];

    let labels = [];

    markerArray = JSON.parse(markerArray);

    markerArray.forEach(function (item) {

        try {

            let lnglat = [item.lng, item.lat];

            labels.push(buildMarkerInfo(icon, item.lng, item.lat, item.name, color));

            // 单字 0.2071005917159763 秒
            let readCircle = speed * 1000 / 3600 * item.textCount * 0.2071005917159763;

            /**
             * 绘制朗读的扇形区域
             */
            if (readCircle > 0 && item.name) {

                if (item.angle > -1) {

                    let pathData = makePolygon(lnglat, readCircle, parseFloat(item.angle));
                    let polygon = new AMap.Polygon({
                        path: pathData,
                        fillColor: color,
                        fillOpacity: 0.2,
                        strokeColor: color,
                        strokeOpacity: 0.7,
                        strokeWeight: 1,
                        strokeStyle: 'dashed',
                        strokeDasharray: [5, 5],
                    });

                    map.add(polygon);
                    dotArray.push(polygon);
                    polygon.on("rightclick", function (ev) {
                        showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.lnglat.lng, ev.lnglat.lat);
                    });

                    /**
                     * 角度标识线
                     */
                    let line = makeAngleLine(lnglat, readCircle + 60, parseFloat(item.angle));

                    var angleLine = new AMap.Polyline({
                        path: line,            // 设置线覆盖物路径
                        strokeColor: color,   // 线颜色
                        strokeOpacity: 0.8,         // 线透明度
                        strokeWeight: 4,          // 线宽
                        strokeStyle: 'dashed',     // 线样式
                        strokeDasharray: [6, 4],
                    });
                    map.add(angleLine);
                    dotArray.push(angleLine);

                } else {

                    let circle = new AMap.Circle({
                        center: new AMap.LngLat(item.lng, item.lat),  // 圆心位置
                        radius: readCircle, // 圆半径
                        fillColor: color,   // 圆形填充颜色
                        fillOpacity: 0.2,
                        strokeColor: color, // 描边颜色
                        strokeOpacity: 0.7,
                        strokeWeight: 2, // 描边宽度
                        strokeStyle: 'dashed',
                        strokeDasharray: [5, 5],
                    });

                    map.add(circle);
                    dotArray.push(circle);

                    circle.on("rightclick", function (ev) {
                        showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.lnglat.lng, ev.lnglat.lat);
                    });
                }
            }

            /**
             * 绘制探测的范围
             */
            if (radius > 0 && item.name) {
                // 圆
                let circle = new AMap.Circle({
                    center: new AMap.LngLat(item.lng, item.lat),  // 圆心位置
                    radius: radius, // 圆半径
                    fillColor: color,   // 圆形填充颜色
                    fillOpacity: 0.6,
                    strokeColor: color, // 描边颜色
                    strokeWeight: 1, // 描边宽度
                });

                map.add(circle);
                dotArray.push(circle);

                circle.on("rightclick", function (ev) {
                    showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.lnglat.lng, ev.lnglat.lat);
                });
            }
        } catch (error) {
            console.error(error);
        }
    });

    let labelsOverlay = new AMap.LabelsLayer({
        zooms: [3, 20],
        zIndex: 1000,
        collision: false
    });

    let labelMarkers = [];

    labels.forEach(element => {
        var labelMarker = new AMap.LabelMarker(element);
        // 为 labelMarker 绑定事件
        labelMarker.on('rightclick', function (ev) {
            showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.target.getPosition().lng, ev.target.getPosition().lat);
        });
        labelMarkers.push(labelMarker);
    });

    // 将 marker 添加到图层
    labelsOverlay.add(labelMarkers);
    // 图层添加到地图
    map.add(labelsOverlay);

    dotArray.push(labelsOverlay);

    if (move) {
        map.setFitView(null, false, [100, 150, 10, 10]);
    }

    return dotArray;
};

var ScopeMarkers = []

window.clearTestScope = function () {
    hideContextMenu();
    ScopeMarkers.forEach(function (item) {
        removeMapElementArray(item);
    });
    ScopeMarkers = [];
}

window.removeScopeElement = function (mark) {
    var mayFind = ScopeMarkers.find(function (element) {
        return element[0] == mark
    });

    if (mayFind) {
        removeMapElementArray(mayFind);
        removeItemInArray(ScopeMarkers, mayFind);
    }
}

const SELECT_MARKER_COLOR = "#0EFAEA";

window.addSpeakTestScope = function (lng, lat, count, speed) {

    // 构造点标记
    var marker = new AMap.Marker({
        icon: "./res/mk_speak.png",
        position: [lng, lat],
        anchor: "center",
        cursor: "default",
    });

    map.add(marker);

    marker.on("rightclick", function (ev) {
        showContextMenu(ev.pixel.x + 10, ev.pixel.y + 10, ev.target.getPosition().lng, ev.target.getPosition().lat);
    });

    marker.on("dblclick", function (ev) {
        removeScopeElement(ev.target);
    });

    // 单字 0.2071005917159763 秒
    let readCircle = count;
    // 朗读时间
    readCircle *= 0.2071005917159763;

    // 直线半径 
    readCircle = speed * 1000 / 3600 * readCircle;

    var circle = null;

    if (readCircle > 0) {
        circle = new AMap.Circle({
            center: new AMap.LngLat(lng, lat),  // 圆心位置
            radius: readCircle, // 圆半径
            fillColor: SELECT_MARKER_COLOR,   // 圆形填充颜色
            fillOpacity: 0.3,
            strokeColor: SELECT_MARKER_COLOR, // 描边颜色
            strokeWeight: 2, // 描边宽度
            strokeStyle: 'dashed'
        });

        map.add(circle);
    }

    ScopeMarkers.push([marker, circle]);
}

window.fillElement = function (element, value) {
    element.value = value
}