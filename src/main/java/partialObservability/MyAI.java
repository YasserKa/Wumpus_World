package partialObservability;
import wumpus.Agent;


import java.io.IOException;
import java.util.List;

import fullObservability.SearchAI;
import java.util.*;
import org.tweetyproject.commons.ParserException;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.sat.*;
import org.tweetyproject.logics.pl.parser.*;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.semantics.*;

// mvn install; mvn exec:java -Dexec.mainClass=partialObservability.Main -Dexec.args="-d maps/simple_map.txt"
// mvn install; mvn exec:java -Dexec.mainClass=partialObservability.Main -Dexec.args="-f Wumpus_World_Generator/Worlds"
//
// mvn exec:java -Dexec.mainClass=fullObservability.MainSearch -Dexec.args="-d"

public class MyAI extends Agent
{
    private LinkedList<Action> plan;
    private AbstractPlReasoner reasoner;
    private PlBeliefSet beliefSet;
    private PlBeliefSet beliefSetNotAxioms;
    private PlBeliefSet beliefSetWumpus;
    private PlParser parser;

    private int positionX;
    private int positionY;

    private int boundaryX;
    private int boundaryY;

    Set<String> visitedTiles;
    Set<String> safeTiles;
    Set<String> notSafeTiles;
    Set<String> notSureTiles;

    private Boolean isWumpusAlive;
    private Boolean isFirstTurn;
    private Boolean hasGold;
    private Boolean hasArrow;
    private Boolean isKillWumpusPlan;

    private Agent.Action prevAction;
    private Direction direction;
    private SearchAI ai;

    public enum Direction {
        RIGHT,
        LEFT,
        TOP,
        BOTTOM,
    }

    public enum Sense {
        STENCH,
        BREEZE,
        GLITTER,
        BUMP,
        SCREAM,
    }

    public MyAI () throws CloneNotSupportedException {
        this.ai = new SearchAI();
        this.positionX = 0;
        this.positionY = 0;

        this.boundaryX = 10;
        this.boundaryY = 10;

        this.direction = Direction.RIGHT;
        this.prevAction = null;

        this.isWumpusAlive = true;
        this.isFirstTurn = true;
        this.hasGold = false;
        this.hasArrow = true;
        this.isKillWumpusPlan = false;

        this.visitedTiles = new HashSet<String>();
        this.safeTiles = new HashSet<String>();
        this.notSafeTiles = new HashSet<String>();
        this.notSureTiles = new HashSet<String>();

        this.plan = new LinkedList<Agent.Action>();
        SatSolver.setDefaultSolver(new Sat4jSolver());
        this.reasoner  = new SatReasoner();
        this.beliefSetNotAxioms  = new PlBeliefSet();
        this.beliefSet  = new PlBeliefSet();
        this.beliefSetWumpus  = new PlBeliefSet();

        String position = this.getPositionInString(this.positionX, this.positionY);

        this.beliefSetNotAxioms.add(new Negation(new Proposition("W"+position)));
        this.beliefSetNotAxioms.add(new Negation(new Proposition("P"+position)));
        this.generateAtemporalAxioms();
    }

    public Conjunction getAdjacentNegConjunction(String term, int posX, int posY) {
        Conjunction c = new Conjunction();

        Set<String> neighboringTiles = this.getNeighboringTiles(posX, posY);
        for(String pos: neighboringTiles) {
            c.add(new Negation(new Proposition(term+pos)));
        }
        return c;
    }

    public Disjunction getAdjacentDisjunction(String term, int posX, int posY) {
        Disjunction d = new Disjunction();

        Set<String> neighboringTiles = this.getNeighboringTiles(posX, posY);
        for(String pos: neighboringTiles) {
            d.add(new Proposition(term+pos));
        }

        return d;
    }

