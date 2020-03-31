package custom;

import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import interfaces.SimpleBroadcastInterface;
import movement.MapRouteMovement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import routing.EpidemicRouter;

import java.util.Arrays;
import java.util.Collections;

@Ignore
public class LikelihoodComparatorTest {

   @Mock
   private SimpleBroadcastInterface simpleBroadcastInterface;

   @Mock
   private MapRouteMovement mapRouteMovement;

   @Mock
   private EpidemicRouter epidemicRouter;

   @Before
   public void init(){
       MockitoAnnotations.initMocks(this);
   }
    @Test
    public void compare_MessageFromEqualsFromNode_FromAndToNodeHasCustomer_FromNodeLikeliHoodGraterThanToNode_MessageShouldNotForward() {
        DTNHost fromNode = getFromNode();
        DTNHost toNode = getToNode();
        Message message = getMessageWhichBelongFromNode();

        //PowerMockito.mockStatic(LikelihoodComparator.class);
        Mockito.when(LikelihoodComparator.likelihoodMobUpdate(fromNode,message)).thenReturn(10D);
        Mockito.when(LikelihoodComparator.likelihoodMobUpdate(toNode,message)).thenReturn(1D);

        Assert.assertNull(LikelihoodComparator.compare(message,fromNode, toNode).getTo());

    }

    private DTNHost getFromNode() {
        return new DTNHost(Collections.emptyList(),
                Collections.emptyList(),
                "",
                Arrays.asList(simpleBroadcastInterface),
                null,
                mapRouteMovement,
                epidemicRouter,
                "node-1");
    }

    private DTNHost getToNode() {
        return new DTNHost(Collections.emptyList(),
                Collections.emptyList(),
                "",
                Arrays.asList(simpleBroadcastInterface),
                null,
                mapRouteMovement,
                epidemicRouter,
                "node-2");
    }

    private Message getMessageWhichBelongFromNode() {
        return new Message(getFromNode(), null, null, 10000);
    }

    private Message getMessageWhichBelongToNode() {
        return new Message(getToNode(), null, null, 10000);
    }

   /* private List<NetworkInterface> getnetworkInterfaces(){
        SimpleBroadcastInterface simpleBroadcastInterface=new SimpleBroadcastInterface(se);
    }*/
}
