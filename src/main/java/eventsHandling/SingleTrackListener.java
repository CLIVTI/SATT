package eventsHandling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;

import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class SingleTrackListener implements MobsimInitializedListener,BeforeMobsimListener, MobsimBeforeSimStepListener,BasicEventHandler {

	private static Logger logger = Logger.getLogger(SingleTrackListener.class);
	private int currentIter=-1;
	private int TPAEventsPosition=0;
	private PriorityQueue<NetworkChangeEvent> networkChangeEventQueue = new PriorityQueue<NetworkChangeEvent>(1, new Comparator<NetworkChangeEvent>() {

		@Override
		public int compare(NetworkChangeEvent o1, NetworkChangeEvent o2) {
			Double o1Double = o1.getStartTime();
			Double o2Double = o2.getStartTime();
			return o1Double.compareTo(o2Double);
		}

	});

	
	// remember that TPAEvents can never have null. save some validity check.
	private ArrayList<NetworkChangeEvent> TPAEvents= new ArrayList<NetworkChangeEvent>();


	private enum linkStatus{
		Double, 
		Single,
		Closed
	}

	private HashMap<String,Integer> nTrainsEnterLinkAtAnyGivenTime = new  HashMap<String,Integer>();
	private HashMap<String,Integer> nTrainsWaitLinkAtAnyGivenTime = new  HashMap<String,Integer>();
	private HashMap<String,linkStatus> allLinkStatus = new HashMap<String,linkStatus>();


	@Inject Scenario scenario;


	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		currentIter=event.getIteration();
		logger.info("current Iteration number: "+currentIter+".");
	}


	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		logger.info("Mobsim is Initialized for iteration: " + currentIter +".");
		// reinitialize all necessary variables, keep TPAEvents not cleared.
		networkChangeEventQueue.clear();
		nTrainsEnterLinkAtAnyGivenTime.clear();
		nTrainsWaitLinkAtAnyGivenTime.clear();
		allLinkStatus.clear();
		TPAEventsPosition=0;
		
		// generate wait and enter link vehiclecounters and linkStatus map.
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		for (Entry<Id<Link>, ? extends Link> eachLink :allLinks.entrySet()) {
			Integer singleTrackDummy = (Integer) eachLink.getValue().getAttributes().getAttribute("SingleTrack");
			String linkID=eachLink.getKey().toString();
			String cleanLinkID=getCleanLinkID(linkID);
			if (singleTrackDummy==1 ) {
				nTrainsEnterLinkAtAnyGivenTime.put(eachLink.getKey().toString(), 0);
				nTrainsWaitLinkAtAnyGivenTime.put(eachLink.getKey().toString(), 0);
				allLinkStatus.put(cleanLinkID, linkStatus.Single);
			}
		}




		QSim qSim=(QSim) e.getQueueSimulation();
		TimeDependentNetwork network = (TimeDependentNetwork) qSim.getScenario().getNetwork();

		// if this is the first iteration there is no NetworkChangeEvents related to the singletrack. We therefore get all existing network change event and save them in a data structure.
		if (currentIter==0) {
			TPAEvents= new ArrayList<NetworkChangeEvent>();
			Queue<NetworkChangeEvent> TPAEventsQueue = network.getNetworkChangeEvents();
			while (TPAEventsQueue.size()>0) {
				NetworkChangeEvent TPAEvent = TPAEventsQueue.poll();
				if (TPAEvent!=null) {
					TPAEvents.add(TPAEvent);
				}
			}
		}
		
		for (NetworkChangeEvent TPAEvent : TPAEvents) {
			Collection<Link> TPALinks = TPAEvent.getLinks();
			for (Link TPALink :TPALinks) {
				String TPALinkId=TPALink.getId().toString();
				String TPALinkIdOpposite = getOppositeLinkID(TPALinkId);
				nTrainsEnterLinkAtAnyGivenTime.put(TPALinkId, 0);
				nTrainsWaitLinkAtAnyGivenTime.put(TPALinkId, 0);
				nTrainsEnterLinkAtAnyGivenTime.put(TPALinkIdOpposite, 0);
				nTrainsWaitLinkAtAnyGivenTime.put(TPALinkIdOpposite, 0);
				String cleanLinkID=getCleanLinkID(TPALinkId);
				Integer singleTrackDummy = (Integer) allLinks.get(TPALink.getId()).getAttributes().getAttribute("SingleTrack");
				if (singleTrackDummy==1) {
					allLinkStatus.put(cleanLinkID, linkStatus.Single);
				} else {
					allLinkStatus.put(cleanLinkID, linkStatus.Double);
				}
			}
		}

		//we need to clear all the existing networkChangeEvents before the next iteration starts.
		List<NetworkChangeEvent> emptyEvents= new ArrayList<NetworkChangeEvent>();
		network.setNetworkChangeEvents(emptyEvents);
	}



	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		// TODO Auto-generated method stub
		double now = e.getSimulationTime();
		QSim qSim=(QSim) e.getQueueSimulation();

		// first check the TPA events
		double initialTimeTPAEvents=Double.NEGATIVE_INFINITY;
		
		while (initialTimeTPAEvents<=now && (TPAEventsPosition+1)<=TPAEvents.size()) {
			initialTimeTPAEvents=TPAEvents.get(TPAEventsPosition).getStartTime();
			NetworkChangeEvent TPAEvent = TPAEvents.get(TPAEventsPosition);
			if (initialTimeTPAEvents<=now){
				NetworkChangeEvent implementedEvent = generatePossibleTPANetworkChangeEventAndUpdateStatusVariables(TPAEvent);
				if (implementedEvent!=null) {
					qSim.addNetworkChangeEvent(implementedEvent);
				}
			}else {
				break;
			}	
			TPAEventsPosition++;

		}

		// then goes through single track events
		double initialTime=Double.NEGATIVE_INFINITY;
		while (initialTime<=now & networkChangeEventQueue.size()>0) {
			NetworkChangeEvent oneNetworkChangeEvent = networkChangeEventQueue.peek();
			if (oneNetworkChangeEvent!=null) {
				initialTime=oneNetworkChangeEvent.getStartTime();
				if (initialTime<=now){
					oneNetworkChangeEvent = networkChangeEventQueue.poll();
					qSim.addNetworkChangeEvent(oneNetworkChangeEvent);
				} else {
					break;
				}
			} else {
				networkChangeEventQueue.poll();
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		// check event and add the corresponding  network change events into networkChangeEvent.
		switch (event.getEventType()){
		case VehicleEntersTrafficEvent.EVENT_TYPE:
			String linkId = ((VehicleEntersTrafficEvent) event).getLinkId().toString();
			createNetworkChangeEventEnterLink( event, linkId);
			break;
		case LinkEnterEvent.EVENT_TYPE:
			linkId = ((LinkEnterEvent) event).getLinkId().toString();
			createNetworkChangeEventEnterLink( event, linkId);
			break;
		case LinkLeaveEvent.EVENT_TYPE:
			linkId = ((LinkLeaveEvent) event).getLinkId().toString();
			createNetworkChangeEventLeaveLink( event, linkId);
			break;
		case VehicleLeavesTrafficEvent.EVENT_TYPE:	
			linkId = ((VehicleLeavesTrafficEvent) event).getLinkId().toString();
			createNetworkChangeEventLeaveLink( event, linkId);
			break;

		}
	}

	private void createNetworkChangeEventEnterLink(Event event, String linkId) {
		boolean isSingleTrack=false;
		String cleanLinkId=getCleanLinkID(linkId);
		int numberOfTrainsOppositeLink=-1;
		int numberOfTrainsCurrentLink=-1;
		int numberOfTrainsWaitOppositeLink=-1;
		int numberOfTrainsWaitCurrentLink =-1;

        // if it is q_link but not necessarily single track we just count the number of waiting and drive-in vehicles
		if (nTrainsEnterLinkAtAnyGivenTime.containsKey(linkId)) {
			linkStatus thisLinkStatus = allLinkStatus.get(cleanLinkId);
			String linkId_opposite=getOppositeLinkID(linkId);
			numberOfTrainsOppositeLink = nTrainsEnterLinkAtAnyGivenTime.get(linkId_opposite);
			numberOfTrainsCurrentLink = nTrainsEnterLinkAtAnyGivenTime.get(linkId);
			numberOfTrainsWaitOppositeLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId_opposite);
			numberOfTrainsWaitCurrentLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId);
			if (thisLinkStatus.equals(linkStatus.Single)) {
				isSingleTrack=true;
			}
		}
        
		if (isSingleTrack) {
			if (numberOfTrainsOppositeLink==0 && numberOfTrainsCurrentLink==0 && numberOfTrainsWaitOppositeLink==0 && numberOfTrainsWaitCurrentLink==0) {
				String linkId_opposite=getOppositeLinkID(linkId);
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);

			} else if (numberOfTrainsOppositeLink==0 && numberOfTrainsWaitOppositeLink>0 && numberOfTrainsWaitCurrentLink==0) {
				Id<Link> linkIDCurrentDirection = Id.createLinkId(linkId);
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( linkIDCurrentDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
				networkChangeEvent.addLink(linkCurrentDirection);
				networkChangeEventQueue.add(networkChangeEvent);
			} 

		}	

		if (nTrainsEnterLinkAtAnyGivenTime.containsKey(linkId))  {
			nTrainsWaitLinkAtAnyGivenTime.put(linkId,numberOfTrainsWaitCurrentLink+1);
		}

		// if the vehicle enters the actual single track link, then the queued link id should be "q_" +linkId, and if the queue link belongs to the hashmap
		String queue_linkId="q_"+linkId;
		if (nTrainsEnterLinkAtAnyGivenTime.containsKey(queue_linkId)) {
			int numberOfTrainsWaitCurrentQLink = nTrainsWaitLinkAtAnyGivenTime.get(queue_linkId);
			int numberOfTrainsEnterCurrentQLink = nTrainsEnterLinkAtAnyGivenTime.get(queue_linkId);
			nTrainsWaitLinkAtAnyGivenTime.put(queue_linkId,numberOfTrainsWaitCurrentQLink-1);
			nTrainsEnterLinkAtAnyGivenTime.put(queue_linkId,numberOfTrainsEnterCurrentQLink+1);
		}

	} // end createNetworkChangeEventEnterLink

	private void createNetworkChangeEventLeaveLink(Event event, String linkId) {
		boolean isSingleTrack=false;
		linkId="q_"+linkId;
		String cleanLinkId=getCleanLinkID(linkId);
		int numberOfTrainsOppositeLink=-1;
		int numberOfTrainsCurrentLink=-1;
		int numberOfTrainsWaitOppositeLink=-1;
		int numberOfTrainsWaitCurrentLink =-1;

		// if it is q_link but not necessarily single track we just count the number of waiting and drive-in vehicles
		if (nTrainsEnterLinkAtAnyGivenTime.containsKey(linkId)) {
			linkStatus thisLinkStatus = allLinkStatus.get(cleanLinkId);
			String linkId_opposite=getOppositeLinkID(linkId);
			numberOfTrainsOppositeLink = nTrainsEnterLinkAtAnyGivenTime.get(linkId_opposite);
			numberOfTrainsCurrentLink = nTrainsEnterLinkAtAnyGivenTime.get(linkId);
			numberOfTrainsWaitOppositeLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId_opposite);
			numberOfTrainsWaitCurrentLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId);
			nTrainsEnterLinkAtAnyGivenTime.put(linkId,numberOfTrainsCurrentLink-1);
			numberOfTrainsCurrentLink = numberOfTrainsCurrentLink-1;
			if (thisLinkStatus.equals(linkStatus.Single)) {
				isSingleTrack=true;
			}
		}



		if (isSingleTrack) {
			String linkId_opposite=getOppositeLinkID(linkId);
			if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitOppositeLink>0) {
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				double capacity=(double) linkOppositeDirection.getAttributes().getAttribute("Capacity");
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);


				Id<Link> linkIDCurrentDirection = Id.createLinkId(linkId);
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( linkIDCurrentDirection ) ;
				NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, 0 ));
				networkChangeEvent2.addLink(linkCurrentDirection);
				networkChangeEventQueue.add(networkChangeEvent2);

			} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitOppositeLink==0 && numberOfTrainsWaitCurrentLink==0) {
				Id<Link> linkIDCurrentDirection = Id.createLinkId(linkId);
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( linkIDCurrentDirection ) ;
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				double capacity=(double) linkOppositeDirection.getAttributes().getAttribute("Capacity");
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity ));
				networkChangeEvent.addLink(linkCurrentDirection);
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);

			} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitOppositeLink==0 && numberOfTrainsWaitCurrentLink>0) {
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, 0 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);
				
				Id<Link> linkIDCurrentDirection = Id.createLinkId(linkId);
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( linkIDCurrentDirection ) ;
				double capacity=(double) linkCurrentDirection.getAttributes().getAttribute("Capacity");
				NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacity ));
				networkChangeEvent2.addLink(linkCurrentDirection);
				networkChangeEventQueue.add(networkChangeEvent2);
			}
		}	
	}

	private NetworkChangeEvent generatePossibleTPANetworkChangeEventAndUpdateStatusVariables(NetworkChangeEvent TPAEvent) {
		double capacityChangedTo=TPAEvent.getFlowCapacityChange().getValue();
		Collection<Link> linksCollection = TPAEvent.getLinks();
		ArrayList<Link> links =new ArrayList<Link>();
		for (Link link:linksCollection) {
			links.add(link);
		}

		if (links.size()>2) {
			throw new RuntimeException( "Please make sure that it is always maximum 2 links that are modified in each NetworkChangeEvent.") ;
		} else if (links.size()==2) {
			String linkId = links.get(0).getId().toString();
			String oppositeLinkId = links.get(1).getId().toString();
			if (getOppositeLinkID(linkId).equals(oppositeLinkId)) {
				if (nTrainsEnterLinkAtAnyGivenTime.containsKey(linkId)) {
					String cleanLinkId=getCleanLinkID(linkId);
					linkStatus thisLinkStatus = allLinkStatus.get(cleanLinkId);
					Link linkCurrentDirection = scenario.getNetwork().getLinks().get( Id.createLinkId(linkId) ) ;
					Link linkOppositeDirection = scenario.getNetwork().getLinks().get( Id.createLinkId(oppositeLinkId) ) ;
					int singleTrackDummy = (Integer) linkCurrentDirection.getAttributes().getAttribute("SingleTrack");
					if (thisLinkStatus.equals(linkStatus.Double) && capacityChangedTo==0) {
						// Double_to_Close
						allLinkStatus.put(cleanLinkId,linkStatus.Closed);
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (thisLinkStatus.equals(linkStatus.Single) && capacityChangedTo==0) {
						// Single_to_Close
						allLinkStatus.put(cleanLinkId,linkStatus.Closed);
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS, capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (thisLinkStatus.equals(linkStatus.Closed) && capacityChangedTo==0)  {
						// nothing happens
					} else if (thisLinkStatus.equals(linkStatus.Double) && capacityChangedTo>0) {
						// nothing happens
					} else if (thisLinkStatus.equals(linkStatus.Single) && capacityChangedTo>0 && singleTrackDummy==0) {
						// Single_to_Double
						allLinkStatus.put(cleanLinkId,linkStatus.Double);
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (thisLinkStatus.equals(linkStatus.Closed) && capacityChangedTo>0 && singleTrackDummy==0) {
						// Close_to_Double;
						allLinkStatus.put(cleanLinkId,linkStatus.Double);
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (thisLinkStatus.equals(linkStatus.Closed) && capacityChangedTo>0 && singleTrackDummy==1) {
						// Close_to_Single;
						allLinkStatus.put(cleanLinkId,linkStatus.Single);
						int numberOfTrainsWaitCurrentLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId);
						int numberOfTrainsWaitOppositeLink = nTrainsWaitLinkAtAnyGivenTime.get(oppositeLinkId);
						if (numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink==0) {
							NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
							networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
							networkChangeEvent.addLink(linkCurrentDirection);
							return networkChangeEvent;
						} else if (numberOfTrainsWaitCurrentLink==0 && numberOfTrainsWaitOppositeLink>0) {
							NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
							networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
							networkChangeEvent.addLink(linkOppositeDirection);
							return networkChangeEvent;
						} else if (numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink>0) {
							NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
							networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
							networkChangeEvent.addLink(linkCurrentDirection);
							return networkChangeEvent;
						}
					}
				} else {
					throw new RuntimeException( linkId + " or "+ oppositeLinkId + " is not a q_ link.") ;
				}
			} else {
				throw new RuntimeException("link Id: "+ linkId+" and link Id: " + oppositeLinkId + " does not belong to the two directions of the same link.") ;
			}	
		} else if (links.size()==1) {
			String linkId = links.get(0).getId().toString();
			String oppositeLinkId=getOppositeLinkID(linkId);
			if (nTrainsEnterLinkAtAnyGivenTime.containsKey(linkId)) {
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( Id.createLinkId(linkId) ) ;
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( Id.createLinkId(oppositeLinkId) ) ;
				int singleTrackDummy = (Integer) linkCurrentDirection.getAttributes().getAttribute("SingleTrack");
				String cleanLinkId=getCleanLinkID(linkId);
				linkStatus thisLinkStatus = allLinkStatus.get(cleanLinkId);
				if (thisLinkStatus.equals(linkStatus.Double) && capacityChangedTo==0) {
					// Double_to_Single;
					allLinkStatus.put(cleanLinkId,linkStatus.Single);
					int numberOfTrainsCurrentLink = nTrainsEnterLinkAtAnyGivenTime.get(linkId);
					int numberOfTrainsOppositeLink = nTrainsEnterLinkAtAnyGivenTime.get(oppositeLinkId);
					int numberOfTrainsWaitCurrentLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId);
					int numberOfTrainsWaitOppositeLink = nTrainsWaitLinkAtAnyGivenTime.get(oppositeLinkId);
					if (numberOfTrainsCurrentLink>0 && numberOfTrainsOppositeLink==0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsCurrentLink>0 && numberOfTrainsOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink==0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitCurrentLink==0 && numberOfTrainsWaitOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsCurrentLink==0 && numberOfTrainsOppositeLink==0 && numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						return networkChangeEvent;
					}
				} else if (thisLinkStatus.equals(linkStatus.Single) && capacityChangedTo==0) {
					// Single_to_Close;
					allLinkStatus.put(cleanLinkId,linkStatus.Closed);
					NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
					networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
					networkChangeEvent.addLink(linkCurrentDirection);
					networkChangeEvent.addLink(linkOppositeDirection);
					return networkChangeEvent;
				} else if (thisLinkStatus.equals(linkStatus.Closed) && capacityChangedTo==0)  {
					// nothing happens
				} else if (thisLinkStatus.equals(linkStatus.Double) && capacityChangedTo>0) {
					// nothing happens
				} else if (thisLinkStatus.equals(linkStatus.Single) && capacityChangedTo>0 && singleTrackDummy==0) {
					// TPAType.Single_to_Double;
					allLinkStatus.put(cleanLinkId,linkStatus.Double);
					NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
					networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
					networkChangeEvent.addLink(linkCurrentDirection);
					networkChangeEvent.addLink(linkOppositeDirection);
					return networkChangeEvent;
				} else if (thisLinkStatus.equals(linkStatus.Closed) && capacityChangedTo>0) {
					// Close_to_Single;
					allLinkStatus.put(cleanLinkId,linkStatus.Single);
					int numberOfTrainsWaitCurrentLink = nTrainsWaitLinkAtAnyGivenTime.get(linkId);
					int numberOfTrainsWaitOppositeLink = nTrainsWaitLinkAtAnyGivenTime.get(oppositeLinkId);
					if (numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink==0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsWaitCurrentLink==0 && numberOfTrainsWaitOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkOppositeDirection);
						return networkChangeEvent;
					} else if (numberOfTrainsWaitCurrentLink>0 && numberOfTrainsWaitOppositeLink>0) {
						NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(TPAEvent.getStartTime()) ;
						networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  capacityChangedTo ));
						networkChangeEvent.addLink(linkCurrentDirection);
						return networkChangeEvent;
					}
				}
			} else {
				throw new RuntimeException( linkId +  " specified as a TPÅ related link is not a q_ link, must be specified on links with ID starting with q_.") ;
			}
		}

		return null;
	}

	private static String getOppositeLinkID(String linkId) {
		String linkId_opposite=null;
		if (linkId.contains("_AB")) {
			linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.contains("_BA")) {
			linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		return linkId_opposite;
	}


	private static String getCleanLinkID(String linkId) {
		String cleanLinkId=linkId.replaceAll("_AB", "");
		cleanLinkId=cleanLinkId.replaceAll("_BA", "");
		cleanLinkId=cleanLinkId.replaceAll("q_", "");
		return cleanLinkId;
	}

}

