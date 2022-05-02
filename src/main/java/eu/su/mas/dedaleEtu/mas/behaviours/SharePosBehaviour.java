package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class SharePosBehaviour extends OneShotBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4037647836519371207L;
	
	private String myNextNode;
	
	public SharePosBehaviour(final Agent myagent,String myNextNode) {
		super(myagent);
		this.myNextNode = myNextNode;
	}
	@Override
	public void action() {
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("ProtocolePos");
		msg.setSender(this.myAgent.getAID());
		//System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
		msg.setContent(myPosition+','+this.myNextNode);

		for (String agentName : ((ExploreCoopAgent)this.myAgent).getList_AgentNames()) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		System.out.println(this.myAgent.getLocalName()+" is not walking and send her pos "+myPosition);

		//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		
	}

}
