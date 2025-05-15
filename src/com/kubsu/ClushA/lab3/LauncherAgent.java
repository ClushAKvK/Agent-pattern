package com.kubsu.ClushA.lab3;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class LauncherAgent extends Agent {

    protected void setup() {
        System.out.println("Инициализация агента-старта...");

        try {
            ContainerController container = getContainerController();

            Object[][] agentParams = {
                    {"Investor1", "investor", new Object[]{"50000", "3","18"}},
                    {"Investor2", "investor", new Object[]{"40000", "1", "12"}},
                    {"Investor3", "investor", new Object[]{"28000", "2", "17"}},
                    {"Investor4", "investor", new Object[]{"50000", "3", "16"}},
                    {"Investor5", "investor", new Object[]{"30000", "1", "12"}},
                    {"Investor6", "investor", new Object[]{"55000", "1", "55"}},
                    {"Condidate1", "condidate", new Object[]{"30000", "65", "12", "1"}},
                    {"Condidate2", "condidate", new Object[]{"25000", "40", "12", "2"}},
                    {"Condidate3", "condidate", new Object[]{"40000", "60", "15", "3"}},
                    {"Condidate4", "condidate", new Object[]{"50000", "50", "20", "3"}},
                    {"Condidate5", "condidate", new Object[]{"47000", "55", "16", "1"}},
                    {"Condidate6", "condidate", new Object[]{"35000", "45", "10", "1"}}
            };

            for (Object[] agentInfo : agentParams) {
                String agentName = (String) agentInfo[0];
                String classSuffix = (String) agentInfo[1];
                Object[] args = (Object[]) agentInfo[2];

                AgentController agent = container.createNewAgent(
                        agentName,
                        "com.kubsu.ClushA.lab3." + capitalize(classSuffix) + "Agent",  // например: agents.InvestorAgent
                        args
                );
                agent.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

