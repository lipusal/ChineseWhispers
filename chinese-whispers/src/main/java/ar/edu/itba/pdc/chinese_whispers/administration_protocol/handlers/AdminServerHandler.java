package ar.edu.itba.pdc.chinese_whispers.administration_protocol.handlers;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.AuthenticationProvider;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.application.Configurations;
import ar.edu.itba.pdc.chinese_whispers.connection.TCPHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Created by Droche on 30/10/16.
 */
public class AdminServerHandler implements TCPHandler { //TODO Make case insensitive for users admins? Make errors responses follow codes. Error msj between "".

    // Constants
    /**
     * Default response for protocol when everything is OK.
     */
    private static final String DEFAULT_OK_RESPONSE = "OK";
    /**
     * Default response for protocol when unknown command is used.
     */
    private static final String DEFAULT_UNKNOWN_RESPONSE = "UNKNOWN COMMAND";
    /**
     * Default response for protocol when command is unknown.
     */
    private static final String DEFAULT_UNAUTHORIZED_RESPONSE = "UNAUTHORIZED. Needs to LogIn using AUTH command";
    /**
     * Default response for protocol when command has wrong parameters.
     */
    private static final String DEFAULT_WRONG_PARAMETERS_RESPONSE = "WRONG PARAMETERS";
    /**
     * The buffers size.
     */
    private static final int BUFFER_SIZE = 1024;


    /**
     * Boolean telling if user has loggedIn
     */
    private boolean isLoggedIn = false;

    // Communication stuff
    /**
     * Contains parcial messages read
     */
    private final Deque<Byte> messageRead;
    /**
     * Contains messges to be written
     */
    protected final Deque<Byte> writeMessages;
    /**
     * Buffer from to fill when reading
     */
    private final ByteBuffer inputBuffer;
    /**
     * Buffer to fill when writing
     */
    private final ByteBuffer outputBuffer;
    /**
     * boolean to tell if the handler is already closing
     */
    private boolean isClosing;


    /**
     * Object that provides the handler with metrics.
     */
    private final MetricsProvider metricsProvider;

    /**
     * Object that will store configurations.
     */
    private final ConfigurationsConsumer configurationsConsumer;

    /**
     * Object that provides the handler with authentication data.
     */
    private final AuthenticationProvider authenticationProvider;


    public AdminServerHandler(MetricsProvider metricsProvider,
                              ConfigurationsConsumer configurationsConsumer,
                              AuthenticationProvider authenticationProvider) {
        messageRead = new ArrayDeque<>();
        writeMessages = new ArrayDeque<>();
        inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.metricsProvider = metricsProvider;
        this.configurationsConsumer = configurationsConsumer;
        this.authenticationProvider = authenticationProvider;
    }