    private Conjunction getWumpusConjunctionPairs(String position) {
        Conjunction c = new Conjunction();
        for (int i = 0; i < this.boundaryX; i++) {
            for (int j = 0; j < this.boundaryY; j++) {
                String anotherPosition = this.getPositionInString(i, j);
                if (anotherPosition.equals(position)) {
                    continue;
                }
                // e.g. W1,1 v W1,2 ^ W1,1 v W1,3 ...
                c.add(
                        new Disjunction(
                            new Negation(new Proposition("W"+position)),
                            new Negation(new Proposition("W"+anotherPosition))
                            )
                     );
            }
        }

        return c;
    }

    private void generateAtemporalAxioms() {
        Disjunction atLeastOneWumpus = new Disjunction();
        Conjunction atMostOneWumpus = new Conjunction();
        Conjunction pitsAndWumpus = new Conjunction();

        for (int i = 0; i < this.boundaryX; i++) {
            for (int j = 0; j < this.boundaryY; j++) {
                // create pit and wumpus equivalence
                String position = this.getPositionInString(i, j);

                Equivalence stench = new Equivalence(
                        new Proposition("S"+position),
                        this.getAdjacentDisjunction("W", i, j));
                Equivalence pit = new Equivalence(
                        new Proposition("B"+position),
                        this.getAdjacentDisjunction("P", i, j));

                Implication stench1 = new Implication(
                        new Negation(new Proposition("S"+position)),
                        this.getAdjacentNegConjunction("W", i, j));

                Implication pit1 = new Implication(
                        new Negation(new Proposition("B"+position)),
                        this.getAdjacentNegConjunction("P", i, j));

                atLeastOneWumpus.add(new Proposition("W"+position));
                atMostOneWumpus.add(this.getWumpusConjunctionPairs(position));

                pitsAndWumpus.add(stench);
                pitsAndWumpus.add(pit);
                pitsAndWumpus.add(stench1);
                pitsAndWumpus.add(pit1);
            }
        }

        this.beliefSet.add(pitsAndWumpus);
        this.beliefSetWumpus.add(pitsAndWumpus);
        this.beliefSetWumpus.add(atLeastOneWumpus);
        this.beliefSetWumpus.add(atMostOneWumpus);

    }

    private void includePerceptSentences(Dictionary <Sense, Boolean> senses) {
        String position = this.getPositionInString(this.positionX, this.positionY);

        if (senses.get(Sense.STENCH)) {
            this.beliefSetNotAxioms.add(new Proposition("S"+position));
        } else {
            this.beliefSetNotAxioms.add(new Negation(new Proposition("S"+position)));
        }
        if (senses.get(Sense.BREEZE)) {
            this.beliefSetNotAxioms.add(new Proposition("B"+position));
        } else {
            this.beliefSetNotAxioms.add(new Negation(new Proposition("B"+position)));
        }

        this.beliefSetNotAxioms.add(new Negation(new Proposition("W"+position)));
        this.beliefSetNotAxioms.add(new Negation(new Proposition("P"+position)));
    }

    private void removeNotNeededSafeTiles() {
        Set<String> tmpTiles = new HashSet<String>(safeTiles);
        for (String pos: tmpTiles) {
            String[] tilePosArr = pos.split("_");
            int x = Integer.parseInt(tilePosArr[0]);
            int y = Integer.parseInt(tilePosArr[1]);
            if (x >= this.boundaryX || y >= this.boundaryY) {
                this.safeTiles.remove(pos);
            }
        }
    }

