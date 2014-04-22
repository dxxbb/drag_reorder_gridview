/**
 * 
 */
package com.dongdxy.android.ui.dragreordergridview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * @author dongxinyu.dxy
 * 
 */
public class DragReorderGridView extends GridView {

	private static final int ANIMATION_DURATION = 150; // in ms
	private static final int GRIDVIEW_SCROLL_STEP = 8; // in dp
	private static final int HOVER_AMPLIFY_PERSENCT = 10; // 0% means no amplify

	private DragReorderListener mDragReorderListener;

	private boolean mIsEditMode = false;
	private int mEditingPosition = INVALID_POSITION;
	private boolean mIsDragging = false;
	private int mDraggingPosition = INVALID_POSITION;

	private BitmapDrawable mHoverView;
	private int mHoverViewOffsetX = 0;
	private int mHoverViewOffsetY = 0;
	private Rect mHoverViewBounds;

	private int mGridViewScrollStep = 0;

	public DragReorderGridView(Context context) {
		this(context, null);
	}

	public DragReorderGridView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragReorderGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void setDragReorderListener(DragReorderListener dragReorderListener) {
		mDragReorderListener = dragReorderListener;
	}

	/**
	 * not same as View.isInEditMode()
	 * 
	 * @return
	 */
	public boolean isDragEditMode() {
		return mIsEditMode;
	}

