package com.realengine.offlineuniplugin.reclass;


import com.realengine.offlineuniplugin.reui.REWebViewManager;

import java.util.HashMap;
import java.util.Map;

public class REWebPopData {

    public REWebViewManager webPopManager;
    public String webPopId = "";
    public boolean webPopShow = false;
    public String webPopUrl = "";
    public int webPopHeight = 400;
    public Map<String, String> webPopParams = new HashMap<>();



    public REWebPopData(String webPopId, String webPopUrl, Map<String, String> webPopParams, int webPopHeight) {
        this.webPopManager = null;
        this.webPopId = webPopId;
        this.webPopUrl = webPopUrl;
        this.webPopParams = webPopParams;
        this.webPopHeight = webPopHeight;
    }


    public REWebViewManager getWebPopManager() {
        return webPopManager;
    }

    public void setWebPopManager(REWebViewManager webPopManager) {
        this.webPopManager = webPopManager;
    }

    public String getWebPopId() {
        return webPopId;
    }

    public void setWebPopId(String webPopId) {
        this.webPopId = webPopId;
    }

    public boolean isWebPopShow() {
        return webPopShow;
    }

    public void setWebPopShow(boolean webPopShow) {
        this.webPopShow = webPopShow;
    }

    public String getWebPopUrl() {
        return webPopUrl;
    }

    public void setWebPopUrl(String webPopUrl) {
        this.webPopUrl = webPopUrl;
    }

    public int getWebPopHeight() {
        return webPopHeight;
    }

    public void setWebPopHeight(int webPopHeight) {
        this.webPopHeight = webPopHeight;
    }

    public Map<String, String> getWebPopParams() {
        return webPopParams;
    }

    public void setWebPopParams(Map<String, String> webPopParams) {
        this.webPopParams = webPopParams;
    }
}
