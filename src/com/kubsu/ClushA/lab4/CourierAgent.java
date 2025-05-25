package com.kubsu.ClushA.lab4;

import com.sun.org.apache.xpath.internal.operations.Or;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;

public class CourierAgent extends Agent {
    private AID administrator;
    private Hashtable<Integer, Order> orders;
    private LinkedList<Integer> queueOrders;
    private Integer currentOrderIdx;

    @Override
    protected void setup() {
        System.out.println("Courier-agent "+getAID().getName()+" is ready.");

        orders = new Hashtable<Integer, Order>();
        queueOrders = new LinkedList<Integer>();
        currentOrderIdx = null;

        getAdministrator();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("pizza-delivery-courier");
        sd.setName("JADE-pizza-delivery-courier");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            //System.out.println(getAID().getName() + " create a proposal");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new GiveNewOrderToDeliveryBehavior());

        addBehaviour(new CompleteOrderBehavior());

        addBehaviour(new OrderExecutionBehavior());
    }

    private class OrderExecutionBehavior extends CyclicBehaviour {

        public void action() {
            if (!queueOrders.isEmpty() && currentOrderIdx == null) {
                currentOrderIdx = queueOrders.pop();
                addBehaviour(new StartProgressOrderBehavior());
                addBehaviour(new InProgressBehavior(myAgent, 15000));

            }
        }
    }

    private class StartProgressOrderBehavior extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage startProgressOrder = new ACLMessage(ACLMessage.INFORM);
            startProgressOrder.addReceiver(administrator);
            startProgressOrder.setConversationId("order-start-progress");
            startProgressOrder.setContent(String.valueOf(currentOrderIdx));
            startProgressOrder.setReplyWith("order" + System.currentTimeMillis());
            myAgent.send(startProgressOrder);
        }
    }

    private class InProgressBehavior extends WakerBehaviour {


        public InProgressBehavior(Agent a, long timeout) {
            super(a, timeout);

            System.out.println("Courier " + getName() + " start delivery of " + currentOrderIdx + " order");
        }

        @Override
        protected void handleElapsedTimeout() {
            try {
                System.out.println("Order " + currentOrderIdx + " was ordered to " + orders.get(currentOrderIdx).client.getName());
                orders.get(currentOrderIdx).setStatus(OrderStatus.ORDERED);
                ACLMessage startProgressOrder = new ACLMessage(ACLMessage.CFP);
                startProgressOrder.addReceiver(orders.get(currentOrderIdx).client);
                startProgressOrder.setConversationId("order-waiting-payment");
                startProgressOrder.setContent(String.valueOf(orders.get(currentOrderIdx).toString()));
                startProgressOrder.setReplyWith("order" + System.currentTimeMillis());
                myAgent.send(startProgressOrder);

//                orders.remove(currentOrderIdx);
//                currentOrderIdx = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class CompleteOrderBehavior extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println("Courier " + getName() + " gave money from " + msg.getSender().getName());
                ACLMessage completeOrder = new ACLMessage(ACLMessage.CONFIRM);
                completeOrder.addReceiver(administrator);
                completeOrder.setConversationId("confirm-order-payment");
                completeOrder.setContent(String.valueOf(currentOrderIdx));
                completeOrder.setReplyWith("order" + System.currentTimeMillis());
                myAgent.send(completeOrder);


                orders.get(currentOrderIdx).setStatus(OrderStatus.RECEIVED);
                queueOrders.remove(currentOrderIdx);
                currentOrderIdx = null;
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class GiveNewOrderToDeliveryBehavior extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                                                     MessageTemplate.MatchConversationId("order-to-delivery"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println(msg.getContent());
                String[] content = msg.getContent().split("\\|");
                Order newOrder = Order.getOrder(content[0]);
                Integer newOrderIdx = Integer.parseInt(content[1]);

                orders.put(newOrderIdx, newOrder);
                queueOrders.add(newOrderIdx);

                System.out.println("Courier " + getName() + " received new order " + newOrderIdx + " to delivery");
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private void getAdministrator() {
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
    }
}
