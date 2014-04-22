/**
 * 
 */
package com.dongdxy.android.ui.dragreordergridview;

import android.widget.ListAdapter;

/**
 * @author dongxinyu.dxy
 * 
 *         Use this adapter intead of normal ListAdapter, in case of some item
 *         is not reorderable, eg. an special "+" or "more" button in the grid
 */
public interface DragReorderListAdapter extends ListAdapter {

	public abstract boolean isReorderableItem(int position);

}
