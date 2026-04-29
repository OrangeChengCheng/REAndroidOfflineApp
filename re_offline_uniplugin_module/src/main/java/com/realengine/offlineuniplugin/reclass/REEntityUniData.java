package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class REEntityUniData {
    public String dataSetId = "";
    public String entityType = "";
    public int elemId = 0;
    public List<Double> scale = new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0));
    public List<Double> rotate = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 1.0));
    public List<Double> offset = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0));
    public String dataSetCRS = "";
    public String entityId = "";

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getElemId() {
        return elemId;
    }

    public void setElemId(int elemId) {
        this.elemId = elemId;
    }

    public List<Double> getScale() {
        return scale;
    }

    public void setScale(List<Double> scale) {
        this.scale = scale;
    }

    public List<Double> getRotate() {
        return rotate;
    }

    public void setRotate(List<Double> rotate) {
        this.rotate = rotate;
    }

    public List<Double> getOffset() {
        return offset;
    }

    public void setOffset(List<Double> offset) {
        this.offset = offset;
    }

    public String getDataSetCRS() {
        return dataSetCRS;
    }

    public void setDataSetCRS(String dataSetCRS) {
        this.dataSetCRS = dataSetCRS;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
