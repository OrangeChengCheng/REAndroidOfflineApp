package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class REMonomerUniData {

    public String monomerId = "";
    public String dataSetId = "";
    public float heightMin = 0.0f;
    public float heightMax = 0.0f;
    public List<Integer> faceClr = new ArrayList<>(Arrays.asList(255, 255, 255, 127));
    public List<Integer> lineClr = new ArrayList<>(Arrays.asList(255, 255, 255, 127));
    public int showState = 1;
    public List<List<List<Double>>> rgnList = new ArrayList<>();

    public String getMonomerId() {
        return monomerId;
    }

    public void setMonomerId(String monomerId) {
        this.monomerId = monomerId;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public float getHeightMin() {
        return heightMin;
    }

    public void setHeightMin(float heightMin) {
        this.heightMin = heightMin;
    }

    public float getHeightMax() {
        return heightMax;
    }

    public void setHeightMax(float heightMax) {
        this.heightMax = heightMax;
    }

    public List<Integer> getFaceClr() {
        return faceClr;
    }

    public void setFaceClr(List<Integer> faceClr) {
        this.faceClr = faceClr;
    }

    public List<Integer> getLineClr() {
        return lineClr;
    }

    public void setLineClr(List<Integer> lineClr) {
        this.lineClr = lineClr;
    }

    public int getShowState() {
        return showState;
    }

    public void setShowState(int showState) {
        this.showState = showState;
    }

    public List<List<List<Double>>> getRgnList() {
        return rgnList;
    }

    public void setRgnList(List<List<List<Double>>> rgnList) {
        this.rgnList = rgnList;
    }
}
