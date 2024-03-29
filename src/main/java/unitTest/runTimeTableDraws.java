package unitTest;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import eventsHandling.VehicleTrackHandler;

public class runTimeTableDraws {

	public static void main(String[] args) {
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\output_doubleSingleTrack_2Routes_SingleToClose_SpeedChange_TrainType_2\\ITERS\\it.21\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String inputEventFilePath = inputDataPath+"21.events.xml.gz";
		String writeTimeTablePath = inputDataPath+"timeTable_2.png";
		EventsManager events = EventsUtils.createEventsManager();
		
		String[] interestedLinks = {"l_2","l_3"};
		VehicleTrackHandler timeTimeHandler = new VehicleTrackHandler(interestedLinks);
		events.addHandler(timeTimeHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventFilePath);
		timeTimeHandler.drawTimeTable(writeTimeTablePath);
	}

}
