package _08final.mvc.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Bullet extends Sprite {



    public Bullet(EnemyShip enemyShip, int speed) {



        setTeam(Team.FOE);
        setColor(Color.RED);

        setRadius(10);


        setCenter(enemyShip.getCenter());

        setOrientation(enemyShip.getOrientation());

        setDeltaY(speed);


        //defined the points on a cartesian grid
        List<Point> listPoints = new ArrayList<>();
        listPoints.add(new Point(0, 3)); //top point
        listPoints.add(new Point(1, -1));
        listPoints.add(new Point(0, -2));
        listPoints.add(new Point(-1, -1));

        setCartesians(listPoints.stream()
                .toArray(Point[]::new));

    }
}

