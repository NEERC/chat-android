package ru.ifmo.neerc.chat.android;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DisabledRecyclerView extends RecyclerView {
    public DisabledRecyclerView(Context context) {
        super(context);
    }

    public DisabledRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisabledRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }
}
