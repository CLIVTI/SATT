package unitTest;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import eventsHandling.SingleTrackListener;


public class runTestScenario_simpleSingleTrack {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String configFile = inputDataPath+"config-DTA_simpleSingleTrack.xml";
		String planFile = inputDataPath+"testPlan_simpleSingleTrack.xml";
		String networkFile = inputDataPath+"testNetwork_singleTrack.xml";
		String outputDirectory = inputDataPath+"output_simpleSingleTrack";
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.network().setInputFile(networkFile);
		config.network().setTimeVariantNetwork(true);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plans().setInputFile(planFile);
		
		
		config.qsim().setStartTime(0*60*60);
		config.qsim().setEndTime(24*60*60);
		
		// create/load the scenario here.  The time variant network does already have to be set at this point
				// in the config, otherwise it will not work.
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		
		
//		Network nt = scenario.getNetwork();
//		
//		Id<Link> linkIDOppositeDirection = Id.createLinkId("l_2_AB");
//		Link linkOppositeDirection = nt.getLinks().get(linkIDOppositeDirection  ) ;
//		NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(7*60*60) ;
//		networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
//		networkChangeEvent.addLink(linkOppositeDirection);
//		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent);
//		
//
//		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8*60*60+120) ;
//		networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  20 ));
//		networkChangeEvent2.addLink(linkOppositeDirection);
//		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent2);
		
		
		// add the events handlers
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {			
				bind(SingleTrackListener.class).asEagerSingleton();
				addMobsimListenerBinding().to(SingleTrackListener.class);
				addEventHandlerBinding().to(SingleTrackListener.class);
			}
		});
		
		controler.run() ;
		
	}

}
