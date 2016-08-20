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
                System.out.println("A client just connected");
                Set<SelectionKey> keys = selector.selectedKeys();


                Iterator<SelectionKey> iT = keys.iterator();

                while (iT.hasNext()){
                    key = iT.next();

                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            read(key);
                        }
                        else if (key.isWritable()) {
                            write("Chat:here you go!!!");
                        }

                    }
                }

                sockets.keySet().removeIf(sockets -> !sockets.isOpen());

            }

        }

        private void write(String string) throws IOException {
            if(key.isWritable()){

            }
            SocketChannel s = (SocketChannel) key.channel();
            ByteBuffer buf = sockets.get(s);

            buf = buf.wrap(string.getBytes());

            while(byteBuffer.hasRemaining()){
                s.write(byteBuffer);
            }
            //s.write(buf); // Wont always write everything
            if (!buf.hasRemaining()) {
                buf.compact();
                key.interestOps(SelectionKey.OP_READ);
            }

        }

        private void read(SelectionKey key) throws IOException {
            System.out.println("Reading ....");

            if(key.isReadable()){
                System.out.println("The key is readable");
                SocketChannel s = (SocketChannel) key.channel();
                s.configureBlocking(false);
                s.register(key.selector(), SelectionKey.OP_READ);

                if(s.isConnected()){
                    System.out.println("The key is connected");
//                    ByteBuffer buf = sockets.get(s);

                    this.byteBuffer = sockets.get(s);

                    if(this.byteBuffer != null){
                        data = s.read(this.byteBuffer);

                        if (data == -1) {
                            s.close();
                            sockets.remove(s);
                            System.out.println("The buffer is empty");
                        }

                        while (this.byteBuffer.hasRemaining()) {
                            char ch = (char) this.byteBuffer.get();
                            System.out.print(ch);
                            clientMessageString += ch;
                        }

                        this.byteBuffer.flip();
                        key.interestOps(SelectionKey.OP_WRITE);
                        System.out.println("MainServer.java -> " + this.clientMessageString);
                        this.parser(this.clientMessageString.trim());

                    }
                    else {
                        System.out.println("The buffer is null");
                    }

                }
                else {
                    System.out.println("This is not connectec, find a way to connect it");
                }

            }
            else {
                System.out.println("The key is not readable");
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

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
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String[] stringPeices = s.split(":", 2);
            String superString = "";

            if (stringPeices[0].equals("Login")) {

                ResultSet resultSet = this.user.get(stringPeices[1]);
                try {
                    resultSet.last();
                    this.username = resultSet.getString("username");

                    superString = this.username;
                    byteBuffer.flip();
                    byteBuffer = ByteBuffer.wrap(superString.getBytes());

                    this.socketChannel.write(this.byteBuffer);
                    this.byteBuffer.clear();

                    String soap = "This is my test string";
                    this.socketChannel.write(ByteBuffer.wrap(soap.getBytes()));
                    this.byteBuffer.flip();

                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (stringPeices[0].equals("Sign Up")) {
                System.out.println("\n(ClientSession.java): This is a Sign Up");

                if (this.user.insert(stringPeices[1])) {
                    this.clientMessageString = "true";
                } else {
                    this.clientMessageString = "false";
                }

            } else if (stringPeices[0].equals("Cha")) {
                System.out.println("\n(ClientSession.java): This is a chat message");

            } else if (stringPeices[0].equals("Update")) {
                this.user.insert(stringPeices[1]);
            } else {
                System.out.println("\n(ClientSession.java): I need to get the string parsing right");
            }
        }

    }

}

