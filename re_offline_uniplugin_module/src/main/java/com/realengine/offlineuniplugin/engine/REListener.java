package com.realengine.offlineuniplugin.engine;

import java.util.ArrayList;
import java.util.List;

import BlackHole3D.REDVec2;
import BlackHole3D.REDataSet;
import BlackHole3D.RealEngineIListener;

public class REListener implements RealEngineIListener {
    @Override
    public void RESystemEngineCreated(int i) {
        List<REDataSet> arrDataSet =new ArrayList<>();
        BlackHole3D.REDataSet dataSet = new BlackHole3D.REDataSet();
        dataSet.setDataSetId("dataSet01");
        dataSet.setResourcesAddress("https://demo.bjblackhole.com/default.aspx?dir=url_res03&path=res_jifang");
        arrDataSet.add(dataSet);
        BlackHole3D.Model.loadDataSet(arrDataSet,true);
    }

    @Override
    public void REDataSetLoadFinish(int i) {

    }

    @Override
    public void REDataSetLoadProgress(int i, String s) {

    }

    @Override
    public void RESystemSelElement(boolean b) {

    }

    @Override
    public void RESystemSelShpElement(boolean b) {

    }

    @Override
    public void RELocateCam(int i) {

    }

    @Override
    public void RECameraMove() {

    }

    @Override
    public void REClipFinish(int i) {

    }

    @Override
    public void REDataSetLoadPanFinish(int i) {

    }

    @Override
    public void REPanLoadSingleFinish(int i) {

    }

    @Override
    public void REPanSelShpElement(boolean b) {

    }

    @Override
    public void RECADLoadFinish(int i) {

    }

    @Override
    public void RECADSelElement(String s, String s1, REDVec2 redVec2) {

    }

    @Override
    public void RECADSelAnchor(String s) {

    }

    @Override
    public void RECADSelShpAnchor(String s) {

    }

    @Override
    public void REMiniMapLoadCAD(int i) {

    }

    @Override
    public void REMiniMapCADSelShpAnchor(String s) {

    }

    @Override
    public void REElevationUpdateFinish(int i) {

    }

    @Override
    public void REAxisGridUpdateFinish(int i) {

    }

    @Override
    public void REAddEntityFinish(int i) {

    }

    @Override
    public void RESystemUIEvent(String s, int i) {

    }
}
