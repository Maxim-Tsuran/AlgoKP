package ru.mpei.sendMessage;

import ru.mpei.xml.GooseConfig;
import ru.mpei.xml.NetworkConfig;
import ru.mpei.xml.PduConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GooseMessageBuilder {

    public byte[] build(GooseConfig config) {
        NetworkConfig net = config.getNetwork();
        PduConfig pdu = config.getPdu();

        byte[] dstMac = parseMac(net.getMacAddressDst());
        byte[] srcMac = parseMac(net.getMacAddressSrc());
        short etherType = (short) Integer.parseInt(net.getEtherType(), 16);
        short appId = (short) Integer.parseInt(net.getAppId(), 16);

        // Заготовка Ethernet II кадра
        ByteBuffer buffer = ByteBuffer.allocate(1500); // MTU Ethernet

        // Ethernet Header: Destination MAC (6), Source MAC (6), EtherType (2)
        buffer.put(dstMac);
        buffer.put(srcMac);
        buffer.putShort(etherType);
        buffer.putShort(appId);



        // Placeholder for GOOSE payload
//        byte[] dummyGooseData = new byte[] {
//                0x00, 0x01, 0x00, (byte) 0x90, 0x00, 0x00, 0x00, 0x00,
//                0x61, (byte) 0x81, (byte) 0x85, (byte) 0x80, 0x1a, 0x47, 0x45, 0x44,
//                0x65, 0x76, 0x69, 0x63, 0x65, 0x46, 0x36, 0x35,
//                0x30, 0x2f, 0x4c, 0x4c, 0x4e, 0x30, 0x24, 0x47,
//                0x4f, 0x24, 0x67, 0x63, 0x62, 0x30, 0x31, (byte) 0x81,
//                0x02, 0x03, (byte) 0xe8, (byte) 0x82, 0x18, 0x47, 0x45, 0x44,
//                0x65, 0x76, 0x69, 0x63, 0x65, 0x46, 0x36, 0x35,
//                0x30, 0x2f, 0x4c, 0x4c, 0x4e, 0x30, 0x24, 0x47,
//                0x4f, 0x4f, 0x53, 0x45, 0x31, (byte) 0x83, 0x0b, 0x46,
//                0x36, 0x35, 0x30, 0x5f, 0x47, 0x4f, 0x4f, 0x53,
//                0x45, 0x31, (byte) 0x84, 0x08, 0x38, 0x6e, (byte) 0xbc, 0x41,
//                (byte) 0xed, 0x76, (byte) 0xec, 0x0a, (byte) 0x85, 0x01, 0x01, (byte) 0x86,
//                0x01, 0x02, (byte) 0x87, 0x01, 0x00, (byte) 0x88, 0x01, 0x01,
//                (byte) 0x89, 0x01, 0x00, (byte) 0x8a, 0x01, 0x08, (byte) 0xab, 0x20,
//                (byte) 0x83, 0x01, 0x00, (byte) 0x84, 0x03, 0x03, 0x00, 0x00,
//                (byte) 0x83, 0x01, 0x00, (byte) 0x84, 0x03, 0x03, 0x00, 0x00,
//                (byte) 0x83, 0x01, 0x00, (byte) 0x84, 0x03, 0x03, 0x00, 0x00,
//                (byte) 0x83, 0x01, 0x00, (byte) 0x84, 0x03, 0x03, 0x00, 0x00
//        }; // Заглушка (BER тип 0x61)
//        buffer.put(dummyGooseData);
//        byte[] gooseMessage = new byte[buffer.position()];
//        buffer.flip();
//        buffer.get(gooseMessage);
//
//        return gooseMessage;
                byte[] dummyGooseData = new byte[] {0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00}; // Заглушка (BER тип 0x61)
        buffer.put(dummyGooseData);
        // GOOSE PDU (TLV-кодировка ASN.1 BER)
        ByteBuffer goosePayload = ByteBuffer.allocate(1400);

        goosePayload.put(encodeTLV((byte) 0x80, pdu.getGocbRef().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x81, toUInt(pdu.getTimeAllowedToLive())));
        goosePayload.put(encodeTLV((byte) 0x82, pdu.getDatSet().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x83, pdu.getGoID().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x84, parseTimestamp(pdu.getT()))); // t
        goosePayload.put(encodeTLV((byte) 0x85, toUInt(pdu.getStNum())));
        goosePayload.put(encodeTLV((byte) 0x86, toUInt(pdu.getSqNum())));
        goosePayload.put(encodeTLV((byte) 0x87, new byte[]{(byte) (pdu.isSimulation() ? 0x01 : 0x00)}));
        goosePayload.put(encodeTLV((byte) 0x88, toUInt(pdu.getConfRev())));
        goosePayload.put(encodeTLV((byte) 0x89, new byte[]{(byte) (pdu.isNdsCom() ? 0x01 : 0x00)}));
        goosePayload.put(encodeTLV((byte) 0x8a, toUInt(pdu.getNumDatSetEntries())));


        // allData (заглушка на 1 bool = false)
        goosePayload.put(encodeTLV((byte) 0xab, new byte[]{(byte) 0x83, 0x01, 0x00}));

        int gooseLength = goosePayload.position();
        byte[] gooseData = new byte[gooseLength];
        goosePayload.flip();
        goosePayload.get(gooseData);

        // Оборачиваем в PDU: 0x61 — GOOSE APDU
        buffer.put((byte) 0x61);
        buffer.put(encodeLength(gooseData.length));
        buffer.put(gooseData);

        byte[] gooseMessage = new byte[buffer.position()];
        buffer.flip();
        buffer.get(gooseMessage);

        return gooseMessage;


    }

    private byte[] parseMac(String mac) {
        String[] hex = mac.split(":");
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) Integer.parseInt(hex[i], 16);
        }
        return bytes;
    }

    private byte[] toUInt(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private byte[] encodeTLV(byte tag, byte[] value) {
        ByteBuffer buf = ByteBuffer.allocate(2 + value.length + (value.length > 127 ? 2 : 1));
        buf.put(tag);
        buf.put(encodeLength(value.length));
        buf.put(value);
        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    private byte[] encodeLength(int length) {
        if (length <= 127) {
            return new byte[]{(byte) length};
        } else {
            return new byte[]{(byte) 0x81, (byte) length};
        }
    }

    // Конвертация строки времени формата "Jan  2, 2000 02:47:29.927595853 UTC"
    private byte[] parseTimestamp(String t) {
        try {
            // Нормализуем пробелы (убираем двойные пробелы)
            t = t.replaceAll(" +", " ");

            // Используем правильный шаблон с наносекундами и зоной UTC
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss.nnnnnnnnn z", Locale.ENGLISH);
            ZonedDateTime zdt = ZonedDateTime.parse(t, formatter);

            // Получаем наносекунды с начала эпохи (1970)
            long nanos = zdt.toInstant().getEpochSecond() * 1_000_000_000L + zdt.getNano();

            return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(nanos).array();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга времени: " + t, e);
        }
    }


}
