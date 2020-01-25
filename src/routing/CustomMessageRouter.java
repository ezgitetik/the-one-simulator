package routing;

import core.Connection;
import core.Message;
import core.Settings;

public class CustomMessageRouter extends MessageRouter {

    private Settings settings;

    public CustomMessageRouter(Settings settings){
        super(settings);
        this.settings=settings;
    }

    @Override
    public void changedConnection(Connection con) {

    }

    @Override
    public CustomMessageRouter replicate() {
        CustomMessageRouter customMessageRouter = new CustomMessageRouter(this.settings);
        return customMessageRouter;
    }

    @Override
    public String toString() {
        return "CustomMessageRouter{" +
                "msgTtl=" + msgTtl +
                '}';
    }
}
