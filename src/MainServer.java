import model.UserMod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class MainServer {

    public static void main(String[] args) throws Throwable {
        new MainServer();
    }

    protected final static ConcurrentHashMap<SelectionKey, ClientSession> clientMap = new ConcurrentHashMap<SelectionKey, ClientSession>();
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    MainServer() throws Throwable {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 6066));
        this.clientMapper();
    }

    void clientMapper() throws Throwable {
        System.out.println("(MainServer.java): -> clientMapper() just started");

        selector = Selector.open(); // selector is open here

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

        clientMap.put(selectionKey, new ClientSession());
        //selector.selectedKeys().clear();
    }


    class ClientSession {

        SocketChannel socketChannel;
        ByteBuffer byteBuffer;
        String clientMessageString = "";
        UserMod user;
        int id;
        int data;
        String username;
        //Selector selector;
        SelectionKey selectionKey;
        SelectionKey key;

        private final Map<SocketChannel, ByteBuffer> sockets = new ConcurrentHashMap<>();

        public ClientSession() throws Throwable {
            System.out.println("Client Session was created");

            this.user = new UserMod();

//        this.selector = Selector.open();

            // Create byte capacity
            this.byteBuffer = ByteBuffer.allocateDirect(1024); // 1000 byte capacity

            this.connectionListener();
        }

        void disconnect() {
            MainServer.clientMap.remove(selectionKey);
            try {
                if (selectionKey != null)
                    selectionKey.cancel();

                if (socketChannel == null)
                    return;

                System.out.println("bye bye " + socketChannel.getRemoteAddress());
                socketChannel.close();
            } catch (Throwable t) { /** quietly ignore  */}
        }

        protected void connectionListener() throws IOException {

            boolean offSwitch = true;

            while (offSwitch) {
                selector.select(); // Blocking
                Set<SelectionKey> keys = selector.selectedKeys();

                Iterator<SelectionKey> iT = keys.iterator();

                while (iT.hasNext()){
                    key = iT.next();

                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            input(key);
                        }

                    }
                    iT.remove();
                }

                sockets.keySet().removeIf(sockets -> !sockets.isOpen());

            }

        }

        private void sendFile(){

        }

        private void output(String string) throws IOException {

            SocketChannel s = (SocketChannel) key.channel();
            //this.byteBuffer = sockets.get(s);


            this.byteBuffer = ByteBuffer.wrap(string.getBytes());
            this.byteBuffer.flip();
            s.write(this.byteBuffer);
            this.byteBuffer.clear();

        }

        private void input(SelectionKey key) throws IOException {
            System.out.println("Reading ....");

            this.socketChannel = (SocketChannel) key.channel();

            ByteBuffer miniBuf = ByteBuffer.allocate(1024);

            StringBuilder sb = new StringBuilder();
            int read = 0;
            while( (read = socketChannel.read(miniBuf)) > 0 ) {
                miniBuf.flip();
                byte[] bytes = new byte[miniBuf.limit()];
                miniBuf.get(bytes);
                sb.append(new String(bytes));
                miniBuf.clear();
            }
            String msg;
            if(read<0) {
                msg = key.attachment()+" left the chat.\n";
                socketChannel.close();
            }
            else {
//                msg = key.attachment()+": "+sb.toString();
                msg = sb.toString();
            }

            miniBuf.clear();
            System.out.println(msg);

            this.parser(msg);

        }

        private void accept(SelectionKey key) throws IOException {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel s = ssc.accept(); // nonblocking and never null

            System.out.println(s);
            s.configureBlocking(false);
            s.register(key.selector(), SelectionKey.OP_READ);
            sockets.put(s, ByteBuffer.allocateDirect(1024));
        }

        private void parser(String s) {
            System.out.println("We are inside the parser and holding");
            System.out.println(s);

            String[] stringPeices = s.split(":");
            System.out.println(stringPeices[0]);


            if (stringPeices[0].equals("Login")) {
                System.out.println(stringPeices[1]);

                ResultSet resultSet = this.user.get(stringPeices[1]);


                try {
                    resultSet.last();
                    this.username = resultSet.getString("username");
                    resultSet.close();

                    ArrayList allUsers = this.user.allUsers();

                    output("Username:" + this.username);
                    System.out.println("(MainServer.java): _> " + this.username);
                    //Object[] arr = allUsers.toArray();

                    for(int i = 0; i < allUsers.size(); i++){ // Send user one by one to client from array list
                        output("UserList:" + allUsers.get(i).toString());
                        System.out.println("UserList:" + allUsers.get(i).toString());

                        allUsers.remove(i);
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (stringPeices[0].equals("Sign Up")) {

                if (this.user.insert(stringPeices[1])) {
                    this.clientMessageString = "true";
                } else {
                    this.clientMessageString = "false";
                }

            } else if (stringPeices[0].equals("Chat")) {
                System.out.println("\n(ClientSession.java): This is a chat message -> " + stringPeices[1]);
                System.out.println("(ClientSession.java): _> I am so happy the this works");
                System.out.println("(ClientSession.java): _> now for DB insert");
                try {
                    output("Chat:" + stringPeices[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (stringPeices[0].equals("Update")) {
                this.user.insert(stringPeices[1]);
            } else {
                System.out.println("\n(ClientSession.java): I need to get the string parsing right");
            }
        }

    }

}

