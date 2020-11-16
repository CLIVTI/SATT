package eventsHandling;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class SingleTrackLinkEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	@Inject
	private Network network;
	
	
	
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		
		// get opposite link
		String linkId = event.getLinkId().toString();
		String linkId_opposite=null;
		if (linkId.equals("l_2_AB")) {
			 linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.equals("l_2_BA")) {
			 linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		if (linkId_opposite!=null) {
			Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
			Link linkOppositeDirection = network.getLinks().get( linkIDOppositeDirection ) ;
			NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()+1) ;
			networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
			networkChangeEvent.addLink(linkOppositeDirection);
			NetworkUtils.addNetworkChangeEvent(network,networkChangeEvent);
		}
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// get opposite link
				String linkId = event.getLinkId().toString();
				String linkId_opposite=null;
				if (linkId.equals("l_2_AB")) {
					 linkId_opposite = linkId.replaceAll("_AB", "_BA");
				} else if (linkId.equals("l_2_BA")) {
					 linkId_opposite = linkId.replaceAll("_BA", "_AB");
				}
				if (linkId_opposite!=null) {
					Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
					Link linkOppositeDirection = network.getLinks().get( linkIDOppositeDirection ) ;
					NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()+1) ;
					networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  20 ));
					networkChangeEvent.addLink(linkOppositeDirection);
					NetworkUtils.addNetworkChangeEvent(network,networkChangeEvent);
				}
				

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// TODO Auto-generated method stub
		String linkId = event.getLinkId().toString();
		String linkId_opposite=null;
		if (linkId.equals("l_2_AB")) {
			 linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.equals("l_2_BA")) {
			 linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		if (linkId_opposite!=null) {
			Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
			Link linkOppositeDirection = network.getLinks().get( linkIDOppositeDirection ) ;
			NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()+1) ;
			networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  20 ));
			networkChangeEvent.addLink(linkOppositeDirection);
			NetworkUtils.addNetworkChangeEvent(network,networkChangeEvent);
		}
		
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		// TODO Auto-generated method stub
		// get opposite link
		String linkId = event.getLinkId().toString();
		String linkId_opposite=null;
		if (linkId.equals("l_2_AB")) {
			 linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.equals("l_2_BA")) {
			 linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		if (linkId_opposite!=null) {
			Id<Link> linkIDOppositeDirection = Id.createLinkId(linkId_opposite);
			Link linkOppositeDirection = network.getLinks().get( linkIDOppositeDirection ) ;
			NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(event.getTime()+1) ;
			networkChangeEvent.setFlowCapacityChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0 ));
			networkChangeEvent.addLink(linkOppositeDirection);
			NetworkUtils.addNetworkChangeEvent(network,networkChangeEvent);
		}
		
	}
	
	

}
