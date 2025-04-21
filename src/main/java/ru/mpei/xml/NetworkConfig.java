package ru.mpei.xml;

import lombok.Data;

@Data
public class NetworkConfig {
    private String macAddressSrc;
    private String macAddressDst;
    private String appId;
    private String etherType;

}