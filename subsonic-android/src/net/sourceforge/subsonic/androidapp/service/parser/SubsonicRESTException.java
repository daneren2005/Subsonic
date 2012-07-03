package net.sourceforge.subsonic.androidapp.service.parser;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class SubsonicRESTException extends Exception {

    private final int code;

    public SubsonicRESTException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
