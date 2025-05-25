package com.kubsu.ClushA.lab4;

import com.kubsu.Template.lab2.BookSellerAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;

public class ClientAgent extends Agent {

    private Order order;

    private AID administrator;

    private ClientGui makeOrderGui;
    
    private ClientAcceptOrderGui acceptOrderGui;

    protected void setup() {
        // Printout a welcome message
        System.out.println("Client-agent "+getAID().getName()+" is ready.");

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("pizza-delivery-admin");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            administrator = result[0].getName();
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        Object[] args = getArguments();
//		System.out.println(args[1].toString());
        if (args != null && args.length > 0) {
            order.type = Integer.parseInt((String) args[0]);
            order.quantity = Integer.parseInt((String) args[1]);
            order.cost = Integer.parseInt((String) args[2]);
            order.address = (String) args[3];

            placeOrder(order.type, order.quantity, order.cost, order.address);
        }
        else {
            addBehaviour(new RequestMenuBehavior());
        }
        // Get the title of the book to buy as a start-up argument
        addBehaviour(new RequestForPaymentOrderBehavior());
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Client-agent "+getAID().getName()+" terminating.");
    }

    private class RequestMenuBehavior extends SimpleBehaviour {

        private Boolean requestSent = false;
        private boolean menuReceived = false;

        @Override
        public void action() {
            if (!requestSent) {
                try {
                    ACLMessage pizzaRequest = new ACLMessage(ACLMessage.REQUEST);
                    pizzaRequest.addReceiver(administrator);
                    pizzaRequest.setConversationId("get-menu");
                    pizzaRequest.setContent("");
                    pizzaRequest.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(pizzaRequest);
                    requestSent = true;
                }
                catch (Exception fe) {
                    fe.printStackTrace();
                }
            }
            else {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    // CFP Message received. Process it
                    String menu = msg.getContent();

                    makeOrderGui = new ClientGui((ClientAgent) myAgent, menu);
                    makeOrderGui.showGui();
                    menuReceived = true;

                }
                else {
                    //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                    block();
                }
            }

        }

        @Override
        public boolean done() {

            return requestSent && menuReceived;
        }
    }

    private class RequestForPaymentOrderBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String content = msg.getContent();
                Order deliveredOrder = Order.getOrder(content);
                acceptOrderGui = new ClientAcceptOrderGui((ClientAgent) myAgent, msg.getSender(), deliveredOrder);
                System.out.println("Waiting payments by " + getName() + " for delivery");
                acceptOrderGui.showGui();
//                Order deliveredOrder = new Order(myAgent.getAID(), Integer.parseInt(content[1]),
//                                                 Integer.parseInt(content[2]), Integer.parseInt(content[3]), content[4]);
//

            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    public void payForOrder(final AID courier) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage pizzaRequest = new ACLMessage(ACLMessage.AGREE);
                pizzaRequest.addReceiver(courier);
                pizzaRequest.setConversationId("accept-order");
                pizzaRequest.setContent("payment");
                pizzaRequest.setReplyWith("order" + System.currentTimeMillis());
                myAgent.send(pizzaRequest);

                myAgent.doDelete();
            }
        });
    }

    void placeOrder(final int productType, final int quantity, final int cost, final String address) {
        order = new Order(getAID(), productType, quantity, cost, address);
//        order.type = productType;
//        order.quantity = productType;
//        order.address = address;
        order.setStatus(OrderStatus.WAITING);
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("Client " + getName() + " make order " + productType + " of " + quantity + ", address = " + address);
                // Accept best offer
                ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(administrator);
                accept.setConversationId("make-order");
                accept.setContent(productType+";"+quantity+";"+address);
                accept.setReplyWith("order" + System.currentTimeMillis());
                myAgent.send(accept);
            }
        });


    }
}
