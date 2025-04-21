package ru.mpei;

import org.pcap4j.core.*;
import ru.mpei.pars.GooseXmlParser;
import ru.mpei.sendMessage.GooseMessageBuilder;
import ru.mpei.sendMessage.GooseSender;
import ru.mpei.xml.DataEntry;
import ru.mpei.xml.GooseConfig;
import ru.mpei.xml.PduConfig;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

// вставить в консоль true 2 2.34
public class Main {

    static GooseConfig config;
    static GooseMessageBuilder builder = new GooseMessageBuilder();
    static GooseSender sender = new GooseSender();
    static String interfaceName = "\\Device\\NPF_{DC89E044-6CBE-456B-A821-CF4675479649}";
    static byte[] lastSentData;
    //    static int[] retransmitIntervals = {4, 4, 4, 4, 8, 16, 32}; // мс
    //не успевает программа обрабатывать так быстро сообщения как требует гост
    static int[] retransmitIntervals = {200, 200, 200, 200, 400, 600, 800}; // мс
    static int retransmitIndex = -1;
    static Timer timer = new Timer();
    static boolean flag = false;

    public static void main(String[] args) throws Exception {
        config = GooseXmlParser.parse(new File("goose_config.xml"));
        startConsoleInputThread();
        startSendingLoop(1000); // начальная периодичность: 1000 мс
    }

    private static void startConsoleInputThread() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Введите новое значение (true|false int float):");
                try {
                    String boolStr = scanner.next();
                    String intStr = scanner.next();
                    String floatStr = scanner.next();

                    List<DataEntry> allData = config.getPdu().getAllData();
                    boolean changed = false;

                    if (!Objects.equals(allData.get(0).getValue(), boolStr)) {
                        allData.get(0).setValue(boolStr);
                        changed = true;
                    }
                    if (!Objects.equals(allData.get(1).getValue(), intStr)) {
                        allData.get(1).setValue(intStr);
                        changed = true;
                    }
                    if (!Objects.equals(allData.get(2).getValue(), floatStr)) {
                        allData.get(2).setValue(floatStr);
                        changed = true;
                    }

                    if (changed) {
                        PduConfig pdu = config.getPdu();
                        pdu.setStNum(pdu.getStNum() + 1); // stNum++
                        pdu.setSqNum(0); // sqNum сброс
                        flag = false;
                        retransmitIndex = 0; // начинаем режим пересылки
                        sendGoose(); // отправка сразу
                        System.out.println("000000000000000000000000000000000");
                    }

                } catch (Exception e) {
                    System.out.println("Ошибка ввода. Попробуйте снова.");
                    scanner.nextLine();
                }
            }
        }).start();
    }

    private static void startSendingLoop(long delay) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    if (retransmitIndex >= 0) {
                        sendGoose();
                        System.out.println("11111111111111111111111111111111111111111111111111111111");
                        retransmitIndex++;
                        if (retransmitIndex >= retransmitIntervals.length) {
                            retransmitIndex = -1;
                            startSendingLoop(1000); // возвращаемся к обычному циклу
                        } else {
                            startSendingLoop(retransmitIntervals[retransmitIndex]);
                        }
                    } else {
                        sendGoose();
                        System.out.println("222222222222222222222222222222222222222222222222222222");

                        startSendingLoop(1000); // регулярный вызов
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    private static void sendGoose() throws PcapNativeException, NotOpenException {
        PduConfig pdu = config.getPdu();
        if (flag) {
            pdu.setSqNum(pdu.getSqNum() + 1);
        }
        if (pdu.getSqNum() == 0) {
            flag = true;
        }
//        pdu.updateTimestamp();

        byte[] gooseMessage = builder.build(config);
        sender.send(gooseMessage, interfaceName);
        lastSentData = gooseMessage;

        System.out.println("GOOSE отправлен. stNum=" + pdu.getStNum() + " sqNum=" + pdu.getSqNum());
    }
}