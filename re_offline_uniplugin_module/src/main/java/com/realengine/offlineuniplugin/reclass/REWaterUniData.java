package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class REWaterUniData {
    public String waterName = "";
    public List<Integer> waterClr = new ArrayList<>(Arrays.asList(61, 158, 135, 255));
    public float blendDist = 1;
    public boolean visible = true;
    public float expandDist = 0.0f;
    public float depthBias = 0.0f;
    public float visDist = 200000.0f;
    public List<RECornerRgnUniData> rgnList = new ArrayList<>();

    public String getWaterName() {
        return waterName;
    }

    public void setWaterName(String waterName) {
        this.waterName = waterName;
    }

    public List<Integer> getWaterClr() {
        return waterClr;
    }

    public void setWaterClr(List<Integer> waterClr) {
        this.waterClr = waterClr;
    }

    public float getBlendDist() {
        return blendDist;
    }

    public void setBlendDist(float blendDist) {
        this.blendDist = blendDist;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getExpandDist() {
        return expandDist;
    }

    public void setExpandDist(float expandDist) {
        this.expandDist = expandDist;
    }

    public float getDepthBias() {
        return depthBias;
    }

    public void setDepthBias(float depthBias) {
        this.depthBias = depthBias;
    }

    public float getVisDist() {
        return visDist;
    }

    public void setVisDist(float visDist) {
        this.visDist = visDist;
    }

    public List<RECornerRgnUniData> getRgnList() {
        return rgnList;
    }

    public void setRgnList(List<RECornerRgnUniData> rgnList) {
        this.rgnList = rgnList;
    }
}
