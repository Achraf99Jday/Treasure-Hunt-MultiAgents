package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Random;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class ReceiveMapBehaviour extends OneShotBehaviour{

	private static final long serialVersionUID = 95283913118770598L;
	
	
	private MapRepresentation myMap;
	
	private boolean haveMsg=true;

	private int exitValue;
	/**
	 * 
	 * This behaviour is a one Shot.
	 * It receives a message tagged with an inform performative, print the content in the console and destroy itlself
	 * @param myagent
	 */
	public ReceiveMapBehaviour(final Agent myagent) {
		super(myagent);
		//this.start = (int) System.currentTimeMillis();
	}


	@SuppressWarnings("unchecked")
	public void action() {
		//1) receive the message
		
		this.exitValue=0;
		
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleShareMap"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msg=this.myAgent.receive(msgTemplate);
		
		if (msg != null) {
			this.myMap = ((fsmAgent)this.myAgent).getMap();
			System.out.println(this.myAgent.getLocalName()+" <--- Map received from "+msg.getSender().getLocalName());

			SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msg.getContentObject();
			} catch (UnreadableException e) {

				e.printStackTrace();
			}
			this.myMap.mergeMap(sgreceived);
			
			((fsmAgent)this.myAgent).updateMap(this.myMap);

			String myAgent = this.myAgent.getLocalName();
			String otherAgent = msg.getSender().getLocalName();
			
			if (myAgent.compareTo(otherAgent) > 0){
				((fsmAgent)this.myAgent).succesMerge = true;
			}
			//If the current agent is on successBlockBehaviour, back to this Behaviour
			if( ((fsmAgent)this.myAgent).successBlock) {
				this.exitValue = 2;
			}else {
				this.exitValue=1;
			}
			System.out.println(this.myAgent.getLocalName()+" <--- End Map merge");
			
		}else{
			//If is the second time this Behaviour is actived
			if ( !haveMsg ) {
				haveMsg=true;
				if( ((fsmAgent)this.myAgent).successBlock) {
					this.exitValue = 2;
				}else {
					this.exitValue=1;
				}
				((fsmAgent)this.myAgent).succesMerge = false;
				System.out.println(this.myAgent.getLocalName()+" ---> Map not received");
				return ;
			}
			this.haveMsg=false;
			block(5000);// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}

	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}

}