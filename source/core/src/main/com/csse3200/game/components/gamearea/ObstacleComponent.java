package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.areas.GameArea;
import com.csse3200.game.utils.math.RandomUtils;

public class ObstacleComponent extends Component {
    private int NumOfObstacles;
    private TerrainComponent terrain;
    private GameArea gameArea;
    private static final String TREE = "tree";
    private static final String ROCK = "rock";

    private void createTrees() {
        GridPoint2 minPos = new GridPoint2(0, 0);
        GridPoint2 maxPos = terrain.getMapBounds(0).sub(2, 2);

        for (int i = 0; i < NumOfObstacles; i++) {
            GridPoint2 randomPos = RandomUtils.random(minPos, maxPos);
            Entity tree = ObstacleFactory.createTree();
            //Spawn entity on game are
        }
    }
}
