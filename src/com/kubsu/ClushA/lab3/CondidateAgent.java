package com.kubsu.ClushA.lab3;


import com.kubsu.Template.lab2.BookBuyerAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Sink;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;

public class CondidateAgent extends Agent {

    private Integer projectCost;

    private Integer projectNovelty;

    private Integer projectExecMonths;

    private Integer projectTheme;

    private Hashtable<AID, String> investors;

    private AID bestInvestor;

    private Integer bestMaxCost;

    private Integer bestMaxExecMonth;


    protected void setup() {
        // Printout a welcome message
        System.out.println("Condidate-agent "+getAID().getName()+" is ready.");
        investors = new Hashtable<AID, String>();

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            projectCost = Integer.parseInt((String) args[0]);
            projectNovelty = Integer.parseInt((String) args[1]);
            projectExecMonths = Integer.parseInt((String) args[2]);
            projectTheme = Integer.parseInt((String) args[3]);

            System.out.println("Project's criteria: \n" +
                    "\t1. Cost = " + projectCost + "\n" +
                    "\t2. Novelty = " + projectNovelty + "\n" +
                    "\t3. Execution month periods = " + projectExecMonths + "\n" +
                    "\t4. Theme number = " + projectTheme);


            addBehaviour(new CreateProposalBehavior());

            addBehaviour(new ReceiveOfferBehavior());

            addBehaviour(new ChooseOfferBehavior(this, 30000));
        }
        else {
            // Make the agent terminate
            System.out.println("No target project's criteria specified");
            doDelete();
        }
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Condidate-agent "+getAID().getName()+" terminating.");
    }

    //
    private class CreateProposalBehavior extends OneShotBehaviour {
        public void action() {
            System.out.println("Propose the project by " + getAID().getName());
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("project-granting");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                System.out.println("\n" + getAID().getName() + " found the following investor agents:");

                ACLMessage project = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < result.length; ++i) {
                    AID investor = result[i].getName();
                    System.out.println("\t- " + investor.getName());

                    project.addReceiver(investor);
                }
                project.setConversationId("investor-search-for-" + projectTheme);
                project.setContent(getProjectProposal());
                project.setReplyWith("project" + System.currentTimeMillis());
                myAgent.send(project);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    private class ReceiveOfferBehavior extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                investors.put(msg.getSender(), msg.getContent());
            }
            else {
                //System.out.println(getAID().getName() + " rejected offer for reason not target theme");
                block();
            }
        }
    }


    private class ChooseOfferBehavior extends TickerBehaviour {

        public ChooseOfferBehavior(Agent a, long period) {
            super(a, period);
        }

        protected void onTick() {
            if (!investors.isEmpty()) {
                for (AID investor: investors.keySet()) {
                    String offer = investors.get(investor);
                    Integer maxCost = Integer.parseInt(offer.split(";")[0]);
                    Integer maxExecMonth = Integer.parseInt(offer.split(";")[1]);

                    if (bestInvestor == null ||
                            maxCost > bestMaxCost ||
                            (maxCost == bestMaxCost && maxExecMonth > bestMaxExecMonth)) {
                        bestInvestor = investor;
                        bestMaxCost = maxCost;
                        bestMaxExecMonth = maxExecMonth;
                    }
                }

                addBehaviour(new AcceptOfferBehavior());
            }

        }
    }

    private class AcceptOfferBehavior extends OneShotBehaviour {
        public void action() {
            // Reject other offers
            ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (AID investor: investors.keySet()) {
                if (investor == bestInvestor) continue;
                reject.addReceiver(investor);
            }
            reject.setConversationId("reject-offer");
            reject.setContent("reject");
            reject.setReplyWith("project" + System.currentTimeMillis());
            myAgent.send(reject);

            // Accept best offer
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(bestInvestor);
            accept.setConversationId("accept-offer");
            accept.setContent("accept");
            accept.setReplyWith("project" + System.currentTimeMillis());
            myAgent.send(accept);
            System.out.println("✓ Condidate " + getAID().getName() + " accept offer of " + bestInvestor.getName());

            myAgent.doDelete();
        }
    }

    private String getProjectProposal() {
        return this.projectCost + ";"
                + this.projectNovelty + ";"
                + this.projectExecMonths;
    }

}
