package fullObservability;

import wumpus.Agent;
import wumpus.World;

// import java.util.LinkedList;
// import java.util.ListIterator;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.PriorityQueue;
import java.util.*;
import java.lang.Math;

// mvn install; mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"
// mvn install; mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-f maps"
//
// mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"

public class SearchAI extends Agent {
    private ListIterator<Action> planIterator;
    private Dictionary<State, State> cameFrom = new Hashtable<State, State>();
    private Dictionary<State, Integer> costSoFar = new Hashtable<State, Integer>();

    public LinkedList<Agent.Action> reconstructPath(Dictionary<State, State> cameFrom, State start, State goal) {
        State current = goal;
        LinkedList<Agent.Action> path = new LinkedList<Agent.Action>();
        LinkedList<Agent.Action> movingBackPath = new LinkedList<Agent.Action>();

        if (goal == null) {
            path.add(Agent.Action.CLIMB);
            return path;
        }


        movingBackPath.add(Agent.Action.TURN_LEFT);
        movingBackPath.add(Agent.Action.TURN_LEFT);

        while (current != start) {
            Agent.Action madeAction =  current.getActionMade();
            path.addFirst(madeAction);
            current = cameFrom.get(current);
            // create the "going back from starting point" path
            if (madeAction == Agent.Action.TURN_RIGHT)
                movingBackPath.add(Agent.Action.TURN_LEFT);
            if (madeAction == Agent.Action.TURN_LEFT)
                movingBackPath.add(Agent.Action.TURN_RIGHT);
            if (madeAction == Agent.Action.FORWARD)
                movingBackPath.add(Agent.Action.FORWARD);
        }

        movingBackPath.add(Agent.Action.CLIMB);


        path.addAll(movingBackPath);
        return path;
    }

    public LinkedList<Action> AStarSearch(World.Tile[][] board) throws CloneNotSupportedException{

        Queue <StateCostPair> frontier = new PriorityQueue<StateCostPair>();
        State initState = new State(board);
        State goalState = null;

        frontier.add(new StateCostPair(initState, 0));

        cameFrom.put(initState, initState);
        costSoFar.put(initState, 0);
        initState.printWorld(initState.getWorld());

        int tmp = 20;
        while (frontier.peek() != null) {
            State currState = frontier.poll().getState();

            if (currState.hasGold) {
                goalState = currState;
                break;
            }

            ArrayList<Agent.Action> actions = currState.getAvailableActions();

            for (Agent.Action action: actions) {
                State nextState = (State) currState.clone();
                nextState.makeAction(action);

                int newCost = costSoFar.get(currState) +  currState.getActionCost(action);


                // System.out.println(nextState);

                if (costSoFar.get(nextState) == null  || newCost < costSoFar.get(nextState))  {
                    costSoFar.put(nextState, newCost);
                    int priority = newCost + nextState.getHeuristicCost();
                    frontier.add(new StateCostPair(nextState, priority));
                    cameFrom.put(nextState, currState);
                }
            }
            // tmp--;
            if (tmp == 0) {
                break;
            }

            // System.out.println("NEW FRONT");
            // System.out.println(frontier.size());
        }

        return this.reconstructPath(cameFrom, initState, goalState);
    }
    private LinkedList<Action> plan;

    public LinkedList<Action> getPlan() {
        return this.plan;
    }

    public SearchAI(World.Tile[][] board) throws CloneNotSupportedException {

        this.plan = this.AStarSearch(board);
        System.out.println(plan);


        // This must be the last instruction.
        planIterator = plan.listIterator();
    }

    public enum Direction {
        RIGHT,
        LEFT,
        TOP,
        BOTTOM,
    }

    private class StateCostPair implements Comparable <StateCostPair> {
        private State state;
        private int cost;

        public StateCostPair(State state, int cost) {
            this.state = state;
            this.cost = cost;
        }

        public State getState() {
            return this.state;
        }

        public int getCost() {
            return this.cost;
        }

