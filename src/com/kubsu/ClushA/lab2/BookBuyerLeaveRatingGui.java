package com.kubsu.ClushA.lab2;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class BookBuyerLeaveRatingGui extends JFrame {
	private Agent myAgent;
	private AID seller;

	private JToggleButton[] ratingButtons = new JToggleButton[10];

	BookBuyerLeaveRatingGui(Agent a, AID aid) {
		super(a.getLocalName());

		this.myAgent = a;
		this.seller = aid;

		JPanel ratingPanel = new JPanel();
		ratingPanel.setLayout(new GridLayout(2, 10, 5, 5)); // 2 строки: кнопки + подписи
		ButtonGroup ratingGroup = new ButtonGroup();

		JLabel[] ratingLabels = new JLabel[10];

		for (int i = 0; i < 10; i++) {
			final int rating = i + 1;
			ratingButtons[i] = new JToggleButton(rating + " ★");
			ratingGroup.add(ratingButtons[i]);
			ratingPanel.add(ratingButtons[i]);
		}
		for (int i = 0; i < 10; i++) {
			ratingLabels[i] = new JLabel(ratingDescription(i + 1), SwingConstants.CENTER);
			ratingPanel.add(ratingLabels[i]);
		}

		getContentPane().add(ratingPanel, BorderLayout.CENTER);

		JButton submitButton = new JButton("Submit Rating");
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				int selectedRating = -1;
				for (int i = 0; i < ratingButtons.length; i++) {
					if (ratingButtons[i].isSelected()) {
						selectedRating = i + 1;
						break;
					}
				}
				if (selectedRating != -1) {
					//myAgent.submitRating(selectedRating);
					ACLMessage rate = new ACLMessage(ACLMessage.INFORM);
					rate.addReceiver(seller);
					rate.setContent(String.valueOf(selectedRating));
					rate.setConversationId("rate-trade");
					rate.setReplyWith("rate" + System.currentTimeMillis());
					myAgent.send(rate);

					System.out.println(myAgent.getName() + " rate " + seller.getName() + "as " + selectedRating);

					JOptionPane.showMessageDialog(BookBuyerLeaveRatingGui.this,
							"Thank you! You rated: " + selectedRating + " ★", "Success", JOptionPane.INFORMATION_MESSAGE);
					dispose();
				} else {
					JOptionPane.showMessageDialog(BookBuyerLeaveRatingGui.this,
							"Please select a rating.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.add(submitButton);
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		// Closing window = kill agent
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		});

		setResizable(false);
	}

	private String ratingDescription(int rating) {
		switch (rating) {
			case 1: return "Terrible";
			case 2: return "Very Bad";
			case 3: return "Bad";
			case 4: return "Poor";
			case 5: return "Average";
			case 6: return "Fine";
			case 7: return "Good";
			case 8: return "Very Good";
			case 9: return "Excellent";
			case 10: return "Perfect";
			default: return "";
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
}
