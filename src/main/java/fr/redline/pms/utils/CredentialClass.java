package fr.redline.pms.utils;

import java.util.HashMap;

public class CredentialClass {

    private final HashMap<String, String> credential = new HashMap<>();
    boolean anonymous = false;

    public boolean isAccount(String account, String password) {
        if (this.anonymous) {
            if (account == null)
                return true;
            if (account.equals("anonymous") && password == null)
                return true;
        }
        if (password == null)
            return false;
        if (!this.credential.containsKey(account))
            return false;
        return this.credential.get(account).equals(password);
    }

    public boolean hasCredential(String name) {
        return this.credential.containsKey(name);
    }

    public void removeCredential(String name) {
        this.credential.remove(name);
    }

    public boolean addCredential(String name, String password) {
        if (hasCredential(name))
            return false;
        this.credential.put(name, password);
        return true;
    }

    public void authorizeAnonymous(boolean b) {
        this.anonymous = b;
    }

    public String getEncryptedPassword(String account){
        return credential.get(account);
    }

}
