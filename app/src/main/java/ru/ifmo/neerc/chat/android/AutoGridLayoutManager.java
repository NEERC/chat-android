package ru.ifmo.neerc.chat.android;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;

public class AutoGridLayoutManager extends GridLayoutManager {

    private Context mContext;

    private int mColumnWidth;

    public AutoGridLayoutManager(Context context, int columnWidth) {
        this(context, columnWidth, VERTICAL, false);
    }

    public AutoGridLayoutManager(Context context, int columnWidth, int orientation,
            boolean reverseLayout) {
        super(context, 1, orientation, reverseLayout);
        mContext = context;
        mColumnWidth = columnWidth;
    }

    public void setColumnWidth(int columnWidth) {
        mColumnWidth = columnWidth;
    }

    private void updateSpanCount() {
        if (mColumnWidth < 0) {
            setSpanCount(1);
            return;
        }

        int totalSpace;
        if (getOrientation() == VERTICAL) {
            totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
        } else {
            totalSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        }

        int columnWidth = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            mColumnWidth,
            mContext.getResources().getDisplayMetrics()
        );

        setSpanCount(Math.max(1, totalSpace / columnWidth));
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateSpanCount();
        super.onLayoutChildren(recycler, state);
    }
}
