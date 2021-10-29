package fr.redline.pms.utils;

public class IpInfo {
    private final String ip;
    private final Integer port;

    public IpInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public static IpInfo fromString(String string) {
        String[] ipSplit = string.split(":");
        return new IpInfo(ipSplit[0], Integer.valueOf(ipSplit[1]));
    }

    public String getIp() {
        return this.ip;
    }

    public Integer getPort() {
        return this.port;
    }

    public boolean equals(IpInfo ipInfo) {
        String one = this.ip + ":" + this.port;
        String second = ipInfo.getIp() + ":" + ipInfo.getPort();
        return one.equals(second);
    }

    public String toString() {
        return this.ip + ":" + this.port;
    }

}
