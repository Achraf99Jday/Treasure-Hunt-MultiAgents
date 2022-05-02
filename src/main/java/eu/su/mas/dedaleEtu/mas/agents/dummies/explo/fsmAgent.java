package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Node;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.CheckWumpusBlockedBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.DumbChaseBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ImNotWumpus;
import eu.su.mas.dedaleEtu.mas.behaviours.InitMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.MoveToBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.NeedHelpBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHelloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SomethingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.StrangeWaitBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SuccessBlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.WaitSomethingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public class fsmAgent extends AbstractDedaleAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1161691655438824095L;
	private MapRepresentation myMap;
	
	public boolean move=true, succesMerge=false, changeNode=false, successBlock=false, forceChangeNode=false, endExplo=false, needToCheck=false;
	public List<String> NodeToBlock, blockedAgent= new ArrayList<String>(), GolemPoop = new ArrayList<String>();
	public String nextNode, agentToContact, moveTo, WumpusPos;
	
	private static final int PokeTime = 3000;
	
	/*
	 * If you increase the speed, that is to say by lowering the time of the doWait, 
	 * then it will be necessary to increase the sensitivity of the agent 
	 * to leave the time that our agents to share their card, 
	 * which is fixed in hard has check every 5000ms.
	 * And if you lower the speed of the agents, 
	 * then you should lower the sensitivity so that our agents are not stuck in place for too long!	
	*/
	public final int AgentSpeed=300;//doWait every AgentSpeed ms
	public final int AgentSensitivity=20;//number of times it is blocked before checking if it is a golem (Only on ExploCoopBehaviour), on DumbChase AgentSinsitivity = 1, for optimize the chase mode
	
	private int nbAgent;
	
	private List<String> list_agentNames;
	
	private List<Behaviour> lb;
	
	private FSMBehaviour fsm;
	
	private static final String INIT = "initMap";
	private static final String A = "Exploration";
	private static final String B = "Poke";
	private static final String C = "ShareMap";
	private static final String D = "ReceiveMap";
	private static final String E = "Chase";
	private static final String F = "CheckWumpus";
	private static final String G = "SuccessBlock";
	private static final String H = "NeedHelpToBlock";
	private static final String I = "StrangeWait";
	private static final String J = "NotGolem";
	private static final String K = "Semething";
	private static final String L = "WaitSomething";
	private static final String M = "MoveTo";
	private static final String Z = "End";

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		final Object[] args = getArguments();
		
		List<String> agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				agentNames.add((String)args[i]);
				i++;
			}
		}
		this.nbAgent = agentNames.size();
		this.list_agentNames = agentNames;

		fsm = new FSMBehaviour(this);
		// Define the different states and behaviours
		fsm.registerFirstState(new InitMapBehaviour(),INIT);
		fsm.registerState(new ExploCoopBehaviour(this,this.myMap, PokeTime), A);
		fsm.registerState(new SayHelloBehaviour(this, list_agentNames), B);
		fsm.registerState(new ShareMapBehaviour(this, this.myMap, this.list_agentNames), C);
		fsm.registerState(new ReceiveMapBehaviour(this), D);
		fsm.registerState(new DumbChaseBehaviour(this,this.myMap, PokeTime), E);
		fsm.registerState(new CheckWumpusBlockedBehaviour(this),F);
		fsm.registerState(new SuccessBlockBehaviour(this),G);
		fsm.registerState(new NeedHelpBehaviour(this),H);
		fsm.registerState(new StrangeWaitBehaviour(this),I);
		fsm.registerState(new ImNotWumpus(),J);
		fsm.registerState(new SomethingBehaviour(),K);
		fsm.registerState(new WaitSomethingBehaviour(),L);
		fsm.registerState(new MoveToBehaviour(),M);
		fsm.registerLastState(new EndBehaviour(), Z);
		
		// Register the transitions
		fsm.registerDefaultTransition(INIT,A);//Wait to receive map
		// A -> ExploCoop
		fsm.registerDefaultTransition(A,A);//Back to explo
		fsm.registerTransition(A,B, 1) ;//poke every PokeTime
		fsm.registerTransition(A,C, 2) ;//Receive msg to ShareMap
		fsm.registerTransition(A,E, 4) ;//End Explo, go to chase, or go to check if is a wumpus
		fsm.registerTransition(A,H, 5) ;//Go to help block
		fsm.registerTransition(A,M, 6) ;//Go to MoveTo, to block a node
		// B -> SayHello
		fsm.registerDefaultTransition(B,A);//Back to explo
		// C -> ShareMap
		fsm.registerDefaultTransition(C,D);//Go to wait to receive map
		// D -> ReceiveMap
		fsm.registerDefaultTransition(D,D) ;//wait to receive a msg, max 5000ms waiting
		fsm.registerTransition(D,A, 1) ;//Back to explo
		fsm.registerTransition(D,G, 2) ;//Back to SuccessBlock
		// E -> DumbChase
		fsm.registerDefaultTransition(E,E) ;//Back to Chase
		fsm.registerTransition(E,K, 3) ;//Go to fast check if is Wumpus or not
		fsm.registerTransition(E,C, 4) ;//Share Map to every agent one time
		fsm.registerTransition(E,M, 5) ;//go to MoveTo, for help to block a note
		fsm.registerTransition(E,H, 6) ;//Go directly to needHelp Behaviour
		fsm.registerTransition(E,J, 7) ;//Go to confirm he is not a Wumpus
		fsm.registerTransition(E,A, 8) ;//Go back to exploration
		// F -> CheckWumpusBlocked
		fsm.registerDefaultTransition(F,A) ;//Back to Explo		
		fsm.registerTransition(F,G, 1) ;//go to succes block
		fsm.registerTransition(F,H, 2) ;//go to need help block
		fsm.registerTransition(F,I, 3) ;//go to strange wait
		// G -> SuccessBlocked
		fsm.registerDefaultTransition(G,G) ;//wait to success block
		fsm.registerTransition(G,C, 1) ;//Error SuccessBlock, back to explo
		fsm.registerTransition(G,A, 2) ;//ShareMap
		// H -> NeedHelp
		fsm.registerDefaultTransition(H,H) ;//wait to need help block
		fsm.registerTransition(H,G, 1) ;//go to SuccessBlock
		fsm.registerTransition(H,A, 2) ;//Error block, back to explo
		// I -> StrangeWait
		fsm.registerDefaultTransition(I,I) ;//wait to strange Wait
		fsm.registerTransition(I,A, 1) ;//back to Explo
		// J -> ImNotWumpus
		fsm.registerDefaultTransition(J,E) ;//back to chase
		fsm.registerTransition(J,L, 1) ;//go to wait Something
		// K -> Something
		fsm.registerDefaultTransition(K,J) ;//Go to ImNotWumpus
		// L -> WaitSomethin
		fsm.registerDefaultTransition(L,L) ;//wait Something, max wait 5000ms 
		fsm.registerTransition(L,E, 1) ;//back to chase
		fsm.registerTransition(L,F, 2) ;//go to ChekWumpusBlocked
		// M -> MoveTo
		fsm.registerDefaultTransition(M,M) ;//continue to moveTo nodeGoal
		fsm.registerTransition(M,H, 10) ;//arrived to nodeGoal
		fsm.registerTransition(M,A, 2) ;//moveTo fail mission, back to explo
		
		// this is not used
		fsm.registerTransition(E,Z, 99) ;//END
		
		this.lb=new ArrayList<Behaviour>();
		this.lb.add(fsm);
		addBehaviour(new startMyBehaviours(this,this.lb));

		System.out.println("the  agent "+this.getLocalName()+ " is started mode FSM");

	}
	
	public void updateMap(MapRepresentation Map) {
		this.myMap = Map;
	}
	
	public MapRepresentation getMap() {
		return this.myMap;
	}
	
	public List<String> getList_AgentNames(){
		return this.list_agentNames;
	}
	
	public List<Behaviour> getLB(){
		return this.lb;
	}
	
	public boolean getFinished() {
		if (!this.myMap.hasOpenNode()){
			return true;
		}
		return false;
	}
	
	public int getNbAgent() {
		return this.nbAgent;
	}
	
	public FSMBehaviour getFSM() {
		return fsm;
	}
}
