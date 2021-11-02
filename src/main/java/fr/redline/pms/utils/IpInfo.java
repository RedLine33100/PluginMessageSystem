package fr.redline.pms.utils;

public class IpInfo {
    private final String ip;

    public IpInfo(String ip, Integer port) {
        this.ip = ip + port;
    }

    public IpInfo(String ip) {
        this.ip = ip;
    }

    public String getIp(boolean port) {
        if (port)
            return this.ip;
        return ip.split(":")[0];
    }

    public Integer getPort() {
        String[] stringSplit = ip.split(":");
        if (stringSplit.length != 2)
            return null;
        return Integer.parseInt(stringSplit[1]);
    }

    public boolean equals(IpInfo ipInfo) {
        return ip.equals(ipInfo.toString());
    }

    public String toString() {
        return this.ip;
    }

}
