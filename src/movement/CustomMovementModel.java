package movement;

import core.Coord;
import core.Settings;
import routing.CustomMessageRouter;

public class CustomMovementModel extends MovementModel {

    private Settings settings;

    public CustomMovementModel(Settings settings) {
        super(settings);
        this.settings=settings;
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public Coord getInitialLocation() {
        return null;
    }

    @Override
    public MovementModel replicate() {
        CustomMovementModel customMovementModel = new CustomMovementModel(this.settings);
        return customMovementModel;
    }
}
