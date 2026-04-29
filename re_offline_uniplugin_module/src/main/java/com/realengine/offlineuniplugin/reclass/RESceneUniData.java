package com.realengine.offlineuniplugin.reclass;


import java.util.ArrayList;
import java.util.List;

public class RESceneUniData {
    public String filePath;// 文件路径
    public String projName = "";
    public int maxInstDrawFaceNum = 1500000;
    public String worldCRS = "";
    public List<REDataSetUniData> dataSetList = new ArrayList<>();
    public int shareType = 0;
    public String sceneId = "";
    public String shareViewMode = "";
    public String shareDataType = "";
    public List<REEntityUniData> entityList = new ArrayList<>();
    public List<REWaterUniData> waterList = new ArrayList<>();
    public List<REExtrudeUniData> extrudeList = new ArrayList<>();
    public List<REExtrudeTexUniData> extrudeTexList = new ArrayList<>();
    public List<REMonomerUniData> monomerList = new ArrayList<>();


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getProjName() {
        return projName;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public int getMaxInstDrawFaceNum() {
        return maxInstDrawFaceNum;
    }

    public void setMaxInstDrawFaceNum(int maxInstDrawFaceNum) {
        this.maxInstDrawFaceNum = maxInstDrawFaceNum;
    }

    public String getWorldCRS() {
        return worldCRS;
    }

    public void setWorldCRS(String worldCRS) {
        this.worldCRS = worldCRS;
    }

    public List<REDataSetUniData> getDataSetList() {
        return dataSetList;
    }

    public void setDataSetList(List<REDataSetUniData> dataSetList) {
        this.dataSetList = dataSetList;
    }

    public int getShareType() {
        return shareType;
    }

    public void setShareType(int shareType) {
        this.shareType = shareType;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public String getShareViewMode() {
        return shareViewMode;
    }

    public void setShareViewMode(String shareViewMode) {
        this.shareViewMode = shareViewMode;
    }

    public String getShareDataType() {
        return shareDataType;
    }

    public void setShareDataType(String shareDataType) {
        this.shareDataType = shareDataType;
    }

    public List<REEntityUniData> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<REEntityUniData> entityList) {
        this.entityList = entityList;
    }

    public List<REWaterUniData> getWaterList() {
        return waterList;
    }

    public void setWaterList(List<REWaterUniData> waterList) {
        this.waterList = waterList;
    }

    public List<REExtrudeUniData> getExtrudeList() {
        return extrudeList;
    }

    public void setExtrudeList(List<REExtrudeUniData> extrudeList) {
        this.extrudeList = extrudeList;
    }

    public List<REExtrudeTexUniData> getExtrudeTexList() {
        return extrudeTexList;
    }

    public void setExtrudeTexList(List<REExtrudeTexUniData> extrudeTexList) {
        this.extrudeTexList = extrudeTexList;
    }

    public List<REMonomerUniData> getMonomerList() {
        return monomerList;
    }

    public void setMonomerList(List<REMonomerUniData> monomerList) {
        this.monomerList = monomerList;
    }
}
