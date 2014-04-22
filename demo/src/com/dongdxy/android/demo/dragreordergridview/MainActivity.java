package com.dongdxy.android.demo.dragreordergridview;

import java.util.ArrayList;
import java.util.List;

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

public class MainActivity extends Activity {

	private DragReorderGridView mGridView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGridView = (DragReorderGridView) findViewById(R.id.grid);

		int[] colors = new int[37];
		for (int i = 0; i < colors.length; i++) {
			int colorLevel = i * 0x80 / colors.length;
			colors[i] = Color.rgb(0x00, 0xFF - colorLevel, 0x80 + colorLevel);
		}
		final ColorAdapter adapter = new ColorAdapter(colors);

		mGridView.setAdapter(adapter);

		mGridView.setDragReorderListener(mDragReorderListener);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Toast.makeText(MainActivity.this, "click item " + adapter.getItem(arg2), Toast.LENGTH_SHORT).show();

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
		public void onDelete(int position) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onItemLongClicked() {
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(50);
		}

	};

	private class ColorAdapter extends BaseAdapter implements DragReorderListAdapter {

		private List<Integer> colors;
		private List<Integer> positions;

		public ColorAdapter(int[] colors) {
			this.colors = new ArrayList<Integer>();
			this.positions = new ArrayList<Integer>();

			for (int color : colors) {
				this.colors.add(color);
				this.positions.add(positions.size());
			}
		}

		public void reorder(int from, int to) {
			if (from != to) {
				int color = colors.remove(from);
				colors.add(to, color);

				int position = positions.remove(from);
				positions.add(to, position);

				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			return colors.size();
		}

		@Override
		public Object getItem(int position) {
			return positions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView result = (TextView) convertView;

			if (result == null) {
				result = (TextView) LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view, parent, false);
			}
			result.setBackgroundColor(colors.get(position));
			result.setText(Integer.toString(positions.get(position)));

			return result;
		}

		@Override
		public boolean isReorderableItem(int position) {
			if (position == positions.size() - 1) {
				return false;
			}

			return true;
		}

	}

}
