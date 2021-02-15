package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Arrays;

// command: mvn install; mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"
// mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"
// mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-f maps"

public class SearchAI extends Agent {
    private ListIterator<Action> planIterator;

    public SearchAI(World.Tile[][] board) {

        LinkedList<Action> plan;
        State state = new State(board);

        // Remove the code below //
        plan = new LinkedList<Action>();
        for (int i = 0; i < 8; i++)
            plan.add(Agent.Action.FORWARD);

        plan.add(Action.TURN_LEFT);
        plan.add(Action.TURN_LEFT);

        for (int i = 10; i < 18; i++)
            plan.add(Action.FORWARD);
        plan.add(Action.CLIMB);

        // This must be the last instruction.
        planIterator = plan.listIterator();
    }

    public enum Direction {
        RIGHT,
        LEFT,
        TOP,
        BOTTOM,
    }

    private class State implements Cloneable {
        private int positionX;
        private int positionY;
        private int colDimension;
        private int rowDimension;

        private Boolean hasArrow;
        private Boolean hasGold;
        private Direction direction;

        private String[][] world;

        public State (World.Tile[][] board) {

            world = new String[board.length][board[0].length];
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
                    }
                    world[i][j] = tileState;
                }
            }
            System.out.println(Arrays.deepToString(world));

            this.colDimension = board.length;
            this.rowDimension = board[0].length;

            this.positionX = 0;
            this.positionY = 0;
            this.hasArrow = true;
            this.hasGold = false;
            this.direction = Direction.RIGHT;
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

        public Boolean isGoalReached() {
            return this.hasGold;
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
                        hasArrow = false;
                        switch (this.direction) {
                            case  RIGHT:
                                for ( int x = this.positionX; x < this.colDimension; ++x )
                                    if ( this.world[x][this.positionY].contains("W") ) {
                                        this.world[x][positionY].replace("W", "");
                                    }
                                break;
                            case  BOTTOM:
                                for ( int y = this.positionY; y >= 0; --y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.world[this.positionX][y].replace("W", "");
                                    }
                                break;
                            case  LEFT:
                                for ( int x = this.positionX; x >= 0; --x )
                                    if ( this.world[x][this.positionY].contains("W") ) {
                                        this.world[x][this.positionY].replace("W", "");
                                    }
                            case  TOP:
                                for ( int y = this.positionY; y < this.rowDimension; ++y )
                                    if ( this.world[this.positionX][y].contains("W") ) {
                                        this.world[this.positionX][y].replace("W", "");
                                    }
                                break;
                        }
                    }
                    break;
                case GRAB:
                    if (this.world[this.positionX][this.positionY].contains("G") ) {
                        this.world[this.positionX][this.positionY].replace("G", "");
                        this.hasGold = true;
                    }
                    break;

                    // case CLIMB:
                    //     if ( agentX == 0 && agentY == 0 ) {
                    //         if ( goldLooted )
                    //             score += 1000;
                    //         if (debug) printWorldInfo();
                    //         return score;
                    //     }
                    //     break;
            }

        }

        public static State getNewState(State state, Agent.Action action) {
            State newState = (State)state.clone();
            newnState.makeAction(action);
            return newnState;
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public ArrayList<Agent.Action> getAvailableActions(State state) {

            ArrayList<Agent.Action> actions = new ArrayList<Agent.Action>();
            actions.add(Agent.Action.TURN_LEFT);
            actions.add(Agent.Action.TURN_RIGHT);
            actions.add(Agent.Action.FORWARD);
            actions.add(Agent.Action.SHOOT);
            actions.add(Agent.Action.GRAB);
            actions.add(Agent.Action.CLIMB);

            // Turning limitation, i.e. at Y == 0 don't turn to right to face a wall
            // TODO: Make limitaitons
            // if (state.getPositionY() == 0) {
            //  if (state.getDirection() == Direction.RIGHT) {
            //     actions.add(Agent.Action.TURN_LEFT);
            //  }
            //  else if (state.getDirection() == Direction.BOTTOM) {
            //     actions.add(Agent.Action.TURN_RIGHT);
            //  }
            //  if (state.getDirection() == Direction.BOTTOM) {
            //     actions.add(Agent.Action.TURN_RIGHT);
            //  }
            // }

            // if (state.getPositionY() == board.length) {
            //  if (state.getDirection() == Direction.RIGHT) {
            //     actions.add(Agent.Action.TURN_LEFT);
            //  }
            //  else if (state.getDirection() == Direction.BOTTOM) {
            //     actions.add(Agent.Action.TURN_RIGHT);
            //  }
            // }

            return actions;
        }

    }


    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