	private void init() {
		setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Log.e("xxx", "onItemLongClick " + position);
				if (!isReorderable(position)) {
					return false;
				}

				quitEditMode();
				enterEditMode(position);
				startDrag(position);
				notifyLongClicked();

				return true;
			}
		});

		setOnScrollListener(mScrollListener);
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		mGridViewScrollStep = (int) (GRIDVIEW_SCROLL_STEP * metrics.density + 0.5f);
	}

	private void enterEditMode(int tiggerPosition) {
		mEditingPosition = tiggerPosition;
		mIsEditMode = true;
	}

	public void quitEditMode() {
		if (!mIsEditMode) {
			return;
		}

		mIsEditMode = false;
		mEditingPosition = INVALID_POSITION;
		invalidate();
	}

	private void startDrag(int draggingPosition) {
		View draggingView = findViewByPosition(draggingPosition);
		if (draggingView == null) {
			return;
		}

		mIsDragging = true;
		createHoverDrawable(draggingView);
		updateDraggingPosition(draggingPosition);
		updateEditingPosition(draggingPosition);
	}

	private void stopDrag() {
		if (!mIsDragging) {
			return;
		}

		mIsDragging = false;
		mHoverView = null;
		mIsWaitingForScrollFinish = false;
		mIsMobileScrolling = false;
		notifyDragEnded();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverView != null) {
			mHoverView.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.e("xxx", "touchEvent " + event.getAction());
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mDownEventX = (int) event.getX();
			mDownEventY = (int) event.getY();
			Log.e("xxx", "ACTION_DOWN x " + mDownEventX);
			Log.e("xxx", "ACTION_DOWN y " + mDownEventY);
			Log.e("xxx", "mIsEditMode " + mIsEditMode);

			if (!mIsEditMode) {
				break;
			}
			layoutChildren();
			int position = pointToPosition((int) event.getX(), (int) event.getY());
			Log.e("xxx", "position " + position + " mEditingPosition " + mEditingPosition);
			// Log.e("xxx", "mEditingPosition visi " +
			// findViewByPosition(mEditingPosition).getVisibility());
			if (position != mEditingPosition) {
				quitEditMode();
				break;
			} else {
				startDrag(position);
				break;
			}

		case MotionEvent.ACTION_MOVE:

			if (!mIsDragging) {
				break;
			}

			mLastEventX = (int) event.getX();
			mLastEventY = (int) event.getY();

			hoverViewFollow(mLastEventX, mLastEventY);
			attemptReorder();
			handleScroll();

			return true;

		case MotionEvent.ACTION_UP:
			finishDrag();
			break;

		case MotionEvent.ACTION_CANCEL:
			stopDrag();
			break;

		default:
			break;
		}

		return super.onTouchEvent(event);
	}

	private int mDownEventX = -1;
	private int mDownEventY = -1;

	private int mLastEventY = -1;
	private int mLastEventX = -1;

	private void hoverViewFollow(int x, int y) {
		mHoverViewBounds.offsetTo(x + mHoverViewOffsetX, y + mHoverViewOffsetY);
		mHoverView.setBounds(mHoverViewBounds);
		invalidate();
	}

	private boolean mIsMobileScrolling = false;

	private void handleScroll() {
		mIsMobileScrolling = handleScroll(mHoverViewBounds);
	}

	private boolean handleScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();

		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mGridViewScrollStep, 0);
			return true;
		}

		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mGridViewScrollStep, 0);
			return true;
		}

		return false;
	}

	private boolean isReorderable(int position) {
		if (getAdapter() instanceof DragReorderListAdapter) {
			return ((DragReorderListAdapter) getAdapter()).isReorderableItem(position);
		} else {
			return true;
		}
	}

	private View findViewByPosition(int position) {
		int firstPosition = this.getFirstVisiblePosition();
		int lastPosition = this.getLastVisiblePosition();

		if (position < firstPosition || position > lastPosition) {
			return null;
		}

		return this.getChildAt(position - firstPosition);
	}

	/**
	 * Creates the hover cell with the appropriate bitmap and of appropriate
	 * size. The hover cell's BitmapDrawable is drawn on top of the bitmap every
	 * single time an invalidate call is made.
	 */
	private void createHoverDrawable(View v) {
		Bitmap b = snapshotBitmap(v);
		mHoverView = new BitmapDrawable(getResources(), b);

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		int wAmplified = w * HOVER_AMPLIFY_PERSENCT / 100;
		int hAmplified = h * HOVER_AMPLIFY_PERSENCT / 100;

		mHoverViewBounds = new Rect(left - wAmplified, top - hAmplified, left + w + wAmplified, top + h + hAmplified);

		mHoverViewOffsetX = mHoverViewBounds.left - mDownEventX;
		mHoverViewOffsetY = mHoverViewBounds.top - mDownEventY;

		mHoverView.setBounds(mHoverViewBounds);
	}

	/**
	 * Returns a bitmap showing a screenshot of the view passed in.
	 */
	private Bitmap snapshotBitmap(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	private void notifyReorder(int from, int to) {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onReorder(from, to);
	}

	private void notifyDragEnded() {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onDragEnded();
	}

	private void notifyLongClicked() {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onItemLongClicked();
	}

	private void onDeletePressed(int position) {
		if (mDragReorderListener == null) {
			return;
		}
		mDragReorderListener.onDelete(position);
	}

	private void attemptReorder() {
		int x = mLastEventX;
		int y = mLastEventY;

		final int targetPosition = pointToPosition(x, y);
		if (targetPosition == AdapterView.INVALID_POSITION || targetPosition == mDraggingPosition) {
			return;
		}

		if (!isReorderable(targetPosition)) {
			return;
		}

		final int origPosition = mDraggingPosition;
		notifyReorder(origPosition, targetPosition);
		Log.e("xxx", "reorderElements originalPosition " + mDraggingPosition + " targetPosition" + targetPosition);
		updateDraggingPosition(targetPosition);
		updateEditingPosition(targetPosition);
		final ViewTreeObserver observer = getViewTreeObserver();
		if (observer != null) {
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);
					animationReorder(origPosition, targetPosition);
					return true;
				}
			});
		}

	}

	private void animationReorder(int oldPosition, int newPosition) {

		boolean isForward = newPosition > oldPosition;
		int fromX;
		int toX;
		int fromY;
		int toY;
		if (isForward) {
			for (int pos = Math.min(oldPosition, newPosition); pos < Math.max(oldPosition, newPosition); pos++) {
				View view = findViewByPosition(pos);
				if (view == null) {
					continue;
				}
				if ((pos + 1) % getNumColumnsCompat() == 0) {
					fromX = -view.getWidth() * (getNumColumnsCompat() - 1);
					toX = 0;
					fromY = view.getHeight();
					toY = 0;

				} else {
					fromX = view.getWidth();
					toX = 0;
					fromY = 0;
					toY = 0;
				}
				TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, fromX, Animation.ABSOLUTE,
						toX, Animation.ABSOLUTE, fromY, Animation.ABSOLUTE, toY);
				Log.e("xxx", "traslate fromX " + fromX + " toX " + toX + " fromY " + fromY + " toY " + toY);
				translate.setDuration(ANIMATION_DURATION);
				translate.setFillEnabled(true);
				translate.setFillAfter(false);
				view.clearAnimation();
				view.startAnimation(translate);
			}
		} else {
			for (int pos = Math.max(oldPosition, newPosition); pos > Math.min(oldPosition, newPosition); pos--) {
				View view = findViewByPosition(pos);
				if (view == null) {
					continue;
				}
				if ((pos + getNumColumnsCompat()) % getNumColumnsCompat() == 0) {
					fromX = view.getWidth() * (getNumColumnsCompat() - 1);
					toX = 0;
					fromY = -view.getHeight();
					toY = 0;
				} else {
					fromX = -view.getWidth();
					toX = 0;
					fromY = 0;
					toY = 0;
				}
				TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, fromX, Animation.ABSOLUTE,
						toX, Animation.ABSOLUTE, fromY, Animation.ABSOLUTE, toY);
				translate.setDuration(ANIMATION_DURATION);
				translate.setFillEnabled(true);
				translate.setFillAfter(false);
				view.clearAnimation();
				view.startAnimation(translate);
			}
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private int getNumColumnsCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return getNumColumns();
		} else {
			int columns = 0;
			int children = getChildCount();
			if (children > 0) {
				int width = getChildAt(0).getMeasuredWidth();
				if (width > 0) {
					columns = getWidth() / width;
				}
			}
			return columns > 0 ? columns : AUTO_FIT;
		}
	}

	private void updateDraggingPosition(int newPosition) {
		Log.e("xxx", "update newPosition " + newPosition + " old " + mDraggingPosition);
		if (mDraggingPosition != INVALID_POSITION) {
			View origDraggingCell = findViewByPosition(mDraggingPosition);
			if (origDraggingCell != null) {
				Log.e("xxx", "visible " + mDraggingPosition);
				origDraggingCell.setVisibility(VISIBLE);
			}
		}

		mDraggingPosition = newPosition;

		View newDraggingCell = findViewByPosition(mDraggingPosition);
		if (newDraggingCell != null) {
			Log.e("xxx", "invisible " + mDraggingPosition);
			newDraggingCell.setVisibility(INVISIBLE);
		}
	}

	private void updateEditingPosition(int newPosition) {
		mEditingPosition = newPosition;
	}

	private void finishDrag() {
		if (mIsDragging || mIsWaitingForScrollFinish) {

			// If the autoscroller has not completed scrolling, we need to wait
			// for it to
			// finish in order to determine the final location of where the
			// hover cell
			// should be animated to.
			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}

			View mobileView = findViewByPosition(mDraggingPosition);
			TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, mHoverViewBounds.left
					- mobileView.getLeft(), Animation.ABSOLUTE, 0, Animation.ABSOLUTE, mHoverViewBounds.top
					- mobileView.getTop(), Animation.ABSOLUTE, 0);
			translate.setDuration(ANIMATION_DURATION);
			translate.setFillEnabled(true);
			translate.setFillAfter(true);
			mobileView.clearAnimation();
			mobileView.startAnimation(translate);
			translate.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation arg0) {
					// reset();
				}
			});

			mHoverView = null;
			updateDraggingPosition(INVALID_POSITION);
			invalidate();
		}

		stopDrag();
	}

	private boolean mIsWaitingForScrollFinish = false;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * This scroll listener is added to the gridview in order to handle cell
	 * swapping when the cell is either at the top or bottom edge of the
	 * gridview. If the hover cell is at either edge of the gridview, the
	 * gridview will begin scrolling. As scrolling takes place, the gridview
	 * continuously checks if new cells became visible and determines whether
	 * they are potential candidates for a cell swap.
	 */
	private OnScrollListener mScrollListener = new OnScrollListener() {

		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		/**
		 * This method is in charge of invoking 1 of 2 actions. Firstly, if the
		 * gridview is in a state of scrolling invoked by the hover cell being
		 * outside the bounds of the gridview, then this scrolling event is
		 * continued. Secondly, if the hover cell has already been released,
		 * this invokes the animation for the hover cell to return to its
		 * correct position after the gridview has entered an idle scroll state.
		 */
		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mIsDragging && mIsMobileScrolling) {
					handleScroll();
				} else if (mIsWaitingForScrollFinish) {
					finishDrag();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled up enough to reveal a new cell at
		 * the top of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mIsDragging && mDraggingPosition != INVALID_POSITION) {
					attemptReorder();
				}
			}
		}

		/**
		 * Determines if the gridview scrolled down enough to reveal a new cell
		 * at the bottom of the list. If so, then the appropriate parameters are
		 * updated.
		 */
		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem + mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem + mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mIsDragging && mDraggingPosition != INVALID_POSITION) {
					attemptReorder();
				}
			}
		}
	};
}
