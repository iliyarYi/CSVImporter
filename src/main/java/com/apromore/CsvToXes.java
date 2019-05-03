package com.apromore;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.*;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.model.*;
import org.deckfour.xes.out.XesXmlSerializer;
import org.zkoss.util.media.Media;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;


import com.opencsv.CSVReader;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;


// TODO: Auto-generated Javadoc
/**
 * The Class CsvToXes.
 */
public class CsvToXes {


	/** The case id values. */
	private String[] caseIdValues = {"case", "case id", "case-id", "service id", "event id"};

	/** The activity values. */
	private String[] activityValues = {"activity", "activity id", "activity-id", "operation", "event"};

	/** The timestamp Values. */
	private String[] timestampValues = {"timestamp", "end date", "complete timestamp", "time:timestamp"};

	/** The Constant caseid. */
	private static final String caseid = "caseid";

	/** The Constant activity. */
	private static final String activity = "activity";

	/** The Constant timestamp. */
	private static final String timestamp = "timestamp";

	private List<Listbox> lists;
	private HashMap<String, Integer> heads;
	private List<Integer> ignoredPos;
    private HashMap<Integer, String> otherTimeStampsPos;
	private String[] line;


	/**
	 * Prepare xes model.
	 *
	 * @param media the media
	 * @return the list
	 */
	@SuppressWarnings("resource")
	public List<LogModel> prepareXesModel(Media media) {
		CSVReader reader = null;

		try {
			// check file format to choose correct file reader.
			if(media.isBinary()){
				reader = new CSVReader(new InputStreamReader(media.getStreamData()));
			}
			else{
				reader = new CSVReader(media.getReaderData());
			}

			Parse parse = new Parse();
			// read first line from CSV as header
			String[] header = reader.readNext();

			// If any of the mandatory fields are missing show alert message to the user and return
			StringBuilder headNOTDefined = checkFields(heads);
			if(headNOTDefined.length() !=0) {
				Messagebox.show(headNOTDefined.toString());
				return null;
			}


			// create model "LogModel" of the log data
			// We set mandatory fields and other fields are set with hash map
			String[] line;
			List<LogModel> logData = new ArrayList<LogModel>();
			HashMap<String, Timestamp> otherTimestamps = new HashMap<String, Timestamp>();
			HashMap<String, String> others = new HashMap<String, String>();
			String foramte = null;

			while ((line = reader.readNext()) != null) {

				for(int p=0; p<= line.length-1; p++) {
					if(otherTimeStampsPos.get(p) !=null){
						otherTimestamps.put(header[p], parse.parseTimestamp(line[p], otherTimeStampsPos.get(p)));
					}
					else if (p!= heads.get(caseid) && p!= heads.get(activity) && p!= heads.get(timestamp) && (ignoredPos.isEmpty() || !ignoredPos.contains(p)) ) {
						others.put(header[p], line[p]);
					}
				}

				if(foramte == null) {
					foramte = parse.determineDateFormat(line[heads.get(timestamp)]);
				}
				Timestamp tStamp =  parse.parseTimestamp(line[heads.get(timestamp)], foramte);

				logData.add(new LogModel(line[heads.get(caseid)], line[heads.get(activity)], tStamp , otherTimestamps, others));

			}
			reader.close();
			return sortTraces(logData);

		} catch (IOException e) {
			e.printStackTrace();
			Messagebox.show(e.getMessage());
		}
		return null;
	}


	/**
	 * Header pos.
	 *
	 * @param line read line from CSV
	 * @return the hash map: including mandatory field as key and position in the array as the value.
	 */

	public void setHeads(String[] line) {
		// initialize map
		heads = new HashMap<String, Integer>();
		heads.put(caseid, -1);
		heads.put(activity, -1);
		heads.put(timestamp, -1);

		for(int i=0; i<= line.length -1; i++) {
			if((heads.get(caseid) == -1) && getPos(caseIdValues, line[i])){
				heads.put(caseid, i);
			}
			else if((heads.get(activity) == -1) && getPos(activityValues, line[i])) {
				heads.put(activity, i);
			}
			else if((heads.get(timestamp) == -1) && getPos(timestampValues, line[i])) {
				heads.put(timestamp, i);
			}
		}
	}

	public HashMap<String, Integer> getHeads() {
		return this.heads;
	}

	public String[] getLine() {
		return line;
	}

	public void setLine(String[] line) {
		this.line = line;
	}

