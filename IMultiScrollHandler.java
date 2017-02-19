package com.msile.views.multiscrollcontainer.multiscroll;

/**
 * view滑动监听
 */

public interface IMultiScrollHandler {

    //是否view滑动到底部\顶部边界 direction=1底部 direction=-1顶部
    boolean isScrolledEdge(int direction);

    //获取滑动view的高度
    int getContainHeight();

}
