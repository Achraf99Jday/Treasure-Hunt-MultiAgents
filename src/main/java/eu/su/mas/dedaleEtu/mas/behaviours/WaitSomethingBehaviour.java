package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class WaitSomethingBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4468820870958544230L;

	private MapRepresentation myMap;
	
	private boolean haveMsg=true;

	private int exitValue;
	
	@Override
	public void action() {
		//1) receive the message
		
		this.exitValue=0;
		
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleWaitSomeone"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msg=this.myAgent.receive(msgTemplate);
		
		if (msg != null) {
			//Check if the Sender position is my next node
			if(((fsmAgent)this.myAgent).nextNode.equals(msg.getContent())) {
				this.myMap = ((fsmAgent)this.myAgent).getMap();
				System.out.println(this.myAgent.getLocalName()+" <--- The Someone is "+msg.getSender().getLocalName()+", back to chase (ProtocoleWaitSomeone)");
				((fsmAgent)this.myAgent).GolemPoop.add(((fsmAgent)this.myAgent).nextNode);
				((fsmAgent)this.myAgent).blockedAgent.add(((fsmAgent)this.myAgent).nextNode);
				haveMsg=true;
				this.exitValue=1;
				return ; 
			}
		}else{
			final MessageTemplate msgTemplateB = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
			final ACLMessage msgB = this.myAgent.receive(msgTemplateB);
			
			if (msgB != null) {
				//Check if the Sender position is my next node
				if(((fsmAgent)this.myAgent).nextNode.equals(msgB.getContent())) {
					this.myMap = ((fsmAgent)this.myAgent).getMap();
					System.out.println(this.myAgent.getLocalName()+" <--- The Someone is "+msgB.getSender().getLocalName()+", back to chase (ProtocoleByPass)");
					((fsmAgent)this.myAgent).GolemPoop.add(((fsmAgent)this.myAgent).nextNode);
					((fsmAgent)this.myAgent).blockedAgent.add(((fsmAgent)this.myAgent).nextNode);
					haveMsg=true;
					this.exitValue=1;
					return ;
				}
			}
			
        	final MessageTemplate msgTemplate2 = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
			final ACLMessage msgBlock = this.myAgent.receive(msgTemplate2);
			
			//Receive another around agents help msg
			if(msgBlock != null) {
				List<String> News = null;
				try {
					News = (List<String>) msgBlock.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				if (News != null) {
					String pos = News.get(0); //get Sender Position
					//Check if the Sender position is my next node
					if(((fsmAgent)this.myAgent).nextNode.equals(pos)) {
						this.myMap = ((fsmAgent)this.myAgent).getMap();
						System.out.println(this.myAgent.getLocalName()+" <--- The Someone is "+msgBlock.getSender().getLocalName()+", back to chase (ProtocoleHelpBlockWumpus)");
						((fsmAgent)this.myAgent).GolemPoop.add(((fsmAgent)this.myAgent).nextNode);
						((fsmAgent)this.myAgent).blockedAgent.add(((fsmAgent)this.myAgent).nextNode);
						haveMsg=true;
						this.exitValue=1;
						return ;
					}
				}
			}
			
			
			if ( !haveMsg ) {
				haveMsg=true;
				this.exitValue=2;
				((fsmAgent)this.myAgent).succesMerge = false;
				System.out.println(this.myAgent.getLocalName()+" ---> No respond, it's probably a Wumpus");
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
