package android.support.design.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class CustomScrollingViewBehavior extends AppBarLayout.ScrollingViewBehavior {

    public CustomScrollingViewBehavior() {}

    public CustomScrollingViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child,
            View dependency) {
        child.requestLayout();
        return super.onDependentViewChanged(parent, child, dependency);
    }

    @Override
    int getScrollRange(View v) {
        if (v instanceof AppBarLayout) {
            final CoordinatorLayout.Behavior behavior =
                    ((CoordinatorLayout.LayoutParams) v.getLayoutParams()).getBehavior();
            if (behavior instanceof AppBarLayout.Behavior) {
                return -((AppBarLayout.Behavior) behavior).getTopAndBottomOffset();
            }
        }

        return super.getScrollRange(v);
    }

}
