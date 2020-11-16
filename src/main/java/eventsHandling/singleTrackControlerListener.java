package eventsHandling;
import javax.inject.Inject;


import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

public class singleTrackControlerListener implements StartupListener, IterationEndsListener, ShutdownListener{
	
	@Inject
	private SingleTrackLinkEventHandler eventHandler; 
	
	@Inject
	private EventsManager events ;
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		this.events.addHandler(this.eventHandler);
		
	}

}