	public void setOtherTimestamps(){
		Parse parse = new Parse();
		otherTimeStampsPos = new HashMap<Integer, String>();
		Integer timeStampPos = heads.get(timestamp);

		for(int i=0; i<= this.line.length -1; i++) {
			String detectedFormat = parse.determineDateFormat(this.line[i]);
			if((i != timeStampPos) && (detectedFormat != null)){
				otherTimeStampsPos.put(i, detectedFormat);
			}
		}
	}


	/**
	 * Gets the pos.
	 *
	 * @param col the col: array which has possible names for each of the mandatory fields.
	 * @param elem the elem: one item of the CSV line array
	 * @return the pos: boolean value confirming if the elem is the required element.
	 */
	private Boolean getPos(String[] col, String elem) {
		return	Arrays.stream(col).anyMatch(elem.toLowerCase()::equals);
	}

	/**
	 * Check fields.
	 *
	 * Check if all mandatory fields are found in the file, otherwise, construct a message based on the missed fields.
	 *
	 * @param posMap the pos map
	 * @return the string builder
	 */
	private StringBuilder checkFields(HashMap<String, Integer> posMap) {
		String[] fieldsToCheck = {caseid,activity,timestamp};
		StringBuilder importMessage = new StringBuilder();

		for(int f=0; f<=fieldsToCheck.length -1; f++) {
			if(posMap.get(fieldsToCheck[f]) == -1) {
				String mess = "No " + fieldsToCheck[f] + " defined!";
				importMessage = (importMessage.length() == 0?  importMessage.append(mess) :  importMessage.append(", " + mess));
			}
		}

		return importMessage;
	}


	private List<LogModel> sortTraces(List<LogModel> traces){
		Comparator<LogModel> compareCaseID = (LogModel o1, LogModel o2) -> {
			try {
				String c1 = o1.getCaseID().replaceAll("[^0-9]", "");
				String c2 = o2.getCaseID().replaceAll("[^0-9]", "");
				Integer case1 = Integer.parseInt(c1);
				Integer case2 = Integer.parseInt(c2);
				return case1.compareTo(case2);
			}
			catch (NumberFormatException e)
			{
				return o1.getCaseID().compareTo( o2.getCaseID());
			}
		};
		Collections.sort(traces, compareCaseID);
		return traces;
	}



	public void setLists(int cols, HashMap<String, Integer> heads, String boxwidth) {

		lists = new ArrayList<Listbox>();
		ignoredPos = new ArrayList<Integer>();
		Parse parse = new Parse();

		LinkedHashMap<String, String> menuItems = new LinkedHashMap<String, String>();
		String other = "other";
		String ignore = "ignore";
		String tsValue = "otherTimestamp";
		menuItems.put(caseid, "Case ID");
		menuItems.put(activity, "Activity");
		menuItems.put(timestamp, "End timestamp");
		menuItems.put(tsValue, "Other timestamp");
		menuItems.put(other, "Other");
		menuItems.put(ignore, "Ignore column");

		// get index of "other" item and select it.
		int otherIndex = new ArrayList<String>(menuItems.keySet()).indexOf(other);

		for(int cl =0; cl<=cols-1; cl++) {

			Listbox box = new Listbox();
			box.setMold("select"); // set listBox to select mode
			box.setId(String.valueOf(cl)); // set id of list as column position.
			box.setWidth(boxwidth + "%");

			for (Map.Entry<String, String> dl : menuItems.entrySet()){
				Listitem item = new Listitem();
				item.setValue(dl.getKey());
				item.setLabel(dl.getValue());


				if((box.getSelectedItem() == null) && (
						( new String(dl.getKey()).equals(caseid) && cl == heads.get(caseid))||
								( new String(dl.getKey()).equals(activity) && cl == heads.get(activity))||
								( new String(dl.getKey()).equals(timestamp) && cl == heads.get(timestamp))||
								( new String(dl.getKey()).equals(tsValue) && otherTimeStampsPos.get(cl) != null)||
								( new String(dl.getKey()).equals(other)))
				) {
					item.setSelected(true);
				}

				box.appendChild(item);
			}


			box.addEventListener("onSelect", (Event event) -> {
				// get selected index, and check if it is caseid, activity or time stamp
				String selected = box.getSelectedItem().getValue();
				int colPos = Integer.parseInt(box.getId());
				removeColPos(colPos);

				if(new String(selected).equals(caseid) || new String(selected).equals(activity)|| new String(selected).equals(timestamp)){

					int oldColPos = heads.get(selected);
					if(oldColPos!= -1){
						Listbox oldBox = lists.get(oldColPos);
						oldBox.setSelectedIndex(otherIndex);
					}

					heads.put(selected,colPos);
				}else if(new String(selected).equals(ignore)) {
					ignoredPos.add(colPos);
				}else if(new String(selected).equals(tsValue)){
					String detectedFormat = parse.determineDateFormat(this.line[colPos]);
					if((detectedFormat != null)){
						otherTimeStampsPos.put(colPos, detectedFormat);
					}
					else{
						Messagebox.show("Could not parse timestamp format!");
					}
				}
			});


			lists.add(box);
		}
	}

