package com.kubsu.ClushA.lab3;

import jade.core.AID;
import jade.core.Agent;
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

public class InvestorAgent extends Agent {
    private Integer maxCost;
    private Integer targetTheme;
    private Integer maxExecMonths;

    private Hashtable<AID, String> condidates;

    private AID bestCondidate;
    private Integer bestCost;
    private Integer bestNovelity;
    private Integer bestExecMonths;

    private boolean waitAnswer;


    protected void setup() {
        condidates = new Hashtable<AID, String>();
        waitAnswer = false;
        // Printout a welcome message
        System.out.println("Investor-agent "+getAID().getName()+" is ready.");

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            maxCost = Integer.parseInt((String) args[0]);
            targetTheme = Integer.parseInt((String) args[1]);
            maxExecMonths = Integer.parseInt((String) args[2]);

            System.out.println("Traget investor criteria for theme " + targetTheme + " is:\n" +
                    "\t1. Max. cost = " + maxCost + "\n" +
                    "\t2. Max execution month periods = " + maxExecMonths);


            // Register the book-selling service in the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("project-granting");
            sd.setName("JADE-project-granting");
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
                //System.out.println(getAID().getName() + " create a proposal");
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

            addBehaviour(new RequestPerformer());

            addBehaviour(new AssessmentOfBunnies(this, 20000));

            addBehaviour(new OfferAcceptPerformer());

            addBehaviour(new OfferRejectPerformer());

        }
        else {
            // Make the agent terminate
            System.out.println("No target investor's project criteria specified");
            doDelete();
        }
    }

    private class RequestPerformer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null && msg.getConversationId().equals("investor-search-for-" + targetTheme)) {
                // CFP Message received. Process it
                ACLMessage reply = msg.createReply();
                condidates.put(msg.getSender(), msg.getContent());
            }
            else {
                System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class AssessmentOfBunnies extends TickerBehaviour {
        public AssessmentOfBunnies(Agent a, long period) {
            super(a, period);
        }

        protected void onTick() {
            if (!condidates.isEmpty()) {
                for (AID condidate : condidates.keySet()) {
                    String criteria = condidates.get(condidate);
                    Integer cost = Integer.parseInt(criteria.split(";")[0]);
                    Integer novelty = Integer.parseInt(criteria.split(";")[1]);
                    Integer execMonths = Integer.parseInt(criteria.split(";")[2]);

                    if (cost <= maxCost && execMonths <= maxExecMonths) {
                        if (bestCondidate == null ||
                                cost < bestCost ||
                                (cost == bestCost && novelty > bestNovelity) ||
                                (cost == bestCost && novelty == bestNovelity && execMonths < bestExecMonths)) {
                            bestCondidate = condidate;
                            bestCost = cost;
                            bestNovelity = novelty;
                            bestExecMonths = execMonths;
                        }
                    }
                }

                addBehaviour(new OfferRequest());
            }
        }
    }


    private class OfferRequest extends OneShotBehaviour {
        public void action() {
            if (!waitAnswer && bestCondidate != null) {
                ACLMessage offer = new ACLMessage(ACLMessage.PROPOSE);
                offer.addReceiver(bestCondidate);
                offer.setConversationId("offer-request");
                offer.setContent(getOffer());
                offer.setReplyWith("project" + System.currentTimeMillis());
                myAgent.send(offer);
                System.out.println(condidates.toString());
                System.out.println(bestCondidate.getName());
                System.out.println("Investor " + getAID().getName() + " offered a deal to " + bestCondidate.getName());
                waitAnswer = true;
            }
        }
    }

    private class OfferAcceptPerformer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                myAgent.doDelete();
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class OfferRejectPerformer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                System.out.println("✖ Condidate " + msg.getSender().getName() + " rejected offer of " + getAID().getName());
                condidates.remove(msg.getSender());
                clearBest();
                waitAnswer = false;
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private String getOffer() {
        return maxCost + ";" + maxExecMonths;
    }

    private void clearBest() {
        bestCondidate = null;
        bestCost = null;
        bestExecMonths = null;
        bestNovelity = null;
    }
}
