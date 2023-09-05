package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.util.*;
import java.lang.Math;

// mvn install; mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d maps/simple_map.txt"
// mvn install; mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-f Wumpus_World_Generator/Worlds"
// mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"

public class SearchAI extends Agent {
    private ListIterator<Action> planIterator;
    private Dictionary<State, State> cameFrom = new Hashtable<State, State>();
    private Dictionary<State, Integer> costSoFar = new Hashtable<State, Integer>();
    private LinkedList<Action> plan;

    public enum Direction {
        RIGHT,
        LEFT,
        TOP,
        BOTTOM,
    }

    public SearchAI() throws CloneNotSupportedException {
    }

    public SearchAI(World.Tile[][] board) throws CloneNotSupportedException {
        this.plan = this.AStarSearch(board);

        planIterator = this.plan.listIterator();
    }

    private LinkedList<Action> AStarSearch(World.Tile[][] board) throws CloneNotSupportedException{

        Queue <StateCostPair> frontier = new PriorityQueue<StateCostPair>();
        State initState = new State(board);
        State goalState = null;

        frontier.add(new StateCostPair(initState, 0));

        cameFrom.put(initState, initState);
        costSoFar.put(initState, 0);

        while (frontier.peek() != null) {
            State currState = frontier.poll().getState();

            if (currState.hasGold() && currState.getPositionX() == 0 && currState.getPositionY() == 0) {
                goalState = currState;

                break;
            }

            ArrayList<Agent.Action> actions = currState.getAvailableActions();

            for (Agent.Action action: actions) {
                State nextState = (State) currState.clone();
                nextState.makeAction(action);

                int newCost = costSoFar.get(currState) +  currState.getActionCost(action);


                if (costSoFar.get(nextState) == null  || newCost < costSoFar.get(nextState))  {
                    costSoFar.put(nextState, newCost);
                    float priority = newCost + nextState.getHeuristicCost();
                    frontier.add(new StateCostPair(nextState, priority));
                    cameFrom.put(nextState, currState);
                }
            }
        }

        return this.reconstructPath(cameFrom, initState, goalState);
    }

    private LinkedList<Agent.Action> reconstructPath(Dictionary<State, State> cameFrom, State start, State goal) {
        State current = goal;
        LinkedList<Agent.Action> path = new LinkedList<Agent.Action>();

        if (goal == null) {
            path.add(Agent.Action.CLIMB);
            return path;
        }

        while (current != start) {
            Agent.Action madeAction =  current.getActionMade();
            path.addFirst(madeAction);
            current = cameFrom.get(current);
        }

        path.add(Agent.Action.CLIMB);

        return path;
    }

    public LinkedList<Action> getPlan() {
        return this.plan;
    }

    private class StateCostPair implements Comparable <StateCostPair> {
        private State state;
        private float cost;

        public StateCostPair(State state, float cost) {
            this.state = state;
            this.cost = cost;
        }

        public State getState() {
            return this.state;
        }

        public float getCost() {
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
        private Boolean isWumpusAlive;
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
                    this.positionY == other.getPositionY() &&
                    this.hasGold == other.hasGold()
               ) {
                return 0;
            } else {
                return 1;
            }
        }
        @Override
        public int hashCode() {
            return Objects.hash(positionX, positionY, direction, actionMade, hasGold, isWumpusAlive);
        }

