package unitTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GenerateTestNetworkAndPlanForSingleTrack {
// network ser ut så här:		
	
	
//              l_1           l_2             l_3 
//          o---------o----------------o------------o
//         n_1       n_2              n_3          n_4

	


	public static void main(String[] args) {
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String outputNetworkFile = inputDataPath+"testNetwork_singleTrack.xml";
		String outputPlanFile = inputDataPath+"testPlan_singleTrack.xml";
		
		
		GenerateTestNetworkAndPlanForSingleTrack test = new GenerateTestNetworkAndPlanForSingleTrack();
		test.createNetwork(outputNetworkFile,outputPlanFile);
	}
	
	
	public void createPlan(String outputPlanFile) {
		
	}
	
	public void createNetwork(String outputNetworkFile, String outputPlanFile){
		final Network matsimNetwork = NetworkUtils.createNetwork();
		NetworkFactory fac = matsimNetwork.getFactory();
		Set<String> allowedModes = new HashSet<>(Arrays.asList(TransportMode.car));


		// add some nodes
		Node node1 = fac.createNode(Id.createNodeId("n_1"), new Coord(0, 0));
		Node node2 = fac.createNode(Id.createNodeId("n_2"), new Coord(1000, 0));
		Node node2q = fac.createNode(Id.createNodeId("n_2_q"), new Coord(1001, 0));
		Node node3 = fac.createNode(Id.createNodeId("n_3"), new Coord(2000, 0));
		Node node3q = fac.createNode(Id.createNodeId("n_3_q"), new Coord(1999, 0));
		Node node4 = fac.createNode(Id.createNodeId("n_4"), new Coord(3000, 0));

		matsimNetwork.addNode(node1);
		matsimNetwork.addNode(node2);
		matsimNetwork.addNode(node3);
		matsimNetwork.addNode(node4);
		matsimNetwork.addNode(node2q);
		matsimNetwork.addNode(node3q);
		
		// add some links
		ArrayList<Link> links = new ArrayList<Link>();

		links.add(fac.createLink(Id.createLinkId("l_1_AB"), node1, node2));
		links.add(fac.createLink(Id.createLinkId("l_1_BA"), node2, node1));
		links.add(fac.createLink(Id.createLinkId("q_l_2_AB"), node2, node2q));
		links.add(fac.createLink(Id.createLinkId("l_2_AB"), node2q, node3));
		links.add(fac.createLink(Id.createLinkId("q_l_2_BA"), node3, node3q));
		links.add(fac.createLink(Id.createLinkId("l_2_BA"), node3q, node2));
		links.add(fac.createLink(Id.createLinkId("l_3_AB"), node3, node4));
		links.add(fac.createLink(Id.createLinkId("l_3_BA"), node4, node3));
		

		for (Link eachLink : links ) {
			if (eachLink.getId().toString().contains("q_l_2")) {
				eachLink.getAttributes().putAttribute("SingleTrack", 1);
			} else {
				eachLink.getAttributes().putAttribute("SingleTrack", 0);
			}
			eachLink.setAllowedModes(allowedModes);
			eachLink.setCapacity(20);
			eachLink.setFreespeed(90/3.6);
			matsimNetwork.addLink(eachLink);
		}

		NetworkWriter plainNetworkWriter = new NetworkWriter(matsimNetwork);
		plainNetworkWriter.write(outputNetworkFile);

		// create plan
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		Person person1 = population.getFactory().createPerson(Id.createPersonId("P1"));
		Plan plan = population.getFactory().createPlan();

		// first add a person-trip from node1 to node4
		Activity home = population.getFactory().createActivityFromLinkId("home", links.get(0).getId());
		home.setEndTime(8*60*60);
		plan.addActivity(home);

		Leg hinweg = population.getFactory().createLeg(TransportMode.car);
		plan.addLeg(hinweg);

		Activity work = population.getFactory().createActivityFromLinkId("home", links.get(6).getId());
		work.setEndTime(21*60*60);
		plan.addActivity(work);
		person1.addPlan(plan);
		population.addPerson(person1);

//		// second add a person-trip from node1 to node4 but 10 sec departure time shift
//		Person person2 = population.getFactory().createPerson(Id.createPersonId("P2"));
//		Plan plan2 = population.getFactory().createPlan();
//		// first add a person-trip from node1 to node8
//		Activity home2 = population.getFactory().createActivityFromLinkId("home", links.get(0).getId());
//		home2.setEndTime(8*60*60+10);
//		plan2.addActivity(home2);
//
//		Leg hinweg2 = population.getFactory().createLeg(TransportMode.car);
//		plan2.addLeg(hinweg2);
//
//		Activity work2 = population.getFactory().createActivityFromLinkId("home", links.get(4).getId());
//		work2.setEndTime(21*60*60);
//		plan2.addActivity(work2);
//		person2.addPlan(plan2);
//		population.addPerson(person2);
//		
		
		
		// third add a person-trip from node1 to node4 but 10 sec departure time shift
				Person person3 = population.getFactory().createPerson(Id.createPersonId("P3"));
				Plan plan3 = population.getFactory().createPlan();
				// first add a person-trip from node1 to node8
				Activity home3 = population.getFactory().createActivityFromLinkId("home", links.get(7).getId());
				home3.setEndTime(8*60*60+5);
				plan3.addActivity(home3);

				Leg hinweg3 = population.getFactory().createLeg(TransportMode.car);
				plan3.addLeg(hinweg3);

				Activity work3 = population.getFactory().createActivityFromLinkId("home", links.get(1).getId());
				work3.setEndTime(21*60*60);
				plan3.addActivity(work3);
				person3.addPlan(plan3);
				population.addPerson(person3);
		
		PopulationWriter popwriter = new PopulationWriter(population, matsimNetwork);
		popwriter.write(outputPlanFile);
	}
}