    public void updateState(Dictionary <Sense, Boolean> senses) {
        if (senses.get(Sense.SCREAM)) {
            this.isWumpusAlive = false;
        }

        if (this.prevAction == null) {
            return;
        }
        switch (this.prevAction) {
            case TURN_LEFT:
            case TURN_RIGHT:
                this.turn(this.prevAction);
                break;
            case FORWARD:
                if (senses.get(Sense.BUMP)) {
                    if (this.direction == Direction.RIGHT) {
                        this.boundaryX = this.positionX+1;
                        this.removeNotNeededSafeTiles();
                        this.beliefSet = new PlBeliefSet();
                        this.beliefSetWumpus = new PlBeliefSet();
                        this.generateAtemporalAxioms();
                    }
                    if (this.direction == Direction.TOP) {
                        this.boundaryY = this.positionY+1;
                        this.removeNotNeededSafeTiles();
                        this.beliefSet = new PlBeliefSet();
                        this.beliefSetWumpus = new PlBeliefSet();
                        this.generateAtemporalAxioms();
                    }
                    break;
                }
                if ( this.direction == Direction.RIGHT)
                    ++this.positionX;
                else if ( this.direction == Direction.BOTTOM)
                    --this.positionY;
                else if ( this.direction == Direction.LEFT)
                    --this.positionX;
                else if ( this.direction == Direction.TOP)
                    ++this.positionY;
                break;
            case GRAB:
                this.hasGold = true;
                break;
            case SHOOT:
                this.hasArrow = false;
                break;
        }
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

    private Set<String> getNeighboringTiles(int posX, int posY) {
        Set<String> neighboringTiles = new HashSet<String>();

        String position = "";
        // left
        if (posX != 0) {
            position = this.getPositionInString(posX-1, posY);
            neighboringTiles.add(position);
        }
        // bot
        if (posY != 0) {
            position = this.getPositionInString(posX, posY-1);
            neighboringTiles.add(position);
        }

        // top
        if (posY != this.boundaryY-1) {
            position = this.getPositionInString(posX, posY+1);
            neighboringTiles.add(position);
        }
        // right
        if (posX != this.boundaryX-1) {
            position = this.getPositionInString(posX+1, posY);
            neighboringTiles.add(position);
        }

        return neighboringTiles;
    }

    private String getPositionInString(int posX, int posY) {
        return String.valueOf(posX) +"_"+ String.valueOf(posY);
    }


    private void updateTilesState(Dictionary <Sense, Boolean> senses) {
        this.safeTiles.add(this.getPositionInString(this.positionX, this.positionY));

        Set<String> neighboringTiles = this.getNeighboringTiles(this.positionX, this.positionY);
        Set<String> tmpTiles = new HashSet<String>(neighboringTiles);
        tmpTiles.removeAll(this.visitedTiles);
        tmpTiles.removeAll(this.safeTiles);
        // tmpTiles.removeAll(this.notSafeTiles);


        if (!senses.get(Sense.STENCH) && !senses.get(Sense.BREEZE)) {
            // We are sure that they are safe
            this.notSureTiles.removeAll(neighboringTiles);
            this.safeTiles.addAll(neighboringTiles);
        } else {
            // Not sure if safe or not
            this.notSureTiles.addAll(neighboringTiles);
        }

        for(String pos: tmpTiles) {
            if (this.isPit(pos)) {
                this.notSureTiles.remove(pos);
                this.notSafeTiles.add(pos);
            }
            else if (this.isWumpus(pos)) {
                this.notSureTiles.remove(pos);
                if (this.isWumpusAlive) {
                    this.notSafeTiles.add(pos);
                } else {
                    this.safeTiles.add(pos);
                }
            } else if (this.isNoPitNoWumpus(pos)) {
                this.notSureTiles.remove(pos);
                this.safeTiles.add(pos);
            }
        }
        // this.notSureTiles.
    }

    private Boolean isGlitter() {
        PlFormula query = new Negation(new Proposition("glitter"));
        Boolean answer = this.reasoner.query(this.beliefSet, query);
        return answer;
    }
    private boolean isNoPitNoWumpus(String pos) {
        // System.out.println(pos);
        // System.out.println(this.isWumpus(pos));
        // System.out.println(this.isNoWumpus(pos));
        // System.out.println(this.isWumpusAlive);
        // System.out.println(this.isNoPit(pos) && (this.isNoWumpus(pos) || (this.isWumpus(pos) && this.isWumpusAlive)));
        // return this.isNoPit(pos) && (this.isNoWumpus(pos) || (!this.isWumpus(pos) && this.isWumpusAlive));
        return this.isNoPit(pos) && !(this.isWumpus(pos) && this.isWumpusAlive);
        // return answer;
    }
    // private boolean isNoPitNoWumpus(String pos) {
    //     PlFormula query = new Conjunction(
    //             new Negation(new Proposition("P"+pos)),
    //             new Negation(new Proposition("W"+pos))
    //             );

    //     Boolean answer = this.reasoner.query(this.beliefSetWumpus, query);
    //     return answer;
    // }

    private Boolean isSafe(String pos) {
        return !this.isPit(pos) && !this.isWumpus(pos);
    }

    private Boolean isPit(String pos) {
        PlFormula query = new Proposition("P"+pos);
        // Boolean answer = this.reasoner.query(this.beliefSet, query);

 	PlBeliefSet set = new PlBeliefSet(this.beliefSet);
        set.addAll(this.beliefSetNotAxioms);
        Boolean answer = this.reasoner.query(set, query);
        return answer;
    }

    private Boolean isNoPit(String pos) {
        PlFormula query = new Negation(new Proposition("P"+pos));
        // Boolean answer = this.reasoner.query(this.beliefSet, query);

 	PlBeliefSet set = new PlBeliefSet(this.beliefSet);
        set.addAll(this.beliefSetNotAxioms);
        Boolean answer = this.reasoner.query(set, query);
        return answer;
    }
    private Boolean isNoWumpus(String pos) {
        PlFormula query = new Negation(new Proposition("W"+pos));
        // Boolean answer = this.reasoner.query(this.beliefSetWumpus, query);

 	PlBeliefSet set = new PlBeliefSet(this.beliefSetWumpus);
        set.addAll(this.beliefSetNotAxioms);
        Boolean answer = this.reasoner.query(set, query);
        return answer;
    }

    private Boolean isWumpus(String pos) {
        PlFormula query = new Proposition("W"+pos);
 	PlBeliefSet set = new PlBeliefSet(this.beliefSetWumpus);
        set.addAll(this.beliefSetNotAxioms);
        Boolean answer = this.reasoner.query(set, query);
        return answer;
    }

    public Agent.Action getAction
        (
         boolean stench,
         boolean breeze,
         boolean glitter,
         boolean bump,
         boolean scream
        )
        {
            // Easier to handle than 5 variables
            Dictionary<Sense, Boolean> senses = new Hashtable<Sense, Boolean>();
            senses.put(Sense.STENCH, stench);
            senses.put(Sense.BREEZE, breeze);
            senses.put(Sense.GLITTER, glitter);
            senses.put(Sense.BUMP, bump);
            senses.put(Sense.SCREAM, scream);

            if (this.isFirstTurn && senses.get(Sense.STENCH)) {
                this.isFirstTurn = false;
                this.prevAction = Agent.Action.SHOOT;
                return this.prevAction;
            }

            this.updateState(senses);
            String position = this.getPositionInString(this.positionX, this.positionY);

            if (!this.visitedTiles.contains(position)) {
                this.visitedTiles.add(position);
                this.includePerceptSentences(senses);
                this.updateTilesState(senses);
            }

            if (senses.get(Sense.SCREAM)) {
                this.updateTilesState(senses);
            }

            try {
                // if glitter is spotted, get gold and get out
                this.ai = new SearchAI();
                if (senses.get(Sense.GLITTER) && !this.hasGold) {
                    // grab and update plan to go to 0,0
                    this.plan.clear();
                    this.plan.addAll(this.ai.getSafeRoute(position, this.getDirectionString(), "0_0", safeTiles));
                    this.plan.addFirst(Agent.Action.GRAB);
                    this.plan.add(Agent.Action.CLIMB);
                    this.prevAction = this.plan.pop();
                } else if (this.plan.size() > 0) {
                    // execute current plan
                    this.prevAction = this.plan.pop();
                } else {
                    // make new plan
                    ArrayList <String> safeUnvisited = new ArrayList <String>(this.safeTiles);
                    safeUnvisited.removeAll(this.visitedTiles);

                    if (safeUnvisited.size() > 0) {
                        String target = this.getNearestTile(safeUnvisited);
                        this.plan.addAll(this.ai.getSafeRoute(position, this.getDirectionString(), target, safeTiles));
                        this.prevAction = this.plan.pop();
                    } else if (false && this.isKillWumpusPlan && this.hasArrow)  {
                        // Turn to the direction of wumpus and shoot him
                        String wumpusPos = this.getWumpusPosition();
                        Direction targetDirection = this.getRelativeDirectionFromOneTileToAnother(position, wumpusPos);
                        if (this.direction != targetDirection) {
                             this.prevAction = Agent.Action.TURN_RIGHT;
                        } else {
                         this.prevAction = Agent.Action.SHOOT;
                        }
                    } else {
                        if (false && this.hasArrow) {
                            String wumpusPos = this.getWumpusPosition();
                            // wumpus found
                            if (wumpusPos != "") {
                                this.isKillWumpusPlan = true;
                                String targetPos = this.getSafeTileAdjacentToTile(wumpusPos);
                                this.plan.addAll(this.ai.getSafeRoute(position, this.getDirectionString(), targetPos, safeTiles));
                            }
                        }
                        // Wumpus was not found, escape cave
                        if (this.plan.size() == 0) {
                            this.plan.addAll(this.ai.getSafeRoute(position, this.getDirectionString(), "0_0", safeTiles));
                            this.plan.add(Agent.Action.CLIMB);
                        }

                        this.prevAction = this.plan.pop();
                    }
                }
            } catch (CloneNotSupportedException e) {

            }

            this.isFirstTurn = false;
            return this.prevAction;
        }

    private String getWumpusPosition() {
        String wumpusPos = "";

        for (String tile: this.notSafeTiles) {
            if (this.isWumpus(tile)) {
                wumpusPos = tile;
                break;
            }
        }
        return wumpusPos;
    }
    private String getSafeTileAdjacentToTile(String tile) {
        String pos = "";

        // get direction and position
        String[] tilePosArr = tile.split("_");
        int x = Integer.parseInt(tilePosArr[0]);
        int y = Integer.parseInt(tilePosArr[1]);
        // get position to shoot
        if (this.safeTiles.contains(this.getPositionInString(x+1, y))) {
            pos = this.getPositionInString(x+1, y);
        } else if (this.safeTiles.contains(this.getPositionInString(x-1, y))) {
            pos = this.getPositionInString(x-1, y);
        } else if (this.safeTiles.contains(this.getPositionInString(x, y+1))) {
            pos = this.getPositionInString(x, y+1);
        } else if (this.safeTiles.contains(this.getPositionInString(x, y-1))) {
            pos = this.getPositionInString(x, y-1);
        }

        return pos;
    }

    private Direction getRelativeDirectionFromOneTileToAnother(String currTile, String targetTile) {
        Direction direction = null;

        String[] currPosArr = currTile.split("_");
        int currX = Integer.parseInt(currPosArr[0]);
        int currY = Integer.parseInt(currPosArr[1]);

        String[] targetPosArr = targetTile.split("_");
        int targetX = Integer.parseInt(targetPosArr[0]);
        int targetY = Integer.parseInt(targetPosArr[1]);

        if (currX > targetX) {
            direction = Direction.LEFT;
        } else if (currX < targetX) {
            direction = Direction.RIGHT;
        } else if (currY > targetY) {
            direction = Direction.BOTTOM;
        } else if (currY < targetY) {
            direction = Direction.TOP;
        }

        return direction;
    }


    private String getNearestTile(ArrayList<String> tiles) {
        String nearestTile = "";
        int nearestDist = 999;

        for (String tile: tiles) {
            String[] pos = tile.split("_");
            int x = Integer.parseInt(pos[0]);
            int y = Integer.parseInt(pos[1]);
            int distance = Math.abs(this.positionX - x) + Math.abs(this.positionY - y);
            if (distance < nearestDist) {
                nearestTile = tile;
                nearestDist = distance;
            }
        }
        return nearestTile;
    }


    private String getDirectionString() {
        String direction = "";
        switch(this.direction) {
            case RIGHT:
                direction = "RIGHT";
                break;
            case BOTTOM:
                direction = "BOTTOM";
                break;
            case LEFT:
                direction = "LEFT";
                break;
            case TOP:
                direction = "TOP";
                break;
        }
        return direction;
    }
}