	private void removeColPos(int colPos){

		if(otherTimeStampsPos.get(colPos) != null){
			otherTimeStampsPos.remove(Integer.valueOf(colPos));

		}else if(ignoredPos.contains(colPos)){
			ignoredPos.remove(Integer.valueOf(colPos));
		}else{

			for (Map.Entry<String, Integer> entry : heads.entrySet()) {
				if(entry.getValue() == colPos){
					heads.put(entry.getKey(), -1);
					break;
				}
			}
		}
	}


	public List<Listbox> getLists() {
		return lists;
	}



	/**
	 * Creates the X log.
	 *
	 *	create xlog element, assign respective extensions and attributes for each event and trace
	 * @param traces the traces
	 * @return the x log
	 */
	public XLog createXLog(List<LogModel> traces){
		if(traces == null) return null;

		XFactory xFactory = new XFactoryBufferedImpl();
		XLog xLog = xFactory.createLog();
		XTrace xTrace = null;
		XEvent xEvent = null;
		List<XEvent> allEvents = new ArrayList<XEvent>();

		// declare standard extensions of the log
		XConceptExtension concept = XConceptExtension.instance();
		XLifecycleExtension lifecycle = XLifecycleExtension.instance();
		XTimeExtension timestamp = XTimeExtension.instance();

		xLog.getExtensions().add(concept);
		xLog.getExtensions().add(lifecycle);
		xLog.getExtensions().add(timestamp);

		lifecycle.assignModel(xLog, XLifecycleExtension.VALUE_MODEL_STANDARD);

		String newTraceID = null;	// to keep track of traces, when a new trace is created we assign its value and add the respective events for the trace.
		String foramte = null;

		Comparator<XEvent> compareTimestamp= (XEvent o1, XEvent o2) -> ((XAttributeTimestampImpl) o1.getAttributes().get("time:timestamp")).getValue().compareTo(((XAttributeTimestampImpl) o2.getAttributes().get("time:timestamp")).getValue());

		for (LogModel trace : traces) {
			String caseID = trace.getCaseID();

			if(newTraceID == null || !newTraceID.equals(caseID)){	// This could be new trace

				if(!allEvents.isEmpty()){
					Collections.sort(allEvents, compareTimestamp);
					xTrace.addAll(allEvents);
					allEvents = new ArrayList<XEvent>();
				}

				xTrace = xFactory.createTrace();
				concept.assignName(xTrace, caseID);
				xLog.add(xTrace);
				newTraceID = caseID;
			}

			// create events for the current trace, assign the respective attributes.
			xEvent = xFactory.createEvent();
			concept.assignName(xEvent, trace.getConcept());
			lifecycle.assignStandardTransition(xEvent, XLifecycleExtension.StandardModel.COMPLETE);
			timestamp.assignTimestamp(xEvent, trace.getTimestamp());

			XAttribute attribute;
			HashMap<String, Timestamp> otherTimestamps = trace.getOtherTimestamps();
			for (Map.Entry<String, Timestamp> entry : otherTimestamps.entrySet()) {
				attribute = new XAttributeTimestampImpl(entry.getKey(), entry.getValue());
				xEvent.getAttributes().put(entry.getKey(), attribute);
			}

			HashMap<String, String> others = trace.getOthers();
			for (String key : others.keySet()) {
				attribute = new XAttributeLiteralImpl(key, others.get(key));
				xEvent.getAttributes().put(key, attribute);
			}
			allEvents.add(xEvent);
		}


		return xLog;
	}


	/**
	 * To XES file.
	 *
	 *	Serialize xLog to XES file.
	 *
	 * @param xLog the x log
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void toXESfile(XLog xLog, String FileName) throws FileNotFoundException, IOException {
		if(xLog == null) return;


		String FileNameWithoutExtention = FileName.replaceFirst("[.][^.]+$", "");
		XesXmlSerializer serializer = new XesXmlSerializer();
		serializer.serialize(xLog, new FileOutputStream(new File(FileNameWithoutExtention + ".xes")));
		Messagebox.show("Your file has been created!");

	}
}