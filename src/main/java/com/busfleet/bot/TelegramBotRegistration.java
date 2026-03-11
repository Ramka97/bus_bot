package com.busfleet.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.util.List;

/**
 * Регистрация Telegram-бота с перехватом 404 (неверный/отозванный токен).
 * Приложение не падает, в лог пишется предупреждение.
 */
@Configuration
public class TelegramBotRegistration {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotRegistration.class);

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    @Bean
    public InitializingBean telegramBotInitializer(TelegramBotsLongPollingApplication application,
                                                   List<SpringLongPollingBot> bots) {
        return () -> {
            for (SpringLongPollingBot bot : bots) {
                try {
                    String token = bot.getBotToken();
                    log.info("Попытка подключения бота (длина токена: {} символов)...", token == null ? 0 : token.length());
                    application.registerBot(token, bot.getUpdatesConsumer());
                    log.info("Telegram-бот успешно подключён.");
                } catch (Exception e) {
                    log.warn("Telegram-бот не запущен: неверный или отозванный токен (404). Получите НОВЫЙ токен в @BotFather (Revoke → скопировать новый) и вставьте в application-local.properties. Ошибка: {}", e.getMessage());
                }
            }
        };
    }
}
