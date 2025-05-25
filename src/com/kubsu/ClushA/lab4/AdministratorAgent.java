package com.kubsu.ClushA.lab4;

import com.sun.org.apache.xpath.internal.operations.Or;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Hashtable;

public class AdministratorAgent extends Agent {

    private Hashtable<AID, String> couriers;

    public static Hashtable<Integer, Product> menu;

    private Hashtable<Integer, Order> orders;

    private AdministratorGui administratorGui;

    private Hashtable<AID, Integer> courierEmployment;

    private Order newOrder;


    protected void setup() {
        // Printout a welcome message
        System.out.println("Administrator-agent "+getAID().getName()+" is ready.");
        couriers = new Hashtable<AID, String>();
        menu = new Hashtable<Integer, Product>();
        orders = new Hashtable<Integer, Order>();
        courierEmployment = new Hashtable<AID, Integer>();

        newOrder = null;
        administratorGui = new AdministratorGui(this);
        // Get the title of the book to buy as a start-up argument
        // Register the administrator-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("pizza-delivery-admin");
        sd.setName("JADE-pizza-delivery-admin");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            //System.out.println(getAID().getName() + " create a proposal");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        administratorGui.showGui();

        addBehaviour(new ProposeMenuBehavior());

        addBehaviour(new GiveOrderBehavior());

        addBehaviour(new StartProgressOrderBehavior());

        addBehaviour(new CompleteProgressOrderBehavior());
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Administrator-agent "+getAID().getName()+" terminating.");
    }

    public void addProductToMenu(Product newProduct) {
        int typeId = menu.size() + 1;
        menu.put(typeId, newProduct);
        System.out.println("Administrator release new product: \n" +
                                "\tname  = " + newProduct.getName() + "\n" +
                                "\tprice = " + newProduct.getPrice());
    }

    private class ProposeMenuBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                String msgReply = "";
                for (Product product: menu.values()) {
                    msgReply += product.toString() + "\n";
                }
                reply.setContent(msgReply);
                myAgent.send(reply);
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class GiveOrderBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();

                AID client = msg.getSender();
                int type = Integer.parseInt(content.split(";")[0]);
                int quantity = Integer.parseInt(content.split(";")[1]);
                int cost = menu.get(type).getPrice() * quantity;
                String address = content.split(";")[2];
                newOrder = new Order(client, type, quantity, cost, address);
                newOrder.setStatus(OrderStatus.ORDERED);
                orders.put(orders.size() + 1, newOrder);

                System.out.println("Admin " + getName() + " gave a new order: \n" +
                                    "\tType = " + type + "\n" +
                                    "\tQuantity = " + quantity + "\n" +
                                    "\tTotal cost = " + cost + "\n" +
                                    "\tAddress = " + address);

                updateTotalStats();
                addBehaviour(new ChooseCourierBehavior());
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class ChooseCourierBehavior extends OneShotBehaviour {

        @Override
        public void action() {
            AID bestCourier = null;
            int bestOrderCount = Integer.MAX_VALUE;
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("pizza-delivery-courier");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; i++) {
                    AID courier = result[i].getName();
                    if (!courierEmployment.containsKey(courier)) {
                        courierEmployment.put(courier, 0);
                        bestCourier = courier;
                        bestOrderCount = 0;
                        break;
                    }
                    else if (bestCourier == null || (courierEmployment.get(courier) < bestOrderCount)) {
                        bestCourier = courier;
                        bestOrderCount = courierEmployment.get(courier);
                    }
                }
                courierEmployment.put(bestCourier, bestOrderCount + 1);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

            System.out.println("Admin choose the best courier " + bestCourier.getName() + " to delivery of " + orders.size());

            final AID finalBestCourier = bestCourier;
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage startProgressOrder = new ACLMessage(ACLMessage.CFP);
                    startProgressOrder.addReceiver(finalBestCourier);
                    startProgressOrder.setConversationId("order-to-delivery");
                    startProgressOrder.setContent(newOrder.toString()+"|"+orders.size());
                    startProgressOrder.setReplyWith("order" + System.currentTimeMillis());
                    System.out.println("Send delivery request for " + finalBestCourier.getName());
                    myAgent.send(startProgressOrder);
                    newOrder = null;
                }
            });
        }
    }

    private class StartProgressOrderBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                Integer orderIdx = Integer.parseInt(msg.getContent());
                orders.get(orderIdx).setStatus(OrderStatus.IN_PROGRESS);
                updateTotalStats();
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private class CompleteProgressOrderBehavior extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                Integer orderIdx = Integer.parseInt(msg.getContent());
                orders.get(orderIdx).setStatus(OrderStatus.COMPLETE);
                updateTotalStats();

                courierEmployment.put(msg.getSender(), courierEmployment.get(msg.getSender()) - 1);

                System.out.println("Complete order " + orderIdx);
            }
            else {
                //System.out.println(getAID().getName() + " rejected proposal for reason not target theme");
                block();
            }
        }
    }

    private void updateTotalStats() {
        int totalOrdersCount = 0;
        int ordersInProgress = 0;
        int totalOrdersCompleted = 0;
        int totalOrdersSum = 0;

        for (Order order : orders.values()) {
            totalOrdersCount++;
            if (order.getStatus() == OrderStatus.IN_PROGRESS) {
                ordersInProgress++;
            }
            else if (order.getStatus() == OrderStatus.COMPLETE) {
                totalOrdersCompleted++;
                totalOrdersSum += order.cost;
            }
        }

        administratorGui.updateStats(totalOrdersCount, totalOrdersCompleted, ordersInProgress, totalOrdersSum);
    }

}