    /**
     * Makes this {@link AdminServerHandler} to be closable (i.e. stop receiving messages, send all unsent messages,
     * send close message, and close the corresponding key's channel).
     * Note: Once this method is executed, there is no chance to go back.
     */
    /* package */ void closeHandler(SelectionKey key) {
        if (isClosing) return;
        System.out.println("Close AdminServerHandler");
        isClosing = true;
        // TODO: What happens if handler contains half a message?
        if (key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ); // Invalidates reading
        } else {
            handleClose(key); // If key is not valid, proceed to close the handler without writing anything
        }
    }


    @Override
    public void handleRead(SelectionKey key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        SocketChannel channel = (SocketChannel) key.channel();
        inputBuffer.clear();
        try {
            int readBytes = channel.read(inputBuffer);
            System.out.println("ReadBytes= " + readBytes);
            metricsProvider.addReadBytes(readBytes);//TODO do we lose bytesSent if exception?
            if (readBytes >= 0) {
                for (int i = 0; i < readBytes; i++) {
                    byte b = inputBuffer.get(i);
                    if (b == 10) {
                        process(messageRead);
                    } else {
                        messageRead.offer(b);
                    }
                }
            } else if (readBytes == -1) {
                handleClose(key);
            }
        } catch (IOException ignored) {
            // I/O error (for example, connection reset by peer)
        }
        //Do NOT remove this if this is merged with XMPPHandler. Needs to be adapted in that case.
        if (!writeMessages.isEmpty()) key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    private void process(Deque<Byte> messageRead) {
        //TODO unsensitive
        //TODO logOut != QUIT
//TODO handle messages right
        //TODO process message
        //TODO what if message is not well formed?
        byte[] byteArray = new byte[messageRead.size()];


        int i = 0;
        while (!messageRead.isEmpty()) {
            byteArray[i] = messageRead.poll();
            i++;
        }

        String string = new String(byteArray);
        String response = "NOT IMPLEMENTED";
        System.out.println(string);
        String[] requestElements = string.split(" ");
        String command = requestElements[0];

        if (!isLoggedIn && !command.equals("AUTH")) response = DEFAULT_UNAUTHORIZED_RESPONSE;
        else {
            switch (command) {
                case "L337":
                    if (requestElements.length != 1) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        configurationsConsumer.setL337Processing(true);
                        response = DEFAULT_OK_RESPONSE;
                    }
                    break;
                case "UNL337":
                    if (requestElements.length != 1) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        configurationsConsumer.setL337Processing(false);
                        response = DEFAULT_OK_RESPONSE;
                    }
                    break;
                case "AUTH":
                    if (isLoggedIn) {
                        response = "Must QUIT to logIn again";
                        break;
                    }
                    if (requestElements.length != 3) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        if (authenticationProvider.isValidUser(requestElements[1], requestElements[2])) {
                            response = DEFAULT_OK_RESPONSE;
                            isLoggedIn = true;
                        } else {
                            response = "WRONG USERNAME/PASSWORD";
                        }
                    }
                    break;
                case "QUIT":
                    if (requestElements.length != 1) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        //TODO QUIT
                        response = "NOT IMPLEMENTED YET";
                    }

                    break;
                case "HELP": //TODO do not need auth
                    if (requestElements.length != 1) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        response = "L337 UNL337 AUTH QUIT HELP BLCK UNBLCK MPLX CNFG MTRC"; //TODO do list
                    }
                    break;
                case "BLCK":
                    if (requestElements.length != 2) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        configurationsConsumer.silenceUser(requestElements[1]);
                        response = DEFAULT_OK_RESPONSE;
                    }
                    break;
                case "UNBLCK":
                    if (requestElements.length != 2) response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    else {
                        configurationsConsumer.unSilenceUser(requestElements[1]);
                        response = DEFAULT_OK_RESPONSE;
                    }
                    break;
                case "MPLX":
                    if (requestElements.length == 4) {
                        response = DEFAULT_OK_RESPONSE;
                        if (requestElements[1].equals("DEFAULT")) {
                            configurationsConsumer.setDefaultServer(requestElements[2],
                                    Integer.valueOf(requestElements[3]));
                        } else {
                            configurationsConsumer.multiplexUser(requestElements[1], requestElements[2],
                                    Integer.valueOf(requestElements[3])); // TODO wat if port not a number?
                        }
                        break;
                    }
                    if (requestElements.length == 3) {
                        if (requestElements[1].equals("DEFAULT")) {
                            response = DEFAULT_OK_RESPONSE;
                            break;
                        }
                        if (requestElements[2].equals("DEFAULT")) {
                            configurationsConsumer.multiplexToDefaultServer(requestElements[1]);
                            break;
                        }
                    }
                    response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    break;
                case "CNFG":
                    if (requestElements.length != 1) {
                        response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    } else {
                        StringBuilder responseBuild = new StringBuilder();
                        responseBuild.append("BLCK");
                        if(configurationsConsumer.getSilencedUsers().isEmpty()){
                            responseBuild.append(" NONE");
                        }else{
                            for(String silencedUser: configurationsConsumer.getSilencedUsers()){
                                responseBuild.append(" "+silencedUser);
                            }
                        }
                        responseBuild.append("\nMPLX");
                        if(configurationsConsumer.getSilencedUsers().isEmpty()){
                            responseBuild.append(" NONE");
                        }else{
                            for(String clientJid: configurationsConsumer.getMultiplexedUsers().keySet()){
                                responseBuild.append(" "+clientJid+" "+configurationsConsumer.getMultiplexedUsers().get(clientJid)+" * "); //TODO way of showing info
                            }
                        }
                        responseBuild.append("\nL337" );
                        responseBuild.append(Configurations.getInstance().isProcessL337()? " ON" : " OFF");
                        response = responseBuild.toString();
                    }
                    break;
                case "MTRC":
                    if (requestElements.length == 1) {
                        //response =  "BytesRead: "+metricsProvider.getReadBytes() + " BytesSent: "+metricsProvider.getSentBytes();
                        Map<String,String> metrics = metricsProvider.getMetrics();
                        StringBuilder responseBuilder = new StringBuilder(); //Reuse?
                        for(String metringName: metrics.keySet()){
                            responseBuilder.append(metringName);
                            responseBuilder.append(" ");
                        }
                        response = responseBuilder.toString();
                        break;
                    }
                    if (requestElements.length == 2) {
                        Map<String,String> metrics = metricsProvider.getMetrics();
                        if(requestElements[1].equals("ALL")){
                            StringBuilder responseBuilder = new StringBuilder(); //Reuse?
                            for(String metringName: metrics.keySet()){
                                responseBuilder.append(metringName+" ");
                                responseBuilder.append(metrics.get(metringName)+" ");
                            }
                            response = responseBuilder.toString();
                        }else{
                            String metricName = requestElements[1];
                            if(!metrics.containsKey(metricName)){
                                response = "Unimplemented metric";
                            }else{
                                response = metrics.get(metricName);
                            }
                        }
                        break;
                    }
                    response = DEFAULT_WRONG_PARAMETERS_RESPONSE;
                    break;
                default:
                    response = DEFAULT_UNKNOWN_RESPONSE;
                    break;
            }
        }


        for (byte b : response.getBytes()) {
            if (b != '\13') writeMessages.offer(b);
        }
        writeMessages.offer(new Byte("10"));
    }

    @Override
    public void handleWrite(SelectionKey key) {// TODO: check how we turn on and off

        int byteWritten = 0;
        byte[] message = null;
        if (!writeMessages.isEmpty()) {
            if (writeMessages.size() > BUFFER_SIZE) {
                message = new byte[BUFFER_SIZE];
            } else {
                message = new byte[writeMessages.size()];
            }
            for (int i = 0; i < message.length; i++) {
                message[i] = writeMessages.poll();
            }
            if (message.length > 0) {
                SocketChannel channel = (SocketChannel) key.channel();
                outputBuffer.clear();
                outputBuffer.put(message);
                outputBuffer.flip();
                try {
                    do {
                        byteWritten += channel.write(outputBuffer);
                    }
                    // TODO check if this is not blocking. In case it's blocking, we can return those bytes to the queue with a push operation (it's a deque)
                    while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written
                } catch (IOException e) {
                    int bytesSent = outputBuffer.limit() - outputBuffer.position();
                    byte[] restOfMessage = new byte[message.length - bytesSent];
                    System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
                }
            }

        }

        if (writeMessages.isEmpty() && (messageRead.isEmpty() || byteWritten==0 ) ){ //TODO check if byteWritten==0 can happen
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            if (isClosing) {
                handleClose(key);
            }
        }

        System.out.print("Bytes written by administrator: " + byteWritten);
        if(message!=null) System.out.println(" Message: " + new String(message));
        else System.out.println("");
        metricsProvider.addSentBytes(byteWritten);


    }


    @Override
    public boolean handleError(SelectionKey key) {
        return false; // TODO: change as specified in javadoc
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        try {
            key.channel().close();
            // TODO: send some message before? Note: if yes, we can't close the peer's key now.
        } catch (IOException e) {

        }
        return false; // TODO: change as specified in javadoc
    }
}
