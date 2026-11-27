package github.paroj.dsub2000.domain;

import java.text.ParseException;

import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.control.ActionResponseMessage;
import org.jupnp.model.meta.Action;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;

import junit.framework.TestCase;

public class JUPNPTest extends TestCase {
	public void testParseWithPooledParsers() throws ParseException {
		// for context see: https://github.com/jupnp/jupnp/issues/232
		SOAPActionProcessorImpl p = new SOAPActionProcessorImpl();
		ActionResponseMessage arg1 = new ActionResponseMessage() {
			final String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:GetPositionInfoResponse xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><Track>1</Track><TrackDuration>0</TrackDuration><TrackMetaData>doesn't really matter</TrackMetaData><TrackURI>doesn't really matter</TrackURI><RelTime>0:00:00</RelTime><AbsTime>0:00:00</AbsTime><RelCount>2147483647</RelCount><AbsCount>2147483647</AbsCount></u:GetPositionInfoResponse></s:Body></s:Envelope>";

			@Override
			public String getActionNamespace() {
				System.out.println("GetActionNamespace");
				return "GetActionNamespace";
			}

			@Override
			public boolean isBodyNonEmptyString() {
				System.out.println("IsBodyNonEmptyString");
				return true;
			}

			@Override
			public String getBodyString() {
				System.out.println("getBodyString");
				return body;
			}

			@Override
			public void setBody(String string) {
				System.out.println("setBody");
			}
		};
		ActionInvocation arg2 = new ActionInvocation(new Action("GetPositionInfo", null));
		for (int i = 0; i < 20; i++) { // 20 is default pool size
			p.readBody(arg1, arg2);
		}
		System.out.println("--- Crash after this line ---");
		p.readBody(arg1, arg2);

	}
}