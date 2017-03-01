import android.view.View;

/**
 * view滑动监听
 */

public interface IMultiScrollHandler {

    //是否view滑动到底部\顶部边界 direction=1底部 direction=-1顶部
    boolean isScrolledEdge(int direction);

    //获取内部真正滑动的view
    View getInnerScrollView();
}
