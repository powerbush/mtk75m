package com.android.Aizhugamecenter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_layout);
		GridView gridview = (GridView) findViewById(R.id.gridview);

		InputStream inputStream = null;
		try {
			inputStream = this.getResources().getAssets().open("games.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<Game> arrayList = (ArrayList<Game>) GameEntry
				.getGameList(inputStream);

		ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();
		for (Game game : arrayList) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemImage", getLauncherIcon(game.pkgName));
			map.put("ItemText", game.name);
			map.put("pkg", game.pkgName);
			map.put("cls", game.className);
			lstImageItem.add(map);
		}

		SimpleAdapter saImageItems = new SimpleAdapter(this, lstImageItem,
				R.layout.grid_item, new String[] { "ItemImage", "ItemText" },
				new int[] { R.id.ItemImage, R.id.ItemText });
		gridview.setAdapter(saImageItems);
		gridview.setOnItemClickListener(new ItemClickListener());
	}

	class ItemClickListener implements OnItemClickListener {
		@SuppressWarnings("unchecked")
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			HashMap<String, Object> item = (HashMap<String, Object>) parent
					.getItemAtPosition(position);
			Intent intent = new Intent();
			intent.setClassName(item.get("pkg").toString(), item.get("cls")
					.toString());
			try {
				startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private int getLauncherIcon(String pkgname) {

		if (pkgname.equals("org.jfedor.frozenbubble")) {
			return R.drawable.app_frozen_bubble;
		} else if (pkgname.equals("com.realarcade.DOJ")) {
			return R.drawable.com_realarcade_doj;
		}
		return 0;
	}

}
