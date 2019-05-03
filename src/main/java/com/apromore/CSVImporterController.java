package com.apromore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.deckfour.xes.model.XLog;
import org.zkoss.util.media.Media;
import org.zkoss.zul.*;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import com.opencsv.CSVReader;

// TODO: Auto-generated Javadoc

/**
 * The Class CSVImporterController.
 * <p>
 * import CSV file from user, display content into the page.
 */
public class CSVImporterController extends SelectorComposer<Component> {

    /**
     * The my grid.
     */
    @Wire
    private Grid myGrid;

    @Wire
    private Div attrBox;

    @Wire
    private Div gridBox;

    @Wire
    private Button toXESButton;

    private Media media;
    /**
     * Upload file.
     *
     * @param event the event: upload event
     *              allows importing CSV file, if imported correctly, it sets the grid model and row renderer.
     */
    @Listen("onUpload = #uploadFile")
    public void uploadFile(UploadEvent event) {

        this.media = event.getMedia();

         if(attrBox != null) {
             attrBox.getChildren().clear();
         }
        String[] allowedExtensions = {"csv", "xls", "xlsx"};
        if (Arrays.asList(allowedExtensions).contains(media.getFormat())) {

            // set grid model
            myGrid.setModel(displayCSVContent(media));

            //set grid row renderer
            gridRendererController rowRenderer = new gridRendererController();
            myGrid.setRowRenderer(rowRenderer);

            toXESButton.setDisabled(false);

        } else {
            Messagebox.show("Please select CSV file!", "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }


    private static CsvToXes CsvToXes = new CsvToXes();
    /**
     * Gets the Content.
     *
     * @param media the imported CSV file
     * @return the model data
     * <p>
     * read CSV content and create list model to be set as grid model.
     */
    @SuppressWarnings("null")
    private ListModel<String[]> displayCSVContent(Media media) {
        CSVReader reader = null;
        Double AttribWidth = 100.0;
        try {
//            System.out.println(media.getStringData());
            // check file format to choose correct file reader.
            if(media.isBinary()){

                    reader = new CSVReader(new InputStreamReader(media.getStreamData()));
            } else {
                    reader = new CSVReader(media.getReaderData());
//                reader = new CSVReader(media.getReaderData());
            }
            ListModelList<String[]> result = new ListModelList<String[]>();
            String[] line;


            /// display first numberOfrows to user and display drop down lists to set attributes
            line = reader.readNext();   // read first line

            // add dropdown lists
            if(attrBox != null) {
                attrBox.getChildren().clear();
            }

            CsvToXes.setHeads(line);
            result.add(line);
            line = reader.readNext();
            CsvToXes.setLine(line);
            CsvToXes.setOtherTimestamps();
            if(line.length < 11) {
                attrBox.setWidth(String.valueOf(AttribWidth) + "%");
                gridBox.setWidth(String.valueOf(AttribWidth) + "%");
                CsvToXes.setLists(line.length, CsvToXes.getHeads(), String.valueOf(95/line.length));
            } else {
                AttribWidth = AttribWidth * (line.length / 10);
                attrBox.setWidth(String.valueOf(AttribWidth) + "%");
                gridBox.setWidth(String.valueOf(AttribWidth) + "%");

                //System.out.println("Width is:" + String.valueOf((int)Math.round((AttribWidth)/(line.length*2.3))));
                CsvToXes.setLists(line.length, CsvToXes.getHeads(), String.valueOf((AttribWidth)/(line.length*2.3)));
            }
   //         System.out.println(line.length);
//            CsvToXes.setLists(line.length, CsvToXes.getHeads(), String.valueOf(90/line.length));
            List<Listbox> lists = CsvToXes.getLists();

            for (Listbox list : lists) {

                attrBox.appendChild(list);

                attrBox.appendChild(new Space());
            }
            attrBox.clone();

            // display first 1000 rows
            int numberOfrows = 1000;
            while (line != null && numberOfrows >= 0) {
                result.add(line);
                numberOfrows--;
                line = reader.readNext();
            }
            reader.close();
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            Messagebox.show(e.getMessage());
            return null;
        }
    }


    @Listen("onClick = #toXESButton")
    public void toXES() throws IOException{
        if (media != null){

            List<LogModel> xesModel = CsvToXes.prepareXesModel(media);

            if (xesModel != null) {
                // create XES file
                XLog xlog = CsvToXes.createXLog(xesModel);
                CsvToXes.toXESfile(xlog, media.getName());
            }

        }else{

            Messagebox.show("Upload file first!");

        }
    }

}