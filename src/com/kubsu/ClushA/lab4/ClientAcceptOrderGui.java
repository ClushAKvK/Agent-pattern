package com.kubsu.ClushA.lab4;

import jade.core.AID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * GUI for displaying delivered order and payment
 */
class ClientAcceptOrderGui extends JFrame {
    private ClientAgent myAgent;

    private AID courier;

    private JLabel orderInfoLabel;
    private JLabel priceLabel;
    private JLabel addressLabel;
    private JLabel statusLabel;

    ClientAcceptOrderGui(ClientAgent agent, AID courierAgent, Order deliveredOrder) {
        super(agent.getLocalName() + " - Order Delivery");

        myAgent = agent;
        courier = courierAgent;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(4, 1, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Информация о заказе
        orderInfoLabel = new JLabel("Your order: " + deliveredOrder.type + " x " + deliveredOrder.quantity);
        orderInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        p.add(orderInfoLabel);

        // Цена
        priceLabel = new JLabel("Total price: " + deliveredOrder.cost + " rub");
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        p.add(priceLabel);

        // Адрес доставки
        addressLabel = new JLabel("Delivery address: " + deliveredOrder.address);
        addressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        p.add(addressLabel);

        // Статус
        statusLabel = new JLabel("Status: Delivered successfully!");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusLabel.setForeground(Color.GREEN);
        p.add(statusLabel);

        getContentPane().add(p, BorderLayout.CENTER);

        // Кнопка оплаты
        JButton payButton = new JButton("Pay Now");
        payButton.setBackground(new Color(76, 175, 80));
        payButton.setForeground(Color.WHITE);
        payButton.setFont(new Font("Arial", Font.BOLD, 14));
        payButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                myAgent.payForOrder(courier);
                JOptionPane.showMessageDialog(ClientAcceptOrderGui.this,
                        "Payment successful! Thank you for your order!",
                        "Payment Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                myAgent.doDelete(); // Удаляем агента после оплаты
                dispose(); // Закрываем окно
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(payButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }
}
