package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.List;

public class RECornerRgnUniData {
    public List<List<Double>> pointList = new ArrayList<>();
    public List<Integer> indexList = new ArrayList<>();

    public List<List<Double>> getPointList() {
        return pointList;
    }

    public void setPointList(List<List<Double>> pointList) {
        this.pointList = pointList;
    }

    public List<Integer> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<Integer> indexList) {
        this.indexList = indexList;
    }
}
