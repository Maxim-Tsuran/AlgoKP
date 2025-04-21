package ru.mpei;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import ru.mpei.pars.GooseXmlParser;
import ru.mpei.sendMessage.GooseMessageBuilder;
import ru.mpei.sendMessage.GooseSender;
import ru.mpei.xml.DataEntry;
import ru.mpei.xml.GooseConfig;

import java.io.File;

public class Main {
    public static void main(String[] args) throws NotOpenException, PcapNativeException {
        File xmlFile = new File("goose_config.xml");
        GooseConfig config;
        try {
            config = GooseXmlParser.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Вывод
        System.out.println("Src MAC: " + config.getNetwork().getMacAddressSrc());
        System.out.println("GOID: " + config.getPdu().getGoID());
        System.out.println("Время из PduConfig: " + config.getPdu().getT());
        System.out.println("Время отправки: " + config.getPdu().updateTimestamp());
        for (DataEntry data : config.getPdu().getAllData()) {
            System.out.println(data.getType() + ": " + data.getValue());
        }

        // build byte Message
        GooseMessageBuilder builder = new GooseMessageBuilder();
        byte[] gooseMessage = builder.build(config);

        for (PcapNetworkInterface dev : Pcaps.findAllDevs()) {
            System.out.println(dev.getName() + " : " + dev.getDescription());
        }
        // send Message
        GooseSender sender = new GooseSender();
        String interfaceName = "\\Device\\NPF_{DC89E044-6CBE-456B-A821-CF4675479649}";
        for (int i = 0; i < 100;i++) {
            sender.send(gooseMessage, interfaceName);
        }
    }
}