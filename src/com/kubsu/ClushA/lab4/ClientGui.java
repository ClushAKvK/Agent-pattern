package com.kubsu.ClushA.lab4;

import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

/**
 * GUI for Pizza Client Agent
 */
class ClientGui extends JFrame {
    private ClientAgent myAgent;

    private JComboBox productTypeCombo;
    private JTextField quantityField;
    private JTextField addressField;
    private JTextField priceField;

    // Пример списка пицц (можно загружать из агента)
    private ArrayList<String> productTypes;
    private ArrayList<Integer> productPrices; // Цены соответствующих пицц

    ClientGui(ClientAgent agent, String menu) {
        super(agent.getLocalName());
        productTypes = new ArrayList<String>();
        productPrices = new ArrayList<Integer>();

        myAgent = agent;

        parseMenu(menu);

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(4, 2));

        // Выбор типа пиццы
        p.add(new JLabel("Pizza type:"));
        productTypeCombo = new JComboBox(productTypes.toArray());
        productTypeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientGui.this.updatePrice();
            }
        });
        p.add(productTypeCombo);

        // Ввод количества
        p.add(new JLabel("Quantity:"));
        quantityField = new JTextField("1", 5);
        quantityField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientGui.this.updatePrice();
            }
        });
        p.add(quantityField);

        // адресс
        p.add(new JLabel("Address:"));
        addressField = new JTextField("", 15);
        addressField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientGui.this.updatePrice();
            }
        });
        p.add(addressField);

        // Поле для отображения цены
        p.add(new JLabel("Total price:"));
        priceField = new JTextField(10);
        priceField.setEditable(false);
        p.add(priceField);

        getContentPane().add(p, BorderLayout.CENTER);

        // Кнопка заказа
        JButton orderButton = new JButton("Order");
        orderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    int pizzaType = productTypeCombo.getSelectedIndex();
                    int quantity = Integer.parseInt(quantityField.getText().trim());
                    int cost = productPrices.get(pizzaType) * quantity;
                    String address = addressField.getText().trim();


                    if (quantity <= 0) {
                        throw new NumberFormatException("Quantity must be positive");
                    }

                    myAgent.placeOrder(pizzaType + 1, quantity, cost, address);
                    quantityField.setText("1");
                    updatePrice();

                    JOptionPane.showMessageDialog(ClientGui.this,
                            "Thank you! You make order: " + pizzaType + " for " + quantity, "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(ClientGui.this,
                            "Invalid values. " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(orderButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
        updatePrice(); // Инициализация цены
    }

    // Обновление отображаемой цены
    private void updatePrice() {
        try {
            int selectedIndex = productTypeCombo.getSelectedIndex();
            int quantity = Integer.parseInt(quantityField.getText().trim());

            if (quantity <= 0) {
                priceField.setText("Invalid quantity");
                return;
            }

            int totalPrice = productPrices.get(selectedIndex) * quantity;
            priceField.setText(totalPrice + " rub");
        } catch (NumberFormatException e) {
            priceField.setText("Invalid quantity");
        }
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

    private void getProductsActual() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("pizza-delivery");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            ACLMessage pizzaRequest = new ACLMessage(ACLMessage.REQUEST);
            AID administrator = result[0].getName();
            pizzaRequest.addReceiver(administrator);
            pizzaRequest.setConversationId("get-menu");
            pizzaRequest.setContent("");
            pizzaRequest.setReplyWith("project" + System.currentTimeMillis());
            myAgent.send(pizzaRequest);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void parseMenu(String menu) {
        int idx = 0;
        for (String row : menu.split("\n")) {
            this.productTypes.add(idx, row.split(";")[0]);
            this.productPrices.add(idx, Integer.parseInt(row.split(";")[1]));
            idx++;
        }
    }
}
