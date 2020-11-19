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

public class GenerateTestNetworkAndPlanForDoubleSingleTrack {

	// network ser ut så här:	
	// operational network topology
//                      		              c_l_3           
//  		           q_l_2_AB        q_l_2_BA  q_l_3_AB      q_l_3_BA
//	              l_1              l_2                     l_3          l_4
//	          o---------o__o-------------o__o<<<>>>o__o-----------o__o---------o
//	         n_1      n_2                  n_3_1 n_3_2              n_4       n_5
//                        n_2_q       n_3_1_q      n_3_2_q      n_4_q
//                                              |
    //   actual network topology                                           |	
//                  l_1        l_2(single)         l_3(single)           l_4 
//            o---------o-----------------------o--------------------o---------o
//           n_1       n_2                     n_3                  n_4	      n_5
 
		


		public static void main(String[] args) {
			String inputDataPath = System.getProperty("user.dir")+"\\src\\main\\resources\\testData\\";
			inputDataPath = inputDataPath.replaceAll("\\\\", "/");
			String outputNetworkFile = inputDataPath+"testNetwork_doubleSingleTrack.xml";
			String outputPlanFile = inputDataPath+"testPlan_doubleSingleTrack.xml";
			
			
			GenerateTestNetworkAndPlanForDoubleSingleTrack test = new GenerateTestNetworkAndPlanForDoubleSingleTrack();
			test.createNetworkAndPlan(outputNetworkFile,outputPlanFile);
		}
		
		
		
		public void createNetworkAndPlan(String outputNetworkFile, String outputPlanFile){
			final Network matsimNetwork = NetworkUtils.createNetwork();
			NetworkFactory fac = matsimNetwork.getFactory();
			Set<String> allowedModes = new HashSet<>(Arrays.asList(TransportMode.car));


			// add some nodes
			Node node1 = fac.createNode(Id.createNodeId("n_1"), new Coord(0, 0));
			Node node2 = fac.createNode(Id.createNodeId("n_2"), new Coord(1000, 0));
			Node node2q = fac.createNode(Id.createNodeId("n_2_q"), new Coord(1050, 0));
			Node node3_1q = fac.createNode(Id.createNodeId("n_3_1_q"), new Coord(1950, 0));
			Node node3_1 = fac.createNode(Id.createNodeId("n_3_1"), new Coord(1990, 0));
			Node node3_2 = fac.createNode(Id.createNodeId("n_3_2"), new Coord(2010, 0));
			Node node3_2q = fac.createNode(Id.createNodeId("n_3_2_q"), new Coord(2050, 0));
			Node node4q = fac.createNode(Id.createNodeId("n_4_q"), new Coord(2950, 0));
			Node node4 = fac.createNode(Id.createNodeId("n_4"), new Coord(3000, 0));
			Node node5 = fac.createNode(Id.createNodeId("n_5"), new Coord(4000, 0));
			
			matsimNetwork.addNode(node1);
			matsimNetwork.addNode(node2);
			matsimNetwork.addNode(node2q);
			matsimNetwork.addNode(node3_1q);
			matsimNetwork.addNode(node3_1);
			matsimNetwork.addNode(node3_2);
			matsimNetwork.addNode(node3_2q);
			matsimNetwork.addNode(node4q);
			matsimNetwork.addNode(node4);
			matsimNetwork.addNode(node5);
			
			// add some links
			ArrayList<Link> links = new ArrayList<Link>();

			links.add(fac.createLink(Id.createLinkId("l_1_AB"), node1, node2));
			links.add(fac.createLink(Id.createLinkId("l_1_BA"), node2, node1));
			links.add(fac.createLink(Id.createLinkId("q_l_2_AB"), node2, node2q));
			links.add(fac.createLink(Id.createLinkId("l_2_AB"), node2q, node3_1));
			links.add(fac.createLink(Id.createLinkId("l_2_BA"), node3_1q, node2));
			links.add(fac.createLink(Id.createLinkId("q_l_2_BA"), node3_1, node3_1q));
			links.add(fac.createLink(Id.createLinkId("c_l_3_AB"), node3_1, node3_2));
			links.add(fac.createLink(Id.createLinkId("c_l_3_BA"), node3_2, node3_1));
			links.add(fac.createLink(Id.createLinkId("q_l_3_AB"), node3_2, node3_2q));
			links.add(fac.createLink(Id.createLinkId("l_3_AB"), node3_2q, node4));
			links.add(fac.createLink(Id.createLinkId("l_3_BA"), node4q, node3_2));
			links.add(fac.createLink(Id.createLinkId("q_l_3_BA"), node4, node4q));
			links.add(fac.createLink(Id.createLinkId("l_4_AB"), node4, node5));
			links.add(fac.createLink(Id.createLinkId("l_4_BA"), node5, node4));
			
			for (Link eachLink : links ) {
				if (eachLink.getId().toString().contains("q_")) {
					eachLink.getAttributes().putAttribute("SingleTrack", 1);
				} else {
					eachLink.getAttributes().putAttribute("SingleTrack", 0);
				}
				
				eachLink.setAllowedModes(allowedModes);
				eachLink.setFreespeed(90/3.6);
				
				if (eachLink.getId().toString().contains("c_")) {
					eachLink.setCapacity(10000000);
				} else {
					eachLink.setCapacity(60);
				}
				eachLink.getAttributes().putAttribute("Capacity", eachLink.getCapacity());
				matsimNetwork.addLink(eachLink);
			}
			
			//matsimNetwork.setEffectiveCellSize(50);
			NetworkWriter plainNetworkWriter = new NetworkWriter(matsimNetwork);
			plainNetworkWriter.write(outputNetworkFile);

			// create plan
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Population population = scenario.getPopulation();
			int numberOfTrips=5;

			for (int np=1;np<=numberOfTrips;np++) {
				Person person = population.getFactory().createPerson(Id.createPersonId("P_AB_"+np));
				Plan plan = population.getFactory().createPlan();
				Activity home = population.getFactory().createActivityFromLinkId("home", links.get(0).getId());
				
				if (np==2) {
					home.setEndTime(8*60*60+7);
				} else {
					home.setEndTime(8*60*60+(np-1)*20);
				}
				
				
				plan.addActivity(home);
				
				Leg hinweg = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(hinweg);
				
				Activity work = population.getFactory().createActivityFromLinkId("home", links.get(12).getId());
				work.setEndTime(21*60*60);
				plan.addActivity(work);
				person.addPlan(plan);
				population.addPerson(person);
			}
			
			for (int np=1;np<=numberOfTrips;np++) {
				Person person = population.getFactory().createPerson(Id.createPersonId("P_BA_"+np));
				Plan plan = population.getFactory().createPlan();
				Activity home = population.getFactory().createActivityFromLinkId("home", links.get(13).getId());
				home.setEndTime(8*60*60+10+(np-1)*20);
				plan.addActivity(home);
				
				Leg hinweg = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(hinweg);
				
				Activity work = population.getFactory().createActivityFromLinkId("home", links.get(1).getId());
				work.setEndTime(21*60*60);
				plan.addActivity(work);
				person.addPlan(plan);
				population.addPerson(person);
			}

			PopulationWriter popwriter = new PopulationWriter(population, matsimNetwork);
			popwriter.write(outputPlanFile);
		}


}
