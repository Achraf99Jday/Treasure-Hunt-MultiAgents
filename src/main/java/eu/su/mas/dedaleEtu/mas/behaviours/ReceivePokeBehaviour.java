package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class ReceivePokeBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 95283913118770598L;
	
	private boolean finished=false;
	
	private MapRepresentation myMap;

	private List<String> list_agentNames;

	/**
	 * 
	 * This behaviour is a one Shot.
	 * It receives a message tagged with an inform performative, print the content in the console and destroy itlself
	 * @param myagent
	 */
	public ReceivePokeBehaviour(final Agent myagent, List<String> agentNames) {
		super(myagent);
		this.list_agentNames=agentNames;
	}

	@Override
	public void action() {
		//1) receive the message

		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
																	MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		if (msg != null) {
			((ExploreCoopAgent)this.myAgent).move=false;
			((ExploreCoopAgent)this.myAgent).succesMerge = false;
			msg.getContent();
			System.out.println(this.myAgent.getLocalName()+"<----Result received Poke from "+msg.getSender().getLocalName());
			this.myMap = ((ExploreCoopAgent)this.myAgent).getMap();
			this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent,this.myMap, this.list_agentNames));
			this.myAgent.addBehaviour(new ReceiveMapBehaviour(this.myAgent));
			this.myAgent.addBehaviour(new MaxWaitingTimeBehaviour(this.myAgent, 2000));
		}else{
			block();// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}
	}
	@Override
	public boolean done() {
		return finished;
	}


}