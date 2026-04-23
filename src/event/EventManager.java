package event;

import event.event_manifest.*;
import nro.server.Manager;
import java.io.*;
import java.util.*;

public class EventManager {

    private static EventManager instance;

    public static boolean LUNNAR_NEW_YEAR = false;
    public static boolean INTERNATIONAL_WOMANS_DAY = false;
    public static boolean CHRISTMAS = false;
    public static boolean HALLOWEEN = false;
    public static boolean HUNG_VUONG = false;
    public static boolean TRUNG_THU = false;
    public static boolean TOP_UP = false;
    public static boolean EVENT_POKEMON = false;
    public static boolean TEACHERS_DAY = false;
    public static boolean PHO_ANH_HAI = false;
    public static boolean EVENT_RANKING_REWARD = false;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        System.out.println("[EventManager] Default Event...");
        resetAllEventFlags();

        List<Integer> activeEvents = loadActiveEventsFromFile();
        System.out.println("[EventManager] ACTIVE_EVENTS = " + activeEvents);

        for (int eventId : activeEvents) {

            switch (eventId) {
                case 1 -> HALLOWEEN = true;
                case 2 -> INTERNATIONAL_WOMANS_DAY = true;
                case 3 -> CHRISTMAS = true;
                case 4 -> LUNNAR_NEW_YEAR = true;
                case 5 -> TRUNG_THU = true;
                case 6 -> HUNG_VUONG = true;
                case 7 -> TOP_UP = true;
                case 8 -> EVENT_POKEMON = true;
                case 9 -> TEACHERS_DAY = true;
                case 10 -> PHO_ANH_HAI = true;
            }
        }

        new Default().init();

        if (LUNNAR_NEW_YEAR) {
            System.out.println("[EventManager] LUNAR NEW YEAR");
            new LunarNewYear().init();
        }
        if (INTERNATIONAL_WOMANS_DAY) {
            System.out.println("[EventManager] INTERNATIONAL WOMENS DAY");
            new InternationalWomensDay().init();
        }
        if (HALLOWEEN) {
            System.out.println("[EventManager] HALLOWEEN");
            new Halloween().init();
        }
        if (CHRISTMAS) {
            System.out.println("[EventManager] CHRISTMAS");
            new Christmas().init();
        }
        if (HUNG_VUONG) {
            System.out.println("[EventManager] HUNG VUONG");
            new HungVuong().init();
        }
        if (TRUNG_THU) {
            System.out.println("[EventManager] TRUNG THU");
            new TrungThu().init();
        }
        if (TOP_UP) {
            System.out.println("[EventManager] TOP UP");
            new TopUp().init();
        }
        if (EVENT_POKEMON) {
            System.out.println("[EventManager] EVENT_POKEMON");
            new Po_Ke_Mon().init();
        }
        if (TEACHERS_DAY) {
            System.out.println("[EventManager] TEACHERS_DAY");
            new InternationalTeachersDay().init();
        }
        if (PHO_ANH_HAI) {
            System.out.println("[EventManager] PHO_ANH_HAI");
            new Pho_Anh_Hai().init();
        }
    }

    private void resetAllEventFlags() {
        LUNNAR_NEW_YEAR = false;
        INTERNATIONAL_WOMANS_DAY = false;
        CHRISTMAS = false;
        HALLOWEEN = false;
        HUNG_VUONG = false;
        TRUNG_THU = false;
        TOP_UP = false;
        EVENT_POKEMON = false;
        TEACHERS_DAY = false;
        PHO_ANH_HAI = false;
    }

    private List<Integer> loadActiveEventsFromFile() {
        List<Integer> ids = new ArrayList<>();

        File file = new File("active_event.txt");

        System.out.println("[EventManager] Working dir = " + System.getProperty("user.dir"));
        System.out.println("[EventManager] Read file = " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("[EventManager] active_event.txt NOT FOUND -> default [7]");
            ids.add(7);
            return ids;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();

            System.out.println("[EventManager] File content = " + line);

            if (line != null && !line.isEmpty()) {
                for (String part : line.split("-")) {
                    ids.add(Integer.parseInt(part.trim()));
                }
            }
        } catch (Exception e) {
            System.out.println("[EventManager] ERROR reading file: " + e.getMessage());
        }

        if (ids.isEmpty()) {
            System.out.println("[EventManager] EMPTY -> default [7]");
            ids.add(7);
        }

        return ids;
    }

}
