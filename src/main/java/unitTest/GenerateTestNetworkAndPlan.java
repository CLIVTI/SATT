package unitTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

public class GenerateTestNetworkAndPlan {
	// network ser ut så här:		
	//                           l_7
	//                       o----------o
	//                  l_6 / n_7    n_6 \ l_8
	//         n_1  l_1    /      l_2     \       l_9  n_8
	//          o---------o----------------o------------o
	//                 n_2 \              / n_5
	//                  l_3 \            / l_5
	//                       o----------o
	//                      n_3   l_4  n_4 



	public static void main(String[] args) {
		String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
		inputDataPath = inputDataPath.replaceAll("\\\\", "/");
		String outputNetworkFile = inputDataPath+"testNetwork.xml";
		String outputPlanFile = inputDataPath+"testPlan.xml";


		GenerateTestNetworkAndPlan test = new GenerateTestNetworkAndPlan();
		test.createNetworkAndPlan(outputNetworkFile,outputPlanFile);
	}



	public void createNetworkAndPlan(String outputNetworkFile, String outputPlanFile){
		final Network matsimNetwork = NetworkUtils.createNetwork();
		NetworkFactory fac = matsimNetwork.getFactory();
		Set<String> allowedModes = new HashSet<>(Arrays.asList(TransportMode.car));


		// add some nodes
		Node node1 = fac.createNode(Id.createNodeId("n_1"), new Coord(0, 0));
		Node node2 = fac.createNode(Id.createNodeId("n_2"), new Coord(1000, 0));
		Node node3 = fac.createNode(Id.createNodeId("n_3"), new Coord(1500, -500*Math.sqrt(3)));
		Node node4 = fac.createNode(Id.createNodeId("n_4"), new Coord(2500, -500*Math.sqrt(3)));
		Node node5 = fac.createNode(Id.createNodeId("n_5"), new Coord(3000, 0));
		Node node6 = fac.createNode(Id.createNodeId("n_6"), new Coord(2500, 500*Math.sqrt(3)));
		Node node7 = fac.createNode(Id.createNodeId("n_7"), new Coord(1500, 500*Math.sqrt(3)));
		Node node8 = fac.createNode(Id.createNodeId("n_8"), new Coord(4000, 0));
		matsimNetwork.addNode(node1);
		matsimNetwork.addNode(node2);
		matsimNetwork.addNode(node3);
		matsimNetwork.addNode(node4);
		matsimNetwork.addNode(node5);
		matsimNetwork.addNode(node6);
		matsimNetwork.addNode(node7);
		matsimNetwork.addNode(node8);

		// add some links
		ArrayList<Link> links = new ArrayList<Link>();

		links.add(fac.createLink(Id.createLinkId("l_1_AB"), node1, node2));
		links.add(fac.createLink(Id.createLinkId("l_1_BA"), node2, node1));
		links.add(fac.createLink(Id.createLinkId("l_2_AB"), node2, node5));
		links.add(fac.createLink(Id.createLinkId("l_2_BA"), node5, node2));
		links.add(fac.createLink(Id.createLinkId("l_3_AB"), node2, node3));
		links.add(fac.createLink(Id.createLinkId("l_3_BA"), node3, node2));
		links.add(fac.createLink(Id.createLinkId("l_4_AB"), node3, node4));
		links.add(fac.createLink(Id.createLinkId("l_4_BA"), node4, node3));
		links.add(fac.createLink(Id.createLinkId("l_5_AB"), node4, node5));
		links.add(fac.createLink(Id.createLinkId("l_5_BA"), node5, node4));
		links.add(fac.createLink(Id.createLinkId("l_6_AB"), node2, node7));
		links.add(fac.createLink(Id.createLinkId("l_6_BA"), node7, node2));
		links.add(fac.createLink(Id.createLinkId("l_7_AB"), node7, node6));
		links.add(fac.createLink(Id.createLinkId("l_7_BA"), node6, node7));
		links.add(fac.createLink(Id.createLinkId("l_8_AB"), node6, node5));
		links.add(fac.createLink(Id.createLinkId("l_8_BA"), node5, node6));
		links.add(fac.createLink(Id.createLinkId("l_9_AB"), node5, node8));
		links.add(fac.createLink(Id.createLinkId("l_9_BA"), node8, node5));

		for (Link eachLink : links ) {
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

		// first add a person-trip from node1 to node8
		Activity home = population.getFactory().createActivityFromCoord("home", node1.getCoord());
		home.setEndTime(8*60*60);
		plan.addActivity(home);

		Leg hinweg = population.getFactory().createLeg(TransportMode.car);
		plan.addLeg(hinweg);

		Activity work = population.getFactory().createActivityFromCoord("work", node8.getCoord());
		work.setEndTime(21*60*60);
		plan.addActivity(work);
		person1.addPlan(plan);
		population.addPerson(person1);

		// second add a person-trip from node8 to node1
		Person person2 = population.getFactory().createPerson(Id.createPersonId("P2"));
		Plan plan2 = population.getFactory().createPlan();
		// first add a person-trip from node1 to node8
		Activity home2 = population.getFactory().createActivityFromCoord("home", node8.getCoord());
		home2.setEndTime(8*60*60);
		plan2.addActivity(home2);

		Leg hinweg2 = population.getFactory().createLeg(TransportMode.car);
		plan2.addLeg(hinweg2);

		Activity work2 = population.getFactory().createActivityFromCoord("work", node1.getCoord());
		work2.setEndTime(21*60*60);
		plan2.addActivity(work2);
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		PopulationWriter popwriter = new PopulationWriter(population, matsimNetwork);
		popwriter.write(outputPlanFile);
	}
}
