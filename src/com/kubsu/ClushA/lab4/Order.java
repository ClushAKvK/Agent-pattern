package com.kubsu.ClushA.lab4;

import jade.core.AID;

public class Order {
    AID client;
    int type;
    int quantity;
    int cost;
    String address;

    private OrderStatus status;

    Order (AID client, int type, int quantity, int cost, String address) {
        this.client = client;
        this.type = type;
        this.quantity = quantity;
        this.cost = cost;
        this.address = address;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return client.getName()+ ";" + type + ";" + quantity + ";" + cost + ";" + address;
    }

    public static Order getOrder(String str) {
        String[] content = str.split(";");
        AID client = new AID(content[0], AID.ISGUID);
        int type = Integer.parseInt(content[1]);
        int quantity = Integer.parseInt(content[2]);
        int cost = Integer.parseInt(content[3]);
        String address = content[4];

        return new Order(client, type, quantity, cost, address);
    }
}
