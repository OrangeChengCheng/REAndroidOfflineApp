package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class REExtrudeUniData {
    public String extrudeId = "";
    public List<String> dataSetIdList = new ArrayList<>();
    public List<List<List<Double>>> rgnList = new ArrayList<>();
    public List<Double> depthLimitRange = new ArrayList<>(Arrays.asList(0.0, 0.0));
    public int type = 0;
    public int texId = 0;
    public String texPath = "";
    public List<Double> texSize = new ArrayList<>(Arrays.asList(0.0, 0.0));

    public String getExtrudeId() {
        return extrudeId;
    }

    public void setExtrudeId(String extrudeId) {
        this.extrudeId = extrudeId;
    }

    public List<String> getDataSetIdList() {
        return dataSetIdList;
    }

    public void setDataSetIdList(List<String> dataSetIdList) {
        this.dataSetIdList = dataSetIdList;
    }

    public List<List<List<Double>>> getRgnList() {
        return rgnList;
    }

    public void setRgnList(List<List<List<Double>>> rgnList) {
        this.rgnList = rgnList;
    }

    public List<Double> getDepthLimitRange() {
        return depthLimitRange;
    }

    public void setDepthLimitRange(List<Double> depthLimitRange) {
        this.depthLimitRange = depthLimitRange;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTexId() {
        return texId;
    }

    public void setTexId(int texId) {
        this.texId = texId;
    }

    public String getTexPath() {
        return texPath;
    }

    public void setTexPath(String texPath) {
        this.texPath = texPath;
    }

    public List<Double> getTexSize() {
        return texSize;
    }

    public void setTexSize(List<Double> texSize) {
        this.texSize = texSize;
    }
}


