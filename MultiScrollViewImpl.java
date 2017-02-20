import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * scrollView嵌套滑动
 */
public class MultiScrollViewImpl extends ScrollView implements IMultiScrollHandler {

    private OnScrollListener onScrollListener;
    private boolean mIsScrollBottomEdge;

    public MultiScrollViewImpl(Context context) {
        this(context, null);
    }

    public MultiScrollViewImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (getChildAt(0).getMeasuredHeight() - t == getHeight()) {
            mIsScrollBottomEdge = true;
        } else {
            mIsScrollBottomEdge = false;
        }
        if (onScrollListener != null) {
            onScrollListener.onScrolled(l, t, oldl, oldt);
        }
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public interface OnScrollListener {
        void onScrolled(int l, int t, int oldl, int oldt);
    }

    @Override
    public boolean isScrolledEdge(int direction) {
        return direction > 0 ? mIsScrollBottomEdge : (getScrollY() <= 0);
    }

    @Override
    public View getInnerScrollView() {
        return this;
    }
}
