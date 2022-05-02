package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * This example behaviour try to send a hello message (every 3s maximum) to agents Collect2 Collect1
 * @author hc
 *
 */
public class SayHelloBehaviour extends OneShotBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2058134622078521998L;
	
	private List<String> receivers;
	/**
	 * An agent tries to contact its friend and to give him its current position
	 * @param myagent the agent who posses the behaviour
	 *  
	 */
	public SayHelloBehaviour (final Agent myagent,List<String> receivers) {
		super(myagent);
		this.receivers=receivers;	
		//super(myagent);
	}

	@Override
	public void action() {

		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("ProtocolePoke");
		msg.setSender(this.myAgent.getAID());
		//System.out.println(this.myAgent.getLocalName()+ " ---> Is trying to reach its friends");
		msg.setContent(this.myAgent.getLocalName()+" ---> Hello World, I'm at "+myPosition);

		for (String agentName : receivers) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		
		
	}

}