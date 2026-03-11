package com.busfleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration;

@SpringBootApplication(exclude = {TelegramBotStarterConfiguration.class})
public class BusFleetApplication {

    private final Environment env;

    public BusFleetApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(BusFleetApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String token = env.getProperty("bot.token", "").trim();
        if (token.isEmpty()) {
            System.out.println("\n>>> Telegram-бот не запущен: токен не задан. Укажите BOT_TOKEN в Environment variables или в application-local.properties (профиль local) и перезапустите.\n");
        }
    }
}
