package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.fsmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;

public class InitMapBehaviour extends OneShotBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7071552183965858387L;

	@Override
	public void action() {
		MapRepresentation myMap= new MapRepresentation();
		
		((fsmAgent)this.myAgent).updateMap(myMap);
		
		System.out.println(this.myAgent.getLocalName()+" ---> Map Created");
	}
	

}
