package fr.redline.pms.utils;

public class IpInfo {
    private final String ip;
    private final Integer port;

    public IpInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public IpInfo(String ip) {
        this.ip = ip;
        if (ip.contains(":"))
            port = Integer.parseInt(ip.split(":")[1]);
        else port = null;
    }

    public static IpInfo fromString(String string) {
        String[] ipSplit = string.split(":");
        return new IpInfo(ipSplit[0]);
    }

    public String getIp() {
        return this.ip;
    }

    public Integer getPort() {
        return this.port;
    }

    public String toString() {
        return this.ip + ":" + this.port;
    }

}
