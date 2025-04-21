package ru.mpei.sendMessage;

import org.pcap4j.core.*;
import org.pcap4j.packet.UnknownPacket;

import java.util.List;

public class GooseSender {

    public void send(byte[] data, String interfaceName) throws PcapNativeException, NotOpenException {
        // Получаем все доступные интерфейсы
        List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();

        PcapNetworkInterface nif = null;

        for (PcapNetworkInterface dev : interfaces) {
            if (dev.getName().equals(interfaceName) || dev.getDescription().contains(interfaceName)) {
                nif = dev;
                break;
            }
        }

        if (nif == null) {
            throw new IllegalArgumentException("Сетевой интерфейс не найден: " + interfaceName);
        }

        PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

        // Оборачиваем массив в UnknownPacket (низкоуровневый кадр)
        UnknownPacket packet = UnknownPacket.newPacket(data, 0, data.length);

        handle.sendPacket(packet);

        handle.close();
    }
}
