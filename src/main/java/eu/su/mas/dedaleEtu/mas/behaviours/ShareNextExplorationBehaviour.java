package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ShareNextExplorationBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4867664323614938980L;

	private boolean finished=false;
	private String nextNode;

	public ShareNextExplorationBehaviour(final Agent myagent) {
		super(myagent);
		
	}

	@Override
	public void action() {
		//1) receive the message

		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePos"), 
																	MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		if (msg != null) {
			this.nextNode = ((ExploreCoopAgent)this.myAgent).nextNode;
			
			String message[] = msg.getContent().split(",");
			String otherAgentPos = message[0];
			String otherAgentNextNode = "";
			if (message.length >1) {
				otherAgentNextNode = message[1];
			}
			String myAgent = this.myAgent.getLocalName();
			String otherAgent = msg.getSender().getLocalName();
			
			System.out.println(myAgent+"<----Result received Pos from "+ otherAgent);
			
			if ( otherAgentNextNode != "" && this.nextNode.equals(otherAgentPos) && myAgent.compareTo(otherAgent) == -1){
				((ExploreCoopAgent)this.myAgent).changeNode = true;
				((ExploreCoopAgent)this.myAgent).nextNode = otherAgentNextNode;
				System.out.println(myAgent+" ----> Find a Golem at ");
			}
			
			//Signaler sois-meme pour recommencer a bouger
			ACLMessage send_msg=new ACLMessage(ACLMessage.INFORM);
			send_msg.setProtocol("re-move");
			send_msg.setSender(this.myAgent.getAID());
			send_msg.setContent("End Merge");

			this.myAgent.postMessage(send_msg);
		}else{
			block();// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}
		
	}
	
	@Override
	public boolean done() {
		return finished;
	}
}
