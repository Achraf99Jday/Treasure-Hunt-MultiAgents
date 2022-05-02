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

public class SuccessBlockBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4318104060489928048L;

	private int exitValue;
	
	private List<String> SendedAgent = new ArrayList<String>(), ShareMapAgent = new ArrayList<String>();

	private String myPosition;

	private MapRepresentation myMap;
	
	public SuccessBlockBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		((fsmAgent)this.myAgent).successBlock = true;
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		myMap = ((fsmAgent)this.myAgent).getMap();
		exitValue = 0;
		String nextNode=null;
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		while(iter.hasNext()){
			Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
			String nodeId= node.getLeft();
			List<Couple<Observation, Integer>> list = node.getRight();

			if (myPosition!=nodeId) {
				this.myMap.addEdge(myPosition, nodeId);
				if (nextNode==null && nodeId.equals(((fsmAgent)this.myAgent).nextNode)) nextNode=nodeId;
			}
		}
		((fsmAgent)this.myAgent).nextNode = nextNode;
		
        if (((fsmAgent)this.myAgent).nextNode == null) {
        	exitValue = 2;
        	return ;
        }
        
		boolean SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (SuccessBehaviour)");
			SendedAgent = new ArrayList<String>();
			ShareMapAgent = new ArrayList<String>();
			exitValue = 2;
			((fsmAgent)this.myAgent).successBlock = false;
			try {
				this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ;
		}
		
		final MessageTemplate msgTemplateSpam = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"),
								MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
		final ACLMessage msgSpam = this.myAgent.receive(msgTemplateSpam);
		
		if (msgSpam == null) {
		
			final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
			final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
			//If receive a message, don't move
			if (msg != null) {
				
				if (msg.getProtocol().equals("ProtocoleSomeone")){
					//Skip if he don't to come on my node
					if (!msg.getContent().equals(((fsmAgent)this.myAgent).nextNode)){
						return ;
					}
				}
				
				System.out.println(this.myAgent.getLocalName() + " --> Receive msg, I am not a Golem bypass me ("+((AbstractDedaleAgent)this.myAgent).getCurrentPosition()+")");
				
				ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
				sendMsg.setProtocol("ProtocoleByPass");
				sendMsg.setSender(this.myAgent.getAID());
				sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		
				sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		
				//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
				((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
				
				MapRepresentation myMap = ((fsmAgent)this.myAgent).getMap();
				myMap.addNode(((fsmAgent)this.myAgent).nextNode, MapAttribute.closed);
				((fsmAgent)this.myAgent).updateMap(myMap);
				((fsmAgent)this.myAgent).successBlock = true;
				if(!ShareMapAgent.contains(msg.getSender().getLocalName())) {
					ShareMapAgent.add(msg.getSender().getLocalName());
					exitValue = 1;
				}
			}
		}else {

			if(!SendedAgent.contains(msgSpam.getSender().getLocalName())) {
				if(msgSpam.getContent().equals(((fsmAgent)this.myAgent).nextNode)) {
					System.out.println(this.myAgent.getLocalName() + " --> Sorry I thought you were a Wumpus "+msgSpam.getSender().getLocalName());
					SendedAgent = new ArrayList<String>();
					ShareMapAgent = new ArrayList<String>();
					exitValue = 2;
					((fsmAgent)this.myAgent).blockedAgent.add(msgSpam.getContent());
					((fsmAgent)this.myAgent).successBlock = false;
					((fsmAgent)this.myAgent).forceChangeNode=true;
					return ;
				}
				SendedAgent.add(msgSpam.getSender().getLocalName());
			}

		}
		block(5000);
	}
	@Override
	public int onEnd() {
		return exitValue;
	}
}
