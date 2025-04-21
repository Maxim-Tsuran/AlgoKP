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
        int gooseStart = buffer.position(); // Здесь начинается нужная нам длина для поля length
        buffer.putShort(appId);
        // Сохраняем позицию, куда потом вставим длину
        int gooseLengthPos = buffer.position();


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
        byte[] dummyGooseData = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; // Заглушка для length
        // (в будущем переписываем) и goose.reserve
        buffer.put(dummyGooseData);
        // GOOSE PDU (TLV-кодировка ASN.1 BER)
        ByteBuffer goosePayload = ByteBuffer.allocate(1400);

        goosePayload.put(encodeTLV((byte) 0x80, pdu.getGocbRef().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x81, toUInt(pdu.getTimeAllowedToLive())));
        goosePayload.put(encodeTLV((byte) 0x82, pdu.getDatSet().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x83, pdu.getGoID().getBytes()));
        goosePayload.put(encodeTLV((byte) 0x84, parseTimestamp(pdu.updateTimestamp()))); // t
        goosePayload.put(encodeTLV((byte) 0x85, toUInt(pdu.getStNum())));
        goosePayload.put(encodeTLV((byte) 0x86, toUInt(pdu.getSqNum())));
        goosePayload.put(encodeTLV((byte) 0x87, new byte[] {(byte) (pdu.isSimulation() ? 0x01 : 0x00)}));
        goosePayload.put(encodeTLV((byte) 0x88, toUInt(pdu.getConfRev())));
        goosePayload.put(encodeTLV((byte) 0x89, new byte[] {(byte) (pdu.isNdsCom() ? 0x01 : 0x00)}));
        goosePayload.put(encodeTLV((byte) 0x8a, toUInt(pdu.getNumDatSetEntries())));
        // Boolean
        byte boolTag = (byte) 0x83;
        byte boolLength = 0x01;
        byte boolValue = (byte) (Boolean.parseBoolean(pdu.getAllData().get(0).getValue()) ? 0x01 : 0x00);
        byte[] boolTLV = new byte[] { boolTag, boolLength, boolValue };

        // Integer (4 байта)
        byte intTag = (byte) 0x85;
        byte[] intValueBytes = ByteBuffer.allocate(4).putInt(
                Integer.parseInt(pdu.getAllData().get(1).getValue())
        ).array();
        byte[] intTLV = new byte[1 + 1 + 4]; // tag + length + 4 bytes
        intTLV[0] = intTag;
        intTLV[1] = 0x04;
        System.arraycopy(intValueBytes, 0, intTLV, 2, 4);

        // Float (4 байта)
        byte floatTag = (byte) 0x87;
        byte[] floatValueBytes = ByteBuffer.allocate(4).putFloat(
                Float.parseFloat(pdu.getAllData().get(2).getValue())
        ).array();
        byte[] floatTLV = new byte[1 + 1 + 4];
        floatTLV[0] = floatTag;
        floatTLV[1] = 0x04;
        System.arraycopy(floatValueBytes, 0, floatTLV, 2, 4);

        // Собираем всё вместе в один allData-блок
        byte[] allData = new byte[boolTLV.length + intTLV.length + floatTLV.length];
        int pos = 0;
        System.arraycopy(boolTLV, 0, allData, pos, boolTLV.length);
        pos += boolTLV.length;
        System.arraycopy(intTLV, 0, allData, pos, intTLV.length);
        pos += intTLV.length;
        System.arraycopy(floatTLV, 0, allData, pos, floatTLV.length);

        // Оборачиваем всё в тег allData (0xAB)
        goosePayload.put(encodeTLV((byte) 0xAB, allData));


        int gooseLength = goosePayload.position();
        byte[] gooseData = new byte[gooseLength];
        goosePayload.flip();
        goosePayload.get(gooseData);

        // Оборачиваем в PDU: 0x61 — GOOSE APDU
        buffer.put((byte) 0x61);
        buffer.put(encodeLength(gooseData.length));
        buffer.put(gooseData);

        int totalGooseLength = buffer.position() - gooseStart;
        System.out.println("GOOSE длина (с APPID до конца): " + totalGooseLength + " байт");

        // Возвращаемся и записываем длину после APPID
        buffer.putShort(gooseLengthPos, (short) (totalGooseLength));


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

//    private byte[] toUInt(int value) {
//        return ByteBuffer.allocate(4).putInt(value).array();
//    }

    private byte[] toUInt(int value) {
        if (value <= 0xFF) {
            return new byte[]{(byte) value};
        } else if (value <= 0xFFFF) {
            return ByteBuffer.allocate(2).putShort((short) value).array();
        } else if (value <= 0xFFFFFF) {
            return new byte[]{
                    (byte) ((value >> 16) & 0xFF),
                    (byte) ((value >> 8) & 0xFF),
                    (byte) (value & 0xFF)
            };
        } else {
            return ByteBuffer.allocate(4).putInt(value).array();
        }
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
            // Убираем лишние пробелы
            t = t.replaceAll(" +", " ");

            // Парсим с наносекундами и зоной
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss.nnnnnnnnn z", Locale.ENGLISH);
            ZonedDateTime zdt = ZonedDateTime.parse(t, formatter);

            Instant instant = zdt.toInstant();

            long epochSeconds = instant.getEpochSecond(); // первые 4 байта
            int nano = instant.getNano();                 // наносекунды (0-999_999_999)

            // По стандарту: дробная часть = (nanos / 1_000_000_000.0) * 2^32
            long fractionOfSecond = (long) ((nano / 1_000_000_000.0) * Math.pow(2, 32));

            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt((int) epochSeconds);          // секунды (4 байта)
            buffer.putInt((int) fractionOfSecond);      // дробная часть (4 байта)

            return buffer.array();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга времени: " + t, e);
        }
    }



}
