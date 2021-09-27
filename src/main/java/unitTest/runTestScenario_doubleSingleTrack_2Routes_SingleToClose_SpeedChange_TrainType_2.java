package unitTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import eventsHandling.SingleTrackListener;

public class runTestScenario_doubleSingleTrack_2Routes_SingleToClose_SpeedChange_TrainType_2 {


	public static void main(String[] args) {

		// TODO Auto-generated method stub

		// TODO Auto-generated method stub
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String configFile = inputDataPath+"config-DTA_complicatedSingleTrack_2Routes_TrainType_2.xml";
		String planFile = inputDataPath+"testPlan_doubleSingleTrack_TrainType_2.xml";
		String networkFile = inputDataPath+"testNetwork_singleTrack_2Routes_SingleToClose_TrainType_2.xml";
		String vehicleFile = inputDataPath+"trainType_2.xml";
		String outputDirectory = inputDataPath+"output_doubleSingleTrack_2Routes_SingleToClose_TrainType_2";
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.network().setInputFile(networkFile);
		config.network().setTimeVariantNetwork(true);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(30);
		config.controler().setWriteEventsInterval(1);
		config.vehicles().setVehiclesFile(vehicleFile);
		config.plans().setInputFile(planFile);
		
		config.qsim().setStartTime(7*60*60);
		config.qsim().setEndTime(9*60*60);
		
		// create/load the scenario here.  The time variant network does already have to be set at this point
				// in the config, otherwise it will not work.
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		Network nt = scenario.getNetwork();
		// make sure you only specify TPÅ on one link if this link is a single track, in the example below, 
		//it doesn't matter if we specify q_l_3_AB or q_l_3_BA.
		Id<Link> linkId = Id.createLinkId("q_l_3_AB");
		Link linkToClose = nt.getLinks().get(linkId  ) ;
		NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(8*60*60+30) ;
		networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
		networkChangeEvent.addLink(linkToClose);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent);
		

		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8*60*60+180) ;
		networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  60 ));
		networkChangeEvent2.addLink(linkToClose);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent2);

		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
		for (Entry<Id<Person>, ? extends Person> person:persons.entrySet()) {
			Map<String, Id<Vehicle>> modeToVehicle = new HashMap<String, Id<Vehicle>>();
			modeToVehicle.put("car", Id.createVehicleId(person.getKey().toString()));
			VehicleUtils.insertVehicleIdsIntoAttributes(person.getValue(), modeToVehicle);
		}
		
//		// speed change events
//		Id<Link> l_6_AB = Id.createLinkId("l_6_AB");
//		Id<Link> l_6_BA = Id.createLinkId("l_6_BA");
//		Link link_l_6_AB = nt.getLinks().get(l_6_AB  ) ;
//		Link link_l_6_BA = nt.getLinks().get(l_6_BA  ) ;
//		NetworkChangeEvent networkChangeEvent3 = new NetworkChangeEvent(8*60*60+60) ;
//		networkChangeEvent3.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  5 ));
//		networkChangeEvent3.addLink(link_l_6_AB);
//		networkChangeEvent3.addLink(link_l_6_BA);
//		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent3);
//		
//		NetworkChangeEvent networkChangeEvent4 = new NetworkChangeEvent(8*60*60+90) ;
//		networkChangeEvent4.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  25 ));
//		networkChangeEvent4.addLink(link_l_6_AB);
//		networkChangeEvent4.addLink(link_l_6_BA);
//		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent4);
		
		// add the events handlers
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {			
				bind(SingleTrackListener.class).asEagerSingleton();
				addMobsimListenerBinding().to(SingleTrackListener.class);
				addControlerListenerBinding().to(SingleTrackListener.class);
				addEventHandlerBinding().to(SingleTrackListener.class);
				
//				final RandomizingTimeDistanceTravelDisutilityFactory carbuilder = new RandomizingTimeDistanceTravelDisutilityFactory( "car", getConfig() );
//				addTravelDisutilityFactoryBinding("slowTrain" ).toInstance( carbuilder );
//				addTravelDisutilityFactoryBinding("fastTrain" ).toInstance( carbuilder );
//				addRoutingModuleBinding("slowTrain").toProvider(new NetworkRoutingProvider("car"));
//				addRoutingModuleBinding("fastTrain").toProvider(new NetworkRoutingProvider("car"));
			}
		});
		
		controler.run() ;
		
	
	
	}



}
