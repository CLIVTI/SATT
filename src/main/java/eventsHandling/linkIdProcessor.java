package eventsHandling;

public class linkIdProcessor {
	
	public static String getOppositeLinkID(String linkId) {
		String linkId_opposite=null;
		if (linkId.contains("_AB")) {
			linkId_opposite = linkId.replaceAll("_AB", "_BA");
		} else if (linkId.contains("_BA")) {
			linkId_opposite = linkId.replaceAll("_BA", "_AB");
		}
		return linkId_opposite;
	}


	public static String getCleanLinkID(String linkId) {
		String cleanLinkId=linkId.replaceAll("_AB", "");
		cleanLinkId=cleanLinkId.replaceAll("_BA", "");
		cleanLinkId=cleanLinkId.replaceAll("q_", "");
		return cleanLinkId;
	}

}