        public int compareTo(StateCostPair other) {
            if (this.cost > other.cost)  {
                return 1;
            } else if (this.cost < other.cost) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private class State implements Cloneable, Comparable<State> {
        private int positionX;
        private int positionY;
        private int colDimension;
        private int rowDimension;

        private Boolean hasArrow;
        private Boolean hasGold;
        private Direction direction;

        private int goldX;
        private int goldY;

        private Agent.Action actionMade;

        private String[][] world;
        public State () {
        }
        public String toString(){
            return "ActionMade: " + this.actionMade + " " +
                "PositionX: " + this.positionX + " " +
                "PositionY: " + this.positionY + " " +
                "Direction: " + this.direction + "\n";
        }
        public int compareTo(State other) {
            if (this.positionX == other.getPositionX() &&
                    this.positionY == other.getPositionY()) {
                return 0;
            } else {
                return 1;
            }
        }
        @Override
        public int hashCode() {
            return Objects.hash(positionX, positionY, direction, actionMade);
            // return Objects.hash(positionX, positionY, direction, actionMade, world);
        }

        @Override
        public boolean equals(Object other)
        {
            // return this.positionX == ((State) other).getPositionX() &&
            //     this.positionY == ((State) other).getPositionY() &&
            //     this.direction == ((State) other).getDirection() &&
            //     // this.hasGold == ((State) other).hasGold() &&
            //     // this.hasArrow() == ((State) other).hasArrow() &&
            //     this.actionMade == ((State) other).getActionMade();

            return this.positionX == ((State) other).getPositionX() &&
            this.direction == ((State) other).getDirection() &&
            this.actionMade == ((State) other).getActionMade() &&
            // this.hasArrow() == ((State) other).hasArrow() &&
                this.positionY == ((State) other).getPositionY();
            // this.hasGold == ((State) other).hasGold() &&
        }

        public String[][] getWorld() {
            return this.world;
        }

        public void printWorld(String[][] board) {
            for (int i = board.length-1; i >= 0 ; i--) {
                StringBuilder tileString = new StringBuilder();
                for (int j = 0; j < board[i].length ; j++) {
                    System.out.printf("%4s", board[j][i]);
                }
                System.out.println();
            }
        }

        public State (World.Tile[][] board) {

            this.world = new String[board.length][board[0].length];
            // traverse column by column from left to right, from bottom to top
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    String tileState = ".";
                    if (board[j][i].getPit()) {
                        tileState += "P";
                    }
                    if (board[j][i].getWumpus()) {
                        tileState += "W";
                    }
                    if (board[j][i].getGold()) {
                        tileState += "G";
                        this.goldX = i;
                        this.goldY = j;
                    }

                    this.world[i][j] = tileState;
                }
            }

            this.colDimension = board.length;
            this.rowDimension = board[0].length;

            this.positionX = 0;
            this.positionY = 0;
            this.hasArrow = true;
            this.hasGold = false;
            this.direction = Direction.RIGHT;
        }

        public int getHeuristicCost() {
            return Math.abs(this.positionX - this.goldX) + Math.abs(this.positionY - this.goldY);
        }

        public int getPositionX() {
            return this.positionX;
        }

        public int getPositionY() {
            return this.positionY;
        }

        public Direction getDirection() {
            return this.direction;
        }

        public Boolean hasGold() {
            return this.hasGold;
        }

        public Boolean hasArrow() {
            return this.hasArrow;
        }

        public Boolean isGoalReached() {
            return this.hasGold;
        }

        public Agent.Action getActionMade() {
            return this.actionMade;
        }

        public int getActionCost(Agent.Action action) {
            int cost = 1;

            switch (action) {
                case FORWARD:
                    int nextPositionX = this.positionX;
                    int nextPositionY = this.positionY;

                    switch (this.direction) {
                        case TOP:
                            ++nextPositionY;
                            break;
                        case RIGHT:
                            ++nextPositionX;
                            break;
                        case BOTTOM:
                            --nextPositionY;
                            break;
                        case LEFT:
                            --nextPositionX;
                            break;
                    }

                    if (this.isWumpus(nextPositionX, nextPositionY) || this.isPit(nextPositionX, nextPositionY)) {
                        cost += 1000;
                    }
                    break;
                case SHOOT:
                    if (this.hasArrow) {
                        cost += 10;
                    }
                    break;
            }
            return cost;
        }

        public void turn(Agent.Action action) {
            if (action == Agent.Action.TURN_RIGHT) {
                switch(this.direction) {
                    case RIGHT:
                        this.direction = Direction.BOTTOM;
                        break;
                    case BOTTOM:
                        this.direction = Direction.LEFT;
                        break;
                    case LEFT:
                        this.direction = Direction.TOP;
                        break;
                    case TOP:
                        this.direction = Direction.RIGHT;
                        break;

                }
            } else if (action == Agent.Action.TURN_LEFT) {
                switch(this.direction) {
                    case RIGHT:
                        this.direction = Direction.TOP;
                        break;
                    case TOP:
                        this.direction = Direction.LEFT;
                        break;
                    case LEFT:
                        this.direction = Direction.BOTTOM;
                        break;
                    case BOTTOM:
                        this.direction = Direction.RIGHT;
                        break;
                }
            }
        }

