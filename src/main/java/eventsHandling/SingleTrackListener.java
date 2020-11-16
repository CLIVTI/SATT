package eventsHandling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class SingleTrackListener implements MobsimInitializedListener, MobsimBeforeSimStepListener,BasicEventHandler {
	

	private PriorityQueue<NetworkChangeEvent> networkChangeEventQueue = new PriorityQueue<NetworkChangeEvent>(1, new Comparator<NetworkChangeEvent>() {

		@Override
		public int compare(NetworkChangeEvent o1, NetworkChangeEvent o2) {
			Double o1Double = o1.getStartTime();
			Double o2Double = o2.getStartTime();
			return o1Double.compareTo(o2Double);
		}

	});
	private HashMap<String,Integer> nTrainsOnLinkAtAnyGivenTime = new  HashMap<String,Integer>();
	private HashMap<String,Integer> nTrainsWaitAtLinkAtAnyGivenTime = new  HashMap<String,Integer>();



	@Inject Scenario scenario;

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		// TODO Auto-generated method stub
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
       for (Entry<Id<Link>, ? extends Link> eachLink :allLinks.entrySet()) {
    	   Integer singleTrackDummy = (Integer) eachLink.getValue().getAttributes().getAttribute("SingleTrack");
    	   if (singleTrackDummy==1) {
    		   nTrainsOnLinkAtAnyGivenTime.put(eachLink.getKey().toString(), 0);
    		   nTrainsWaitAtLinkAtAnyGivenTime.put(eachLink.getKey().toString(), 0);
    	   }
       }
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		// TODO Auto-generated method stub
		double now = e.getSimulationTime();
		QSim qSim=(QSim) e.getQueueSimulation();
		ArrayList<NetworkChangeEvent> networkChangeEventToProcess = new ArrayList<NetworkChangeEvent>();
		
	    double initialTime=-10;
	    while (initialTime<=now & networkChangeEventQueue.size()>0) {
	    	NetworkChangeEvent oneNetworkChangeEvent = networkChangeEventQueue.poll();
	    	if (oneNetworkChangeEvent!=null) {
	    		initialTime=oneNetworkChangeEvent.getStartTime();
	    		if (initialTime<=now){
	    			networkChangeEventToProcess.add(oneNetworkChangeEvent);
	    		}
			}
	    }
	    
	    for (int i=0; i<networkChangeEventToProcess.size();i++) {
	    	qSim.addNetworkChangeEvent(networkChangeEventToProcess.get(i));
	    	
	    }

		
		
		//qSim.addNetworkChangeEvent(null);
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
		String linkId_opposite=null;
		if (linkId.equals("q_l_2_AB")) {
			linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.equals("q_l_2_BA")) {
			linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		
		
		if (linkId_opposite!=null) {
			Integer numberOfTrainsOnOppositeLink = nTrainsOnLinkAtAnyGivenTime.get(linkId_opposite);
			Integer numberOfTrainsWaitAtOppositeLink = nTrainsWaitAtLinkAtAnyGivenTime.get(linkId_opposite);
			Integer numberOfTrainsWaitCurrentLink = nTrainsWaitAtLinkAtAnyGivenTime.get(linkId);
			nTrainsWaitAtLinkAtAnyGivenTime.put(linkId,numberOfTrainsWaitCurrentLink+1);
			if (numberOfTrainsOnOppositeLink==0 && numberOfTrainsWaitAtOppositeLink==0) {
				
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);
				Integer numberOfTrainsOnCurrentLink = nTrainsOnLinkAtAnyGivenTime.get(linkId);
				nTrainsOnLinkAtAnyGivenTime.put(linkId,numberOfTrainsOnCurrentLink+1);
				
				numberOfTrainsWaitCurrentLink = nTrainsWaitAtLinkAtAnyGivenTime.get(linkId);
				nTrainsWaitAtLinkAtAnyGivenTime.put(linkId,numberOfTrainsWaitCurrentLink-1);

			} else if (numberOfTrainsOnOppositeLink>0 && numberOfTrainsWaitAtOppositeLink==0) {
				// nothing happen
			} else if (numberOfTrainsOnOppositeLink==0 && numberOfTrainsWaitAtOppositeLink>0) {
				
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);
				Integer numberOfTrainsOnCurrentLink = nTrainsOnLinkAtAnyGivenTime.get(linkId);
				nTrainsOnLinkAtAnyGivenTime.put(linkId,numberOfTrainsOnCurrentLink+1);
				numberOfTrainsWaitCurrentLink = nTrainsWaitAtLinkAtAnyGivenTime.get(linkId);
				nTrainsWaitAtLinkAtAnyGivenTime.put(linkId,numberOfTrainsWaitCurrentLink-1);
			} else if (numberOfTrainsOnOppositeLink>0 && numberOfTrainsWaitAtOppositeLink>0) {
				// nothing happen
			}
			
			
		}

	}
	
	
	private void createNetworkChangeEventLeaveLink(Event event, String linkId) {
		String linkId_opposite=null;
		if (linkId.equals("l_2_AB")) {
			linkId_opposite = linkId.replaceAll("_AB", "_BA");
			linkId_opposite="q_"+linkId_opposite;
			linkId="q_"+linkId;
		} else if (linkId.equals("l_2_BA")) {
			linkId_opposite = linkId.replaceAll("_BA", "_AB");
			linkId_opposite="q_"+linkId_opposite;
			linkId="q_"+linkId;
		}
		
		
		if (linkId_opposite!=null) {
			Integer numberOfTrainsOnOppositeLink = nTrainsOnLinkAtAnyGivenTime.get(linkId_opposite);
			Integer numberOfTrainsWaitAtOppositeLink = nTrainsWaitAtLinkAtAnyGivenTime.get(linkId_opposite);
			
			Integer numberOfTrainsOnCurrentLink = nTrainsOnLinkAtAnyGivenTime.get(linkId);
			nTrainsOnLinkAtAnyGivenTime.put(linkId,numberOfTrainsOnCurrentLink-1);
			if (numberOfTrainsOnOppositeLink==0 && numberOfTrainsWaitAtOppositeLink==0) {
				
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  20 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);
				

			} else if (numberOfTrainsOnOppositeLink>0 && numberOfTrainsWaitAtOppositeLink==0) {
				// wont happen
			} else if (numberOfTrainsOnOppositeLink==0 && numberOfTrainsWaitAtOppositeLink>0) {
				
				Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
				Link linkOppositeDirection = scenario.getNetwork().getLinks().get( linkIDOppositeDirection ) ;
				NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  20 ));
				networkChangeEvent.addLink(linkOppositeDirection);
				networkChangeEventQueue.add(networkChangeEvent);
				
				Id<Link> linkIDCurrentDirection = Id.createLinkId(linkId);
				Link linkCurrentDirection = scenario.getNetwork().getLinks().get( linkIDCurrentDirection ) ;
				NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(event.getTime()) ;
				networkChangeEvent2.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
				networkChangeEvent2.addLink(linkCurrentDirection);
				networkChangeEventQueue.add(networkChangeEvent2);
				

				nTrainsWaitAtLinkAtAnyGivenTime.put(linkId_opposite,numberOfTrainsWaitAtOppositeLink-1);
				nTrainsOnLinkAtAnyGivenTime.put(linkId_opposite,numberOfTrainsOnOppositeLink+1);
				
			} else if (numberOfTrainsOnOppositeLink>0 && numberOfTrainsWaitAtOppositeLink>0) {
				// wont happen
			}	
		}	
	}	
}

