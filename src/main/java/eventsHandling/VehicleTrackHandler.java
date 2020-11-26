package eventsHandling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.charts.XYLineChart;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class VehicleTrackHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	private static Logger logger = Logger.getLogger(VehicleTrackHandler.class);
	private ArrayList<String> interestedLinks=new ArrayList<String>();
	private ArrayList<String> stationAxis = new ArrayList<String>();
	private Table<String, String, Double> trainTimeTable= HashBasedTable.create();

	public VehicleTrackHandler(ArrayList<String> interestedLinks) {
		super();
		for (String interestedLinkId :interestedLinks) {
			String fromNode=interestedLinkId+"_AB_fromNode";
			String toNode=interestedLinkId+"_AB_toNode";
			stationAxis.add(fromNode);
			stationAxis.add(toNode);
		}
		this.interestedLinks=interestedLinks;
	}
	
	public VehicleTrackHandler(String[] interestedLinks) {
		super();
		for (int i=0;i<interestedLinks.length;i++) {
			this.interestedLinks.add(interestedLinks[i]);
			String fromNode=interestedLinks[i]+"_AB_fromNode";
			String toNode=interestedLinks[i]+"_AB_toNode";
			stationAxis.add(fromNode);
			stationAxis.add(toNode);
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// check when vehicle enters the interestedLink
		String linkId=event.getLinkId().toString();
		String cleanLinkId = linkIdProcessor.getCleanLinkID(linkId);
		if (interestedLinks.contains(cleanLinkId) & !linkId.contains("q_") & !linkId.contains("c_")) {
			String vehicleId=event.getVehicleId().toString();
			if (linkId.contains("_AB")) {
				String linkEnter=linkId+"_fromNode";
				trainTimeTable.put(linkEnter, vehicleId, event.getTime());
			} else if (linkId.contains("_BA")){
				String linkEnter=linkIdProcessor.getOppositeLinkID(linkId)+"_toNode";
				trainTimeTable.put(linkEnter, vehicleId, event.getTime());
			} else {
				logger.warn("the link Id:" +linkId + " is not supposed to be defined in this way. It should have the format XXX_AB or XXX_BA.");
			}
		}

	}
	

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// check when vehicle enters the interestedLink
		String linkId=event.getLinkId().toString();
		String cleanLinkId = linkIdProcessor.getCleanLinkID(linkId);
		if (interestedLinks.contains(cleanLinkId) & !linkId.contains("q_") & !linkId.contains("c_")) {
			String vehicleId=event.getVehicleId().toString();
			if (linkId.contains("_AB")) {
				String linkEnter=linkId+"_toNode";
				trainTimeTable.put(linkEnter, vehicleId, event.getTime());
			} else if (linkId.contains("_BA")){
				String linkEnter=linkIdProcessor.getOppositeLinkID(linkId)+"_fromNode";
				trainTimeTable.put(linkEnter, vehicleId, event.getTime());
			} else {
				logger.warn("the link Id:" +linkId + " is not supposed to be defined in this way. It should have the format XXX_AB or XXX_BA.");
			}
		}
	}
	

	public void drawTimeTable(String filename) {
		 String[] stations = stationAxis.toArray(String[]::new);
		 double[] labelPosition=new double[stations.length];
		 String comparedId = stations[0];
		 labelPosition[0]=1;
		 for (int i=1;i<stations.length;i++) {
			 comparedId=comparedId.replaceAll("_fromNode", "");
			 comparedId=comparedId.replaceAll("_toNode", "");
			 String nextId= stations[i];
			 nextId=nextId.replaceAll("_fromNode", "");
			 nextId=nextId.replaceAll("_toNode", "");
			 if (linkIdProcessor.getCleanLinkID(comparedId).equals(linkIdProcessor.getCleanLinkID(nextId))) {
				 labelPosition[i]= labelPosition[i-1]+9;
			 } else {
				 labelPosition[i]=labelPosition[i-1]+2;
			 }
			 comparedId= stations[i];
		 }
		
		XYLineChart chart = new XYLineChart("train time table", "time","check points" );
		Set<String> vehicleIds = trainTimeTable.columnKeySet();
		double minimumTime=Double.POSITIVE_INFINITY;
		double maximumTime=Double.NEGATIVE_INFINITY;
		
		// get the maximum and minimu time
		for (String vehicleId : vehicleIds) {
			Map<String, Double> column = trainTimeTable.column(vehicleId);
			for (int i=0;i<stations.length;i++) {
				Double thisTime = column.get(stations[i]);
				if (thisTime<minimumTime) {
					minimumTime=thisTime;
				}
				if (thisTime>maximumTime) {
					maximumTime=thisTime;
				}
			}
		}
		
		// loop again to add it in chat
		for (String vehicleId : vehicleIds) {
			Map<String, Double> column = trainTimeTable.column(vehicleId);
			double[] times = new double[stations.length];
			for (int i=0;i<stations.length;i++) {
				Double thisTime = column.get(stations[i]);
				times[i]=thisTime-minimumTime;
			}
			chart.addSeries(vehicleId, times,labelPosition);
		}
		
		// formating the time table plot
		JFreeChart jFreeChart = chart.getChart();
		XYPlot  plot = (XYPlot)jFreeChart.getPlot();
		for (int i=0;i<stations.length;i++) {
			ValueMarker vm = new ValueMarker(labelPosition[i],Color.black, new BasicStroke());
			plot.addRangeMarker(vm);
		}
		
		
		NumberAxis domain = (NumberAxis) plot.getDomainAxis(); // x axis
		domain.setRange(-60, maximumTime-minimumTime+60);
		plot.setDomainAxis(domain);
		// create an empty string array
		ArrayList<String> yAxis = new ArrayList<String>() ;
		int currentNumber=0;
		int count=0;
		while(currentNumber<=(labelPosition[labelPosition.length-1]+1.1)) {
			if (checkElement(currentNumber,labelPosition)) {
				yAxis.add(stations[count]);
				count++;
			} else {
				yAxis.add("");
			}
			currentNumber=currentNumber+1;
			
		}
		// create the yAxis display
		String[] yAxisDisplay = yAxis.toArray(String[]::new);
		SymbolAxis rangeAxis = new SymbolAxis("", yAxisDisplay);
		rangeAxis.setTickUnit(new NumberTickUnit(1));
		rangeAxis.setRange(0,labelPosition[labelPosition.length-1]+1);
		rangeAxis.setVisible(true);
		rangeAxis.setTickLabelsVisible(true);
		plot.setRangeAxis(rangeAxis);
		chart.saveAsPng(filename, 800, 600);
		
	}
	
	private static boolean checkElement(double number,double[] labelPosition) {
		for (int i=0;i<labelPosition.length;i++) {
			if (Math.abs(number-labelPosition[i])<0.05) {
				return true;
			}
		}
		return false;
	}
	
	
}
