package ru.mpei.xml;

import lombok.Data;

@Data
public class GooseConfig {
    private NetworkConfig network;
    private PduConfig pdu;
}

