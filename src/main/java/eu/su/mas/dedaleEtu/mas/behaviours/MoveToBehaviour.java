package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class MoveToBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7703994282714623870L;
	private int exitValue, fail = 10,count_fail;
	private boolean SuccessMove;
	private MapRepresentation myMap;
	private List<String> temp;
	private String nextNode;
	private String myPosition;


	@Override
	public void action() {
		this.exitValue = 0;
		
		myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		if(((fsmAgent)this.myAgent).moveTo == null) {
			System.out.println(this.myAgent.getLocalName()+" ---> moveTo is null, quit MoveToBehaviour");
			((fsmAgent)this.myAgent).blockedAgent.clear();
			this.exitValue = 2;
			return ;
		}
		
		if(((fsmAgent)this.myAgent).moveTo.equals(myPosition)) {
			((fsmAgent)this.myAgent).moveTo = null;
			((fsmAgent)this.myAgent).blockedAgent.clear();
			((fsmAgent)this.myAgent).nextNode = ((fsmAgent)this.myAgent).WumpusPos ;
			System.out.println(this.myAgent.getLocalName()+" ---> moveTo successfull, go to NeedHelpBehaviour");
			((fsmAgent)this.myAgent).NodeToBlock.remove(myPosition);
			this.exitValue = 10;
			return ;
		}
		if(!SuccessMove) {
			if(checkMsg()) {
				clearMail();
				return ;
			}
			if (count_fail >= fail) {
				((fsmAgent)this.myAgent).moveTo = null;
				System.out.println(this.myAgent.getLocalName()+" ---> Ok I give up my mission, something blocks me and does not want to move, certainly a Wumpus (or a very stupid agent)!");
				exitValue = 2;
				count_fail=0;
				return ;
			}
			count_fail++;
		}else {
			count_fail = 0;
		}
		
		
		this.myMap = ((fsmAgent)this.myAgent).getMap();
		
		//If no msg and agent can move
		if(((fsmAgent)this.myAgent).move && this.myMap!=null) {

			//0) Retrieve the current position
			
	
			if (myPosition!=null){
				//List of observable from the agent's current position
	
				/**
				 * Just added here to let you see what the agent is doing, otherwise he will be too quick
				 */
				try {
					this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
				} catch (Exception e) {
					e.printStackTrace();
				}

				this.myMap.addNode(myPosition, MapAttribute.closed);

				nextNode=null;

				temp = this.myMap.getShortestPath(myPosition, ((fsmAgent)this.myAgent).moveTo, ((fsmAgent)this.myAgent).blockedAgent);
				if (temp != null) {
					if(temp.size()>0) {
						nextNode = temp.get(0);
					}else {
						System.out.println(this.myAgent.getLocalName()+" ---> No Path finded, quit MoveToBehaviour");
						((fsmAgent)this.myAgent).blockedAgent.clear();
						((fsmAgent)this.myAgent).moveTo = null;
						this.exitValue = 2;
						return ;
					}
				}else {
					System.out.println(this.myAgent.getLocalName()+" ---> No Path finded, quit MoveToBehaviour");
					((fsmAgent)this.myAgent).blockedAgent.clear();
					((fsmAgent)this.myAgent).moveTo = null;
					this.exitValue = 2;
					return ;
				}

				((fsmAgent)this.myAgent).nextNode=nextNode;
				((fsmAgent)this.myAgent).updateMap(this.myMap);
				SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
				
				if(!SuccessMove) {
					System.out.println(this.myAgent.getLocalName()+" ---> Leave me a path, I have an important mission !");
					
	    			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
	    			sendMsg.setProtocol("ProtocoleLeavePath");
	    			sendMsg.setSender(this.myAgent.getAID());
	    			this.temp.add(0, myPosition);
	    			try {
						sendMsg.setContentObject((Serializable) this.temp);
					} catch (IOException e) {
						e.printStackTrace();
					}
	    			
	    			for(String n: ((fsmAgent)this.myAgent).getList_AgentNames()) {
		    			sendMsg.addReceiver(new AID(n,AID.ISLOCALNAME));

	    			}

	    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
	    			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
	    			this.temp.remove(0);
	    			
	    			if (count_fail >= fail) {
						System.out.println(this.myAgent.getLocalName()+" ---> Ok I give up my mission, something blocks me and does not want to move, certainly a Wumpus (or a very stupid agent)!");
						((fsmAgent)this.myAgent).moveTo = null;
						exitValue = 2;
						count_fail=0;
						return ;
	    			}
	    			count_fail++;

					return ;
				}else {
					count_fail = 0;
				}
				
			}
				
	
			
		}
	}
	
	public boolean checkMsg() {
		
		while(true) {
			MessageTemplate msgTemplateSorry=MessageTemplate.and(
					MessageTemplate.MatchProtocol("ProtocoleSorry"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			
			final ACLMessage msgSorry = this.myAgent.receive(msgTemplateSorry);
			if ( msgSorry != null ) {
				System.out.println(this.myAgent.getLocalName()+" ---> Receive sorry msg, ok i will try to bypass you "+msgSorry.getSender().getLocalName());
				((fsmAgent)this.myAgent).blockedAgent.add(msgSorry.getContent());
			}else {
				break;
			}
		}
		
		while(true) {
			MessageTemplate msgTemplateByPass=MessageTemplate.and(
					MessageTemplate.MatchProtocol("ProtocoleByPass"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			
			final ACLMessage msgByPass = this.myAgent.receive(msgTemplateByPass);
			if ( msgByPass != null ) {
				System.out.println(this.myAgent.getLocalName()+" ---> Receive ByPass msg, ok i will try to bypass you "+msgByPass.getSender().getLocalName());
				((fsmAgent)this.myAgent).blockedAgent.add(msgByPass.getContent());
			}else {
				break;
			}
		}
		List<String> agentToContact = ((fsmAgent)this.myAgent).getList_AgentNames();
		while(true) {
			MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			final ACLMessage msg = this.myAgent.receive(msgTemplate);
			
			if (msg !=null) {
				if (msg.getProtocol().equals("ProtocoleByPass") || msg.getProtocol().equals("ProtocoleLeavePath") || this.temp == null ) {
					return false;
				}
				
    			System.out.println(this.myAgent.getLocalName() + " --> Send a msg to leave me a path to "+msg.getSender().getLocalName());

    			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
    			sendMsg.setProtocol("ProtocoleLeavePath");
    			sendMsg.setSender(this.myAgent.getAID());
    			this.temp.add(0, myPosition);
    			try {
					sendMsg.setContentObject((Serializable) this.temp);
				} catch (IOException e) {
					e.printStackTrace();
				}
    			if(!agentToContact.contains(msg.getSender().getLocalName())) {
    				sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
    				agentToContact.remove(msg.getSender().getLocalName());
    			}
    			

    			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
    			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
    			this.temp.remove(0);
			}else {
				break;
			}
		}
		
		
		return false;


	}
	public void clearMail() {
		while(true) {
			MessageTemplate msgTemplate= MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg=this.myAgent.receive(msgTemplate);
			
			if(msg == null) {
				break;
			}
		}
	}

	@Override
	public int onEnd() {
		return exitValue;
	}

}
