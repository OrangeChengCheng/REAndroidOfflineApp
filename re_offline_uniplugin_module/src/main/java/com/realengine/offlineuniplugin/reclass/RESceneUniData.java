package com.realengine.offlineuniplugin.reclass;


import java.util.ArrayList;
import java.util.List;

public class RESceneUniData {
    public String filePath;// 文件路径
//    public String worldCRS = "";
    public List<REDataSetUniData> dataSetList = new ArrayList<>();
    public int shareType = 0;
    public String sceneId = "";
//    public String shareViewMode = "";
//    public String shareDataType = "";
//    public RECamInfoUniData defaultCamLoc = null;
//    public REAuthorUniData authorData;
//    public List<REToolData> toolBtnList = new ArrayList<>();
//    public List<REEntityUniData> entityList = new ArrayList<>();
//    public List<REWaterUniData> waterList = new ArrayList<>();
//    public List<REExtrudeUniData> extrudeList = new ArrayList<>();
//    public List<REExtrudeTexUniData> extrudeTexList = new ArrayList<>();
//    public List<REMonomerUniData> monomerList = new ArrayList<>();


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
}
