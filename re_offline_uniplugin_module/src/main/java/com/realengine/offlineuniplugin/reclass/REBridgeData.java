package com.realengine.offlineuniplugin.reclass;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

import BlackHole3D.REBBox3D;
import BlackHole3D.RECamDirEm;
import BlackHole3D.REColor;
import BlackHole3D.REDVec3;
import BlackHole3D.REDVec4;
import BlackHole3D.RESelElemInfo;
import BlackHole3D.RETerrResEm;

public class REBridgeData {
    public String dataSetId = "";
    public List<Integer> elemIdList = new ArrayList<>();
    public List<Integer> elemClr = new ArrayList<>();
    public REColor elemClrObj = new REColor();
    public boolean visible = true;
    public int alpha = 255;
    public boolean attrValid = true;
    public int probeMask = 1;
    public String unitId = "";
    public String resType = "";
    public RETerrResEm terrResEm = RETerrResEm.ALL;
    public boolean active = true;
    public List<String> waterNameList = new ArrayList<>();
    public List<String> extrudeIdst = new ArrayList<>();
    public List<String> monomerIds = new ArrayList<>();

    public String waterName = "";
    public String extrudeId = "";
    public double backDepth = 1.0;
    public List<RESelElemInfo> locIDList = new ArrayList<>();
    public String locType = "CAM_DIR_CURRENT";
    public RECamDirEm locTypeEm = RECamDirEm.CAM_DIR_CURRENT;
    public List<List<Double>> arrBound = new ArrayList<>();
    public REBBox3D box3D = new REBBox3D();
    public float depthBias = 0;
    public double topHeight = 0;
    public double bottomHeight = 0;
    public boolean single = false;
    public List<Integer> lineClr = new ArrayList<>();
    public REColor lineClrObj = new REColor();
    public int lineClrWeight = 255;
    public int lineAlphaWeight = 255;
    public List<Integer> faceClr = new ArrayList<>();
    public REColor faceClrObj = new REColor();
    public int faceClrWeight = 255;
    public int faceAlphaWeight = 255;
    public boolean visibalOnly = false;
    public List<Double> camPos = new ArrayList<>();
    public REDVec3 camPosObj = new REDVec3();
    public List<Double> camRotate = new ArrayList<>();
    public REDVec4 camRotateObj = new REDVec4();
    public List<Double> camDir = new ArrayList<>();
    public REDVec3 camDirObj = new REDVec3();
    public boolean force = true;
    public double locDelay = 0;
    public double locTime = 0;
    public boolean full = false;
    public String webPopId = "";
    public String log = "";
    public JSONObject requestData = null;
    public List<JSONObject> treeData = new ArrayList<>();

    public String filePath = "";
    public boolean keepDir = true;
    public String suffix = "";
    public String dbPath = "";
    public String sql = "";
    public String tableName = "";
    public String folderPath = "";



    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public List<Integer> getElemIdList() {
        return elemIdList;
    }

    public void setElemIdList(List<Integer> elemIdList) {
        this.elemIdList = elemIdList;
    }

    public List<Integer> getElemClr() {
        return elemClr;
    }

    public void setElemClr(List<Integer> elemClr) {
        this.elemClr = elemClr;
        this.elemClrObj = new REColor(elemClr.get(0), elemClr.get(1), elemClr.get(2), elemClr.get(3));
    }

    public REColor getElemClrObj() {
        return elemClrObj;
    }

