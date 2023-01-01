package _08final.mvc.model;


import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.awt.*;

import _08final.mvc.controller.Game;

public class Coin extends Sprite{
    private final int radius = 5;
    public Coin () {
        setTeam(Team.FLOATER);
        setSpin(5);
        setDeltaX(0);
        setDeltaY(0);
        setColor(Color.YELLOW);
        int randomX = Game.R.nextInt(9) + 1;
        int randomY = Game.R.nextInt(4) + 2;
        setCenter(new Point(Game.DIM.width * randomX / 10, Game.DIM.height * randomY / 10));
        setRadius(radius);
        setCartesians(genCircle());

    }

    private Point[] genCircle(){
        //6.283 is the max radians
        final int MAX_RADIANS_X1000 =6283;

        Supplier<PolarPoint> polarPointSupplier = () -> {
            double r = 0.8; //number between 0.8 and 0.999

            double theta = Game.R.nextInt(MAX_RADIANS_X1000) / 1000.0; // number between 0 and 6.282

            return new PolarPoint(r,theta);
        };

        //random number of vertices between 17 and 23
        final int vertices = 100;

        return polarToCartesian(
                Stream.generate(polarPointSupplier)
                        .limit(vertices)
                        .sorted(new Comparator<PolarPoint>() {
                            @Override
                            public int compare(PolarPoint pp1, PolarPoint pp2) {
                                return  pp1.getTheta().compareTo(pp2.getTheta());
                            }
                        })
                        .collect(Collectors.toList())
        );

    }

    @Override
    public void render(Graphics g) {

            // to render this Sprite, we need to, 1: convert raw cartesians to raw polars, 2: adjust polars
            // for orientation of sprite. Convert back to cartesians 3: adjust for center-point (location).
            // and 4: pass the cartesian-x and cartesian-y coords as arrays, along with length, to drawPolygon().

            //convert raw cartesians to raw polars
            List<PolarPoint> polars = cartesianToPolar(Arrays.asList(getCartesians()));

            //rotate raw polars given the orientation of the sprite. Then convert back to cartesians.
            Function<PolarPoint, Point> adjustForOrientation =
                    pp -> new Point(
                            (int)  (pp.getR() * getRadius()
                                    * Math.sin(Math.toRadians(getOrientation())
                                    + pp.getTheta())),

                            (int)  (pp.getR() * getRadius()
                                    * Math.cos(Math.toRadians(getOrientation())
                                    + pp.getTheta())));

            // adjust for the location (center-point) of the sprite.
            // the reason we subtract the y-value has to do with how Java plots the vertical axis for
            // graphics (from top to bottom)
            Function<Point, Point> adjustForLocation =
                    p -> new Point(
                            getCenter().x + p.x,
                            getCenter().y - p.y);

            //for debugging center-point. Feel free to remove these two lines.
            //#########################################
            g.setColor(Color.ORANGE);
            g.fillOval(getCenter().x - 1, getCenter().y - 1, 8, 8);
            //g.drawOval(getCenter().x - getRadius(), getCenter().y - getRadius(), getRadius() *2, getRadius() *2);
            //#########################################
    }
}
