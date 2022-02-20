package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
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
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;

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
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false, SuccessMove, modeLeavePath=false;
	private String oldNode="", receiveAgentName, nodeGoal="";
	private MapRepresentation myMap;
	private List<String> temp, leavePath;
	private int cpt_null, max_null=5;

	/**
	 * Current knowledge of the agent regarding the environment
	 */

	private List<String> list_agentNames;

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		
		
	}

	@Override
	public void action() {
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();	
	//paths conflict resolution:
		if(modeLeavePath) {
			if(!leavePath.contains(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
		
		
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
				
				//If no nextNode found will just says to him
				if (nextNode == null) {
					System.out.println(this.myAgent.getLocalName()+" ---> Sorry I'm not smart enough to find a knot to let you through");
					//sendSorryMsg(); // Send of the Sorry msg
					modeLeavePath = false;
					leavePath= null;
					return ; 
				}else{
					
					((ExploreCoopAgent)this.myAgent).nextNode=nextNode;
					((ExploreCoopAgent)this.myAgent).updateMap(this.myMap);
					SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
					
					if(SuccessMove) {
						System.out.println(this.myAgent.getLocalName()+" ---> I found a node to go back ! Bye! ");
					}else {
						System.out.println(this.myAgent.getLocalName()+" ---> Sorry I found a node to back, but something is blocking me ");
						//sendSorryMsg();
						modeLeavePath = false;
						leavePath= null;
					}
				}
				return ;
			}
		}
		
		
		
		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
			this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent,500,this.myMap,list_agentNames));
		}

		//0) Retrieve the current position
		myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(1000);
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
				if (myPosition!=nodeId) {
					this.myMap.addEdge(myPosition, nodeId);
					if (nextNode==null && isNewNode) nextNode=nodeId;
				}
			}

			//3) while openNodes is not empty, continues.
			if (!this.myMap.hasOpenNode()){
				//Explo finished
				finished=true;
				System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
			}else{
				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (((ExploreCoopAgent)this.myAgent).successMerge || nextNode==null || ((ExploreCoopAgent)this.myAgent).forceChangeNode){
					//If I need to take an another path, Init randomly
					if (((ExploreCoopAgent)this.myAgent).successMerge || ((ExploreCoopAgent)this.myAgent).forceChangeNode) {
						if (nodeGoal.equals("") || oldNode.equals(myPosition) || ((ExploreCoopAgent)this.myAgent).forceChangeNode) {
							List<String> opennodes=this.myMap.getOpenNodes();
							Random rand = new Random();
							nodeGoal = opennodes.get(rand.nextInt(opennodes.size()));
							cpt_null=0;
						}
						temp = this.myMap.getShortestPath(myPosition, nodeGoal, ((ExploreCoopAgent)this.myAgent).blockedAgent);

						if (temp != null ) {
							if (temp.size()>0) {
								nextNode = temp.get(0);
							}
							
						}else {
							nodeGoal = "";
							((ExploreCoopAgent)this.myAgent).blockedAgent.clear();
							System.out.println(this.myAgent.getLocalName()+" ---> null Path, reset");
							
							if (cpt_null >= max_null && this.myMap.getOpenNodes().size()==1) {
								this.myMap.addNode(this.myMap.getOpenNodes().get(0), MapAttribute.closed);
								System.out.println(this.myAgent.getLocalName()+" --->  Max null Path, auto closed the last node");
							}
							cpt_null++;
							
							return ;
						}
						
						if(nextNode.equals(nodeGoal)) {
							((ExploreCoopAgent)this.myAgent).forceChangeNode = false;
							((ExploreCoopAgent)this.myAgent).successMerge = false;
							nodeGoal = "";
						}
					}
					else {
						temp = this.myMap.getShortestPathToClosestOpenNode(myPosition, ((ExploreCoopAgent)this.myAgent).blockedAgent);
						if (temp != null) {
							nextNode = temp.get(0);
						}else {
							((ExploreCoopAgent)this.myAgent).blockedAgent.clear();
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

				((ExploreCoopAgent)this.myAgent).nextNode=nextNode;
				((ExploreCoopAgent)this.myAgent).updateMap(this.myMap);
				SuccessMove = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);	
			}
				//4) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.

//				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
//				msg.setProtocol("SHARE-TOPO");
//				msg.setSender(this.myAgent.getAID());
//				if (this.myAgent.getLocalName().equals("1stAgent")) {
//					msg.addReceiver(new AID("2ndAgent",false));
//				}else {
//					msg.addReceiver(new AID("1stAgent",false));
//				}
//				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
//				try {					
//					msg.setContentObject(sg);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

				//5) At each time step, the agent check if he received a graph from a teammate. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set.
				MessageTemplate msgTemplate=MessageTemplate.and(
						MessageTemplate.MatchProtocol("SHARE-TOPO"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
				if (msgReceived!=null) {
					SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
					try {
						sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					this.myMap.mergeMap(sgreceived);
				}

				((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
			}

		}

	@Override
	public boolean done() {
		return finished;
	}

}
