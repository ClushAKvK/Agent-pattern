package com.kubsu.ClushA.lab4;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AdministratorGui extends JFrame {
    private final AdministratorAgent myAgent;

    private final JTextField nameField, priceField;
    private final JLabel totalOrdersLabel, deliveredLabel, deliveringLabel, totalRevenueLabel;

    public AdministratorGui(AdministratorAgent agent) {
        super("Pizza Network Admin");
        myAgent = agent;

        // Панель добавления пиццы
        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Добавить пиццу"));
        inputPanel.add(new JLabel("Название пиццы:"));
        nameField = new JTextField(15);
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Цена:"));
        priceField = new JTextField(15);
        inputPanel.add(priceField);
        getContentPane().add(inputPanel, BorderLayout.NORTH);

        JButton addButton = new JButton("Добавить");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String name = nameField.getText().trim();
                    int price = Integer.parseInt(priceField.getText().trim());
                    myAgent.addProductToMenu(new Product(name, price));
                    nameField.setText("");
                    priceField.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AdministratorGui.this,
                            "Ошибка: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);

        // Панель статистики
        JPanel statsPanel = new JPanel(new GridLayout(4, 1));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Статистика заказов"));
        totalOrdersLabel = new JLabel("Всего заказов: 0");
        deliveredLabel = new JLabel("Доставлено: 0");
        deliveringLabel = new JLabel("В доставке: 0");
        totalRevenueLabel = new JLabel("Общая сумма: 0₽");

        statsPanel.add(totalOrdersLabel);
        statsPanel.add(deliveredLabel);
        statsPanel.add(deliveringLabel);
        statsPanel.add(totalRevenueLabel);

        getContentPane().add(statsPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void updateStats(int totalOrders, int delivered, int delivering, int totalRevenue) {
        totalOrdersLabel.setText("Всего заказов: " + totalOrders);
        deliveredLabel.setText("Доставлено: " + delivered);
        deliveringLabel.setText("В доставке: " + delivering);
        totalRevenueLabel.setText("Общая сумма: " + totalRevenue + "₽");
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getWidth() / 2,
                screenSize.height / 2 - getHeight() / 2);
        super.setVisible(true);
    }
}

