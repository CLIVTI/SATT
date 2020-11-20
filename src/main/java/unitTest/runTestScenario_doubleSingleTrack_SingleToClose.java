package unitTest;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import eventsHandling.SingleTrackListener;

public class runTestScenario_doubleSingleTrack_SingleToClose {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// TODO Auto-generated method stub
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String configFile = inputDataPath+"config-DTA_complicatedSingleTrack.xml";
		String planFile = inputDataPath+"testPlan_doubleSingleTrack.xml";
		String networkFile = inputDataPath+"testNetwork_doubleSingleTrack_SingleToClose.xml";
		String outputDirectory = inputDataPath+"output_doubleSingleTrack_SingleToClose";
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.network().setInputFile(networkFile);
		config.network().setTimeVariantNetwork(true);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1);
		config.plans().setInputFile(planFile);
		
		config.qsim().setStartTime(0*60*60);
		config.qsim().setEndTime(24*60*60);
		
		// create/load the scenario here.  The time variant network does already have to be set at this point
				// in the config, otherwise it will not work.
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		
		
		Network nt = scenario.getNetwork();
		
		Id<Link> linkId = Id.createLinkId("q_l_3_AB");
		Link linkToClose = nt.getLinks().get(linkId  ) ;
		NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(8*60*60+120) ;
		networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
		networkChangeEvent.addLink(linkToClose);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent);
		

		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8*60*60+300) ;
		networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  60 ));
		networkChangeEvent2.addLink(linkToClose);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent2);
		
		
		// add the events handlers
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {			
				bind(SingleTrackListener.class).asEagerSingleton();
				addMobsimListenerBinding().to(SingleTrackListener.class);
				addControlerListenerBinding().to(SingleTrackListener.class);
				addEventHandlerBinding().to(SingleTrackListener.class);
			}
		});
		
		controler.run() ;
		
	
	}

}
