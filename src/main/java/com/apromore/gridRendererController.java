package com.apromore;

import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;


/**
 * The Class gridRendererController.
 */
public class gridRendererController implements RowRenderer<String[]> {

	/*
	 * @see org.zkoss.zul.RowRenderer#render(org.zkoss.zul.Row, java.lang.Object, int)
	 * Append rows to the grid
	 */
	public void render(Row row, String[] data, int index) throws Exception  {
		for (int i = 0; i < data.length; i++) {
			row.appendChild(new Label((String) data[i]));
		}
	}
}
