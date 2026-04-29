package com.realengine.offlineuniplugin.reclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import BlackHole3D.REDVec2;

public class REExtrudeTexUniData {

    public String picPath = "";
    public List<Double> picSize = new ArrayList<>(Arrays.asList(5.0, 5.0));
    public REDVec2 picSizeObj = new REDVec2(5.0, 5.0);
    public String textureGuid = "";




    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public List<Double> getPicSize() {
        return picSize;
    }

    public void setPicSize(List<Double> picSize) {
        this.picSize = picSize;
        this.picSizeObj = new REDVec2(picSize.get(0), picSize.get(1));
    }

    public REDVec2 getPicSizeObj() {
        return picSizeObj;
    }

    public void setPicSizeObj(REDVec2 picSizeObj) {
        this.picSizeObj = picSizeObj;
    }

    public String getTextureGuid() {
        return textureGuid;
    }

    public void setTextureGuid(String textureGuid) {
        this.textureGuid = textureGuid;
    }
}
