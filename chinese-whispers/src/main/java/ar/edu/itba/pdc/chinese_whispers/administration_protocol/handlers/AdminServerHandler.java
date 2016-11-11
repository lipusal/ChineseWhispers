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
import java.util.*;

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

    private static final int OK_CODE = 100;

    private static final int UNAUTHORIZED_CODE = 200;
    private static final int FORBIDDEN_CODE = 201;
    private static final int FAILURE_CODE = 202;
    private static final int UNEXPECTED_COMMAND_CODE = 203;
    private static final int WRONG_NUMBER_OF_PARAMETERS_CODE = 204;
    private static final int WRONG_SYNTAX_OF_PARAMETERS_CODE = 205;
    private static final int UNKNOWN_COMMAND_CODE = 206;
    private static final int TOO_MANY_REQUEST_CODE = 207;
    private static final int NOT_FOUND_CODE = 208;

    private static final int INTERNAL_SERVER_ERROR_CODE = 300;
    private static final int SERVICE_UNAVAILABLE__CODE = 301;
    private static final int PROTOCOL_VERSION_NOT_SUPPORTED_CODE = 302;
    private static final int COMMAND_NOT_IMPLEMENTED_CODE = 303;





    /**
     * Boolean telling if user has loggedIn
     */
    private boolean isLoggedIn;
    /**
     * String with user LogguedIn
     */
    private String userLogged;
    /**
     * Set with the names of the commands that need authorization
     */
    private Set<String> authCommand;
    /**
     * Language set for responses
     */
    private String language;

    // Communication stuff
    /**
     * Contains parcial messages read
     */
    private final Deque<Byte> messageRead; //TODO no cola infinita
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
        language = "ENG";
        isLoggedIn = false;
        //Commands that need authorization:
        authCommand = new HashSet<>();
        authCommand.add("L337");
        authCommand.add("UNL337");
        authCommand.add("BLCK");
        authCommand.add("UNBLCK");
        authCommand.add("MPLX");
        authCommand.add("CNFG");
        authCommand.add("MTRC");
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
            metricsProvider.addAdministrationReadBytes(readBytes);
            if (readBytes >= 0) {
                for (int i = 0; i < readBytes; i++) {
                    byte b = inputBuffer.get(i);
                    if (b == 10) {
                        process(messageRead, key);
                    } else if(b!=13){
                        messageRead.offer(b);
                    }
                }
            } else if (readBytes == -1) {
                closeHandler(key);
            }
        } catch (IOException ignored) {//TODO why ignored?
            // I/O error (for example, connection reset by peer)
        }
        //Do NOT remove this if this is merged with XMPPHandler. Needs to be adapted in that case.
        if (!writeMessages.isEmpty()) key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    private void process(Deque<Byte> messageRead, SelectionKey key) {

        //TODO what if message is not well formed?
        byte[] byteArray = new byte[messageRead.size()];


        int i = 0;
        while (!messageRead.isEmpty()) {
            byteArray[i] = messageRead.poll();
            i++;
        }

        String string = new String(byteArray);
        System.out.println(string);
        String[] requestElements = string.split(" ");
        Response response = new Response();
        processCommand(response,requestElements, key);


       for (byte b: (response.getResponseCode()+" \""+response.getResponseMessage()+"\"").getBytes()){
            writeMessages.offer(b);
        }
        writeMessages.offer(new Byte("10"));
    }

    private void processCommand(Response response, String[] requestElements, SelectionKey key) {
        if(requestElements.length==0){
            response.setResponseMessage(DEFAULT_UNKNOWN_RESPONSE);
            response.setResponseCode(UNKNOWN_COMMAND_CODE);
            return;
        }

        String command = requestElements[0].toUpperCase();
        if (!isLoggedIn && authCommand.contains(command)) {
            response.setResponseMessage(DEFAULT_UNAUTHORIZED_RESPONSE);
            response.setResponseCode(UNAUTHORIZED_CODE);
            return;
        }

        switch (command) {
            case "L337":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    configurationsConsumer.setL337Processing(true);
                    response.setToDefaultOK();
                }
                break;
            case "UNL337":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    configurationsConsumer.setL337Processing(false);
                    response.setToDefaultOK();
                }
                break;
            case "LANG":
                if (checkLength(requestElements.length, new int[]{1, 2}, response)) {
                    if (requestElements.length == 1) {
                        response.setResponseMessage("ENG");
                        response.setResponseCode(OK_CODE);
                    }
                    if (requestElements.length == 2) {
                        if (requestElements[1].equals("ENG")) {
                            response.setToDefaultOK();
                        } else {
                            response.setResponseCode(NOT_FOUND_CODE);
                            response.setResponseMessage("Language not supported");
                        }
                    }
                }
                break;
            case "AUTH":
                if (isLoggedIn) {
                    response.setResponseMessage("Must QUIT to logIn again");
                    response.setResponseCode(UNEXPECTED_COMMAND_CODE);
                    break;
                }
                if (checkLength(requestElements.length, new int[]{3}, response)) {
                    if (authenticationProvider.isValidUser(requestElements[1], requestElements[2])) {
                        response.setToDefaultOK();
                        isLoggedIn = true;
                    } else {
                        response.setResponseMessage("WRONG USERNAME/PASSWORD");
                        response.setResponseCode(FAILURE_CODE);
                    }
                }
                break;
            case "LOGOUT":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    response.setToDefaultOK();
                    isLoggedIn = false;
                    userLogged = null;
                }
                break;
            case "QUIT":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    response.setToDefaultOK();
                    closeHandler(key);
                }
                break;
            case "HELP":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    response.setResponseMessage("AUTH LANG HELP QUIT L337 UNL337 BLCK UNBLCK MPLX CNFG MTRC LOGOUT"); //TODO do list
                    response.setResponseCode(OK_CODE);
                }
                break;
            case "BLCK":
                if (checkLength(requestElements.length, new int[]{2}, response)) {
                    configurationsConsumer.silenceUser(requestElements[1]);
                    response.setToDefaultOK();
                }
                break;
            case "UNBLCK":
                if (checkLength(requestElements.length, new int[]{2}, response)) {
                    configurationsConsumer.unSilenceUser(requestElements[1]);
                    response.setToDefaultOK();
                }
                break;
            case "MPLX":
                if (checkLength(requestElements.length, new int[]{3, 4}, response)) {//TODO default port?
                    if (requestElements.length == 4) {
                        if (requestElements[1].equals("DEFAULT")) {
                            try {
                                Integer port = Integer.valueOf(requestElements[3]);
                                if (port < 0 || port > 0xFFFF) {
                                    response.setResponseCode(WRONG_SYNTAX_OF_PARAMETERS_CODE);
                                    response.setResponseMessage("Port needs to be a number between 1 and FFFE");
                                } else {
                                    configurationsConsumer.setDefaultServer(requestElements[2], port);
                                    response.setToDefaultOK();
                                }
                            } catch (NumberFormatException nfe) {
                                response.setResponseCode(WRONG_SYNTAX_OF_PARAMETERS_CODE);
                                response.setResponseMessage("Port needs to be a number between 1 and FFFE");//TODO FFFE
                            }
                        } else {
                            try {
                                Integer port = Integer.valueOf(requestElements[3]);
                                if (port < 0 || port > 0xFFFF) {
                                    response.setResponseCode(WRONG_SYNTAX_OF_PARAMETERS_CODE);
                                    response.setResponseMessage("Port needs to be a number between 1 and FFFE");
                                } else {
                                    configurationsConsumer.multiplexUser(requestElements[1], requestElements[2],
                                            Integer.valueOf(requestElements[3]));
                                    response.setToDefaultOK();
                                }
                            } catch (NumberFormatException nfe) {
                                response.setResponseCode(WRONG_SYNTAX_OF_PARAMETERS_CODE);
                                response.setResponseMessage("Port needs to be a number between 1 and FFFE");
                            }
                        }
                        break;
                    }
                    if (requestElements.length == 3) {
                        if (requestElements[1].equals("DEFAULT")) {
                            response.setToDefaultOK();
                            break;
                        }
                        if (requestElements[2].equals("DEFAULT")) {
                            configurationsConsumer.multiplexToDefaultServer(requestElements[1]);
                            response.setToDefaultOK();
                            break;
                        }
                        response.setResponseCode(WRONG_SYNTAX_OF_PARAMETERS_CODE);
                        response.setResponseMessage("Must add port or set server to DEFAULT");
                    }
                }
                break;
            case "CNFG":
                if (checkLength(requestElements.length, new int[]{1}, response)) {
                    StringBuilder responseBuild = new StringBuilder();
                    responseBuild.append("BLCK");
                    if (configurationsConsumer.getSilencedUsers().isEmpty()) {
                        responseBuild.append(" NONE");
                    } else {
                        for (String silencedUser : configurationsConsumer.getSilencedUsers()) {
                            responseBuild.append(" " + silencedUser);
                        }
                    }
                    responseBuild.append("\nMPLX");
                    if (configurationsConsumer.getMultiplexedUsers().isEmpty()) {
                        responseBuild.append(" NONE");
                    } else {
                        for (String clientJid : configurationsConsumer.getMultiplexedUsers().keySet()) {
                            responseBuild.append(" " + clientJid + " " + configurationsConsumer.getMultiplexedUsers().get(clientJid) + " * "); //TODO way of showing info
                        }
                    }
                    responseBuild.append("\nDEFAULT ");
                    if (Configurations.getInstance().getDefaultServerHost() == null || Configurations.getInstance().getDefaultServerPort() == null) {
                        responseBuild.append("NONE");
                    } else {
                        responseBuild.append(Configurations.getInstance().getDefaultServerHost() + " " + Configurations.getInstance().getDefaultServerPort());

                    }
                    responseBuild.append("\nL337");
                    responseBuild.append(Configurations.getInstance().isProcessL337() ? " ON" : " OFF");

                    response.setResponseCode(OK_CODE);
                    response.setResponseMessage(responseBuild.toString());
                }
                break;
            case "MTRC":
                if (checkLength(requestElements.length, new int[]{1, 2}, response)) {
                    if (requestElements.length == 1) {
                        //response =  "BytesRead: "+metricsProvider.getReadBytes() + " BytesSent: "+metricsProvider.getSentBytes();
                        Map<String, String> metrics = metricsProvider.getMetrics();
                        StringBuilder responseBuilder = new StringBuilder(); //Reuse?
                        for (String metringName : metrics.keySet()) {
                            responseBuilder.append(metringName);
                            responseBuilder.append(" ");
                        }
                        response.setResponseMessage(responseBuilder.toString());
                        response.setResponseCode(OK_CODE);
                    }
                    if (requestElements.length == 2) {
                        Map<String, String> metrics = metricsProvider.getMetrics();
                        if (requestElements[1].equals("ALL")) {
                            StringBuilder responseBuilder = new StringBuilder(); //TODO Reuse?
                            for (String metringName : metrics.keySet()) {
                                responseBuilder.append(metringName + " ");
                                responseBuilder.append(metrics.get(metringName) + " ");
                            }
                            response.setResponseMessage(responseBuilder.toString());
                            response.setResponseCode(OK_CODE);
                        } else {
                            String metricName = requestElements[1];
                            if (!metrics.containsKey(metricName)) {
                                response.setResponseMessage("Unimplemented metric");
                                response.setResponseCode(NOT_FOUND_CODE);
                            } else {
                                response.setResponseMessage(metrics.get(metricName));
                                response.setResponseCode(OK_CODE);
                            }
                        }
                    }
                }
                break;
            default:
                response.setResponseMessage(DEFAULT_UNKNOWN_RESPONSE);
                response.setResponseCode(UNKNOWN_COMMAND_CODE);
                break;
        }
    }

    private boolean checkLength(int length, int[] lengths, Response response) {
        for(int posibleLength: lengths){
            if(posibleLength==length){
                return true;
            }
        }
        response.setResponseMessage(DEFAULT_WRONG_PARAMETERS_RESPONSE);
        response.setResponseCode(WRONG_NUMBER_OF_PARAMETERS_CODE);
        return false;
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
                    //TODO change to non-blocking
                    while (outputBuffer.hasRemaining()); // Continue writing if message wasn't totally written
                } catch (IOException e) {
                    int bytesSent = outputBuffer.limit() - outputBuffer.position();
                    byte[] restOfMessage = new byte[message.length - bytesSent];
                    System.arraycopy(message, bytesSent, restOfMessage, 0, restOfMessage.length);
                }
            }

        }

        if (writeMessages.isEmpty()){
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            if (isClosing) {
                handleClose(key);
            }
        }

        System.out.print("Bytes written by administrator: " + byteWritten);
        if(message!=null) System.out.println(" Message: " + new String(message));
        else System.out.println("");
       metricsProvider.addAdministrationSentBytes(byteWritten);
    }

    private static class Response{

        private String responseMessage;

        private int responseCode;

        public Response(){
            responseCode= COMMAND_NOT_IMPLEMENTED_CODE;
            responseMessage="NOT IMPLEMENTED YET";
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        protected void setToDefaultOK(){
            this.responseMessage = DEFAULT_OK_RESPONSE;
            this.responseCode = OK_CODE;
        }
    }

    @Override
    public boolean handleError(SelectionKey key) {
        return false; // TODO: change as specified in javadoc
    }

    @Override
    public boolean handleClose(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException e) {
            // TODO: what should we do here?
            return false;
        }
        return true;
    }
}
