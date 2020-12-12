package randchat;

import com.vdurmont.emoji.EmojiParser;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/*
* Helper class for sending messages
* */
public class BotUtils {

    public static InputStream getInputStream(String fileName) throws IllegalAccessException {
        ClassLoader loader = BotUtils.class.getClassLoader();
        InputStream inputStream = loader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalAccessException("file not found" + fileName);
        } else return inputStream;
    }

    public static void log(LogLevel logLevel, String event, TelegramLongPollingBot bot) {
        StringBuilder builder = new StringBuilder();
        if (logLevel == LogLevel.INFO) {
            builder.append("**INFO**").append("\n");
            builder.append(event);
        } else if (logLevel == LogLevel.ERROR) {
            builder.append("**ERROR**").append("\n");
            builder.append(event);
        }

        sendMessage(builder.toString(), bot, BotConfig.LOGCHANNEL);
    }

    public enum LogLevel {INFO, ERROR}

    public static void sendMessage(String text, TelegramLongPollingBot bot, long chatId, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableMarkdown(true);
        message.setText(text);
        if (keyboard!= null) {
            message.setReplyMarkup(keyboard);
        }
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(String text, TelegramLongPollingBot bot, String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableMarkdown(true);
        message.setText(text);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static ReplyKeyboardMarkup genderOptions() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":man: I'm Male"));
        row.add(EmojiParser.parseToUnicode(":woman: I'm Female"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup backHomeOptions() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":back: Main Menu"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup mainOptions() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":telescope: Search"));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":bust_in_silhouette: Profile"));
        row.add(EmojiParser.parseToUnicode(":envelope: Invites"));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":notebook: Rules"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup searchOption() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":telescope: Search"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup chatOptions() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":x: Leave Chat"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup profileOptions() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":pencil: Edit Name"));
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(EmojiParser.parseToUnicode(":back: Main Menu"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

}
