package ar.edu.itba.chinese_whispers.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by jbellini on 27/10/16.
 */
public abstract class TCPSelector {

    /* package */ static final int BUFFER_SIZE = 1024;

    protected final Selector selector;

    private final TCPSelector childSelector;

    final private TCPHandler handler;


    public TCPSelector(TCPHandler handler) throws IOException {
        if (this.getClass() == TCPSelector.class) {
            throw new IllegalArgumentException("The child selector can't be an abstract TCPSelector");
        }
        this.selector = Selector.open();
        this.childSelector = this;
        this.handler = handler;

    }

    protected TCPHandler getTCPHandler() {
        return handler;
    }


    public boolean doSelect() {

        try {
            if (selector.selectNow() == 0) {
                return false; // No IO operation to be performed on this selector
            }
        } catch (IOException e) {
            return false;
        }

        // If control reached here, there are IO Operations pending to be handled
        for (SelectionKey key : selector.selectedKeys()) {
//            if (key.isAcceptable()) {
//                handler.handleAccept(key);
//            }
            TCPSelectorSpecificOperation(key);

            if (key.isReadable()) {
                handler.handleRead(key);
            }

            if (key.isValid() && key.isWritable()) {
                handler.handleWrite(key);
            }
        }
        selector.selectedKeys().clear();
        return true;

    }


    protected abstract void TCPSelectorSpecificOperation(SelectionKey key);


}
