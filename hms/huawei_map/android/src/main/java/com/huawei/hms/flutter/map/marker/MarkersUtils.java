/*
Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.huawei.hms.flutter.map.marker;

import com.huawei.hms.flutter.map.constants.Method;
import com.huawei.hms.flutter.map.constants.Param;
import com.huawei.hms.flutter.map.utils.Convert;
import com.huawei.hms.flutter.map.utils.ToJson;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;

import io.flutter.plugin.common.MethodChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkersUtils {
    private HuaweiMap huaweiMap;
    private final MethodChannel mChannel;

    private final Map<String, MarkerController> idsOnMap;
    private final Map<String, String> ids;

    public MarkersUtils(MethodChannel mChannel) {
        this.idsOnMap = new HashMap<>();
        this.ids = new HashMap<>();
        this.mChannel = mChannel;
    }

    public void setMap(HuaweiMap huaweiMap) {
        this.huaweiMap = huaweiMap;
    }

    public void insertMulti(List<HashMap<String, Object>> markerList) {
        if (markerList == null) return;

        for (HashMap<String, Object> markerToAdd : markerList) {
            insert(markerToAdd);
        }
    }

    private void insert(HashMap<String, Object> marker) {
        if (huaweiMap == null) return;
        if (marker == null) return;

        MarkerBuilder markerBuilder = new MarkerBuilder();
        String id = Convert.processMarkerOptions(marker, markerBuilder);
        MarkerOptions options = markerBuilder.build();

        final Marker newMarker = huaweiMap.addMarker(options);
        MarkerController controller = new MarkerController(newMarker, markerBuilder.isClickable());
        idsOnMap.put(id, controller);
        ids.put(newMarker.getId(), id);
    }

    private void update(HashMap<String, Object> marker) {
        if (marker == null) {
            return;
        }
        String markerId = getId(marker);
        MarkerController markerController = idsOnMap.get(markerId);
        if (markerController != null) {
            Convert.processMarkerOptions(marker, markerController);
        }
    }


    public void updateMulti(List<HashMap<String, Object>> marker) {
        if (marker == null) return;

        for (HashMap<String, Object> markerToChange : marker) {
            update(markerToChange);
        }
    }

    public void deleteMulti(List<String> markerList) {
        if (markerList == null) return;

        for (String id : markerList) {
            if (id == null) continue;

            final MarkerController markerController = idsOnMap.remove(id);
            if (markerController != null) {
                markerController.delete();
                ids.remove(markerController.getMapMarkerId());
            }
        }
    }

    public void showInfoWindow(String id, MethodChannel.Result result) {
        MarkerController markerController = idsOnMap.get(id);
        if (markerController == null) return;

        markerController.showInfoWindow();
        result.success(null);
    }

    public void hideInfoWindow(String id, MethodChannel.Result result) {
        MarkerController markerController = idsOnMap.get(id);
        if (markerController == null) return;

        markerController.hideInfoWindow();
        result.success(null);
    }

    public void isInfoWindowShown(String id, MethodChannel.Result result) {
        MarkerController markerController = idsOnMap.get(id);
        if (markerController == null) return;
        result.success(markerController.isInfoWindowShown());
    }

    public boolean onMarkerClick(String idOnMap) {
        String id = ids.get(idOnMap);
        if (id == null) {
            return false;
        }
        mChannel.invokeMethod(Method.MARKER_CLICK, markerIdToJson(id));

        MarkerController markerController = idsOnMap.get(id);

        if (markerController != null) {
            markerController.showInfoWindow();
            return markerController.isClickable();
        }
        return false;

    }

    public void onMarkerDragEnd(String idOnMap, LatLng latLng) {
        String id = ids.get(idOnMap);
        if (id == null) return;

        final Map<String, Object> args = new HashMap<>();
        args.put(Param.MARKER_ID, id);
        args.put(Param.POSITION, ToJson.latLng(latLng));
        mChannel.invokeMethod(Method.MARKER_ON_DRAG_END, args);
    }

    public void onInfoWindowClick(String idOnMap) {
        String markerId = ids.get(idOnMap);
        if (markerId == null) return;

        mChannel.invokeMethod(Method.INFO_WINDOW_CLICK, markerIdToJson(markerId));
    }


    private static String getId(HashMap<String, Object> marker) {
        return (String) marker.get(Param.MARKER_ID);
    }


    private static HashMap<String, Object> markerIdToJson(String markerId) {
        if (markerId == null) return null;

        final HashMap<String, Object> data = new HashMap<>();
        data.put(Param.MARKER_ID, markerId);
        return data;
    }

}
