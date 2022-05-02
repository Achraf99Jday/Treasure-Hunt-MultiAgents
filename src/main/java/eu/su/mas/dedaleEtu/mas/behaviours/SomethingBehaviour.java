package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class SomethingBehaviour extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3620817025717629555L;

	@Override
	public void action() {
		
		ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("ProtocoleSomeone");
		msg.setSender(this.myAgent.getAID());
		System.out.println(this.myAgent.getLocalName()+ " ---> Trying to contact the Something");
		msg.setContent(((fsmAgent)this.myAgent).nextNode);

		for (String agentName : ((fsmAgent)this.myAgent).getList_AgentNames()) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		
	}

}
