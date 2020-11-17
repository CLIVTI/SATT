package unitTest;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;



public class runTestScenario2 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String configFile = inputDataPath+"config-DTATest.xml";
		String outputDirectory = inputDataPath+"output_singleTrack";
		
		Config config = ConfigUtils.loadConfig( configFile ) ;
		config.network().setTimeVariantNetwork(true);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setStartTime(0*60*60);
		config.qsim().setEndTime(24*60*60);
		// config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		
		// create/load the scenario here.  The time variant network does already have to be set at this point
				// in the config, otherwise it will not work.
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		
		
		Network nt = scenario.getNetwork();
		
		Id<Link> linkIDOppositeDirection = Id.createLinkId("l_2_AB");
		Link linkOppositeDirection = nt.getLinks().get(linkIDOppositeDirection  ) ;
		NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(8*60*60+20) ;
		networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
		// networkChangeEvent.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0.01 ));
		networkChangeEvent.addLink(linkOppositeDirection);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent);
		

		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8*60*60+120) ;
		networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, 10000000 ));
		// networkChangeEvent2.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  90/3.6 ));
		networkChangeEvent2.addLink(linkOppositeDirection);
		NetworkUtils.addNetworkChangeEvent(nt,networkChangeEvent2);


		
		

		Controler controler = new Controler( scenario ) ;
		controler.run() ;
		
	}

}
