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

        Agent.Action [] expectedPlan = {
        Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT, Agent.Action.FORWARD, Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.FORWARD, Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.GRAB, Agent.Action.TURN_LEFT, Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT, Agent.Action.FORWARD, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT, Agent.Action.FORWARD, Agent.Action.TURN_LEFT, Agent.Action.FORWARD, Agent.Action.TURN_RIGHT, Agent.Action.CLIMB};

        try {
            World world = new World(false, false, false, false, new File("maps/simple_map.txt"));
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
