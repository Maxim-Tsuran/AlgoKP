package ru.mpei.pars;

import org.w3c.dom.*;
import ru.mpei.xml.DataEntry;
import ru.mpei.xml.GooseConfig;
import ru.mpei.xml.NetworkConfig;
import ru.mpei.xml.PduConfig;

import javax.xml.parsers.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GooseXmlParser {

    public static GooseConfig parse(File xmlFile) throws Exception {
        GooseConfig config = new GooseConfig();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Parse Network
        Element netElem = (Element) doc.getElementsByTagName("Network").item(0);
        NetworkConfig netConfig = new NetworkConfig();
        netConfig.setMacAddressSrc(getText(netElem, "MacAddressSrc"));
        netConfig.setMacAddressDst(getText(netElem, "MacAddressDst"));
        netConfig.setAppId(getText(netElem, "AppID"));
        netConfig.setEtherType(getText(netElem, "EtherType"));
        config.setNetwork(netConfig);

        // Parse Pdu
        Element pduElem = (Element) doc.getElementsByTagName("Pdu").item(0);
        PduConfig pdu = new PduConfig();
        pdu.setGocbRef(getText(pduElem, "gocbRef"));
        pdu.setDatSet(getText(pduElem, "datSet"));
        pdu.setGoID(getText(pduElem, "goID"));
        pdu.setTimeAllowedToLive(Integer.parseInt(getText(pduElem, "timeAllowedToLive")));
        pdu.setConfRev(Integer.parseInt(getText(pduElem, "confRev")));
        pdu.setStNum(Integer.parseInt(getText(pduElem, "stNum")));
        pdu.setSqNum(Integer.parseInt(getText(pduElem, "sqNum")));
        pdu.setSimulation(Boolean.parseBoolean(getText(pduElem, "simulation")));
        pdu.setNdsCom(Boolean.parseBoolean(getText(pduElem, "ndsCom")));
        pdu.setNumDatSetEntries(Integer.parseInt(getText(pduElem, "numDatSetEntries")));
        pdu.setT(getText(pduElem, "t"));


        // Parse allData
        NodeList dataList = ((Element)pduElem.getElementsByTagName("allData").item(0)).getElementsByTagName("Data");
        List<DataEntry> entries = new ArrayList<>();

        for (int i = 0; i < dataList.getLength(); i++) {
            Element dataElem = (Element) dataList.item(i);
            String type = dataElem.getAttribute("type");
            String value = dataElem.getTextContent().trim();
            entries.add(new DataEntry(type, value));
        }

        pdu.setAllData(entries);
        config.setPdu(pdu);

        return config;
    }

    private static String getText(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent().trim();
    }
}
