package fr.redline.pms.socket.credential;

import java.util.HashMap;

public class CredentialManager {

    private final HashMap<String, Credential> credential = new HashMap<>();
    boolean anonymous = false;

    public boolean hasCredential(String account) {
        return this.credential.containsKey(account);
    }

    public boolean removeCredential(String account) {
        this.credential.remove(account);
        return !hasCredential(account);
    }

    public boolean addCredential(String account, String password) {
        if (hasCredential(account))
            return false;
        this.credential.put(account, new Credential(account, password));
        return true;
    }

    public Credential getCredential(String username) {
        return credential.get(username);
    }

    public Credential getCredential(String username, String password) {
        Credential credential = getCredential(username);
        return credential.getPassword().equals(password) ? credential : null;
    }

    public Credential generateCredential(String username, String password) {
        return new Credential(username, password);
    }

    public void authorizeAnonymous(boolean b) {
        this.anonymous = b;
        if (b)
            addCredential("anonymous", "null");
        else
            removeCredential("anonymous");
    }

    public Credential getAnonymousCredential() {
        return new Credential("anonymous", "null");
    }

}
