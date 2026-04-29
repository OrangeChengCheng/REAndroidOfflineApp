package com.realengine.offlineuniplugin.reui;

import static io.dcloud.feature.uniapp.common.TypeUniModuleFactory.TAG;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.realengine.offlineuniplugin.retool.REDeviceUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * WebView弹窗管理器
 * 功能：管理独立的WebView弹窗，支持显示/隐藏、全屏切换、URL加载等功能，
 * 每个实例相互独立，可同时管理多个不同弹窗
 */
public class REWebViewManager {
    // 弹窗总容器的唯一标识（用于在父布局中查找）
    private static final int POPUPS_CONTAINER_ID = 0x12345678;

    private Context mContext;                  // 上下文
    private WebView mWebView;                  // 核心WebView组件
    public String mWebViewUrl;                 // 加载的URL
    public Map<String, String> mWebViewUrlParams = new HashMap<>(); // URL参数
    private ViewGroup mParent;                 // 父容器（外部传入）
    private int mHeight;                       // 初始高度
    private ViewGroup mContainer;              // 当前弹窗的独立容器
    private ViewGroup mPopupsContainer;        // 所有弹窗的总容器
    private boolean mIsShowing = false;        // 当前弹窗是否显示
    private REWebJSInterface mWebAppInterface; // JS交互接口
    private final Handler mMainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler
    private Callback mCallback;                // 消息回调接口
    public boolean mIsUrlLoaded = false;      // URL是否已加载完成
    private int mOriginalWidth;                // 原始宽度（用于全屏恢复）
    private int mOriginalHeight;               // 原始高度（用于全屏恢复）
    private int mFullHeight;                   // 全屏状态下的高度
    private boolean mIsFullScreen = false;     // 是否处于全屏状态

    /**
     * 消息回调接口
     * 用于接收WebView中JavaScript发送的消息
     */
    public interface Callback {
        /**
         * 接收消息回调
         * @param jsonObject 解析后的JSON消息对象
         */
        void onMessageReceived(JSONObject jsonObject);
    }

    /**
     * 构造方法
     * @param context        上下文
     * @param parent         父容器（弹窗将添加到该容器中）
     * @param height         弹窗初始高度
     * @param webViewUrl     加载的URL
     * @param webViewParams  URL参数
     * @param callback       消息回调接口
     */
    public REWebViewManager(@NonNull Context context, ViewGroup parent, int height,
                            String webViewUrl, Map<String, String> webViewParams, Callback callback) {
        this.mContext = context;
        this.mParent = parent;
        this.mHeight = height;
        this.mOriginalHeight = height; // 初始化原始高度
        this.mWebViewUrl = webViewUrl;
        this.mWebViewUrlParams = webViewParams;
        this.mCallback = callback;

        initPopupsContainer();    // 初始化总容器
        createPopupContainer();   // 创建当前弹窗的独立容器
        initWebView();            // 初始化WebView
    }

    /**
     * 获取JS交互接口实例
     * @return REWebJSInterface实例，用于原生向JS发送消息
     */
    public REWebJSInterface getWebAppInterface() {
        return mWebAppInterface;
    }

    /**
     * 初始化所有弹窗的总容器
     * 若父布局中已存在总容器则复用，否则创建新容器并添加到父布局
     */
    private void initPopupsContainer() {
        // 从父布局中查找总容器
        mPopupsContainer = mParent.findViewById(POPUPS_CONTAINER_ID);

        if (mPopupsContainer == null) {
            // 创建总容器（使用FrameLayout兼容大多数场景）
            mPopupsContainer = new FrameLayout(mContext);
            mPopupsContainer.setId(POPUPS_CONTAINER_ID); // 设置唯一标识

            // 设置总容器布局参数（填满父布局）
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            mParent.addView(mPopupsContainer, params);
        }
    }

    /**
     * 创建当前弹窗的独立容器
     * 每个弹窗对应一个独立容器，确保相互不干扰
     */
    private void createPopupContainer() {
        mContainer = new FrameLayout(mContext); // 创建独立容器

        // 设置容器布局参数（填满总容器）
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        mPopupsContainer.addView(mContainer, params);
    }

