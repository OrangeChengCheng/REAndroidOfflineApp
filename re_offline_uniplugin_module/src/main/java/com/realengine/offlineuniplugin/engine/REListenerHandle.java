package com.realengine.offlineuniplugin.engine;

public interface REListenerHandle {

    void engineCreated(int procRet);

    void loadDataSetFinish(int procRet);

    void loadCADFinish(int procRet);

    void loadProgress(int progress, String str);

    void systemUIEvent(String btnName, int btnState);

    void systemSelElement(boolean success);

    void systemSelShpElement(boolean success);

}
