import model.UserMod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.sql.SQLException;

class ClientSession {

    SelectionKey selkey;
    SocketChannel chan;
    ByteBuffer buf;
    CharBuffer bufString;
    String clientMessageString = "";
    UserMod user;
    int id;
    String username;

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

    void read() {

        try {
            int amount_read;

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

            }
            catch (Throwable t) {
                t.printStackTrace();
            }

            System.out.println("(ClientSession.java): sending back " + buf.position() + " bytes");

        }
        catch (Throwable t) {
            disconnect();
            t.printStackTrace();
        }


    }

    private void parser(String s){
        String[] stringPeices = s.split(":", 2);
        String superString = "";

        if(stringPeices[0].equals("Login")){

            ResultSet resultSet = this.user.get(stringPeices[1]);
            try {
                resultSet.last();
                this.username = resultSet.getString("username");

                superString = this.username;
                buf.flip();
                buf = ByteBuffer.wrap(superString.getBytes());

                this.chan.write(this.buf);
                this.buf.clear();

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(stringPeices[0].equals("Sign Up")){
            System.out.println("\n(ClientSession.java): This is a Sign Up");

            if(this.user.insert(stringPeices[1])){
                this.clientMessageString = "true";
            }
            else {
                this.clientMessageString = "false";
            }

        }
        else if(stringPeices[0].equals("Chat")){
            System.out.println("\n(ClientSession.java): This is a chat message");

        }
        else if(stringPeices[0].equals("Update")){
            this.user.insert(stringPeices[1]);
        }
        else {
            System.out.println("\n(ClientSession.java): I need to get the string parsing right");
        }
    }

}