    /**
     * 初始化WebView
     * 配置WebView参数、设置JS交互、加载URL等
     */
    private void initWebView() {
        // 创建WebView实例（使用应用上下文避免内存泄漏）
        mWebView = new WebView(mContext.getApplicationContext());
        WebSettings settings = mWebView.getSettings();

        // 基础配置
        settings.setJavaScriptEnabled(true);          // 启用JS
        settings.setDomStorageEnabled(true);          // 启用DOM存储
        settings.setLoadsImagesAutomatically(true);   // 自动加载图片
        settings.setAllowFileAccess(true);            // 允许访问文件
        settings.setCacheMode(WebSettings.LOAD_DEFAULT); // 缓存模式
        settings.setAllowUniversalAccessFromFileURLs(true); // 解决跨域

        // 支持HTTP/HTTPS混合内容（API 21+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 启用硬件加速（API 19+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // 提高渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // 设置WebView客户端（监听页面加载状态）
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mIsUrlLoaded = false; // 开始加载时标记为未完成
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mIsUrlLoaded = true; // 加载完成标记
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView加载错误: " + description + ", URL: " + failingUrl);
            }
        });

        // 设置Chrome客户端（支持JS弹窗等功能）
        mWebView.setWebChromeClient(new WebChromeClient());

        // 设置背景透明
        mWebView.setBackgroundColor(Color.TRANSPARENT);

        // 初始化JS交互接口
        mWebAppInterface = new REWebJSInterface(mContext, mWebView, new REWebJSInterface.Callback() {
            @Override
            public void onMessageReceived(String jsonMessage) {
                try {
                    // 解析JS消息并通过回调传递给外部
                    JSONObject jsonObject = JSON.parseObject(jsonMessage);
                    if (mCallback != null) {
                        mCallback.onMessageReceived(jsonObject);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析JS消息失败: " + e.getMessage());
                }
            }
        });
        mWebView.addJavascriptInterface(mWebAppInterface, "REMobileApp"); // 注册JS接口

        // 初始化布局样式并隐藏WebView
        setWebviewStyle(0, mHeight, 0);
        mWebView.setVisibility(View.GONE);

        // 加载URL（仅首次初始化时加载）
        if (mWebView != null && mWebViewUrl != null && !mWebViewUrl.isEmpty() && !mIsUrlLoaded) {
            loadUrl(mWebViewUrl, mWebViewUrlParams);
        }
    }

    /**
     * 设置WebView弹窗的样式（宽度、高度、边距等）
     * @param width        宽度（像素，0则使用屏幕宽度）
     * @param height       高度（像素，0则使用屏幕50%高度）
     * @param marginBottom 底部边距（像素）
     */
    public void setWebviewStyle(int width, int height, int marginBottom) {
        // 获取屏幕尺寸相关参数
        Resources resources = mContext.getResources();
        int screenWidth = resources.getDisplayMetrics().widthPixels;
        int drawScreenHeight = resources.getDisplayMetrics().heightPixels;
        int realScreenHeight = REDeviceUtils.getRealScreenHeight(mContext);
        float density = resources.getDisplayMetrics().density;
        int statusBarHeight = REDeviceUtils.getStatusBarHeight(mContext);
        int customNavHeight = (int) (44 * density); // 自定义导航栏高度

        // 计算有效屏幕高度（适配底部导航栏）
        int screenHeight = drawScreenHeight;
        if (realScreenHeight - screenHeight >= statusBarHeight) {
            screenHeight = realScreenHeight - statusBarHeight;
        }
        mFullHeight = screenHeight - customNavHeight; // 计算全屏高度

        // 处理宽度（0则使用屏幕宽度）
        if (width <= 0) {
            width = screenWidth;
        }

        // 处理高度（0则使用屏幕50%高度，否则按密度转换）
        if (height <= 0) {
            height = (int) (mFullHeight * 0.5);
        } else {
            height = (int) (height * density);
        }

        // 限制高度不超过屏幕可用高度
        if (height > mFullHeight - marginBottom) {
            height = mFullHeight - marginBottom;
        }

        // 保存原始尺寸（用于全屏恢复）
        mOriginalWidth = width;
        mOriginalHeight = height;

        // 应用布局参数
        setWebViewLayout(width, height, marginBottom);
    }

    /**
     * 应用WebView的布局参数到容器
     * 根据父布局类型（ConstraintLayout/FrameLayout）设置对应布局参数
     * @param width        宽度
     * @param height       高度
     * @param marginBottom 底部边距
     */
    public void setWebViewLayout(int width, int height, int marginBottom) {
        // 先将WebView从旧容器中移除（避免重复添加报错）
        if (mWebView != null && mWebView.getParent() != null) {
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
        }

        // 判断父布局类型（适配不同布局）
        boolean isParentConstraintLayout = (mParent instanceof ConstraintLayout);

        // 创建对应类型的容器
        if (isParentConstraintLayout) {
            mContainer = new ConstraintLayout(mContext);
        } else {
            mContainer = new FrameLayout(mContext);
        }

        // 移除旧容器（避免重复添加）
        mPopupsContainer.removeView(mContainer);

        // 设置WebView在容器中的布局参数
        ViewGroup.LayoutParams webViewParams;
        if (isParentConstraintLayout) {
            // ConstraintLayout布局参数（底部对齐父容器）
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(width, height);
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            params.horizontalBias = 0.5f; // 水平居中
            params.bottomMargin = marginBottom;
            webViewParams = params;
        } else {
            // FrameLayout布局参数（底部居中）
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = marginBottom;
            webViewParams = params;
        }

        // 将WebView添加到容器
        mContainer.removeAllViews();
        mContainer.addView(mWebView, webViewParams);

        // 将容器添加到总容器
        if (isParentConstraintLayout) {
            ConstraintLayout.LayoutParams containerParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
            );
            mPopupsContainer.addView(mContainer, containerParams);
        } else {
            mPopupsContainer.addView(mContainer, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    /**
     * 显示WebView弹窗
     * 在主线程中设置WebView可见
     */
    public void showWebPop() {
        mMainHandler.post(() -> {
            if (mWebView != null && mWebView.getVisibility() != View.VISIBLE) {
                mWebView.setVisibility(View.VISIBLE);
                mIsShowing = true;
            }
        });
    }

    /**
     * 隐藏WebView弹窗
     * 在主线程中设置WebView不可见
     */
    public void hiddenWebPop() {
        mMainHandler.post(() -> {
            if (mWebView != null && mWebView.getVisibility() != View.GONE) {
                mWebView.setVisibility(View.GONE);
                mIsShowing = false;
            }
        });
    }

    /**
     * 切换全屏/原始高度状态
     * @param isFull true-切换到全屏；false-恢复原始高度
     */
    public void setFullScreen(boolean isFull) {
        if (mIsFullScreen == isFull) {
            return; // 状态不变则直接返回
        }

        mIsFullScreen = isFull;
        mMainHandler.post(() -> {
            // 计算目标高度（全屏/原始）
            int targetHeight = isFull ? mFullHeight : mOriginalHeight;
            // 应用新布局
            setWebViewLayout(mOriginalWidth, targetHeight, 0);

            // 确保视图可见并刷新布局
            if (mWebView != null) {
                mWebView.setVisibility(View.VISIBLE);
                mWebView.requestLayout();
            }
            if (mContainer != null) {
                mContainer.requestLayout();
            }
        });
    }

    /**
     * 快速切换全屏状态（全屏↔原始高度）
     */
    public void toggleFullScreen() {
        setFullScreen(!mIsFullScreen);
    }

    /**
     * 加载指定URL（不带参数）
     * @param url 要加载的URL
     */
    public void loadUrl(String url) {
        if (mWebView != null && !mIsUrlLoaded) {
            Log.d("URLParams", "加载URL: " + url);
            mWebView.loadUrl(url);
        }
    }

    /**
     * 加载指定URL（带参数）
     * @param url    要加载的URL
     * @param params URL参数映射
     */
    public void loadUrl(String url, Map<String, String> params) {
        if (mWebView != null && !mIsUrlLoaded) {
            String fullUrl = appendParamsToUrl(url, params);
            loadUrl(fullUrl);
        }
    }

    /**
     * 将参数拼接到底URL后（处理编码）
     * @param url    基础URL
     * @param params 要拼接的参数
     * @return 拼接后的完整URL
     */
    private String appendParamsToUrl(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        boolean hasQuery = url.contains("?"); // 判断是否已有查询参数

        // 处理URL前缀（添加?或&）
        if (hasQuery) {
            if (!url.endsWith("?") && !url.endsWith("&")) {
                sb.append("&");
            }
        } else {
            sb.append("?");
            hasQuery = true;
        }

        // 拼接参数（UTF-8编码）
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                continue; // 跳过空键值
            }

            try {
                String encodedKey = URLEncoder.encode(key, "UTF-8");
                String encodedValue = URLEncoder.encode(value, "UTF-8");
                sb.append(encodedKey).append("=").append(encodedValue).append("&");
            } catch (UnsupportedEncodingException e) {
                Log.e("URLParams", "参数编码失败: " + e.getMessage());
            }
        }

        // 移除末尾多余的&
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '&') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 销毁当前WebView及相关资源
     * 释放内存，避免内存泄漏
     */
    public void destroy() {
        // 清除主线程未执行的任务
        mMainHandler.removeCallbacksAndMessages(null);

        // 销毁WebView
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView.destroy();
            mWebView = null;
            mIsUrlLoaded = false;
        }

        // 移除当前弹窗的独立容器
        if (mContainer != null && mContainer.getParent() != null) {
            ((ViewGroup) mContainer.getParent()).removeView(mContainer);
        }

        // 若总容器已无子视图，移除总容器
        if (mPopupsContainer != null && mPopupsContainer.getChildCount() == 0) {
            if (mPopupsContainer.getParent() != null) {
                ((ViewGroup) mPopupsContainer.getParent()).removeView(mPopupsContainer);
            }
        }

        mIsShowing = false;
    }

    /**
     * 判断当前弹窗是否显示
     * @return true-显示中；false-已隐藏
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * 判断当前是否处于全屏状态
     * @return true-全屏；false-非全屏
     */
    public boolean isFullScreen() {
        return mIsFullScreen;
    }
}