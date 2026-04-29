package com.realengine.offlineuniplugin.engine;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.realengine.offlineuniplugin.R;
import com.realengine.offlineuniplugin.reclass.FileInfo;
import com.realengine.offlineuniplugin.reclass.REBridgeData;
import com.realengine.offlineuniplugin.reclass.RECornerRgnUniData;
import com.realengine.offlineuniplugin.reclass.REDataSetUniData;
import com.realengine.offlineuniplugin.reclass.REEntityUniData;
import com.realengine.offlineuniplugin.reclass.REExtrudeTexUniData;
import com.realengine.offlineuniplugin.reclass.REExtrudeUniData;
import com.realengine.offlineuniplugin.reclass.REMonomerUniData;
import com.realengine.offlineuniplugin.reclass.RESceneUniData;
import com.realengine.offlineuniplugin.reclass.REWaterUniData;
import com.realengine.offlineuniplugin.reclass.REWebPopData;
import com.realengine.offlineuniplugin.retool.DBUtil;
import com.realengine.offlineuniplugin.retool.FileUtil;
import com.realengine.offlineuniplugin.retool.StoragePermissionHelper;
import com.realengine.offlineuniplugin.reui.REBtnPlane;
import com.realengine.offlineuniplugin.reui.RELoading;
import com.realengine.offlineuniplugin.reui.RENav;
import com.realengine.offlineuniplugin.reui.RENavHandle;
import com.realengine.offlineuniplugin.reui.RETip;
import com.realengine.offlineuniplugin.reui.REWebViewManager;
import com.realengine.offlineuniplugin.uni.REModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import BlackHole3D.*;

public class REEngineActivity extends RealEngineActivity {
    static String TAG = "REEngineActivity";
    private RELoading re_loading;
    private RENav re_nav;
    private REBtnPlane re_btnPlane;
    private RESceneUniData sceneUniData;
    private List<REWebPopData> webPopList = new ArrayList<>();
    private REWebPopData currWebPop;
    private Map<String, Object> curSelProInfo;
    private boolean currIsRoomClip = false;

