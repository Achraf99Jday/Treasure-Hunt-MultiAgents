package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class StrangeWaitBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1147419770546269281L;

	private int exitValue;

	private String myPosition;

	private MapRepresentation myMap;

	public StrangeWaitBehaviour(Agent a) {
		super(a);
	}
	@Override
	public void action() {
		exitValue = 0;
		myMap = ((fsmAgent)this.myAgent).getMap();
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		String nextNode=null;
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
		this.myMap.addNode(myPosition, MapAttribute.closed);
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		while(iter.hasNext()){
			Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
			String nodeId= node.getLeft();

		if (myPosition!=nodeId) {
				this.myMap.addEdge(myPosition, nodeId);
				if (nextNode==null && nodeId.equals(((fsmAgent)this.myAgent).nextNode)) nextNode=nodeId;
			}
		}
		((fsmAgent)this.myAgent).nextNode = nextNode;
		
        if (((fsmAgent)this.myAgent).nextNode == null) {
        	exitValue = 1;
        	return ;
        }
        
		boolean SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (StrangeWaitBehaviour)");
			exitValue = 1;
			((fsmAgent)this.myAgent).successBlock = false;
			try {
				this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ;
		}
		
    	final MessageTemplate msgSomeone = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleSomeone"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgSo = this.myAgent.receive(msgSomeone);
    	
    	if (msgSo != null) {
    		String m =msgSo.getContent();
    		//If his nextNode is my current pos
    		if ( m.equals(myPosition)) {
        		System.out.println(this.myAgent.getLocalName() + " --> Receive a check Someone msg (StrangeWaitBehaviour)");
    			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
    			msg.setProtocol("ProtocoleWaitSomeone");
    			msg.setSender(this.myAgent.getAID());
    			System.out.println(this.myAgent.getLocalName()+ " ---> I'm not a Wumpus "+msgSo.getSender().getLocalName());
    			msg.setContent(myPosition);

    			msg.addReceiver(new AID(msgSo.getSender().getLocalName(),AID.ISLOCALNAME));
    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
    			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
    		}

    	}
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		if(((fsmAgent)this.myAgent).nextNode == null) {
			exitValue = 1;
			return ;
		}
		SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);

		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (StrangeWaitBehaviour)");
			exitValue = 1;
			((fsmAgent)this.myAgent).nextNode = null;
			try {
				this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ;
		}else {
			System.out.println(this.myAgent.getLocalName() + " --> The potential Wumpus still probably blocked ! (StrangeWaitBehaviour) "+((fsmAgent)this.myAgent).nextNode);
		}
		
		//If receive a message, don't move
		if (msg != null) {

			System.out.println(this.myAgent.getLocalName() + " --> Receive msg, I am not a Golem bypass me");
			
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleByPass");
			sendMsg.setSender(this.myAgent.getAID());
			sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

			sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
		}else {
			final MessageTemplate msgSpam = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"), 
					MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
			final ACLMessage msgS = this.myAgent.receive(msgSpam);
			while(true) {
				if(msgS == null) {

					ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
					sendMsg.setProtocol("ProtocoleByPass");
					sendMsg.setSender(this.myAgent.getAID());
					sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

					for(String n: ((fsmAgent)this.myAgent).getList_AgentNames()) {
						sendMsg.addReceiver(new AID(n,AID.ISLOCALNAME));
						
					}
					
					((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
					break;
				}else {
					if(myPosition.equals(msgS.getContent())) {
						System.out.println(this.myAgent.getLocalName() + " --> It's not a Wumpus back to explo");
						exitValue = 1;
						return ;
					}
				}
			}
			

		}
		
		

		
		block(5000);
		
	}

	@Override
	public int onEnd() {
		return exitValue;
	}
}
