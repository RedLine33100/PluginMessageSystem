package fr.redline.pms.socket.credential;

public class Credential {

    final String account, password;

    Credential(String account, String password) {
        this.account = account;
        this.password = password;
    }

    public String getAccount() {
        return this.account;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean isCorrectPassword(String password) {
        return this.password.equals(password);
    }

}
