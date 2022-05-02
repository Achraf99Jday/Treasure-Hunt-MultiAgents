package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class ImNotWumpus extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1473102498604445208L;

	private int exitValue;

	@Override
	public void action() {
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		int lastExit = ((fsmAgent)this.myAgent).getFSM().getLastExitValue();
		exitValue = 0;
		if (lastExit != 7) {
			exitValue = 1;
			
			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("ProtocoleWaitSomeone");
			msg.setSender(this.myAgent.getAID());
			System.out.println(this.myAgent.getLocalName()+ " ---> Are you a Wumpus ? ");
			msg.setContent(myPosition);

			for(String n: ((fsmAgent)this.myAgent).getList_AgentNames()) {
				msg.addReceiver(new AID(n,AID.ISLOCALNAME));
			}
			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
		}else {
			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("ProtocoleWaitSomeone");
			msg.setSender(this.myAgent.getAID());
			System.out.println(this.myAgent.getLocalName()+ " ---> I'm not a Wumpus "+((fsmAgent)this.myAgent).agentToContact);
			msg.setContent(myPosition);


			msg.addReceiver(new AID(((fsmAgent)this.myAgent).agentToContact,AID.ISLOCALNAME));
			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		}
		
	}
	
	@Override
	public int onEnd() {
		return exitValue;
	}
}