    //添加引擎回调监听
    public REListener reListener = new REListener(this, new REListenerHandle() {
        @Override
        public void engineCreated(int procRet) {

            // 设置世界坐标系
            if (!sceneUniData.worldCRS.isEmpty()) {
                BlackHole3D.Coordinate.setEngineWorldCRS(sceneUniData.worldCRS);
            }
            // 设置渲染模式
            BlackHole3D.Common.setFakeSphMode(!sceneUniData.shareViewMode.isEmpty() && sceneUniData.shareViewMode.equals("Sphere"));

            //提前加载挤出纹理资源
            if (!sceneUniData.extrudeList.isEmpty() && !sceneUniData.extrudeTexList.isEmpty()) {
                for(REExtrudeTexUniData texUniData : sceneUniData.extrudeTexList) {
                    BlackHole3D.Extrude.addExtrudeFaceTex(texUniData.picPath, texUniData.picSizeObj);
                }
            }

            if (sceneUniData.shareType == 1) {
                switch (sceneUniData.shareDataType) {
                    case "Bim":
                    case "bim":
                    case "Rs":
                    case "rs":
                    case "Wmts":
                    case "wmts":
                    case "Osgb":
                    case "osgb":
                    case "PointCloud":
                    case "pc":
                    case "Vector":
                    case "vector":
                    case "model":
                    case "models":
                        loadBim();
                        break;
                    case "CAD":
                    case "Cad":
                    case "cad":
                        loadCad();
                        break;
                    default:
                        cancelLoadAction(null);
                        break;
                }
            } else {
                //默认相机设置
//                if (!sceneUniData.defaultCamLoc.camPos.isEmpty()) {
//                    REForceCamLoc defaultCamLoc = new REForceCamLoc();
//                    defaultCamLoc.setForce(true);
//                    defaultCamLoc.setCamPos(new REDVec3(sceneUniData.defaultCamLoc.camPos.get(0), sceneUniData.defaultCamLoc.camPos.get(1), sceneUniData.defaultCamLoc.camPos.get(2)));
//                    defaultCamLoc.setCamDir(new REDVec3(sceneUniData.defaultCamLoc.camDir.get(0), sceneUniData.defaultCamLoc.camDir.get(1), sceneUniData.defaultCamLoc.camDir.get(2)));
//                    defaultCamLoc.setCamRotate(new REDVec4(sceneUniData.defaultCamLoc.camRotate.get(0), sceneUniData.defaultCamLoc.camRotate.get(1), sceneUniData.defaultCamLoc.camRotate.get(2), sceneUniData.defaultCamLoc.camRotate.get(3)));
//                    BlackHole3D.Camera.setCamForcedInitLoc(defaultCamLoc);
//                }
                loadBim();
            }

            // 设置最大渲染面数
            BlackHole3D.Common.setExpectMaxInstDrawFaceNum(sceneUniData.maxInstDrawFaceNum);
        }
        @Override
        public void loadDataSetFinish(int procRet) {
            if (procRet == 1) {
                Log.d("RealEngine","" + "REDataSetLoadFinish----------"+String.valueOf(procRet));
                if (!BlackHole3D.Model.getAllDataSetReady()) return;

                BlackHole3D.BIM.setContourLineClr("", new  BlackHole3D.REColor(255,0,0,-1));// 禁用轮廓线

//                if (sceneUniData.shareType == 2 && !sceneUniData.camDefaultDataSetId.isEmpty() && sceneUniData.defaultCamLoc == null) {
//                    BlackHole3D.Camera.setCamLocateToDataSet(sceneUniData.camDefaultDataSetId, 1.0, RECamDirEm.CAM_DIR_CURRENT);
//                }
//                BlackHole3D.Camera.setCamLocateToDataSet("3a1e60e5-00f9-3f4d-a9f4-a7a4e497a7ee", 1.0, RECamDirEm.CAM_DIR_CURRENT);

                // 处理地形数据层级
                for (REDataSetUniData dataSetInfo : sceneUniData.dataSetList) {
                    if (dataSetInfo.terrainLayerLev > 0) {
                        List<String> unitIDs = BlackHole3D.Terrain.getAllUnitNames(dataSetInfo.dataSetId);
                        String unitID = !unitIDs.isEmpty() ? unitIDs.get(0) : "";
                        BlackHole3D.Terrain.setUnitLayerlev(dataSetInfo.dataSetId, unitID, BlackHole3D.RETerrResEm.ALL, dataSetInfo.terrainLayerLev);
                    }
                }

//                initToolUI();// 加载面板信息
                initBtnPlane();//初始化按钮面板
                addEntity();// 添加单构件
                addWater();// 添加水面
                addExtrude();// 添加挤出
                addMonomer();// 添加单体化
                re_loading.hide();
            } else {
                REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T2, "模型资源加载失败！");
                cancelLoadAction(null);
            }
            for (REWebPopData webPopData : webPopList) {
                if (webPopData.webPopManager != null && webPopData.webPopManager.mIsUrlLoaded) {
                    webPopData.webPopManager.getWebAppInterface().sendObjAppToWeb(new Object(), "Listen.loadDataSetFinish");
                }
            }
        }
        @Override
        public void loadCADFinish(int procRet) {
            if (procRet == 1) {
                Log.d("RealEngine","" + "loadCADFinish----------"+String.valueOf(procRet));
                re_loading.hide();
            } else {
                REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T2, "模型资源加载失败！");
                cancelLoadAction(null);
            }
        }
        @Override
        public void loadProgress(int progress, String str) { re_loading.updateProgress(progress); }
        @Override
        public void systemUIEvent(String btnName, int btnState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                RETip.RETip_Level level = RETip.RETip_Level.RETip_L0;
                if (btnName.equals("BuiltIn_Btn_MainView") || btnName.equals("BuiltIn_Btn_PickClipPlane")) {
                    level = RETip.RETip_Level.RETip_L1;
                } else {
                    level = RETip.RETip_Level.RETip_L2;
                }
                if ((btnName.equals("BuiltIn_Btn_PickClipPlane") && btnState == 1)) {
                    if (currIsRoomClip && BlackHole3D.Clip.getClipState()) {
                        currIsRoomClip = false;
                        BlackHole3D.Clip.endClip();
                        BlackHole3D.BIM.resetElemAttr("", new ArrayList<>());
                    }
                    RETip.showRETip(REEngineActivity.this, findViewById(R.id.main_layout), "请在场景中选择剖切基点", level);
                }
                if ((btnName.equals("BuiltIn_Btn_More"))) {
                    // 更多操作引擎已经退出了剖切，需要处理样式重置
                    if (currIsRoomClip) {
                        currIsRoomClip = false;
                        BlackHole3D.BIM.resetElemAttr("", new ArrayList<>());
                    }
                }
            }
        }
        @Override
        public void systemSelElement(boolean success) {
            if (!BlackHole3D.Monomer.getAllCurSel().isEmpty()) {
                BlackHole3D.Monomer.delFromSel(new ArrayList<>());
            }
            if (success) {
                REProbeInfo probe = Probe.getCurCombProbeRet();
                if (Objects.equals(probe.getElemType(), "BIMElem")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        REDataSetUniData dataSet = sceneUniData.dataSetList
                                .stream()
                                .filter(item -> item.getDataSetId().equals(probe.getDataSetId()))
                                .findFirst()
                                .orElse(null);
                        if (dataSet == null) {
                            curSelProInfo = null;
                            return;
                        };
                        curSelProInfo = new HashMap<>();
                        curSelProInfo.put("selType", "Listen.systemSelElement");
                        curSelProInfo.put("dataSetType", dataSet.dataSetType);
                        curSelProInfo.put("probeInfo", probe);
                        if (dataSet.dataSetType != 0) {
                            curSelProInfo.put("entityList", sceneUniData.entityList);
                        }
                    }
                } else {
                    curSelProInfo = null;
                    return;
                }
            } else {
                curSelProInfo = null;
                return;
            }
            if (currWebPop != null && curSelProInfo != null) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWeb(curSelProInfo, "Listen.systemSelElement");
            }
        }
        @Override
        public void systemSelShpElement(boolean success) {
            if (success) {
                REProbeInfo probe = Probe.getCurCombProbeRet();
                if (!BlackHole3D.Monomer.getAllCurSel().isEmpty()) {
                    if (!Objects.equals(probe.getElemType(), "ShapeElem") || !probe.elemIdStr.contains("Monomer")) {
                        BlackHole3D.Monomer.delFromSel(new ArrayList<>());// 平台逻辑需要清理单体化的选择操作，如果是单体化矢量则无需操作，自行按照点击逻辑，其他则清除单体化选择
                    }
                }
                if (Objects.equals(probe.getElemType(), "ShapeElem")) {
                    List<REDataSetUniData> temp = new ArrayList<>(sceneUniData.dataSetList);
                    for (REDataSetUniData dataSet : temp) {
                        dataSet.dataSetSGContent = "";
                    }
                    curSelProInfo = new HashMap<>();
                    curSelProInfo.put("selType", "Listen.systemSelShpElement");
                    curSelProInfo.put("probeInfo", probe);
                    curSelProInfo.put("dataSetList", temp);// 交互操作的信息不需要content的数据，减少数据传输
//                    curSelProInfo.put("dataSetList", sceneUniData.dataSetList);
                } else {
                    curSelProInfo = null;
                    return;
                }
            } else {
                curSelProInfo = null;
                return;
            }
            if (currWebPop != null && curSelProInfo != null) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWeb(curSelProInfo, "Listen.systemSelShpElement");
            }
        }
    });

    public void cancelLoadAction(View view) {
        if (!sceneUniData.waterList.isEmpty()) { BlackHole3D.Water.delData(new ArrayList<>()); }
        if (!sceneUniData.extrudeList.isEmpty()) { BlackHole3D.Extrude.delData(new ArrayList<>()); }
        if (!sceneUniData.monomerList.isEmpty()) { BlackHole3D.Monomer.delData(new ArrayList<>()); }

        if (sceneUniData.shareType == 1 && sceneUniData.shareDataType.equals("Cad")) {
            BlackHole3D.CAD.unloadCAD();
        } else {
            BlackHole3D.Model.unloadAllDataSet();
        }
        BlackHole3D.System.releaseEngine();
        re_loading.stopUpdatingTip();
        finish();
    }

    public void loadBim() {
        BlackHole3D.System.setViewMode(REVpTypeEm.BIM,REVpTypeEm.None,REVpRankEm.Single);
        List<REDataSet> arrDataSet =new ArrayList<>();
        for (REDataSetUniData dataSetInfo : sceneUniData.dataSetList) {
            REDataSet dataSet = new REDataSet();
            dataSet.setDataSetId(dataSetInfo.dataSetId);
            dataSet.setResourcesAddress(dataSetInfo.resourcesAddress);
            dataSet.setScale(new REDVec3(dataSetInfo.scale.get(0), dataSetInfo.scale.get(1), dataSetInfo.scale.get(2)));
            dataSet.setRotate(new REDVec4(dataSetInfo.rotate.get(0), dataSetInfo.rotate.get(1), dataSetInfo.rotate.get(2), dataSetInfo.rotate.get(3)));
            dataSet.setOffset(new REDVec3(dataSetInfo.offset.get(0), dataSetInfo.offset.get(1), dataSetInfo.offset.get(2)));
            dataSet.setDataSetCRS(dataSetInfo.dataSetCRS);
            dataSet.setDataSetCRSNorth(dataSetInfo.dataSetCRSNorth);
            dataSet.setEngineOrigin(new REDVec3(dataSetInfo.engineOrigin.get(0), dataSetInfo.engineOrigin.get(1), dataSetInfo.engineOrigin.get(2)));
            dataSet.setDataSetSGContent(dataSetInfo.dataSetSGContent);
            arrDataSet.add(dataSet);
        }
        BlackHole3D.Model.loadDataSet(arrDataSet,true);
    }
    public void loadCad() {
        //设置横向单屏显示CAD
        BlackHole3D.System.setViewMode(REVpTypeEm.CAD,REVpTypeEm.None,REVpRankEm.Single);
        REDataSetUniData dataSetInfo = sceneUniData.dataSetList.get(0);
        BlackHole3D.CAD.loadCAD(dataSetInfo.resourcesAddress, RECadUnitEm.valueOf(dataSetInfo.unit),1.0);
    }
    public void addEntity() {
        if (sceneUniData.entityList.isEmpty()) { return; }
        List<BlackHole3D.REEntityInfo> re_entityList =new ArrayList<>();
        for (REEntityUniData entityInfo : sceneUniData.entityList) {
            BlackHole3D.REEntityInfo entity = new BlackHole3D.REEntityInfo();
            entity.setDataSetId(entityInfo.dataSetId);
            entity.setEntityType(entityInfo.entityType);
            entity.setElemId(entityInfo.elemId);
            entity.setScale(new REDVec3(entityInfo.scale.get(0), entityInfo.scale.get(1), entityInfo.scale.get(2)));
            entity.setRotate(new REDVec4(entityInfo.rotate.get(0), entityInfo.rotate.get(1), entityInfo.rotate.get(2), entityInfo.rotate.get(3)));
            entity.setOffset(new REDVec3(entityInfo.offset.get(0), entityInfo.offset.get(1), entityInfo.offset.get(2)));
            entity.setDataSetCRS(entityInfo.dataSetCRS);
            re_entityList.add(entity);
        }
        BlackHole3D.Entity.enterEditMode();
        BlackHole3D.Entity.addEntities(re_entityList); //添加实例对象
        BlackHole3D.Entity.exitEditMode(); //结束编辑模式
    }
    public void addWater() {
        if (sceneUniData.waterList.isEmpty()) { return; }

        List<BlackHole3D.REWaterInfo> re_waterList =new ArrayList<>();
        for (REWaterUniData waterInfo : sceneUniData.waterList) {
            RECornerRgnUniData uni_cornerRgnInfo = waterInfo.rgnList.get(0);
            List<RECornerRgnInfo> rgnList = new ArrayList<>();
            RECornerRgnInfo cornerRgnInfo = new BlackHole3D.RECornerRgnInfo();
            List<REDVec3> potList = new ArrayList<>();
            for (List<Double> pot : uni_cornerRgnInfo.pointList) {
                potList.add(new REDVec3(pot.get(0), pot.get(1), pot.get(2)));
            }
            cornerRgnInfo.pointList = potList;
            cornerRgnInfo.indexList = uni_cornerRgnInfo.indexList;
            rgnList.add(cornerRgnInfo);

            REWaterInfo re_waterInfo = new BlackHole3D.REWaterInfo();
            re_waterInfo.waterName = waterInfo.waterName;
            re_waterInfo.waterClr = new REColor(waterInfo.waterClr.get(0), waterInfo.waterClr.get(1), waterInfo.waterClr.get(2), waterInfo.waterClr.get(3));
            re_waterInfo.blendDist = waterInfo.blendDist;
            re_waterInfo.visible = waterInfo.visible;
            re_waterInfo.depthBias = waterInfo.depthBias;
            re_waterInfo.expandDist = waterInfo.expandDist;
            re_waterInfo.visDist = waterInfo.visDist;
            re_waterInfo.rgnList = rgnList;
            re_waterList.add(re_waterInfo);
        }
        BlackHole3D.Water.setData(re_waterList);
    }
    public void addExtrude() {
        if (sceneUniData.extrudeList.isEmpty()) { return; }

        List<REExtrudeInfo> re_extrudeList = new ArrayList<>();
        for (REExtrudeUniData extrudeInfo : sceneUniData.extrudeList) {
            List<String> dataSetIdList_line = new ArrayList<>();
            for (REDataSetUniData dataSet : sceneUniData.dataSetList) {
                //无奈处理平台横线数据不一致的问题
                if (extrudeInfo.dataSetIdList.contains(dataSet.dataSetId_noline)) {
                    dataSetIdList_line.add(dataSet.dataSetId);
                }
            }
            REExtrudeInfo re_extrudeInfo = new BlackHole3D.REExtrudeInfo();
            re_extrudeInfo.setExtrudeId(extrudeInfo.extrudeId);
            re_extrudeInfo.setDataSetIdList(dataSetIdList_line);
            List<REDVec3> potList = new ArrayList<>();
            List<List<Double>> potList_temp = extrudeInfo.rgnList.get(0);
            for (List<Double> pot : potList_temp) {
                potList.add(new REDVec3(pot.get(0), pot.get(1), pot.get(2)));
            }
            List<List<REDVec3>> rgnList = new ArrayList<>();
            rgnList.add(potList);
            re_extrudeInfo.setRgnList(rgnList);
            re_extrudeInfo.setDepthLimitRange(new REDVec2(extrudeInfo.depthLimitRange.get(0), extrudeInfo.depthLimitRange.get(1)));
            re_extrudeInfo.setType(extrudeInfo.type);
            re_extrudeInfo.setTexId(extrudeInfo.texId);
            re_extrudeInfo.setTexPath(extrudeInfo.texPath);
            re_extrudeInfo.setTexSize(new REDVec2(extrudeInfo.texSize.get(0), extrudeInfo.texSize.get(1)));
            re_extrudeList.add(re_extrudeInfo);
        }
        BlackHole3D.Extrude.setData(re_extrudeList);
    }
    public void addMonomer() {
        if (sceneUniData.monomerList.isEmpty()) { return; }

        List<REMonomerInfo> re_monomerList = new ArrayList<>();
        for (REMonomerUniData monomerInfo : sceneUniData.monomerList) {

            REMonomerInfo re_monomerInfo = new BlackHole3D.REMonomerInfo();
            re_monomerInfo.monomerId = monomerInfo.monomerId;
            re_monomerInfo.dataSetId = monomerInfo.dataSetId;
            List<REDVec3> potList = new ArrayList<>();
            List<List<Double>> potList_temp = monomerInfo.rgnList.get(0);
            for (List<Double> pot : potList_temp) {
                potList.add(new REDVec3(pot.get(0), pot.get(1), pot.get(2)));
            }
            List<List<REDVec3>> rgnList = new ArrayList<>();
            rgnList.add(potList);
            re_monomerInfo.rgnList = rgnList;
            re_monomerInfo.heightMax = monomerInfo.heightMax;
            re_monomerInfo.heightMin = monomerInfo.heightMin;
            re_monomerInfo.faceClr = new REColor(monomerInfo.faceClr.get(0), monomerInfo.faceClr.get(1), monomerInfo.faceClr.get(2), monomerInfo.faceClr.get(3));
            re_monomerInfo.lineClr = new REColor(monomerInfo.lineClr.get(0), monomerInfo.lineClr.get(1), monomerInfo.lineClr.get(2), monomerInfo.lineClr.get(3));
            re_monomerInfo.showState = monomerInfo.showState;
            re_monomerList.add(re_monomerInfo);
        }
        BlackHole3D.Monomer.setData(re_monomerList);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        accessPermission();
        //处理数据
        handleData();
        //注册引擎监听
        registerUniListener();
        //加载主界面要在注册监听之后
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //注册引擎渲染界面
        initEngine();
        //初始化导航栏
        initNav();
        //初始化RELoading
        re_loading = new RELoading(this, findViewById(R.id.main_layout));
        //初始化webview
        initWebView();
        initUI();
    }
    private void accessPermission() {
        // 获取存储权限
        StoragePermissionHelper.hasStoragePermission(this);
//        if (Build.VERSION.SDK_INT >= 30) {
//            if (!Environment.isExternalStorageManager()) {
//                Intent intent = new Intent();
//                // 使用正确的 Action: ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//
//                // 关键步骤：设置 Data 为 "package:你的应用包名"
//                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
//                intent.setData(uri);
//
//                // 启动 Intent
//                startActivity(intent);
//            }
//        }
    }
    private void initUI() {
        // 设置状态栏颜色与导航栏一致（#0368ff）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#000000"));

            // 沉浸式设置（让布局延伸到状态栏）
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  );
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
    private void initEngine() {
        mSurface = new BlackHole3D.RealEngineSurface(getApplication());
        mLayout =findViewById(R.id.re_win);
        if (mLayout!=null){
            mLayout.addView(mSurface);
        }
    }
    private void handleData() {
        String intent_obj = getIntent().getStringExtra("intent_obj");
        // 将JSON字符串解析为JSONObject
        JSONObject jsonObject = JSON.parseObject(intent_obj);

        if (jsonObject != null) {
            sceneUniData = JSON.parseObject(jsonObject.toJSONString(), new TypeReference<RESceneUniData>(){});
        }

        // 添加webPop数据
        {
            String webUrl = "file:///android_asset/AppExpand/index.html#";
//            String webUrl = "http://192.168.31.197:8080/#";
            Map<String, String> params = new HashMap<>();
            params.put("shareType", String.valueOf(sceneUniData.shareType));
            params.put("sceneId", sceneUniData.sceneId);
            params.put("filePath", sceneUniData.filePath);

            Map<String, String> params_tree = new HashMap<>(params);
            params_tree.put("webPopId", "web_pop_tree");
            REWebPopData webPopData_tree = new REWebPopData("web_pop_tree", webUrl + "/tree", params_tree, 360);

            Map<String, String> params_property = new HashMap<>(params);
            params_property.put("webPopId", "web_pop_property");
            REWebPopData webPopData_property = new REWebPopData("web_pop_property", webUrl + "/property", params_property, 360);

            webPopList = new ArrayList<>(Arrays.asList(webPopData_tree, webPopData_property));
        }
    }
    private void registerUniListener() {
        BlackHole3D.System.AddEventListener(reListener);
        REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_T1, "Engine Activity onCreate");
        REModule.registerUniToAppMsg(jsonObject -> {
            handleEngineSDK(jsonObject, 1);
        });
    }
    private void initNav() {
        re_nav = new RENav(this, findViewById(R.id.main_layout), "黑洞引擎", new RENavHandle() {
            @Override
            public void navBackCallBack() {
                Log.d(TAG,"" + "navBackCallBack----------");
                cancelLoadAction(null);
            }
            @Override
            public void navScanCallBack() { }
        });

    }
    private void initBtnPlane() {
        if (sceneUniData.shareType == 1
                && !(sceneUniData.shareDataType.equals("Bim")
                || sceneUniData.shareDataType.equals("bim")
                || sceneUniData.shareDataType.equals("model")
                || sceneUniData.shareDataType.equals("models"))) {
            return;
        }

        re_btnPlane = new REBtnPlane(this, findViewById(R.id.main_layout), new REBtnPlane.BtnCallback() {
            @Override
            public void onClickCallback(String btnId, boolean isSelected) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    REWebPopData webPopData = webPopList
                            .stream()
                            .filter(item -> item.webPopId.equals(btnId))
                            .findFirst()
                            .orElse(null);
                    if (webPopData != null) {
                        Map<String, String> params = new HashMap<>();
                        params.put("webPopId", webPopData.webPopId);
                        if (isSelected) {
                            webPopData.webPopManager.showWebPop();
                            webPopData.webPopManager.getWebAppInterface().sendObjAppToWeb(params, "open");
                            if (curSelProInfo != null) {
                                webPopData.webPopManager.getWebAppInterface().sendObjAppToWeb(curSelProInfo, (String) curSelProInfo.get("selType"));
                            }
                            currWebPop = webPopData;
                        } else {
                            webPopData.webPopManager.getWebAppInterface().sendObjAppToWeb(params, "cloose");
                        }
                        webPopData.webPopShow = isSelected;
                    }
                }
            }
        });
    }
    private void initWebView() {
        for (REWebPopData webPopData : webPopList) {
            webPopData.webPopManager = new REWebViewManager(
                    this,
                    findViewById(R.id.main_layout),
                    webPopData.webPopHeight,
                    webPopData.webPopUrl,
                    webPopData.webPopParams,
                    new REWebViewManager.Callback() {
                        @Override
                        public void onMessageReceived(JSONObject jsonObject) {
                            handleEngineSDK(jsonObject, 2);
                        }
                    });
        }
    }
    private void handleEngineSDK (JSONObject jsonObject, int msgWhere) {
        String msgId = jsonObject.containsKey("msgId") ? String.valueOf(jsonObject.get("msgId")) : "";
        String webPopId = jsonObject.containsKey("webPopId") ? String.valueOf(jsonObject.get("webPopId")) : "";
        String type = jsonObject.containsKey("type") ? String.valueOf(jsonObject.get("type")) : "";
        JSONObject json_data = jsonObject.containsKey("data") ? jsonObject.getObject("data", jsonObject.getClass()) : null;
        if (json_data == null) return;
        REBridgeData bridgeData = json_data.toJavaObject(REBridgeData.class);

        // 获取弹窗数据
        REWebPopData webPopData = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            webPopData = webPopList
                    .stream()
                    .filter(item -> item.webPopId.equals(webPopId))
                    .findFirst()
                    .orElse(null);
        }

        if (type.equals("log")) {
            Log.d(TAG, "--->>【WebLog】: (WebPop)   " + bridgeData.log);
        } else if (type.equals("requestAppToWeb")) {

        }  else if (type.equals("dbQuery")) {
            List<Map<String, Object>> data = DBUtil.queryToList(bridgeData.dbPath, bridgeData.sql, null);
            JSONObject result = new JSONObject();
            result.put("success", data.isEmpty() ? false : true);
            result.put("data", data);
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(result, msgId);
            }
        }  else if (type.equals("dbTableExist")) {
            boolean isExist = DBUtil.isTableExists(bridgeData.dbPath, bridgeData.tableName);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", isExist);
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(result, msgId);
            }
        }  else if (type.equals("fileGetAllChild")) {
            List<FileInfo> fileList = FileUtil.getAllChild(bridgeData.filePath);
            JSONObject result = new JSONObject();
            result.put("success", fileList.size() > 0 ? true : false);
            result.put("data", fileList);
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(result, msgId);
            }
        }  else if (type.equals("fileGetChildBySuffix")) {
            List<FileInfo> fileList = FileUtil.getChildFilesBySuffix(bridgeData.filePath, bridgeData.suffix);
            JSONObject result = new JSONObject();
            result.put("success", fileList.size() > 0 ? true : false);
            result.put("data", fileList);
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(result, msgId);
            }
        } else if (type.equals("cloose")) {
            if (webPopData != null) {
                webPopData.webPopManager.hiddenWebPop();
                webPopData.webPopShow = false;
                re_btnPlane.updataState(webPopData.webPopId, false);
                if (currWebPop.webPopId.equals(webPopData.webPopId)) {
                    currWebPop = null;
                }
            }
        } else if (type.equals("updateTreeData")) {
            if (webPopData != null && msgWhere == 2) {
                REWebPopData webPopData_property = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    webPopData_property = webPopList
                            .stream()
                            .filter(item -> item.webPopId.equals("web_pop_property"))
                            .findFirst()
                            .orElse(null);
                }
                if (webPopData_property != null) {
                    webPopData_property.webPopManager.getWebAppInterface().sendObjAppToWeb(bridgeData.treeData, "updateTreeData");
                }
            }
        } else if (type.equals("setElemAlpha")) {
            BlackHole3D.BIM.setElemAlpha(bridgeData.dataSetId, bridgeData.elemIdList, bridgeData.alpha);
        } else if (type.equals("setElemsValidState")) {
            BlackHole3D.BIM.setElemsValidState(bridgeData.dataSetId, bridgeData.elemIdList, bridgeData.visible);
        } else if (type.equals("setCamLocateToDataSet")) {
            BlackHole3D.Camera.setCamLocateToDataSet(bridgeData.dataSetId, bridgeData.backDepth, bridgeData.locTypeEm);
        } else if (type.equals("setCamLocateToElem")) {
            BlackHole3D.Camera.setCamLocateToElem(bridgeData.locIDList, bridgeData.backDepth, bridgeData.locTypeEm);
        } else if (type.equals("popWebFull")) {
            currWebPop.webPopManager.setFullScreen(bridgeData.full);
        } else if (type.equals("setElemAttr")) {
            REElemAttr elemAttr = new BlackHole3D.REElemAttr();
            elemAttr.dataSetId = bridgeData.dataSetId;
            elemAttr.elemIdList = bridgeData.elemIdList;
            elemAttr.elemClr = bridgeData.elemClrObj;
            BlackHole3D.BIM.setElemAttr(elemAttr);
        } else if (type.equals("resetElemAttr")) {
            BlackHole3D.BIM.resetElemAttr(bridgeData.dataSetId, bridgeData.elemIdList);
        } else if (type.equals("setCamLocateTo")) {
            BlackHole3D.RECamLoc reCamLoc =new BlackHole3D.RECamLoc();
            reCamLoc.camRotate = bridgeData.camRotateObj;
            reCamLoc.camPos = bridgeData.camPosObj;
            BlackHole3D.Camera.setCamLocateTo(reCamLoc, bridgeData.locDelay, bridgeData.locTime);
        } else if (type.equals("delAllSelElems")) {
            BlackHole3D.BIM.delAllSelElems();
        } else if (type.equals("getCamLocate")) {
            RECamLoc camLoc = BlackHole3D.Camera.getCamLocate();
            if (currWebPop != null && msgWhere == 2) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWebCallback(camLoc, msgId);
            } else if (msgWhere == 1) {
                runOnUiThread(() -> {
                    REModule.sendMsgAppToUni(REModule.REModule_CallBackEm.REModule_GetCamLoc, camLoc);
                });
            }
        } else if (type.equals("webOnMounted")) {

        } else if (type.equals("addToSelElems")) {
            BlackHole3D.BIM.addToSelElems(bridgeData.dataSetId, bridgeData.elemIdList);
        } else if (type.equals("setSelElemsAttr")) {
            BlackHole3D.RESelElemsAttr reSelElemsAttr =new RESelElemsAttr();
            reSelElemsAttr.elemClr = new BlackHole3D.REColor(bridgeData.elemClr.get(0), bridgeData.elemClr.get(1), bridgeData.elemClr.get(2), bridgeData.elemClr.get(3));
            reSelElemsAttr.probeMask = bridgeData.probeMask;
            reSelElemsAttr.attrValid = bridgeData.attrValid;
            BlackHole3D.BIM.setSelElemsAttr(reSelElemsAttr);
        } else if (type.equals("getBIMDataSetBV")) {
            BlackHole3D.REBBox3D bbox = BlackHole3D.BIM.getTotalBV(bridgeData.dataSetId);
            if (currWebPop != null && msgWhere == 2) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWebCallback(bbox, msgId);
            }
        } else if (type.equals("getGridDataSetBV")) {
            BlackHole3D.REBBox3D bbox = BlackHole3D.Grid.getDataSetBV(bridgeData.dataSetId);
            if (currWebPop != null && msgWhere == 2) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWebCallback(bbox, msgId);
            }
        } else if (type.equals("setCamLocToWater")) {
            BlackHole3D.Water.setCamToData(bridgeData.waterNameList);
        } else if (type.equals("setCamLocToExtrude")) {
            BlackHole3D.Extrude.setCamToData(bridgeData.extrudeIdst);
        } else if (type.equals("getDataSetTerrId")) {
            String terrId = BlackHole3D.Terrain.getDataSetTerrId(bridgeData.dataSetId);
            if (currWebPop != null && msgWhere == 2) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWebCallback(terrId, msgId);
            }
        } else if (type.equals("getAllUnitNames")) {
            List<String> UnitNames = BlackHole3D.Terrain.getAllUnitNames(bridgeData.dataSetId);
            if (currWebPop != null && msgWhere == 2) {
                currWebPop.webPopManager.getWebAppInterface().sendObjAppToWebCallback(UnitNames, msgId);
            }
        } else if (type.equals("setUnitActive")) {
            BlackHole3D.Terrain.setUnitActive(bridgeData.dataSetId, bridgeData.unitId, bridgeData.terrResEm, bridgeData.active);
        } else if (type.equals("setGridValidState")) {
            BlackHole3D.Grid.setValidState(bridgeData.dataSetId, bridgeData.visible);
        } else if (type.equals("waterVisible")) {
            BlackHole3D.Water.setVisible(bridgeData.waterName,bridgeData.visible);
        } else if (type.equals("extrudeVisible")) {
            BlackHole3D.Extrude.setVisible(bridgeData.waterName,bridgeData.visible);
        } else if (type.equals("setCamLocateToBound")) {
            BlackHole3D.Camera.setCamLocateToBound(bridgeData.box3D, bridgeData.backDepth, bridgeData.locTypeEm);
        } else if (type.equals("getDataSetAllElemIDs")) {
            List<Integer> elemIdList = BlackHole3D.BIM.getDataSetAllElemIDs(bridgeData.dataSetId, bridgeData.visibalOnly);
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(elemIdList, msgId);
            }
        } else if (type.equals("setClipPlanesContourLineClr")) {
            BlackHole3D.BIM.setClipPlanesContourLineClr(bridgeData.lineClrObj);
        } else if (type.equals("setClipSpecifyHeight")) {
            BlackHole3D.Clip.setClipSpecifyHeight(bridgeData.dataSetId, bridgeData.topHeight, bridgeData.bottomHeight, bridgeData.single);
            currIsRoomClip = true;
        } else if (type.equals("endClip")) {
            currIsRoomClip = false;
            BlackHole3D.Clip.endClip();
        } else if (type.equals("setElemDepthBias")) {
            BlackHole3D.BIM.setElemDepthBias(bridgeData.dataSetId, bridgeData.elemIdList, bridgeData.depthBias);
        } else if (type.equals("setCamLocToMonomer")) {
            BlackHole3D.Monomer.setCamToData(bridgeData.monomerIds);
        } else if (type.equals("addToSelMonomer")) {
            BlackHole3D.Monomer.addToSel(bridgeData.monomerIds);
        } else if (type.equals("delFromSelMonomer")) {
            BlackHole3D.Monomer.delFromSel(bridgeData.monomerIds);
        } else if (type.equals("getMonomerSelAttr")) {
            REMonomerClrAttr monomerClrAttr = BlackHole3D.Monomer.getSelAttr();
            if (webPopData != null && msgWhere == 2) {
                webPopData.webPopManager.getWebAppInterface().sendObjAppToWebCallback(monomerClrAttr, msgId);
            }
        } else if (type.equals("setMonomerSelAttr")) {
            REMonomerClrAttr monomerClrAttr = new REMonomerClrAttr();
            monomerClrAttr.faceClr = bridgeData.faceClrObj;
            monomerClrAttr.faceClrWeight = bridgeData.faceClrWeight;
            monomerClrAttr.faceAlphaWeight = bridgeData.faceAlphaWeight;
            monomerClrAttr.lineClr = bridgeData.lineClrObj;
            monomerClrAttr.lineClrWeight = bridgeData.lineClrWeight;
            monomerClrAttr.lineAlphaWeight = bridgeData.lineAlphaWeight;
            BlackHole3D.Monomer.setSelAttr(monomerClrAttr);
        } else if (type.equals("monomerVisible")) {
            BlackHole3D.Monomer.setVisible(bridgeData.monomerIds, bridgeData.visible);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!webPopList.isEmpty()) {
            // 销毁web管理器
            for (REWebPopData webPopData : webPopList) {
                webPopData.webPopManager.destroy();
            }
            webPopList.clear();
            webPopList = null;
        }
    }
}
