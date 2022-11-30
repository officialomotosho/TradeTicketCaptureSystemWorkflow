package calypsox.tk.engine;

import java.beans.XMLEncoder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.apps.startup.AppStarter;
import com.calypso.engine.Engine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeStatus;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventAccounting;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.CSVToXML;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.TaskArray;

public class TTCSTradeAmend extends Engine {

	private static final String ENGINE_NAME = "WorkflowEngine";
	private static DSConnection ds = null;
	private String _configName = null;

	public static final Action BO_RETURN = Action.valueOf("BO_RETURN");

	public static final Status HYPO_TRADE = Status.valueOf("HYPO_TRADE");
	public static final Status Verified = Status.valueOf("Verified");
	public static final Status Terminated = Status.valueOf("Terminated");



	public static final Action FO_AMEND = Action.valueOf("FO_AMEND");
	
	private String configName;
	String TTCSTradeAmend;
	String TTCSTradeTerminate;
	String TTCSTradeVerified;

	// private ScheduledExecutorService executor;
	// private ScheduledFuture<?> future;
	
	public TTCSTradeAmend (DSConnection dsCon, String hostName, int port) { 
		 super (dsCon, hostName, port);
		 }

	public boolean process(PSEvent event) {
		boolean bRet = false;

		// Custom logic
		if (event instanceof PSEventTrade) {
			String eventType = event.getEventType();
			System.err.println("eventType : " + eventType);
			
			PSEventTrade tradeEvent = (PSEventTrade) event;
			bRet = handleEvent(tradeEvent, getDS());
			
			try {
				if (bRet) {
					getDS().getRemoteTrade().eventProcessed(event.getLongId(), getEngineName()); 
					Log.system(getEngineName(), "Processed Event: " + event.getLongId());
				} else {
					Log.error(getEngineName(), "Event " + event.getLongId() + " not succesfully consumed.");
				}
			} catch (Exception e) {
				Log.error(getEngineName(), "Unable to retrieve event.", e);
			}

//			PSEventTrade tradeEvent = (PSEventTrade) event;
			bRet = true;
			System.err.println("eventType : TTCSTradeAmend" );

		}

		return bRet;
	}
	
	public boolean handleEvent(PSEventTrade tradeEvent, DSConnection dsCon) {
		boolean bRet = true;

		Trade trade = tradeEvent.getTrade();
//		if (trade == null)
//			return bRet;

		Log.system(getEngineName(), "Trade Id Received in WorkflowEngine : " + trade.getLongId() + ", Trade Status: "
				+ trade.getStatus()   + ", Trade ExternalRef: "	+ trade.getExternalReference() + ", Trade Action : " + trade.getAction()); // TODO: To be removed

		// Skip any non-Bond trade - Both FinNote/Thesis and BACP are Bond trades
		if (trade.getStatus().equals(HYPO_TRADE) && trade.getExternalReference().startsWith("TTCS")) {
			
			writeXml(tradeEvent);
			
			TTCSTradeAmend = trade.getLongId()    +" - " +   trade.getExternalReference()     +" - " +  trade.getStatus();
			
			Log.system("Picking Trade For Amendment <<< 0", "AmendTrade:\n" + TTCSTradeAmend);
			
		
			return true;

		}

		return bRet;
	}
	
	public String terminatedTrade(PSEventTrade tradeEvent, DSConnection dsCon, BOPosting boposting) {
		
		Trade trade = tradeEvent.getTrade();
		
		
		Log.system(getEngineName(), "Trade Id Received in WorkflowEngine : " + trade.getLongId() + ", Trade Status: "
				+ trade.getStatus() + ", Trade ExternalRef: "	+    trade.getExternalReference() + ", Trade Action : " + trade.getAction()); // TODO: To be removed
		
		
		if (trade.getStatus().equals(Terminated) && trade.getExternalReference().startsWith("TTCS")) {
			
			writeXml(tradeEvent);
			
			TTCSTradeTerminate = trade.getLongId()    +    trade.getExternalReference()     +     trade.getComment()     +     trade.getStatus()        +       trade.getTerminationDate()       +     +   boposting.getTradeId();
			
			Log.system("Setting Trades For Termination <<< 123", "TerminatedTrades:\n" + TTCSTradeTerminate);

		
		return TTCSTradeTerminate;
		
		}
		return TTCSTradeTerminate;
		
	}
	
	public String verifiedTrade(PSEventTrade tradeEvent, DSConnection dsCon, BOPosting boposting) {
		
		Trade trade = tradeEvent.getTrade();
		
		Log.system(getEngineName(), "Trade Id Received in WorkflowEngine : " + trade.getLongId() + ", Trade Status: "
				+ trade.getStatus() + ", Trade ExternalRef: "	+ trade.getExternalReference() + ", Trade ID: "	+ boposting.getTradeId() + ", Trade Action : " + trade.getAction()); // TODO: To be removed
		
		if (trade.getStatus().equals(Verified) && trade.getExternalReference().startsWith("TTCS")) {
			
			writeXml(tradeEvent);
			
			TTCSTradeVerified = trade.getLongId()    +    trade.getExternalReference()         +     trade.getStatus()    +   boposting.getTradeId();
			
			Log.system("Sending Verified Trades <<< 12345", "VerifiedTrades:\n" + TTCSTradeVerified);


		}

		return TTCSTradeVerified;
		
	}
	
	public void writeXml (PSEventTrade tradeEvent) {
		
		Trade trade = tradeEvent.getTrade();
		try {
		FileOutputStream fos = new FileOutputStream(new File("C:/Users/lenono/Downloads/new jar/iso_translator/amend.xml"));
        XMLEncoder encoder  = new XMLEncoder(fos);
			
			//You first have to arrange what you're putting in the XML file 
			String xmlData = "<ExternalReference>"+trade.getExternalReference()+"</ExternalReference> \n"+
				"<TradeStatus>"+trade.getStatus()+"</TradeStatus>"+
				"<TradeID>"+trade.getLongId()+"</TradeID>"+
				"<Comment>"+ trade.getComment()+"</Comment>";
        encoder.writeObject(xmlData);
			
        Log.system(getEngineName(),  "File Has Been Written");
        encoder.close();
        fos.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
		
	}
	
	
	@Override
	 protected void init(EngineContext engineContext) {
	 super.init(engineContext);
	 configName = engineContext.getInitParameter("config", null);
	 // use configName to perform any engine initialization
	 }
}
