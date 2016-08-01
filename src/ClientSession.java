import model.UserMod;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 *
 * @author eric
 *
 * https://www.youtube.com/user/thepoorMechanic
 *
 * Highly Scalable Server with Java NIO (part 1) https://www.youtube.com/watch?v=nUI4zO6abH0
 * Highly Scalable Server with Java NIO (part 2) https://www.youtube.com/watch?v=AofvCRyvkAk
 *
 */
class ClientSession {

    SelectionKey selkey;
    SocketChannel chan;
    ByteBuffer buf;
    CharBuffer bufString;
    String clientMessageString = "";
    UserMod user;

    ClientSession(SelectionKey selkey, SocketChannel chan) throws Throwable {
        this.user = new UserMod();
        this.selkey = selkey;
        this.chan = (SocketChannel) chan.configureBlocking(false); // asynchronous/non-blocking
        buf = ByteBuffer.allocateDirect(1000); // 1000 byte capacity
        bufString = CharBuffer.allocate(1024); // 1Gb capacity
    }

    void disconnect() {
        MainServer.clientMap.remove(selkey);
        try {
            if (selkey != null)
                selkey.cancel();

            if (chan == null)
                return;

            System.out.println("bye bye " + (InetSocketAddress) chan.getRemoteAddress());
            chan.close();
        } catch (Throwable t) { /** quietly ignore  */ }
    }

    private void parser(String s){
        String[] stringPeices = s.split(":", 2);

        if(stringPeices[0].equals("Login")){
            this.user.get(stringPeices[1]);
            System.out.println("This is a login");
        }
        else if(stringPeices[0].equals("Sign Up")){
            System.out.println("\nThis is a Sign Up");
            System.out.println("\n" + this.user.insert(stringPeices[1]));

            if(this.user.insert(stringPeices[1])){
                this.clientMessageString = "true";
            }
            else {
                this.clientMessageString = "false";
            }

        }
        else {
            System.out.println("\nI need to get the string parsing right");
        }
    }

    void read() {

        try {
            int amount_read = -1;

            try {
                amount_read = chan.read((ByteBuffer) buf.clear());

                buf = (ByteBuffer) buf.clear();

                while(buf.hasRemaining()){
                    char ch = (char) buf.get();
                    System.out.print(ch);
                    clientMessageString += ch;
                }

                this.parser(this.clientMessageString.trim());

                System.out.println("(ClientSession.java): " + amount_read);
                System.out.println("(ClientSession.java): compiled string -> " + this.clientMessageString);

                buf.flip();
                buf = ByteBuffer.wrap(clientMessageString.getBytes());
            }
            catch (Throwable t) {
                t.printStackTrace();
            }

            System.out.println("sending back " + buf.position() + " bytes");

            // turn this bus right around and send it back!
            //buf.flip();
            chan.write(buf);
            buf.clear();
        }
        catch (Throwable t) {
            disconnect();
            t.printStackTrace();
        }


    }

}