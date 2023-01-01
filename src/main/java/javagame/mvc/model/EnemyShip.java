package javagame.mvc.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javagame.mvc.controller.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class EnemyShip extends Sprite {

    // ==============================================================
    // FIELDS
    // ==============================================================

    final int DEGREE_STEP = 9;
    //must be multiple of 3


    // ==============================================================
    // CONSTRUCTOR
    // ==============================================================

    public EnemyShip() {

        setTeam(Team.FLOATER);

        //this is the size (radius) of the falcon
        setRadius(25);
        setColor(Color.RED);
        setOrientation(270);


        List<Point> pntCs = new ArrayList<>();

        //Alien Ship Design
        pntCs.add(new Point(-3,1));
        pntCs.add(new Point(-6,0));
        pntCs.add(new Point(-6,-1));
        pntCs.add(new Point(-2,-2));
        pntCs.add(new Point(3,-2));
        pntCs.add(new Point(7,-1));
        pntCs.add(new Point(7,0));
        pntCs.add(new Point(4,1));
        pntCs.add(new Point(1,0));
        pntCs.add(new Point(0,0));
        pntCs.add(new Point(-3,1));
        pntCs.add(new Point(-3,0));
        pntCs.add(new Point(0,-1));
        pntCs.add(new Point(1,-1));
        pntCs.add(new Point(5,0));
        pntCs.add(new Point(4,1));
        pntCs.add(new Point(4,2));
        pntCs.add(new Point(3,3));
        pntCs.add(new Point(1,4));
        pntCs.add(new Point(0,4));
        pntCs.add(new Point(-2,3));
        pntCs.add(new Point(-3,2));
        pntCs.add(new Point(-3,1));





        setCartesians(pntCs);
    }

    @Override
    public boolean isProtected() {
        return getFade() < 255;
    }

    // ==============================================================
    // METHODS
    // ==============================================================




} //end class