        @Override
        public boolean equals(Object other)
        {
            return this.positionX == ((State) other).getPositionX() &&
                this.positionY == ((State) other).getPositionY() &&
                this.direction == ((State) other).getDirection() &&
                this.hasGold == ((State) other).hasGold() &&
                this.isWumpusAlive == ((State) other).isWumpusAlive() &&
                this.actionMade == ((State) other).getActionMade();
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

        public State (Set<String> safeTiles, String currentPos, Direction direction) {
            this.colDimension = 0;
            this.rowDimension = 0;

            for (String safeTile: safeTiles) {
                String[] pos = safeTile.split("_");
                int x = Integer.parseInt(pos[0]);
                int y = Integer.parseInt(pos[1]);
                if (x > this.rowDimension)
                    this.rowDimension = x;
                if (y > this.colDimension)
                    this.colDimension = y;
            }
            this.rowDimension++;
            this.colDimension++;
            // System.out.println("DIMENSION");
            // System.out.print(rowDimension);
            // System.out.println(colDimension);
            this.world = new String[this.rowDimension][this.colDimension];
             // Arrays.fill(this.world, "");
            // Arrays.fill(this.world, "");
            // for (double[] row: matrix)
            //     Arrays.fill(row, 1.0);

            // System.out.println(Arrays.deepToString(this.world));
            for (String safeTile: safeTiles) {
                String[] pos = safeTile.split("_");
                int x = Integer.parseInt(pos[0]);
                int y = Integer.parseInt(pos[1]);
                this.world[x][y] = "S";
            }
            // System.out.println(Arrays.deepToString(this.world));

            String[] pos = currentPos.split("_");
            this.positionX = Integer.parseInt(pos[0]);
            this.positionY = Integer.parseInt(pos[1]);
            // this.hasArrow = true;
            // this.hasGold = false;
            this.direction = direction;
        }

        public State (World.Tile[][] board) {

            this.world = new String[board.length][board[0].length];
            // traverse column by column from left to right, from bottom to top
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    String tileState = ".";
                    if (board[i][j].getPit()) {
                        tileState += "P";
                    }
                    if (board[i][j].getWumpus()) {
                        tileState += "W";
                    }
                    if (board[i][j].getGold()) {
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
            this.isWumpusAlive = true;
            this.direction = Direction.RIGHT;
        }

        public float getHeuristicCostPartial(int distX, int distY) {
            return (Math.abs(this.positionX - distX) + Math.abs(this.positionY - distY));
        }

        public float getHeuristicCost() {
            if (this.hasGold) {
                return (Math.abs(this.positionX) + Math.abs(this.positionY));
            } else {
                return (Math.abs(this.positionX - this.goldX) + Math.abs(this.positionY - this.goldY));
            }
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

        public String[][] getWorld() {
            return this.world;
        }

        public Boolean isWumpusAlive() {
            return this.isWumpusAlive;
        }

        private Boolean isPit(int positionX, int positionY) {
            return this.world[positionX][positionY].contains("P");
        }
        private Boolean isWumpus(int positionX, int positionY) {
            return this.world[positionX][positionY].contains("W");
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
                                        this.isWumpusAlive = false;
                                        this.world[x][this.positionY] = this.world[x][this.positionY].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  BOTTOM:
                                for ( int y = this.positionY; y >= 0; --y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.isWumpusAlive = false;
                                        this.world[this.positionX][y] = this.world[this.positionX][y].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  LEFT:
                                for ( int x = this.positionX; x >= 0; --x )
                                    if ( this.world[x][this.positionY].contains("W") ) {
                                        this.isWumpusAlive = false;
                                        this.world[x][this.positionY] = this.world[x][this.positionY].replaceFirst("W", "");
                                        break;
                                    }
                                break;
                            case  TOP:
                                for ( int y = this.positionY; y < this.rowDimension; ++y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.isWumpusAlive = false;
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

                actions.add(Agent.Action.FORWARD);

               }


            // turning limitation. e.g. don't turn to right
            // if at direciton = left  X=0 or
            // X-1 is dangerous
            if (this.direction == Direction.RIGHT) {
                if ((this.positionY != 0 && !this.isPit(this.positionX, this.positionY-1)) ||(this.positionX != 0 && !this.isPit(this.positionX-1, this.positionY))) {
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
                if ((this.positionY != 0 && !this.isPit(this.positionX, this.positionY-1)) || (this.positionX != this.colDimension-1 && !this.isPit(this.positionX+1, this.positionY))) {
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
                if ((this.positionX != 0 && !this.isPit(this.positionX-1, this.positionY)) || (this.positionY != 0 && !this.isPit(this.positionX, this.positionY-1))) {
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
                if ((this.positionX != 0 && !this.isPit(this.positionX-1, this.positionY)) || (this.positionY != this.rowDimension-1 && !this.isPit(this.positionX, this.positionY+1))) {
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

        // PARTIALLY OBSERVABLE CODE
        public ArrayList<Agent.Action> getAvailableActionsPartial() {

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

            // System.out.println(Arrays.deepToString(this.world));
            // this.isSafePartial(nextPositionX, nextPositionY);
            if ( nextPositionX < this.rowDimension &&
                    nextPositionX >= 0 &&
                    nextPositionY < this.colDimension &&
                    nextPositionY >= 0 &&
                    this.isSafePartial(nextPositionX, nextPositionY)
               ) {
                actions.add(Agent.Action.FORWARD);
               }


            // turning limitation. e.g. don't turn to right
            // if at direciton = left  X=0 or
            // X-1 is dangerous
            if (this.direction == Direction.RIGHT) {
                if ((this.positionY != 0 && this.isSafePartial(this.positionX, this.positionY-1)) ||(this.positionX != 0 && this.isSafePartial(this.positionX-1, this.positionY))) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                if (this.positionY != this.colDimension-1 && this.isSafePartial(this.positionX, this.positionY+1)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
            }

            if (this.direction == Direction.LEFT) {
                if ((this.positionY != 0 && this.isSafePartial(this.positionX, this.positionY-1)) || (this.positionX != this.rowDimension-1 && this.isSafePartial(this.positionX+1, this.positionY))) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
                if (this.positionY != this.colDimension-1 && this.isSafePartial(this.positionX, this.positionY+1)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
            }

            if (this.direction == Direction.TOP) {
                if ((this.positionX != 0 && this.isSafePartial(this.positionX-1, this.positionY)) || (this.positionY != 0 && this.isSafePartial(this.positionX, this.positionY-1))) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
                if (this.positionX != this.rowDimension-1 && this.isSafePartial(this.positionX+1, this.positionY)) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
            }

            if (this.direction == Direction.BOTTOM) {
                if ((this.positionX != 0 && this.isSafePartial(this.positionX-1, this.positionY)) || (this.positionY != this.colDimension-1 && this.isSafePartial(this.positionX, this.positionY+1))) {
                    actions.add(Agent.Action.TURN_RIGHT);
                }
                if (this.positionX != this.rowDimension-1 && this.isSafePartial(this.positionX+1, this.positionY)) {
                    actions.add(Agent.Action.TURN_LEFT);
                }
            }
            return actions;
        }

        private Boolean isSafePartial(int x, int y) {
            if (this.world[x][y] == null) {
                return false;
            }
            return this.world[x][y].equals("S");
        }

        public int getActionCostPartial(Agent.Action action) {
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

                    if (!this.isSafePartial(nextPositionX, nextPositionY)) {
                        cost += 1000;
                    }
                    break;
            }
            return cost;
        }

        public void makeActionPartial(Agent.Action action) {
            this.actionMade = action;

            switch (action)
            {
                case TURN_LEFT:
                case TURN_RIGHT:
                    this.turn(action);
                    break;
                case FORWARD:
                    if ( this.direction == Direction.RIGHT && this.positionX+1 < this.rowDimension )
                        ++this.positionX;
                    else if ( this.direction == Direction.BOTTOM && this.positionY-1 >= 0 )
                        --this.positionY;
                    else if ( this.direction == Direction.LEFT && this.positionX-1 >= 0 )
                        --this.positionX;
                    else if ( this.direction == Direction.TOP && this.positionY+1 < this.colDimension )
                        ++this.positionY;
                    break;
            }

        }

    }

    //// FOR PARTIAL OBSERVABLE

    public LinkedList<Action> getSafeRoute(String currentPos, String direction, String distPos, Set<String> safeTiles) throws CloneNotSupportedException {
        LinkedList<Agent.Action> plan = new LinkedList<Agent.Action>();

        // System.out.println(currentPos);
        // System.out.println(distPos);
        // System.out.println(safeTiles);
        // include it for the state object
        safeTiles.add(distPos);
        plan = this.AStarSearchForUnobservable(currentPos, this.getDirectionFromString(direction), distPos, safeTiles);

        // System.out.println(plan);

        return plan;
    }

    private Direction getDirectionFromString(String direction) {
        Direction directionEnum = null;
        switch(direction) {
            case "RIGHT":
                directionEnum = Direction.RIGHT;
                break;
            case "BOTTOM":
                directionEnum = Direction.BOTTOM;
                break;
            case "LEFT":
                directionEnum = Direction.LEFT;
                break;
            case "TOP":
                directionEnum = Direction.TOP;
                break;
        }
        return directionEnum;
    }

    public LinkedList<Action> AStarSearchForUnobservable(String currentPos, Direction direction, String distPos, Set<String> safeTiles) throws CloneNotSupportedException{

        Queue <StateCostPair> frontier = new PriorityQueue<StateCostPair>();

        State initState = new State (safeTiles, currentPos, direction);
        State goalState = null;

        frontier.add(new StateCostPair(initState, 0));

        String[] pos = distPos.split("_");
        int goalX = Integer.parseInt(pos[0]);
        int goalY = Integer.parseInt(pos[1]);

        cameFrom.put(initState, initState);
        costSoFar.put(initState, 0);

        while (frontier.peek() != null) {
            State currState = frontier.poll().getState();


            if (currState.getPositionX() == goalX && currState.getPositionY() == goalY) {
                goalState = currState;

                break;
            }

            ArrayList<Agent.Action> actions = currState.getAvailableActionsPartial();

            for (Agent.Action action: actions) {
                State nextState = (State) currState.clone();
                nextState.makeActionPartial(action);


                int newCost = costSoFar.get(currState) +  currState.getActionCostPartial(action);
                // System.out.println("NEW ACTION");
                // System.out.print(nextState.getPositionX());
                // System.out.println(nextState.getPositionY());
                // System.out.println(newCost);
                // System.out.println(costSoFar.get(nextState));

                if (costSoFar.get(nextState) == null  || newCost < costSoFar.get(nextState))  {
                    costSoFar.put(nextState, newCost);
                    float priority = newCost + nextState.getHeuristicCostPartial(goalX, goalY);
                    frontier.add(new StateCostPair(nextState, priority));
                    cameFrom.put(nextState, currState);
                }
            }
        }

        return this.reconstructPathPartial(cameFrom, initState, goalState);
    }


    public LinkedList<Agent.Action> reconstructPathPartial(Dictionary<State, State> cameFrom, State start, State goal) {
        State current = goal;
        LinkedList<Agent.Action> path = new LinkedList<Agent.Action>();

        while (current != start) {
            Agent.Action madeAction =  current.getActionMade();
            path.addFirst(madeAction);
            current = cameFrom.get(current);
        }

        return path;
    }



    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
