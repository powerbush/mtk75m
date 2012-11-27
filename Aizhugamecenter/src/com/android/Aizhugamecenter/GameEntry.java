package com.android.Aizhugamecenter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GameEntry {

	public static List<Game> getGameList(InputStream stream) {
		List<Game> list = new ArrayList<Game>();
		// DocumentBuilderFactory for DocumentBuilder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			// DocumentBuilder
			DocumentBuilder builder = factory.newDocumentBuilder();
			// Document
			Document document = builder.parse(stream);
			// root
			Element root = document.getDocumentElement();
			// items
			NodeList items = root.getElementsByTagName("item");
			// for each
			for (int i = 0; i < items.getLength(); i++) {
				Game game = new Game();
				Element item = (Element) items.item(i);
				game.name = item.getAttribute("name");
				game.pkgName = item.getAttribute("pkgname");
				game.className = item.getAttribute("classname");
				game.id = Integer.parseInt(item.getAttribute("id"));
				list.add(game);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}
}
