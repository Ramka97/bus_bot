package com.busfleet.bot;

import com.busfleet.model.Bus;
import com.busfleet.model.BusModel;
import com.busfleet.service.BusService;
import com.busfleet.service.MaintenanceCalculator;
import com.busfleet.service.MaintenanceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnExpression("!'${bot.token:}'.trim().isEmpty()")
public class BusFleetBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final BusService busService;
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public BusFleetBot(@Value("${bot.token}") String token, BusService busService) {
        this.botToken = token;
        this.telegramClient = new OkHttpTelegramClient(token);
        this.busService = busService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
                return;
            }
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText().trim();
                long chatId = update.getMessage().getChatId();
                if (text.startsWith("/") || isMenuButton(text)) {
                    handleCommand(chatId, text);
                } else {
                    handleMessage(chatId, text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isMenuButton(String text) {
        return "➕ Добавить автобус".equals(text) || "🗑 Удалить автобус".equals(text)
                || "📝 Обновить пробег".equals(text) || "📋 Список парка".equals(text)
                || "📊 Статистика".equals(text) || "🔍 Поиск автобуса".equals(text)
                || "❓ Помощь".equals(text);
    }

    private void handleCommand(long chatId, String text) throws TelegramApiException {
        String[] parts = text.split("\\s+", 2);
        String cmd = isMenuButton(text) ? text : parts[0];
        if (!isMenuButton(text)) cmd = cmd.toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";
        sessions.put(chatId, new UserSession());
        switch (cmd) {
            case "/start" -> sendStart(chatId);
            case "/help", "❓ Помощь" -> sendHelp(chatId);
            case "/addbus", "➕ Добавить автобус" -> startAddBus(chatId);
            case "/removebus", "🗑 Удалить автобус" -> startRemoveBus(chatId);
            case "/updatemileage", "/update", "📝 Обновить пробег" -> startUpdateMileage(chatId);
            case "/list", "📋 Список парка" -> sendList(chatId);
            case "/stats", "📊 Статистика" -> sendStats(chatId);
            case "/search", "🔍 Поиск автобуса" -> handleSearch(chatId, arg);
            default -> send(chatId, "Неизвестная команда. Используйте /start или ❓ Помощь.");
        }
    }

    private void handleCallback(Update update) throws TelegramApiException {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (data.startsWith("model:")) {
            BusModel model = BusModel.valueOf(data.substring(6));
            UserSession session = sessions.get(chatId);
            if (session != null && session.getStep() == UserSession.Step.ADD_MODEL) {
                session.setSelectedModel(model);
                session.setStep(UserSession.Step.ADD_STATE_NUMBER);
                editAndSend(chatId, messageId, "✅ Выбрана модель: " + model.getDisplayName());
                send(chatId, "Введите государственный номер автобуса:");
            }
        } else if (data.startsWith("updmode:")) {
            String mode = data.substring(8);
            UserSession session = sessions.get(chatId);
            if (session != null && session.getStep() == UserSession.Step.UPDATE_MODE) {
                session.setUpdateMode(mode);
                session.setStep(UserSession.Step.UPDATE_VALUE);
                String prompt = "set".equals(mode) ? "Введите новое значение пробега (км):" : "Введите количество километров для добавления:";
                editAndSend(chatId, messageId, "Режим: " + ("set".equals(mode) ? "Установить значение" : "Добавить км") + "\n\n" + prompt);
                send(chatId, prompt);
            }
        } else if (data.startsWith("bus:")) {
            String stateNumber = data.substring(4);
            UserSession session = sessions.get(chatId);
            if (session != null && session.getStep() == UserSession.Step.UPDATE_SELECT_BUS) {
                session.setStateNumber(stateNumber);
                session.setStep(UserSession.Step.UPDATE_MODE);
                var busOpt = busService.findBus(stateNumber);
                if (busOpt.isPresent()) {
                    Bus bus = busOpt.get();
                    editAndSend(chatId, messageId, "Автобус: " + bus.getStateNumber() + " — " + bus.getMileageKm() + " км\n\nВыберите режим обновления:", buildUpdateModeKeyboard());
                }
            }
        } else if (data.startsWith("listmodel:")) {
            BusModel model = BusModel.valueOf(data.substring(10));
            sendListByModel(chatId, model);
            editAndSend(chatId, messageId, "✅ " + model.getDisplayName());
        } else if (data.startsWith("listbus:")) {
            String stateNumber = data.substring(8);
            var opt = busService.findBus(stateNumber);
            if (opt.isPresent()) {
                send(chatId, formatBusWithMaintenance(opt.get()));
            } else {
                send(chatId, "Автобус не найден.");
            }
        } else if (data.startsWith("delconfirm:")) {
            String stateNumber = data.substring(10);
            if (busService.removeBus(stateNumber)) {
                editAndSend(chatId, messageId, "✅ Автобус " + stateNumber + " удалён.");
            } else {
                editAndSend(chatId, messageId, "❌ Ошибка удаления.");
            }
            sessions.remove(chatId);
        } else if (data.equals("cancel")) {
            sessions.remove(chatId);
            editAndSend(chatId, messageId, "Действие отменено.");
        }
    }

    private void handleMessage(long chatId, String text) throws TelegramApiException {
        UserSession session = sessions.get(chatId);
        if (session == null) return;
        switch (session.getStep()) {
            case ADD_STATE_NUMBER -> {
                session.setStateNumber(text);
                session.setStep(UserSession.Step.ADD_MILEAGE);
                send(chatId, "Введите начальный пробег (км):");
            }
            case ADD_MILEAGE -> {
                try {
                    long mileage = Long.parseLong(text.trim());
                    if (mileage < 0) { send(chatId, "Пробег не может быть отрицательным."); return; }
                    if (busService.addBus(session.getStateNumber(), session.getSelectedModel(), mileage)) {
                        send(chatId, "✅ Автобус " + session.getStateNumber() + " добавлен.");
                    } else {
                        send(chatId, "❌ Ошибка: автобус с таким госномером уже существует.");
                    }
                    sessions.remove(chatId);
                } catch (NumberFormatException e) {
                    send(chatId, "Введите число.");
                }
            }
            case REMOVE_STATE_NUMBER -> {
                var opt = busService.findBus(text);
                if (opt.isEmpty()) {
                    send(chatId, "❌ Автобус не найден.");
                    sessions.remove(chatId);
                } else {
                    session.setStateNumber(opt.get().getStateNumber());
                    send(chatId, "Удалить автобус " + opt.get().getStateNumber() + "?\n\n" + formatBusWithMaintenance(opt.get()), buildConfirmDeleteKeyboard(opt.get().getStateNumber()));
                }
            }
            case UPDATE_VALUE -> {
                try {
                    long value = Long.parseLong(text.trim());
                    String stateNumber = session.getStateNumber();
                    boolean ok = "set".equals(session.getUpdateMode()) ? busService.updateMileageSet(stateNumber, value) : busService.updateMileageAdd(stateNumber, value);
                    if (ok) {
                        var busOpt = busService.findBus(stateNumber);
                        if (busOpt.isPresent()) send(chatId, formatBusWithMaintenance(busOpt.get()));
                    } else {
                        send(chatId, "❌ Ошибка обновления.");
                    }
                    sessions.remove(chatId);
                } catch (NumberFormatException e) {
                    send(chatId, "Введите число.");
                }
            }
            case SEARCH_NUMBER -> {
                handleSearch(chatId, text);
                sessions.remove(chatId);
            }
            default -> {}
        }
    }

    private void sendStart(long chatId) throws TelegramApiException {
        send(chatId, "🚌 Учёт технического обслуживания автобусов парка\n\nВыберите действие:", buildMainMenuKeyboard());
    }

    private void sendHelp(long chatId) throws TelegramApiException {
        send(chatId, """
                ❓ Помощь
                ➕ Добавить автобус — модель, госномер, пробег.
                🗑 Удалить автобус — по госномеру (с подтверждением).
                📝 Обновить пробег — установить или добавить км.
                📋 Список парка — выбор модели → количество и перечень по гос. номерам → нажатие на автобус для ТО-1/ТО-2.
                📊 Статистика — количество по моделям.
                🔍 Поиск — по госномеру.
                ТО-1 — каждые 15 000 км, ТО-2 — каждые 30 000 км.
                """);
    }

    private void startAddBus(long chatId) throws TelegramApiException {
        UserSession session = new UserSession();
        session.setStep(UserSession.Step.ADD_MODEL);
        sessions.put(chatId, session);
        send(chatId, "Выберите модель автобуса:", buildModelKeyboard());
    }

    private void startRemoveBus(long chatId) throws TelegramApiException {
        UserSession session = new UserSession();
        session.setStep(UserSession.Step.REMOVE_STATE_NUMBER);
        sessions.put(chatId, session);
        send(chatId, "Введите государственный номер автобуса для удаления:");
    }

    private void startUpdateMileage(long chatId) throws TelegramApiException {
        List<Bus> buses = busService.getAllBuses();
        if (buses.isEmpty()) { send(chatId, "Парк пуст."); return; }
        UserSession session = new UserSession();
        session.setStep(UserSession.Step.UPDATE_SELECT_BUS);
        sessions.put(chatId, session);
        send(chatId, "Выберите автобус:", buildBusListKeyboard(buses, "bus:"));
    }

    private void handleSearch(long chatId, String arg) throws TelegramApiException {
        if (arg.isEmpty()) {
            UserSession session = new UserSession();
            session.setStep(UserSession.Step.SEARCH_NUMBER);
            sessions.put(chatId, session);
            send(chatId, "Введите госномер для поиска:");
            return;
        }
        var opt = busService.findBus(arg);
        if (opt.isPresent()) send(chatId, formatBusWithMaintenance(opt.get()));
        else send(chatId, "Автобус не найден.");
    }

    private void sendList(long chatId) throws TelegramApiException {
        if (busService.getAllBuses().isEmpty()) { send(chatId, "Парк пуст."); return; }
        send(chatId, "Выберите модель транспорта:", buildListModelKeyboard());
    }

    private InlineKeyboardMarkup buildListModelKeyboard() {
        var rows = new ArrayList<InlineKeyboardRow>();
        for (BusModel m : BusModel.values()) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text("🚌 " + m.getDisplayName()).callbackData("listmodel:" + m.name()).build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    private void sendListByModel(long chatId, BusModel model) throws TelegramApiException {
        var grouped = busService.getAllGroupedByModel();
        var buses = grouped.getOrDefault(model, List.<Bus>of());
        int count = buses.size();
        String countWord = vehicleCountWord(count);
        StringBuilder sb = new StringBuilder();
        sb.append("📌 ").append(model.getDisplayName()).append("\n");
        sb.append("Количество: ").append(count).append(" ").append(countWord).append("\n\n");
        if (buses.isEmpty()) {
            sb.append("В этой модели пока нет автобусов.");
            send(chatId, sb.toString());
        } else {
            sb.append("Перечень по гос. номерам:\n");
            for (Bus bus : buses) {
                sb.append("  • ").append(bus.getStateNumber()).append("\n");
            }
            sb.append("\nНажмите на автобус — откроется подробная информация (ТО-1, ТО-2):");
            send(chatId, sb.toString(), buildListBusKeyboard(buses));
        }
    }

    private InlineKeyboardMarkup buildListBusKeyboard(List<Bus> buses) {
        var rows = new ArrayList<InlineKeyboardRow>();
        for (Bus bus : buses) {
            String data = "listbus:" + bus.getStateNumber();
            if (data.length() > 64) data = data.substring(0, 64);
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("🚌 " + bus.getStateNumber()).callbackData(data).build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    private static String vehicleCountWord(int n) {
        if (n % 100 >= 11 && n % 100 <= 19) return "автобусов";
        return switch (n % 10) {
            case 1 -> "автобус";
            case 2, 3, 4 -> "автобуса";
            default -> "автобусов";
        };
    }

    private void sendStats(long chatId) throws TelegramApiException {
        var stats = busService.getStatsByModel();
        if (stats.isEmpty()) { send(chatId, "Парк пуст."); return; }
        var sb = new StringBuilder("📊 Статистика по моделям:\n\n");
        long total = 0;
        for (var e : stats.entrySet()) { sb.append(e.getKey().getDisplayName()).append(": ").append(e.getValue()).append(" шт.\n"); total += e.getValue(); }
        sb.append("\nВсего: ").append(total).append(" автобусов");
        send(chatId, sb.toString());
    }

    private static final java.time.format.DateTimeFormatter DATE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private String formatBusWithMaintenance(Bus bus) {
        MaintenanceInfo info = MaintenanceCalculator.calculate(bus);
        var sb = new StringBuilder();
        sb.append("🚌 ").append(bus.getStateNumber()).append(" (").append(bus.getModel().getDisplayName()).append(")\n");
        sb.append("Пробег: ").append(bus.getMileageKm()).append(" км\n");
        if (bus.getLastMileageUpdateAt() != null) {
            sb.append("Последнее обновление пробега: ").append(bus.getLastMileageUpdateAt().format(DATE_FORMAT)).append("\n");
        }
        if (info.isZhongTong()) {
            sb.append("(Для ZHONG TONG считается только ТО-2 каждые 50 000 км)\n");
            sb.append("ТО-2: с последнего ").append(info.getMileageSinceLastTO2()).append(" км, до след. ").append(info.getKmUntilNextTO2()).append(" км\n");
        } else {
            sb.append("ТО-1: с последнего ").append(info.getMileageSinceLastTO1()).append(" км, до след. ").append(info.getKmUntilNextTO1()).append(" км\n");
            sb.append("ТО-2: с последнего ").append(info.getMileageSinceLastTO2()).append(" км, до след. ").append(info.getKmUntilNextTO2()).append(" км\n");
        }
        if (info.isWarningTO1()) sb.append("⚠ Менее 1000 км до ТО-1!\n");
        if (info.isWarningTO2()) sb.append("⚠ Менее 1000 км до ТО-2!\n");
        return sb.toString();
    }

    private InlineKeyboardMarkup buildModelKeyboard() {
        var row = new InlineKeyboardRow();
        for (BusModel m : BusModel.values()) {
            row.add(InlineKeyboardButton.builder().text("🚌 " + m.getDisplayName()).callbackData("model:" + m.name()).build());
        }
        return new InlineKeyboardMarkup(List.of(row));
    }

    private InlineKeyboardMarkup buildUpdateModeKeyboard() {
        var row1 = new InlineKeyboardRow(
                InlineKeyboardButton.builder().text("📌 Установить значение").callbackData("updmode:set").build(),
                InlineKeyboardButton.builder().text("➕ Добавить км").callbackData("updmode:add").build());
        var row2 = new InlineKeyboardRow(InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel").build());
        return new InlineKeyboardMarkup(List.of(row1, row2));
    }

    private InlineKeyboardMarkup buildBusListKeyboard(List<Bus> buses, String prefix) {
        var rows = new ArrayList<InlineKeyboardRow>();
        var currentRow = new InlineKeyboardRow();
        for (Bus bus : buses) {
            String data = prefix + bus.getStateNumber();
            if (data.length() > 64) data = data.substring(0, 64);
            currentRow.add(InlineKeyboardButton.builder().text("🚌 " + bus.getStateNumber() + " — " + bus.getMileageKm() + " км").callbackData(data).build());
            if (currentRow.size() == 2) { rows.add(currentRow); currentRow = new InlineKeyboardRow(); }
        }
        if (!currentRow.isEmpty()) rows.add(currentRow);
        rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel").build()));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup buildConfirmDeleteKeyboard(String stateNumber) {
        return new InlineKeyboardMarkup(List.of(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("✅ Удалить").callbackData("delconfirm:" + stateNumber).build(),
                        InlineKeyboardButton.builder().text("❌ Отмена").callbackData("cancel").build())));
    }

    private ReplyKeyboardMarkup buildMainMenuKeyboard() {
        var row1 = new KeyboardRow(); row1.add("➕ Добавить автобус"); row1.add("🗑 Удалить автобус");
        var row2 = new KeyboardRow(); row2.add("📝 Обновить пробег"); row2.add("📋 Список парка");
        var row3 = new KeyboardRow(); row3.add("📊 Статистика"); row3.add("🔍 Поиск автобуса"); row3.add("❓ Помощь");
        return ReplyKeyboardMarkup.builder().keyboard(List.of(row1, row2, row3)).resizeKeyboard(true).build();
    }

    private void send(long chatId, String text) throws TelegramApiException { send(chatId, text, null); }

    private void send(long chatId, String text, Object keyboard) throws TelegramApiException {
        var b = SendMessage.builder().chatId(chatId).text(text);
        if (keyboard instanceof InlineKeyboardMarkup ik) b.replyMarkup(ik);
        if (keyboard instanceof ReplyKeyboardMarkup rk) b.replyMarkup(rk);
        telegramClient.execute(b.build());
    }

    private void editAndSend(long chatId, int messageId, String text) throws TelegramApiException {
        editAndSend(chatId, messageId, text, null);
    }

    private void editAndSend(long chatId, int messageId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        var b = EditMessageText.builder().chatId(chatId).messageId(messageId).text(text);
        if (keyboard != null) b.replyMarkup(keyboard);
        telegramClient.execute(b.build());
    }
}
