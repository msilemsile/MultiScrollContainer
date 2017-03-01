package com.memebox.cn.android.widget.multiscroll;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.ScrollView;

import java.lang.reflect.Field;

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
    private int mSmoothPageOffset;
    private int mMaxSkipPage = -1;
    private int mMinSkipPage = -1;
    private OverScroller mOverScroller;

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
        getScrollerField();
    }

    private void getScrollerField() {
        try {
            Field scrollerField = ScrollView.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            mOverScroller = (OverScroller) scrollerField.get(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mContentView == null && getChildCount() > 0) {
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
        if (mOverScroller != null && !mOverScroller.isFinished()) {
            return false;
        }
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                yDown = ev.getY();
                findCurrentScrollView();
                break;
            case MotionEvent.ACTION_MOVE:
                float yDistance = ev.getY() - yDown;
                if (Math.abs(yDistance) > 0) {
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
     * 设置滚动offSet
     */
    public void setSmoothPageOffset(int mSmoothPageOffset) {
        this.mSmoothPageOffset = mSmoothPageOffset;
    }

    /**
     * 设置最大可跳转页面
     *
     * @param mMaxSkipPage 最大可滑动页数
     */
    public void setMaxSkipPage(int mMaxSkipPage) {
        this.mMaxSkipPage = mMaxSkipPage;
    }

    /**
     * 设置最小可跳转页面
     *
     * @param mMinSkipPage 最小可滑动页数
     */
    public void setMinSkipPage(int mMinSkipPage) {
        this.mMinSkipPage = mMinSkipPage;
    }

    /**
     * 获取当前滑动的view
     */
    private void findCurrentScrollView() {
        if (mContentView == null) {
            return;
        }
        mCurrentScrollView = null;
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
        if (view instanceof IMultiScrollHandler) {
            mCurrentScrollView = ((IMultiScrollHandler) view).getInnerScrollView();
            if (mCurrentScrollView == null) {
                mCurrentScrollView = view;
            }
        } else {
            mCurrentScrollView = view;
        }
        //去除过度滑动回弹效果和nestedScroll
        mCurrentScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mCurrentScrollView.isNestedScrollingEnabled()) {
            mCurrentScrollView.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_POINTER_UP:
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
     *
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
        //最小可跳转页数判断
        if (mMinSkipPage >= 0) {
            int nextScrollIndex = mCurrentScrollIndex - 1;
            if (nextScrollIndex <= mMinSkipPage) {
                resetLastPage();
                return;
            }
        }
        mScrollState = SCROLL_UP;
        mCurrentScrollIndex--;
        smoothToIndexPage(mCurrentScrollIndex);
        mLastPageScrollY = -1;
        Log.i(TAG, "smoothToUpPage=" + mCurrentScrollIndex);
    }

    /**
     * 滑动下一页
     */
    private void smoothToDownPage() {
        //最大可跳转页数判断
        if (mMaxSkipPage >= 0) {
            int nextScrollIndex = mCurrentScrollIndex + 1;
            if (nextScrollIndex >= mMaxSkipPage) {
                resetLastPage();
                return;
            }
        }
        mScrollState = SCROLL_DOWN;
        mCurrentScrollIndex++;
        smoothToIndexPage(mCurrentScrollIndex);
        mLastPageScrollY = -1;
        Log.i(TAG, "smoothToDownPage=" + mCurrentScrollIndex);
    }

    /**
     * 滑动到索引页
     */
    private void smoothToIndexPage(int pageIndex) {
        View pageView = mContentView.getChildAt(pageIndex);
        if (pageView != null) {
            smoothScrollTo(0, pageView.getTop() - mSmoothPageOffset);
        }
        mCurrentScrollIndex = pageIndex;
    }

    /**
     * 滑动到指定页
     *
     * @param page             页数
     * @param needContentToTop 是否需要内容回到顶部
     */
    public void smoothToPage(int page, boolean needContentToTop) {
        if (mContentChildCount == 0 || mContentView == null) {
            return;
        }
        int pageIndex = page - 1;
        //当前页则不跳转
        if (mCurrentScrollIndex == pageIndex) {
            if (needContentToTop) {
                innerScrollToTop(mCurrentScrollIndex);
            }
            return;
        }
        //边界页数判断
        if (pageIndex < 0) {
            pageIndex = 0;
        }
        if (pageIndex >= mContentChildCount) {
            pageIndex = mContentChildCount - 1;
        }
        //当前页则不跳转
        if (mCurrentScrollIndex == pageIndex) {
            if (needContentToTop) {
                innerScrollToTop(mCurrentScrollIndex);
            }
            return;
        }
        if (needContentToTop) {
            int startPageIndex = 0;
            int endPageIndex = 0;
            if (mCurrentScrollIndex < pageIndex) {
                startPageIndex = mCurrentScrollIndex;
                endPageIndex = pageIndex;
            } else {
                startPageIndex = pageIndex;
                endPageIndex = mCurrentScrollIndex;
            }
            for (int i = startPageIndex; i <= endPageIndex; i++) {
                innerScrollToTop(i);
            }
        }
        if (mPageListener != null) {
            mPageListener.onScrolledPage(pageIndex, mCurrentScrollIndex);
        }
        smoothToIndexPage(pageIndex);
    }

    /**
     * 内部滑动view回到顶部
     */
    private void innerScrollToTop(int pageIndex) {
        View childContentView = mContentView.getChildAt(pageIndex);
        if (childContentView == null) {
            return;
        }
        View innerScrollView;
        if (childContentView instanceof IMultiScrollHandler) {
            innerScrollView = ((IMultiScrollHandler) childContentView).getInnerScrollView();
        } else {
            innerScrollView = childContentView;
        }
        if (innerScrollView != null) {
            if (innerScrollView instanceof ListView) {
                ((ListView) innerScrollView).setStackFromBottom(true);
            } else if (innerScrollView instanceof RecyclerView) {
                ((RecyclerView) innerScrollView).scrollToPosition(0);
            } else {
                innerScrollView.scrollTo(0, 0);
            }
        }
    }

    /**
     * 获取当前页索引
     */
    public int getCurrentPageIndex() {
        return mCurrentScrollIndex;
    }

    /**
     * 回到顶部
     */
    public void smoothToTopPage() {
        smoothToTopPage(false);
    }

    /**
     * 回到顶部
     *
     * @param needContentToTop 是否需要内容回到顶部
     */
    public void smoothToTopPage(boolean needContentToTop) {
        smoothToPage(1, needContentToTop);
    }

    public void setMultiScrollListener(MultiScrollListener listener) {
        this.mPageListener = listener;
    }

    public interface MultiScrollListener {
        void onScrolledPage(int currentPageIndex, int lastPageIndex);
    }
}