package dev.crzto3.pocketworld.client;

import java.util.List;

/** Настройки текущего кармана и данные окна конфигуратора, известные клиенту. */
public final class ClientPocketState {

    private static volatile String sky = "twilight";
    private static volatile String weather = "clear";

    private static volatile List<String> invited = List.of();
    private static volatile int animals;
    private static volatile String message = "";

    private ClientPocketState() {
    }

    public static String getSky() {
        return sky;
    }

    public static String getWeather() {
        return weather;
    }

    public static void setState(String newSky, String newWeather) {
        sky = newSky;
        weather = newWeather;
    }

    public static List<String> getInvited() {
        return invited;
    }

    public static int getAnimals() {
        return animals;
    }

    public static String getMessage() {
        return message;
    }

    public static void setInfo(List<String> newInvited, int newAnimals, String newMessage) {
        invited = List.copyOf(newInvited);
        animals = newAnimals;

        if (!newMessage.isEmpty()) {
            message = newMessage;
        }
    }

    public static void clearMessage() {
        message = "";
    }
}
