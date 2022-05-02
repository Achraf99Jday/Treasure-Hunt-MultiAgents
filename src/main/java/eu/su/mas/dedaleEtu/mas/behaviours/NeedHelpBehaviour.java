package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.graphstream.graph.Node;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class NeedHelpBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8780845722125588676L;
	
	private String myPosition;
	private String nextNode;
	private MapRepresentation myMap;
	private List<String> NodeToBlock, News, agentToContact = ((fsmAgent) this.myAgent).getList_AgentNames();
	private int exitValue;

	private boolean SuccessMove, change;
	
	public NeedHelpBehaviour(Agent a) {
		super(a);
	}
	
	@Override
	public void action() {
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		nextNode = ((fsmAgent)this.myAgent).nextNode;
		myMap = ((fsmAgent)this.myAgent).getMap();
		NodeToBlock = ((fsmAgent)this.myAgent).NodeToBlock;
		change = false;
		exitValue = 0;

    	final MessageTemplate msgTemplateEnd = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleNodeBlocked"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgE = this.myAgent.receive(msgTemplateEnd);
    	//Check if all node is blocked
    	if(msgE != null) {
    		//check if is the same node to block
    		if ( msgE.getContent().equals(((fsmAgent)this.myAgent).nextNode)) {
        		System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
            	exitValue = 1;
            	return ;
    		}

    	}
		//All Node is blocked and send to other agent the mission is a success 
        if(NodeToBlock.size() == 0) {
        	System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleNodeBlocked");
			sendMsg.setSender(this.myAgent.getAID());
			
			sendMsg.setContent(((fsmAgent)this.myAgent).nextNode);

			for( String n : ((fsmAgent)this.myAgent).getList_AgentNames()) {
				sendMsg.addReceiver(new AID(n,AID.ISLOCALNAME));
			}
			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
        	exitValue = 1;
        	return ;
        }else {
        	final MessageTemplate msgTemplate2 = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
        														MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    		final ACLMessage msgBlock = this.myAgent.receive(msgTemplate2);
    		
    		//Receive another around agents help msg
    		if(msgBlock != null) {
    			try {
					News = (List<String>) msgBlock.getContentObject();
					String error = News.get(0); //get Sender Position
					News.remove(0);
					String errorNextNode = News.get(0); // getSender NextNode
					News.remove(0);
					//If my nextNode is a current Agent node 
					if (error.equals(((fsmAgent) this.myAgent).nextNode)){
						//Ignore didn't concert me
						if(!errorNextNode.equals(((fsmAgent)this.myAgent).nextNode)) {
							System.out.println(this.myAgent.getLocalName() + " --> Ignore it's didn't concern me at "+errorNextNode);
							exitValue = 2;
							return ;
						}
		    			System.out.println(this.myAgent.getLocalName() + " --> Wrong ! It's not a golem at "+error);
		    			//update all parameters and back to Explo
						NodeToBlock = News;
						((fsmAgent)this.myAgent).NodeToBlock = News;
						((fsmAgent)this.myAgent).blockedAgent.add(error);
						((fsmAgent)this.myAgent).NodeToBlock.clear();;
						exitValue = 2;
						return ;
					}else {// It's not wrong alert
						if(!errorNextNode.equals(((fsmAgent)this.myAgent).nextNode)) {
							System.out.println(this.myAgent.getLocalName() + " --> Ignore it's didn't concern me at "+errorNextNode);
							return ;
						}
		    			System.out.println(this.myAgent.getLocalName() + " --> Receive serialized msg ");
		    			
		    			News.remove(myPosition);
		    			Iterator<String> itr = News.iterator(); 
		    			//update the block node with the sender block node
		    			while (itr.hasNext()) {
		    				String n = itr.next(); 
		    				if (!NodeToBlock.contains(n)) { 
		    					itr.remove();
		    					change = true;
		    				} 
		    			}
					}
	    			
	    			NodeToBlock = News;
	    			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock ;
	    			System.out.println(this.myAgent.getLocalName() + " --> MAJ NodeToBlock : "+NodeToBlock);
	    			
				} catch (UnreadableException e) {
					e.printStackTrace();
				}

    		}
    		
    		final MessageTemplate msgTemplateB =  MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));		
    		final ACLMessage msgB = this.myAgent.receive(msgTemplateB);
    		
    		if (msgB != null) {
        		if (msgB.getContent().equals(((fsmAgent)this.myAgent).nextNode)) {
    	        	System.out.println(this.myAgent.getLocalName()+ " ---> Wrong it's not a Wumpus, its "+msgB.getSender().getLocalName());
    	        	exitValue = 2;
    	        	return ;
        		}
    			boolean error2 = NodeToBlock.remove(msgB.getContent());
    			
    			if ( error2 ) {
        			agentToContact.remove(msgB.getSender().getLocalName());
        			System.out.println(this.myAgent.getLocalName() + " --> Remove to NodeToBlock "+msgB.getContent());
        			
        	        if(NodeToBlock.size() == 0) {
        	        	System.out.println(this.myAgent.getLocalName()+ " ---> All NodeToBlock is blocked !");
        	        	exitValue = 1;
        	        	return ;
        	        }
    			}

    	        
    		}
    		
    		final MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
    		final ACLMessage msg = this.myAgent.receive(msgTemplate);
    		
			
			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
    		//If receive a message send him especially the all node to block
    		if (msg != null ) {
    			//If nothing change with msgBlock, do not send again
    			if(!change && msgBlock == null) {
        			System.out.println(this.myAgent.getLocalName() + " --> Receive msg from "+msg.getSender().getLocalName());
        			System.out.println(this.myAgent.getLocalName() + " --> Need to block "+NodeToBlock);

        			NodeToBlock.add(0, ((fsmAgent)this.myAgent).nextNode);
        			NodeToBlock.add(0, myPosition);
        			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
        			sendMsg.setProtocol("ProtocoleHelpBlockWumpus");
        			sendMsg.setSender(this.myAgent.getAID());
        			
        			try {
    					sendMsg.setContentObject((Serializable) NodeToBlock);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}

        			sendMsg.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
        			
        			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
        			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
        			NodeToBlock.remove(0);
        			NodeToBlock.remove(0);
    			}

    		}else {
    			//If nothing change with msgBlock, do not send again
    			if(!change && msgBlock == null) {
        			//If no msg send every 5000ms a need help to block to every agent around
        			System.out.println(this.myAgent.getLocalName() + " --> Send a Help to block Wumpus msg");
        			System.out.println(this.myAgent.getLocalName() + " --> Need to block "+NodeToBlock);
        			
        			NodeToBlock.add(0, ((fsmAgent)this.myAgent).nextNode);
        			NodeToBlock.add(0, myPosition);
        			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
        			sendMsg.setProtocol("ProtocoleHelpBlockWumpus");
        			sendMsg.setSender(this.myAgent.getAID());
        			
        			try {
    					sendMsg.setContentObject((Serializable) NodeToBlock);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}

        			for (String agentName : agentToContact) {
        				sendMsg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
        			}
        			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
        			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
        			NodeToBlock.remove(0);
        			NodeToBlock.remove(0);
    			}
    		}
        }
        
		String nextNode=null;
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		//Check if the nextNode is around me, because sometime nextNode will change in comunication
		while(iter.hasNext()){
			Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
			String nodeId= node.getLeft();
			//System.out.print(nodeId+" ");
			if (myPosition!=nodeId) {
				if (nextNode==null && nodeId.equals(((fsmAgent)this.myAgent).nextNode)) {
					nextNode=nodeId;
				}
			}
		}
		((fsmAgent)this.myAgent).nextNode = nextNode;

		//If loss nextNode back to explo
        if (((fsmAgent)this.myAgent).nextNode == null) {
			System.out.println(this.myAgent.getLocalName() + " --> nextNode null, back to explo ! (NeedHelpBehaviour)");

        	exitValue = 2;
        	return ;
        }
		SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(((fsmAgent)this.myAgent).nextNode);
		
		//if I can move, back to explo
		if(SuccessMove) {
			System.out.println(this.myAgent.getLocalName() + " --> The Wumpus is not blocked ! (NeedHelpBehaviour)");
			exitValue = 2;
			
			while(true) {
				final MessageTemplate msgTemplateClean = MessageTemplate.MatchPerformative(ACLMessage.INFORM);	
	    		final ACLMessage msgClean = this.myAgent.receive(msgTemplateClean);
	    		if(msgClean == null) {
	    			break;
	    		}
			}
    		
			((fsmAgent)this.myAgent).nextNode = null;
			try {
				this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ;
		}else {
			System.out.println(this.myAgent.getLocalName() + " --> The potential Wumpus still probably blocked at "+((fsmAgent)this.myAgent).nextNode+" ! (NeedHelpBehaviour) ");
		}
        block(5000);
	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}
}

