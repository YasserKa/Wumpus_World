package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.io.File;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapsTester {

    @Test
    public void simpleMapTest() {

        // Kill wumpus ontop since there's a pit to the right
        Agent.Action [] expectedPlan = {
            Agent.Action.SHOOT, Agent.Action.FORWARD, Agent.Action.TURN_LEFT,
            Agent.Action.FORWARD, Agent.Action.FORWARD, Agent.Action.TURN_LEFT,
            Agent.Action.FORWARD, Agent.Action.GRAB, Agent.Action.TURN_LEFT,
            Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT,
            Agent.Action.FORWARD, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT,
            Agent.Action.FORWARD, Agent.Action.CLIMB};

        try {
            World world = new World(false, false, false, false, new File("maps_test/simple_map.txt"));
            SearchAI agent = new SearchAI(world.getBoard());

            LinkedList<Agent.Action> plan = agent.getPlan();

            int i = 0;

            assertEquals(expectedPlan.length, plan.size());


            for (Agent.Action val : plan) {
                assertEquals(expectedPlan[i], val);
                i++;
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void wumpusInFrontTest() {

        // Not killing Wumpus is faster
        Agent.Action [] expectedPlan = {
            Agent.Action.TURN_LEFT, Agent.Action.FORWARD,
            Agent.Action.TURN_RIGHT, Agent.Action.FORWARD, Agent.Action.FORWARD,
            Agent.Action.TURN_RIGHT, Agent.Action.FORWARD, Agent.Action.GRAB,
            Agent.Action.TURN_LEFT, Agent.Action.TURN_LEFT,
            Agent.Action.FORWARD, Agent.Action.TURN_LEFT, Agent.Action.FORWARD,
            Agent.Action.FORWARD, Agent.Action.TURN_LEFT, Agent.Action.FORWARD,
            Agent.Action.TURN_RIGHT, Agent.Action.CLIMB
        };

        try {
            World world = new World(false, false, false, false, new File("maps_test/wumpusInFront.txt"));
            SearchAI agent = new SearchAI(world.getBoard());

            LinkedList<Agent.Action> plan = agent.getPlan();

            int i = 0;

            assertEquals(expectedPlan.length, plan.size());


            for (Agent.Action val : plan) {
                assertEquals(expectedPlan[i], val);
                i++;
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void wumpusInFrontWithPitOntopTest() {

        // Killing Wumpus is faster
        Agent.Action [] expectedPlan = {Agent.Action.SHOOT, Agent.Action.FORWARD, Agent.Action.FORWARD,
            Agent.Action.GRAB, Agent.Action.TURN_LEFT, Agent.Action.TURN_LEFT,
            Agent.Action.FORWARD, Agent.Action.FORWARD, Agent.Action.CLIMB };


        try {
            World world = new World(false, false, false, false, new File("maps_test/wumpusInFrontWithPitOntopTest.txt"));
            SearchAI agent = new SearchAI(world.getBoard());

            LinkedList<Agent.Action> plan = agent.getPlan();

            int i = 0;

            assertEquals(expectedPlan.length, plan.size());


            for (Agent.Action val : plan) {
                assertEquals(expectedPlan[i], val);
                i++;
            }
        } catch (Exception e) {
        }
    }


    @Test
    public void blockadeAtSecondColumnTest() {

        // Unreachable
        Agent.Action [] expectedPlan = {Agent.Action.CLIMB };

        try {
            World world = new World(false, false, false, false, new File("maps_test/blockadeAtSecondColumn.txt"));
            SearchAI agent = new SearchAI(world.getBoard());

            LinkedList<Agent.Action> plan = agent.getPlan();

            int i = 0;

            assertEquals(expectedPlan.length, plan.size());


            for (Agent.Action val : plan) {
                assertEquals(expectedPlan[i], val);
                i++;
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void goldOnPitTest() {

        // Unreachable
        Agent.Action [] expectedPlan = {Agent.Action.CLIMB };

        try {
            World world = new World(false, false, false, false, new File("maps_test/goldOnPit.txt"));
            SearchAI agent = new SearchAI(world.getBoard());

            LinkedList<Agent.Action> plan = agent.getPlan();

            int i = 0;

            assertEquals(expectedPlan.length, plan.size());


            for (Agent.Action val : plan) {
                assertEquals(expectedPlan[i], val);
                i++;
            }
        } catch (Exception e) {
        }
    }

}
