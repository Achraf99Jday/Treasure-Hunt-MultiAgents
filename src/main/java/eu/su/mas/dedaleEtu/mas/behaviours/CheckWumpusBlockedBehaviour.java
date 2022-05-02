package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import org.graphstream.graph.Node;

public class CheckWumpusBlockedBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private String myPosition;
	private String nextNode;
	private MapRepresentation myMap;
	private List<Node> NeighborNode;
	private List<String> NodeToBlock;
	private int exitValue;
	
	private static final long serialVersionUID = 2364288375185614674L;
	
	public CheckWumpusBlockedBehaviour(Agent a) {
		super(a);
	}
	
	@Override
	public void action() {
		exitValue=0;

		
		myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		nextNode = ((fsmAgent)this.myAgent).nextNode;
		myMap = ((fsmAgent)this.myAgent).getMap();
		NeighborNode =  myMap.getNeighborNode(nextNode).collect(Collectors.toList());
		NodeToBlock = new ArrayList<String>();
		
		if(myMap.getNode(nextNode).getAttribute("ui.class").equals("closed")) {
			
	        for (Node n : NeighborNode) {
	        	
	            if( !n.toString().equals(myPosition) ) {
	            	NodeToBlock.add(n.getId());
	            	System.out.println("Need to block node : "+n);
	            }
	            
	        }
	        
		    if(NodeToBlock.size()==0) {
		    	System.out.println(this.myAgent.getLocalName()+ " ---> I Really blocked a Wumpus");
		    	exitValue = 1;
		    	return ;
		    }
		    if(NodeToBlock.size()>0) {
		    	((fsmAgent)this.myAgent).NodeToBlock = NodeToBlock;
				System.out.println(this.myAgent.getLocalName()+ " ---> I need help to block the Wumpus");
				exitValue = 2;
				return ;
		    }
		    
        }else {
			System.out.println(this.myAgent.getLocalName()+ " ---> I'm not 100% sure that the golem is stuck here, but it doesn't move anymore");
			exitValue = 3;
			return ;
        }
	}

	@Override
	public int onEnd() {
		return exitValue;
	}
}