        public void makeAction(Agent.Action action) {
            this.actionMade = action;

            switch (action)
            {
                case TURN_LEFT:
                case TURN_RIGHT:
                    this.turn(action);
                    break;
                case FORWARD:
                    if ( this.direction == Direction.RIGHT && this.positionX+1 < this.colDimension )
                        ++this.positionX;
                    else if ( this.direction == Direction.BOTTOM && this.positionY-1 >= 0 )
                        --this.positionY;
                    else if ( this.direction == Direction.LEFT && this.positionX-1 >= 0 )
                        --this.positionX;
                    else if ( this.direction == Direction.TOP && this.positionY+1 < this.rowDimension )
                        ++this.positionY;
                    break;

                case SHOOT:
                    if (this.hasArrow) {
                        this.hasArrow = false;
                        switch (this.direction) {
                            case  RIGHT:
                                for ( int x = this.positionX; x < this.colDimension; ++x )
                                    if ( this.world[x][this.positionY].contains("W") ) {
                                        this.world[x][this.positionY] = this.world[x][this.positionY].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  BOTTOM:
                                for ( int y = this.positionY; y >= 0; --y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.world[this.positionX][y] = this.world[this.positionX][y].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  LEFT:
                                for ( int x = this.positionX; x >= 0; --x )
                                    if ( this.world[x][this.positionY].contains("W") ) {
                                        this.world[x][this.positionY] = this.world[x][this.positionY].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  TOP:
                                for ( int y = this.positionY; y < this.rowDimension; ++y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.world[this.positionX][y] = this.world[this.positionX][y].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                        }
                    }
                    break;
                case GRAB:
                    if (this.world[this.positionX][this.positionY].contains("G") ) {
                        this.world[this.positionX][this.positionY] = this.world[this.positionX][this.positionY].replaceFirst("G", "");
                        this.hasGold = true;
                    }
                    break;
            }

        }

        public void setWorld(String[][] world) {
            this.world = world;
        }

        public Object clone() throws CloneNotSupportedException {
            State clone = (State)super.clone();
            String[][] clonedWorld = this.world.clone();

            for (int i = 0; i < clonedWorld.length; i++) {
                clonedWorld[i] = clonedWorld[i].clone();
            }

            clone.setWorld(clonedWorld);
            return clone;
        }

        private Boolean isPit(int positionX, int positionY) {
            return this.world[positionX][positionY].contains("P");
        }
        private Boolean isWumpus(int positionX, int positionY) {
            return this.world[positionX][positionY].contains("W");
        }

        public ArrayList<Agent.Action> getAvailableActions() {

            ArrayList<Agent.Action> actions = new ArrayList<Agent.Action>();

            int nextPositionX = this.positionX;
            int nextPositionY = this.positionY;

            switch (this.direction) {
                case TOP:
                    ++nextPositionY;
                    break;
                case RIGHT:
                    ++nextPositionX;
                    break;
                case BOTTOM:
                    --nextPositionY;
                    break;
                case LEFT:
                    --nextPositionX;
                    break;
            }

            // movement limitation. e.g. don't move forward if at
            // direction = left X=0 or
            // X-1 dangerous
            if ( nextPositionX < this.colDimension &&
                    nextPositionX >= 0 &&
                    nextPositionY < this.rowDimension &&
                    nextPositionY >= 0 &&
                    !this.isPit(nextPositionX, nextPositionY) &&
                    !this.isWumpus(nextPositionX, nextPositionY)
               ) {
                // this.printWorld(this.world);
                // System.out.print(Arrays.deepToString(this.world));
                // System.out.print(nextPositionX);
                // System.out.println(nextPositionY);

                actions.add(Agent.Action.FORWARD);

               }

            // turning limitation. e.g. don't turn to right
            // if at direciton = left  X=0 or
            // X-1 is dangerous
            if (this.direction == Direction.RIGHT) {
                if (this.positionY != 0 && !this.isPit(this.positionX, this.positionY-1)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                if (this.positionY != this.rowDimension-1 && !this.isPit(this.positionX, this.positionY+1)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
                for ( int x = this.positionX; x < this.colDimension; ++x )
                    if (this.isWumpus(x, this.positionY)) {
                        actions.add(Agent.Action.SHOOT);
                        break;
                    }
            }

            if (this.direction == Direction.LEFT) {
                if (this.positionY != 0 && !this.isPit(this.positionX, this.positionY-1)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
                if (this.positionY != this.rowDimension-1 && !this.isPit(this.positionX, this.positionY+1)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                for ( int x = this.positionX; x >= 0; --x )
                    if (this.isWumpus(x, this.positionY)) {
                        actions.add(Agent.Action.SHOOT);
                        break;
                    }
            }

            if (this.direction == Direction.TOP) {
                if (this.positionX != 0 && !this.isPit(this.positionX-1, this.positionY)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
                if (this.positionX != this.colDimension-1 && !this.isPit(this.positionX+1, this.positionY)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                for ( int y = this.positionY; y < this.rowDimension; ++y )
                    if (this.isWumpus(this.positionX, y)) {
                        actions.add(Agent.Action.SHOOT);
                        break;
                    }
            }

            if (this.direction == Direction.BOTTOM) {
                if (this.positionX != 0 && !this.isPit(this.positionX-1, this.positionY)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                if (this.positionX != this.colDimension-1 && !this.isPit(this.positionX+1, this.positionY)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }

                for ( int y = this.positionY; y >= 0; --y )
                    if (this.isWumpus(this.positionX, y)) {
                        actions.add(Agent.Action.SHOOT);
                        break;
                    }
            }

            if (this.world[this.positionX][this.positionY].contains("G")) {
                actions.add(Agent.Action.GRAB);
            }

            return actions;
        }

    }


    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
