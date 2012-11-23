package com.android.Aizhugamecenter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GameAdapter extends BaseAdapter {
	private ArrayList<Game> arrayList;
	private LayoutInflater inflater;
	private Context context;

	public GameAdapter(ArrayList<Game> arrayList, Context context) {
		inflater = LayoutInflater.from(context);
		this.arrayList = arrayList;
		this.context = context;
	}

	@Override
	public int getCount() {
		return arrayList.size();
	}

	@Override
	public Game getItem(int position) {
		return arrayList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHoldler viewHoldler = null;

		if (convertView == null) {
			viewHoldler = new ViewHoldler();
			convertView = inflater.inflate(R.layout.main_item, null);
			viewHoldler.ivIcon = (ImageView) convertView
					.findViewById(R.id.ivIcon);
			viewHoldler.tvName = (TextView) convertView
					.findViewById(R.id.tvName);
			convertView.setTag(viewHoldler);
		}

		viewHoldler = (ViewHoldler) convertView.getTag();
		//viewHoldler.ivIcon.setImageResource(getItem(position).iconPath);
		viewHoldler.tvName.setText(getItem(position).name);

		return convertView;
	}

	class ViewHoldler {
		TextView tvName;
		ImageView ivIcon;
	}

}
