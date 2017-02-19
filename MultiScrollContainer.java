package com.msile.views.multiscrollcontainer.multiscroll;

import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.msile.views.multiscrollcontainer.R;

/**
 * 上下翻页 多个View视图
 */
public class MultiScrollContainer extends ScrollView {

    private static final String TAG = "MultiScrollContainer";

    private static final int mCanScrollNextHeight = 50;     //可滑动到下一个view的高度
    private View mCurrentScrollView;                        //当前滚动的view
    private View mCurrentContentChild;                      //当前滚动的子view
    private int mScrollState = SCROLL_UP;                   //当前滑动的状态
    public static final int SCROLL_UP = 1;                  //上翻页状态
    public static final int SCROLL_DOWN = -1;               //下翻页状态
    private MultiScrollListener mPageListener;
    private boolean canInterceptTouchEvent;                 //是否拦截事件
    private float yDown;                                    //触摸Y轴坐标
    private int mCurrentScrollIndex;
    private int mLastPageScrollY = -1;
    private ViewGroup mContentView;
    private int mContentChildCount;

    public MultiScrollContainer(Context context) {
        this(context, null);
    }

    public MultiScrollContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiScrollContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setNestedScrollingEnabled(false);
        }
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mContentView == null) {
            mContentView = (ViewGroup) getChildAt(0);
            mContentChildCount = mContentView.getChildCount();
        }
        // TODO: 17/2/19 计算子view的高度
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (canInterceptTouchEvent) {
            return super.onInterceptTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                yDown = ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                float yDistance = ev.getY(pointerIndex) - yDown;
                if (Math.abs(yDistance) > 0) {
                    findCurrentScrollView();
                    if (mCurrentScrollView != null) {
                        mCurrentScrollView.computeScroll();
                        if (yDistance > 0) {
                            if (isCurrentScrolledEdge(-1)) {
                                canInterceptTouchEvent = true;
                                mScrollState = SCROLL_UP;
                                if (mLastPageScrollY == -1) {
                                    mLastPageScrollY = getScrollY();
                                }
                            } else {
                                canInterceptTouchEvent = false;
                                mScrollState = 0;
                                mLastPageScrollY = -1;
                            }
                        } else {
                            if (isCurrentScrolledEdge(1)) {
                                canInterceptTouchEvent = true;
                                mScrollState = SCROLL_DOWN;
                                if (mLastPageScrollY == -1) {
                                    mLastPageScrollY = getScrollY();
                                }
                            } else {
                                canInterceptTouchEvent = false;
                                mScrollState = 0;
                                mLastPageScrollY = -1;
                            }
                        }
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 是否滑动到边界
     *
     * @param direction 方向
     */
    private boolean isCurrentScrolledEdge(int direction) {
        if (mCurrentScrollView == null) {
            return true;
        }
        if (mCurrentScrollView instanceof IMultiScrollHandler) {
            IMultiScrollHandler scrollHandler = (IMultiScrollHandler) mCurrentScrollView;
            return scrollHandler.isScrolledEdge(direction);
        }
        return !mCurrentScrollView.canScrollVertically(direction);
    }

    /**
     * 获取当前滑动的view
     */
    private void findCurrentScrollView() {
        if (mContentView == null) {
            return;
        }
        mCurrentContentChild = mContentView.getChildAt(mCurrentScrollIndex);
        findInnerScrollView();
    }

    /**
     * 获取子view的滚动视图
     */
    private void findInnerScrollView() {
        if (mCurrentContentChild == null) {
            return;
        }
        View view = mCurrentContentChild;
        if (view instanceof ViewPager) {
            getViewPagerInnerScroll((ViewPager) view);
        } else {
            mCurrentScrollView = view;
        }
        //去除过度滑动回弹效果和nestedScroll
        if (mCurrentScrollView != null) {
            mCurrentScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mCurrentScrollView.isNestedScrollingEnabled()) {
                mCurrentScrollView.setNestedScrollingEnabled(false);
            }
        }
    }

    /**
     * 获取子view是ViewPager里面的滚动视图
     */
    private void getViewPagerInnerScroll(ViewPager viewPager) {
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter != null) {
            if (adapter instanceof FragmentPagerAdapter || adapter instanceof FragmentStatePagerAdapter) {
                Fragment item = (Fragment) adapter.instantiateItem(viewPager, viewPager.getCurrentItem());
                View view = item.getView();
                if (view != null) {
                    mCurrentScrollView = view.findViewById(R.id.multi_scroll_inner_scroll);
                }
            }
        } else {
            mCurrentScrollView = viewPager;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mCurrentContentChild == null || mScrollState == 0 || mContentChildCount == 0) {
                    return false;
                }
                int scrollY = getScrollY();
                int scrollOffset;
                if (mScrollState == SCROLL_UP) {
                    //顶部边界view判断
                    if (mCurrentScrollIndex == 0) {
                        Log.i(TAG, "top edge view....");
                        return false;
                    }
                    scrollOffset = mLastPageScrollY - scrollY;
                    if (scrollOffset >= mCanScrollNextHeight) {
                        smoothToPage(-1);
                    } else {
                        resetLastPage();
                    }
                } else {
                    //底部边界view判断
                    if (mCurrentScrollIndex == (mContentChildCount - 1)) {
                        Log.i(TAG, "bottom edge view....");
                        return false;
                    }
                    scrollOffset = scrollY - mLastPageScrollY;
                    if (scrollOffset >= mCanScrollNextHeight) {
                        smoothToPage(1);
                    } else {
                        resetLastPage();
                    }
                }
                return true;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 滑动距离过短恢复到原始状态
     */
    private void resetLastPage() {
        if (mLastPageScrollY >= 0) {
            smoothScrollTo(0, mLastPageScrollY);
            mLastPageScrollY = -1;
        }
    }

    /**
     * 滑动到上一页或者下一页
     * @param direction 方向
     */
    public void smoothToPage(int direction) {
        int lastPage = mCurrentScrollIndex;
        if (direction > 0) {
            smoothToDownPage();
        } else {
            smoothToUpPage();
        }
        if (mPageListener != null) {
            mPageListener.onScrolledPage(mCurrentScrollIndex, lastPage);
        }
    }

    /**
     * 滑动上一页
     */
    private void smoothToUpPage() {
        mScrollState = SCROLL_UP;
        smoothScrollTo(0, (mCurrentScrollIndex - 1) * getMeasuredHeight());
        mCurrentScrollIndex--;
        mLastPageScrollY = -1;
        Log.i(TAG, "smoothToUpPage=" + mCurrentScrollIndex);
    }

    /**
     * 滑动下一页
     */
    private void smoothToDownPage() {
        mScrollState = SCROLL_DOWN;
        smoothScrollTo(0, (mCurrentScrollIndex + 1) * getMeasuredHeight());
        mCurrentScrollIndex++;
        mLastPageScrollY = -1;
        Log.i(TAG, "smoothToDownPage=" + mCurrentScrollIndex);
    }

    public void setMultiScrollListener(MultiScrollListener listener) {
        this.mPageListener = listener;
    }

    public interface MultiScrollListener {
        void onScrolledPage(int currentPageIndex, int lastPageIndex);
    }
}