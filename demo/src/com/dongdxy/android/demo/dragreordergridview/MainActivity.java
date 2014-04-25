package com.dongdxy.android.demo.dragreordergridview;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dongdxy.android.ui.dragreordergridview.DragReorderGridView;
import com.dongdxy.android.ui.dragreordergridview.DragReorderListAdapter;
import com.dongdxy.android.ui.dragreordergridview.DragReorderListener;
import com.dongdxy.android.ui.dragreordergridview.EditActionListener;

public class MainActivity extends Activity {

	private DragReorderGridView mGridView;
	private List<Item> mItems;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGridView = (DragReorderGridView) findViewById(R.id.grid);

		initData();
		
		mAdapter = new ColorAdapter(mItems);
		mGridView.setAdapter(mAdapter);
		mGridView.setDragReorderListener(mDragReorderListener);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				Toast.makeText(MainActivity.this, "click item " + mItems.get(pos).label, Toast.LENGTH_SHORT)
						.show();
				if (mItems.get(pos).label.endsWith("+")) {
					Item item = new Item();
					Random random = new Random();
					item.color = Color.rgb(random.nextInt(0xff), random.nextInt(0xff), random.nextInt(0xff));
					int insertPos = mItems.size() - 1;
					item.label = "" + insertPos;
					mItems.add(insertPos, item);
					mAdapter.notifyDataSetChanged();
				}
			}
		});

		mGridView.enableEditMode(R.id.delete_icon, new EditActionListener() {

			@Override
			public void onEditAction(int position) {
				Toast.makeText(MainActivity.this, "deleting " + mAdapter.list.get(position).label, Toast.LENGTH_SHORT)
						.show();
				mAdapter.list.remove(position);
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		if (mGridView.isDragEditMode()) {
			mGridView.quitEditMode();
			return;
		}
		super.onBackPressed();
	}
	
	private void initData(){
		mItems = new ArrayList<Item>();
		int count = 37;
		for (int i = 0; i < count; i++) {
			Item item = new Item();
			int colorLevel = i * 0x80 / count;
			item.color = Color.rgb(0x00, 0xFF - colorLevel, 0x80 + colorLevel);
			item.label = "" + i;
			mItems.add(item);
		}

		Item addBtn = new Item();
		addBtn.color = Color.rgb(0xa0, 0xa0, 0xa0);
		addBtn.label = "+";
		addBtn.isFixed = true;
		mItems.add(addBtn);

	}

	private DragReorderListener mDragReorderListener = new DragReorderListener() {

		@Override
		public void onReorder(int fromPosition, int toPosition) {
			((ColorAdapter) mGridView.getAdapter()).reorder(fromPosition, toPosition);
		}

		@Override
		public void onDragEnded() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onItemLongClicked() {
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(50);
		}

	};
	private ColorAdapter mAdapter;

	private class Item {
		String label;
		int color;
		boolean isFixed = false;
	}

	private class ColorAdapter extends BaseAdapter implements DragReorderListAdapter {

		List<Item> list;

		public ColorAdapter(List<Item> list) {
			this.list = list;
		}

		public void reorder(int from, int to) {
			if (from != to) {
				Item item = list.remove(from);
				list.add(to, item);
				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewGroup result = (ViewGroup) convertView;
			if (result == null) {
				result = (ViewGroup) LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view, parent, false);
			}

			TextView textView = (TextView) result.findViewById(R.id.text);
			result.setBackgroundColor(list.get(position).color);
			textView.setText(list.get(position).label);

			return result;
		}

		@Override
		public boolean isReorderableItem(int position) {
			return !list.get(position).isFixed;
		}

	}

}
