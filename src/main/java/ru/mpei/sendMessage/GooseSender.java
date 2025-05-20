package ru.mpei.sendMessage;

import org.pcap4j.core.*;
import org.pcap4j.packet.UnknownPacket;

import java.util.List;

public class GooseSender {

    boolean interfaceOpen = false;
    PcapHandle handle = null;
    PcapNetworkInterface nif = null;
    public void send(byte[] data, String interfaceName) throws PcapNativeException, NotOpenException {


        if (interfaceOpen == false){
            nif = Pcaps.getDevByName(interfaceName);
            handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,10);
            interfaceOpen = true;
        }

        // Оборачиваем массив в UnknownPacket (низкоуровневый кадр)
        UnknownPacket packet = UnknownPacket.newPacket(data, 0, data.length);

        handle.sendPacket(packet);
        System.out.println("sendMessage in GooseSender");
    }
}