import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.memebox.cn.android.base.ui.view.FixedViewPager;

/**
 * viewPager嵌套滑动
 */

public class MultiViewPagerImpl extends FixedViewPager implements IMultiScrollHandler {

    private View mInnerScrollView;
    private int mInnerScrollResId;

    public MultiViewPagerImpl(Context context) {
        super(context);
    }

    public MultiViewPagerImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInnerScrollResId(int innerScrollResId) {
        this.mInnerScrollResId = innerScrollResId;
    }

    public void findInnerScroll() {
        PagerAdapter adapter = getAdapter();
        if (adapter != null) {
            if (adapter instanceof FragmentPagerAdapter || adapter instanceof FragmentStatePagerAdapter) {
                Fragment item = (Fragment) adapter.instantiateItem(this, getCurrentItem());
                View view = item.getView();
                if (view != null) {
                    mInnerScrollView = view.findViewById(mInnerScrollResId);
                }
            } else {
                int centerPos = getChildCount() / 2;
                View centerView = getChildAt(centerPos);
                if (centerView != null && centerView instanceof ViewGroup) {
                    mInnerScrollView = centerView.findViewById(mInnerScrollResId);
                }
            }
        }
    }

    @Override
    public boolean isScrolledEdge(int direction) {
        if (mInnerScrollView == null) {
            return true;
        }
        return mInnerScrollView.canScrollVertically(direction);
    }

    @Override
    public View getInnerScrollView() {
        if (mInnerScrollView == null) {
            findInnerScroll();
        }
        return mInnerScrollView;
    }
}
