import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketAddressTest {

    @Test
    public void doTest() {
        InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("192.168.1.1", 25565);
        System.out.println(socketAddress.getHostName() + ":" + socketAddress.getPort());
        System.out.println(socketAddress.getHostString());
    }

}
