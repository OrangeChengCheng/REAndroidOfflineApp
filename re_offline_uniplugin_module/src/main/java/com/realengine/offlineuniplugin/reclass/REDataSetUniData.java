package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class REDataSetUniData {

    public int type = 0;// 0:模型  13：遥感影像 10：WMTS工程  11：OSGB工程  14：360全景图  15：点云工程  16：CAD图纸  19：单构件  20：矢量数据
    public String dataSetId = "";
    public String dataSetId_noline = "";//平台处理了数据造成数据使用混乱，无奈之举，自行调整（平台的数据中有的有横线，有的没有）
    public String resourcesAddress = "";
    public List<Double> scale = new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0));
    public List<Double> rotate = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 1.0));
    public List<Double> offset = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0));
    public String dataSetCRS = "";
    public double dataSetCRSNorth = 0;
    public List<Double> engineOrigin = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0));
    public String dataSetSGContent = "";
    public int dataSetType = 0;
    public String unit = "CAD_UNIT_Meter";// 单位 Meter：米 Centimeter：厘米 Millimeter：毫米 Kilometer：千米 Inch：英寸 Foot：英尺 Mile：英里
    public int terrainLayerLev = 0; //地形层级


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
        this.dataSetId_noline = dataSetId.replace("-", "");
    }

    public String getDataSetId_noline() {
        return dataSetId_noline;
    }

    public void setDataSetId_noline(String dataSetId_noline) {
        this.dataSetId_noline = dataSetId_noline;
    }

    public String getResourcesAddress() {
        return resourcesAddress;
    }

    public void setResourcesAddress(String resourcesAddress) {
        this.resourcesAddress = resourcesAddress;
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

    public double getDataSetCRSNorth() {
        return dataSetCRSNorth;
    }

    public void setDataSetCRSNorth(double dataSetCRSNorth) {
        this.dataSetCRSNorth = dataSetCRSNorth;
    }

    public String getDataSetSGContent() {
        return dataSetSGContent;
    }

    public void setDataSetSGContent(String dataSetSGContent) {
        this.dataSetSGContent = dataSetSGContent;
    }

    public int getDataSetType() {
        return dataSetType;
    }

    public void setDataSetType(int dataSetType) {
        this.dataSetType = dataSetType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<Double> getEngineOrigin() {
        return engineOrigin;
    }

    public void setEngineOrigin(List<Double> engineOrigin) {
        this.engineOrigin = engineOrigin;
    }

    public int getTerrainLayerLev() {
        return terrainLayerLev;
    }

    public void setTerrainLayerLev(int terrainLayerLev) {
        this.terrainLayerLev = terrainLayerLev;
    }
}
