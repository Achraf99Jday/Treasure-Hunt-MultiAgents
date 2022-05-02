package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class ExploCoopBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	
	private int exitValue;
	
	private String nodeGoal = "";
	
	private int timer, start, now, nb_move_fail, max_move_fail=((fsmAgent)this.myAgent).AgentSensitivity, timer_spam = 1000;
	
	private String oldNode="", receiveAgentName;
	
	private boolean SuccessMove, modeLeavePath=false;
	
	private List<String> temp, leavePath;
	
	private List<Couple<String,Integer>> list_spam = new ArrayList<>();

	private int cpt_null, max_null=5;

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, int timer) {
		super(myagent);
		this.myMap=((fsmAgent)this.myAgent).getMap();
		this.timer=timer;
		this.start = 0;
	}

	@Override
	public void action() {
		this.exitValue = 0;
		//list_spam.add(new Couple<>("teste", Integer.valueOf(5)));
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		//If Someone have a special Mision, I will try to not block him
		if(modeLeavePath) {
			//If I'm not on the path i will juste stop a few time
			if(!leavePath.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				try {
					this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed * 3);
				} catch (Exception e) {
					e.printStackTrace();
				}

	
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
	
				//1) remove the current node from openlist and add it to closedNodes.
				this.myMap.addNode(myPosition, MapAttribute.closed);
	
				//2) get the surrounding nodes and, if a surrounding node is not on the path i will go
				String nextNode=null;
				Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
				while(iter.hasNext()){
					Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
					String nodeId= node.getLeft();
	
					if (myPosition!=nodeId) {
						this.myMap.addEdge(myPosition, nodeId);
						if (nextNode==null && !leavePath.contains(nodeId)) nextNode=nodeId;
					}
				}
				modeLeavePath = false;
				leavePath= null;
				
				//If no nextNode finded i will just says to him
				if (nextNode == null) {
					System.out.println(this.myAgent.getLocalName()+" ---> Sorry I'm not smart enough to find a knot to let you through");
					sendSorryMsg(); // Send of the Sorry msg
					modeLeavePath = false;
					leavePath= null;
					try {
						this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed * 3);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return ; 
				}else{
					
					((fsmAgent)this.myAgent).nextNode=nextNode;
					((fsmAgent)this.myAgent).updateMap(this.myMap);
					SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
					
					if(SuccessMove) {
						System.out.println(this.myAgent.getLocalName()+" ---> I find a node to back ! Good luck to your important mision ");
					}else {
						System.out.println(this.myAgent.getLocalName()+" ---> Sorry I find a node to back, but somethink block me ");
						sendSorryMsg();
						modeLeavePath = false;
						leavePath= null;
					}
				}
				return ;
			}
			
		}
		//If I receive a special mission to MoveTo, go to MoveTo protocole
		if (((fsmAgent)this.myAgent).moveTo != null) {
			this.exitValue = 6;
		}
		//If the Exploration is finished go directly to chase
		if(((fsmAgent)this.myAgent).endExplo) {
			this.exitValue = 4;//End of exploration
			((fsmAgent)this.myAgent).forceChangeNode=true;
			return ;
		}
		//Check every msg et check timer
		if(((fsmAgent)this.myAgent).nextNode != null) {
			if(checkMsg() || checkTimer()) {
				//clearMail();
				return ;
			}
		}
		
		this.myMap = ((fsmAgent)this.myAgent).getMap();
		

		if(((fsmAgent)this.myAgent).move && this.myMap!=null) {

			//0) Retrieve the current position
			myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
			if (myPosition!=null){
				//List of observable from the agent's current position
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
	
				/**
				 * Just added here to let you see what the agent is doing, otherwise he will be too quick
				 */
				try {
					this.myAgent.doWait(((fsmAgent)this.myAgent).AgentSpeed);
				} catch (Exception e) {
					e.printStackTrace();
				}
	
				//1) remove the current node from openlist and add it to closedNodes.
				this.myMap.addNode(myPosition, MapAttribute.closed);
	
				//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
				String nextNode=null;
				Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
				while(iter.hasNext()){
					Couple<String, List<Couple<Observation, Integer>>> node = iter.next();
					String nodeId= node.getLeft();
					List<Couple<Observation, Integer>> list = node.getRight();
					boolean isNewNode=this.myMap.addNewNode(nodeId);
					//the node may exist, but not necessarily the edge
					/*
			        for(Couple<Observation, Integer> model : list) {
			            System.out.print("Observation "+model.getLeft()+ " Integer "+model.getRight()+" | ");
			        }
			        */
			        //System.out.println(this.myAgent.getLocalName()+" nodeID "+nodeId);
					if (myPosition!=nodeId) {
						this.myMap.addEdge(myPosition, nodeId);
						if (nextNode==null && isNewNode) nextNode=nodeId;
					}
				}
	
				//3) while openNodes is not empty, continues.
				if (!this.myMap.hasOpenNode()){
					//Explo finished
					this.exitValue = 4;//End of exploration
					((fsmAgent)this.myAgent).endExplo=true;
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done");
				}else{
					
					if (((fsmAgent)this.myAgent).succesMerge || nextNode==null || ((fsmAgent)this.myAgent).forceChangeNode){
						//If I need to take an another path, Init randomly
						if (((fsmAgent)this.myAgent).succesMerge || ((fsmAgent)this.myAgent).forceChangeNode) {
							if (nodeGoal.equals("") || oldNode.equals(myPosition) || ((fsmAgent)this.myAgent).forceChangeNode) {
								List<String> opennodes=this.myMap.getOpenNodes();
								Random rand = new Random();
								nodeGoal = opennodes.get(rand.nextInt(opennodes.size()));
								cpt_null=0;
							}
							temp = this.myMap.getShortestPath(myPosition, nodeGoal, ((fsmAgent)this.myAgent).blockedAgent);

							if (temp != null ) {
								if (temp.size()>0) {
									nextNode = temp.get(0);
								}
								
							}else {
								nodeGoal = "";
								((fsmAgent)this.myAgent).blockedAgent.clear();
								System.out.println(this.myAgent.getLocalName()+" ---> null Path, reset");
								
								if (cpt_null >= max_null && this.myMap.getOpenNodes().size()==1) {
									this.myMap.addNode(this.myMap.getOpenNodes().get(0), MapAttribute.closed);
									System.out.println(this.myAgent.getLocalName()+" --->  Max null Path, auto closed the last node");
								}
								cpt_null++;
								
								return ;
							}
							
							if(nextNode.equals(nodeGoal)) {
								((fsmAgent)this.myAgent).forceChangeNode = false;
								((fsmAgent)this.myAgent).succesMerge = false;
								nodeGoal = "";
							}
						}
						else {
							temp = this.myMap.getShortestPathToClosestOpenNode(myPosition, ((fsmAgent)this.myAgent).blockedAgent);
							if (temp != null) {
								nextNode = temp.get(0);
							}else {
								((fsmAgent)this.myAgent).blockedAgent.clear();
								System.out.println(this.myAgent.getLocalName()+" ---> null Path, reset");
								
								if (cpt_null >= max_null && this.myMap.getOpenNodes().size()==1) {
									this.myMap.addNode(this.myMap.getOpenNodes().get(0), MapAttribute.closed);
									System.out.println(this.myAgent.getLocalName()+" --->  Max null Path, auto closed the last node");
								}
								cpt_null++;
								
								return ;
							}
							nextNode=temp.get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
						}
						//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode+ " actual node :"+myPosition);
					}else {
						//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode + " actual node :"+myPosition);
					}

					((fsmAgent)this.myAgent).nextNode=nextNode;
					((fsmAgent)this.myAgent).updateMap(this.myMap);
					SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
				}
				
				
				if (!SuccessMove) {
					//mas_move_fail = the Agent sensibility
					if ( nb_move_fail >= max_move_fail) {
						((fsmAgent)this.myAgent).needToCheck = true;
						System.out.println(this.myAgent.getLocalName() + " --> I probably blocked a Wumpus, need to check ! (stop move)");
						this.exitValue = 4;
						return ;
					}
					nb_move_fail++;
					//System.out.println(this.myAgent.getLocalName()+" : nb_move = "+nb_move_fail);
				}else {
					oldNode = myPosition;
					nb_move_fail = 0;
				}
	
			}
		}
	}
	//Check every msg
	public boolean checkMsg() {
		
		//Protocole leave the path receive
		while(true) {
			MessageTemplate msgTemplatelp=MessageTemplate.and(
					MessageTemplate.MatchProtocol("ProtocoleLeavePath"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		
			ACLMessage msglp=this.myAgent.receive(msgTemplatelp);
			
			if(msglp!=null) {
				try {
					leavePath = (List<String>) msglp.getContentObject();
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				receiveAgentName = msglp.getSender().getLocalName();
				modeLeavePath  = true;
			}
			else {
				break;
			}
		}
		if (modeLeavePath) {
			return true;
		}
		
		//protocole exchange Map
		MessageTemplate msgTemplateMap=MessageTemplate.and(
				MessageTemplate.MatchProtocol("ProtocoleShareMap"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msgMap=this.myAgent.receive(msgTemplateMap);
		//System.out.println(this.myAgent.getLocalName() + " --> "+this.myAgent.getCurQueueSize());
		if (msgMap != null && !containToList_spam(msgMap.getSender().getLocalName())) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a Map (stop move)");
			this.exitValue = 2;//Go to share map
			this.start = 0;
			list_spam.add(new Couple<>(msgMap.getSender().getLocalName(), Integer.valueOf((int) System.currentTimeMillis())));
			((AbstractDedaleAgent)this.myAgent).postMessage(msgMap);
			return true;
		}
		
		//protocole exchange Map
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocolePoke"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msg = this.myAgent.receive(msgTemplate);

		//If receive a message, don't move
		if (msg != null && !containToList_spam(msg.getSender().getLocalName())) {
			System.out.println(this.myAgent.getLocalName() + " --> Receive a poke (stop move)");
			list_spam.add(new Couple<>(msg.getSender().getLocalName(), Integer.valueOf((int) System.currentTimeMillis())));
			this.exitValue = 2;//Go to share map
			this.start = 0;
			return true;
		}
		
		
		final MessageTemplate msgTPass = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleByPass"), 
				MessageTemplate.MatchPerformative(ACLMessage.INFORM) );	
		final ACLMessage msgPass = this.myAgent.receive(msgTPass);
		//Receive a msg whit the position of the agent where he is blocked
		if(msgPass != null) {
			((fsmAgent)this.myAgent).blockedAgent.add(msgPass.getContent());
			return false;
		}
		
    	final MessageTemplate msgTemplateBlock = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleHelpBlockWumpus"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgBlock = this.myAgent.receive(msgTemplateBlock);
    	
    	if (msgBlock != null) {
    		List<String> NodeToBlock = new ArrayList<String>();
			try {
				NodeToBlock = (List<String>) msgBlock.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();			
			}
			((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
			String AgentPos = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
			((fsmAgent)this.myAgent).blockedAgent.add(AgentPos);
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			
			String AgentNextPos = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
			((fsmAgent)this.myAgent).NodeToBlock.remove(0);
			
			if(((fsmAgent)this.myAgent).NodeToBlock.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				System.out.println(this.myAgent.getLocalName() + " --> I'm already on the Node to block ("+((AbstractDedaleAgent)this.myAgent).getCurrentPosition()+")");
				((fsmAgent)this.myAgent).nextNode = AgentNextPos;
				((fsmAgent)this.myAgent).NodeToBlock.remove(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				
				exitValue = 5;
				return true;
			}
			//If the Sender want to come to my position
			if(AgentNextPos.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
				sendMsg.setProtocol("ProtocoleByPass");
				sendMsg.setSender(this.myAgent.getAID());
				sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		
				sendMsg.addReceiver(new AID(msgBlock.getSender().getLocalName(),AID.ISLOCALNAME));

				((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
				
				System.out.println(this.myAgent.getLocalName() + " --> I'm not a Wumpus "+AgentNextPos+" ! (ExploCoopBehaviour)");
				return false;
			}
			/*
			if(!AgentNextPos.equals(((fsmAgent)this.myAgent).nextNode)) {
				System.out.println(this.myAgent.getLocalName() + " --> Ignore it's didn't concern me at "+AgentNextPos+" (ExploCoopBehaviour)");
				return false;
			}*/
			System.out.println(this.myAgent.getLocalName() + " --> NodeToBlock "+((fsmAgent)this.myAgent).NodeToBlock+" (ExploCoopBehaviour)");
			if(((fsmAgent)this.myAgent).NodeToBlock.size()>0) {
				((fsmAgent)this.myAgent).moveTo = ((fsmAgent)this.myAgent).NodeToBlock.get(0);
				((fsmAgent)this.myAgent).NodeToBlock.remove(0);
				((fsmAgent)this.myAgent).blockedAgent.add(AgentNextPos);
				((fsmAgent)this.myAgent).WumpusPos = AgentNextPos;
				System.out.println(this.myAgent.getLocalName() + " --> Go to MoveToBehaviour "+((fsmAgent)this.myAgent).moveTo);
				exitValue = 6;
				return true;
			}
			return false;
			
    	}
    	final MessageTemplate msgSomeone = MessageTemplate.and(MessageTemplate.MatchProtocol("ProtocoleSomeone"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));	
    	final ACLMessage msgS = this.myAgent.receive(msgSomeone);
    	
    	if (msgS != null) {
    		String m =msgS.getContent();
    		//If his nextNode is my current pos
    		if ( m.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
    			((AbstractDedaleAgent)this.myAgent).postMessage(msgS);
        		System.out.println(this.myAgent.getLocalName() + " --> Receive a check Someone msg on ExploCoop, go to Chase mode to check");
        		exitValue = 4;
        		return true;
    		}
    	}
		return false;
	}
	
	public void sendSorryMsg() {
		if (receiveAgentName != null) {
			ACLMessage sendMsg=new ACLMessage(ACLMessage.INFORM);
			sendMsg.setProtocol("ProtocoleSorry");
			sendMsg.setSender(this.myAgent.getAID());

			sendMsg.setContent(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());

			sendMsg.addReceiver(new AID(receiveAgentName,AID.ISLOCALNAME));

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(sendMsg);
		}
	}
	
	//check the timer and every PokeTime, go to SayHello
	public boolean checkTimer() {
		Iterator<Couple<String,  Integer>> iter=list_spam.iterator();
		
		while(iter.hasNext()){
			Couple<String, Integer> agent = iter.next();
			Integer time = agent.getRight();
			this.now = (int) System.currentTimeMillis();
			int res = this.now - time.intValue();
			//System.out.println(this.myAgent.getLocalName()+" ---> blocked "+ agent.getLeft());
			if ( res > this.timer_spam) {
				//System.out.println(this.myAgent.getLocalName()+" ---> remove blocked "+ agent.getLeft());
				iter.remove();
			}
		}
		if (this.start == 0) {
			this.start = (int) System.currentTimeMillis();
		}
		this.now = (int) System.currentTimeMillis();
		int res = this.now - this.start;
		//System.out.println("Timer : "+res);
		if ( res > this.timer) {
			//System.out.println(this.myAgent.getLocalName()+" ---> Take a break to breathe");
			this.exitValue = 1;//Go to say hello, to know if there are friends nearby.
			this.start = 0;
			return true;
		}
		return false;
	}
	
	public boolean containToList_spam(String agent_name) {
		Iterator<Couple<String,  Integer>> iter=list_spam.iterator();

		while(iter.hasNext()){
			Couple<String, Integer> agent = iter.next();
			String name= agent.getLeft();
			if ( name.equals(agent_name)) {
				return true;
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