    public void setElemClrObj(REColor elemClrObj) {
        this.elemClrObj = elemClrObj;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public boolean isAttrValid() {
        return attrValid;
    }

    public void setAttrValid(boolean attrValid) {
        this.attrValid = attrValid;
    }

    public int getProbeMask() {
        return probeMask;
    }

    public void setProbeMask(int probeMask) {
        this.probeMask = probeMask;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getResType() {
        return resType;
    }

    public void setResType(String resType) {
        this.resType = resType;
        if (resType.equals("HEIGHT")) {
            this.terrResEm =  RETerrResEm.HEIGHT;
        } else if (resType.equals("EXTRUDE")) {
            this.terrResEm =  RETerrResEm.EXTRUDE;
        } else if (resType.equals("IMG_SHP")) {
            this.terrResEm =  RETerrResEm.IMG_SHP;
        } else if (resType.equals("IMG_PIC")) {
            this.terrResEm =  RETerrResEm.IMG_PIC;
        } else if (resType.equals("ALL")) {
            this.terrResEm =  RETerrResEm.ALL;
        }
    }

    public RETerrResEm getTerrResEm() {
        return terrResEm;
    }

    public void setTerrResEm(RETerrResEm terrResEm) {
        this.terrResEm = terrResEm;
    }

    public List<String> getWaterNameList() {
        return waterNameList;
    }

    public void setWaterNameList(List<String> waterNameList) {
        this.waterNameList = waterNameList;
    }

    public List<String> getExtrudeIdst() {
        return extrudeIdst;
    }

    public void setExtrudeIdst(List<String> extrudeIdst) {
        this.extrudeIdst = extrudeIdst;
    }

    public List<String> getMonomerIds() {
        return monomerIds;
    }

    public void setMonomerIds(List<String> monomerIds) {
        this.monomerIds = monomerIds;
    }

    public String getWaterName() {
        return waterName;
    }

    public void setWaterName(String waterName) {
        this.waterName = waterName;
    }

    public String getExtrudeId() {
        return extrudeId;
    }

    public void setExtrudeId(String extrudeId) {
        this.extrudeId = extrudeId;
    }

    public double getBackDepth() {
        return backDepth;
    }

    public void setBackDepth(double backDepth) {
        this.backDepth = backDepth;
    }

    public List<RESelElemInfo> getLocIDList() {
        return locIDList;
    }

    public void setLocIDList(List<RESelElemInfo> locIDList) {
        this.locIDList = locIDList;
    }

    public String getLocType() {
        return locType;
    }

    public void setLocType(String locType) {
        this.locType = locType;

        switch (locType) {
            case "CAM_DIR_FRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_FRONT;
                break;
            case "CAM_DIR_BACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_BACK;
                break;
            case "CAM_DIR_LEFT":
                this.locTypeEm = RECamDirEm.CAM_DIR_LEFT;
                break;
            case "CAM_DIR_RIGHT":
                this.locTypeEm = RECamDirEm.CAM_DIR_RIGHT;
                break;
            case "CAM_DIR_TOP":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOP;
                break;
            case "CAM_DIR_BOTTOM":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOM;
                break;
            case "CAM_DIR_TOPFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPFRONT;
                break;
            case "CAM_DIR_TOPRIGHT":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPRIGHT;
                break;
            case "CAM_DIR_TOPBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPBACK;
                break;
            case "CAM_DIR_TOPLEFT":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPLEFT;
                break;
            case "CAM_DIR_LEFTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_LEFTFRONT;
                break;
            case "CAM_DIR_RIGHTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_RIGHTFRONT;
                break;
            case "CAM_DIR_RIGHTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_RIGHTBACK;
                break;
            case "CAM_DIR_LEFTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_LEFTBACK;
                break;
            case "CAM_DIR_BOTTOMFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMFRONT;
                break;
            case "CAM_DIR_BOTTOMRIGHT":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMRIGHT;
                break;
            case "CAM_DIR_BOTTOMBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMBACK;
                break;
            case "CAM_DIR_BOTTOMLEFT":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMLEFT;
                break;
            case "CAM_DIR_TOPRIGHTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPRIGHTBACK;
                break;
            case "CAM_DIR_TOPLEFTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPLEFTBACK;
                break;
            case "CAM_DIR_TOPLEFTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPLEFTFRONT;
                break;
            case "CAM_DIR_TOPRIGHTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_TOPRIGHTFRONT;
                break;
            case "CAM_DIR_BOTTOMRIGHTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMRIGHTBACK;
                break;
            case "CAM_DIR_BOTTOMLEFTBACK":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMLEFTBACK;
                break;
            case "CAM_DIR_BOTTOMLEFTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMLEFTFRONT;
                break;
            case "CAM_DIR_BOTTOMRIGHTFRONT":
                this.locTypeEm = RECamDirEm.CAM_DIR_BOTTOMRIGHTFRONT;
                break;
            case "CAM_DIR_DEFAULT":
                this.locTypeEm = RECamDirEm.CAM_DIR_DEFAULT;
                break;
            case "CAM_DIR_CURRENT":
                this.locTypeEm = RECamDirEm.CAM_DIR_CURRENT;
                break;
        }
    }

    public RECamDirEm getLocTypeEm() {
        return locTypeEm;
    }

    public void setLocTypeEm(RECamDirEm locTypeEm) {
        this.locTypeEm = locTypeEm;
    }

    public List<List<Double>> getArrBound() {
        return arrBound;
    }

    public void setArrBound(List<List<Double>> arrBound) {
        this.arrBound = arrBound;

        REDVec3 dMix = new REDVec3(arrBound.get(0).get(0), arrBound.get(0).get(1), arrBound.get(0).get(2));
        REDVec3 dMax = new REDVec3(arrBound.get(1).get(0), arrBound.get(1).get(1), arrBound.get(1).get(2));
        this.box3D = new REBBox3D(dMix, dMax);
    }

    public REBBox3D getBox3D() {
        return box3D;
    }

    public void setBox3D(REBBox3D box3D) {
        this.box3D = box3D;
    }

    public float getDepthBias() {
        return depthBias;
    }

    public void setDepthBias(float depthBias) {
        this.depthBias = depthBias;
    }

    public double getTopHeight() {
        return topHeight;
    }

    public void setTopHeight(double topHeight) {
        this.topHeight = topHeight;
    }

    public double getBottomHeight() {
        return bottomHeight;
    }

    public void setBottomHeight(double bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    public boolean isSingle() {
        return single;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }

    public List<Integer> getLineClr() {
        return lineClr;
    }

    public void setLineClr(List<Integer> lineClr) {
        this.lineClr = lineClr;
        this.lineClrObj = new REColor(lineClr.get(0), lineClr.get(1), lineClr.get(2), lineClr.get(3));
    }

    public REColor getLineClrObj() {
        return lineClrObj;
    }

    public void setLineClrObj(REColor lineClrObj) {
        this.lineClrObj = lineClrObj;
    }

    public int getLineClrWeight() {
        return lineClrWeight;
    }

    public void setLineClrWeight(int lineClrWeight) {
        this.lineClrWeight = lineClrWeight;
    }

    public int getLineAlphaWeight() {
        return lineAlphaWeight;
    }

    public void setLineAlphaWeight(int lineAlphaWeight) {
        this.lineAlphaWeight = lineAlphaWeight;
    }

    public List<Integer> getFaceClr() {
        return faceClr;
    }

    public void setFaceClr(List<Integer> faceClr) {
        this.faceClr = faceClr;
        this.faceClrObj = new REColor(faceClr.get(0), faceClr.get(1), faceClr.get(2), faceClr.get(3));
    }

    public REColor getFaceClrObj() {
        return faceClrObj;
    }

    public void setFaceClrObj(REColor faceClrObj) {
        this.faceClrObj = faceClrObj;
    }

    public int getFaceClrWeight() {
        return faceClrWeight;
    }

    public void setFaceClrWeight(int faceClrWeight) {
        this.faceClrWeight = faceClrWeight;
    }

    public int getFaceAlphaWeight() {
        return faceAlphaWeight;
    }

    public void setFaceAlphaWeight(int faceAlphaWeight) {
        this.faceAlphaWeight = faceAlphaWeight;
    }

    public boolean isVisibalOnly() {
        return visibalOnly;
    }

    public void setVisibalOnly(boolean visibalOnly) {
        this.visibalOnly = visibalOnly;
    }

    public List<Double> getCamPos() {
        return camPos;
    }

    public void setCamPos(List<Double> camPos) {
        this.camPos = camPos;
        this.camPosObj = new REDVec3(camPos.get(0), camPos.get(1), camPos.get(2));
    }

    public REDVec3 getCamPosObj() {
        return camPosObj;
    }

    public void setCamPosObj(REDVec3 camPosObj) {
        this.camPosObj = camPosObj;
    }

    public List<Double> getCamRotate() {
        return camRotate;
    }

    public void setCamRotate(List<Double> camRotate) {
        this.camRotate = camRotate;
        this.camRotateObj = new REDVec4(camRotate.get(0), camRotate.get(1), camRotate.get(2), camRotate.get(3));
    }

    public REDVec4 getCamRotateObj() {
        return camRotateObj;
    }

    public void setCamRotateObj(REDVec4 camRotateObj) {
        this.camRotateObj = camRotateObj;
    }

    public List<Double> getCamDir() {
        return camDir;
    }

    public void setCamDir(List<Double> camDir) {
        this.camDir = camDir;
        this.camDirObj = new REDVec3(camDir.get(0), camDir.get(1), camDir.get(2));
    }

    public REDVec3 getCamDirObj() {
        return camDirObj;
    }

    public void setCamDirObj(REDVec3 camDirObj) {
        this.camDirObj = camDirObj;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public double getLocDelay() {
        return locDelay;
    }

    public void setLocDelay(double locDelay) {
        this.locDelay = locDelay;
    }

    public double getLocTime() {
        return locTime;
    }

    public void setLocTime(double locTime) {
        this.locTime = locTime;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public String getWebPopId() {
        return webPopId;
    }

    public void setWebPopId(String webPopId) {
        this.webPopId = webPopId;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public JSONObject getRequestData() {
        return requestData;
    }

    public void setRequestData(JSONObject requestData) {
        this.requestData = requestData;
    }

    public List<JSONObject> getTreeData() {
        return treeData;
    }

    public void setTreeData(List<JSONObject> treeData) {
        this.treeData = treeData;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isKeepDir() {
        return keepDir;
    }

    public void setKeepDir(boolean keepDir) {
        this.keepDir = keepDir;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
}
