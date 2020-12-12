package randchat.service;

import com.vdurmont.emoji.EmojiParser;

import org.javalite.activejdbc.Base;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.net.URI;
import java.net.URISyntaxException;

import randchat.BotUtils;
import randchat.model.Stranger;

import static randchat.handler.RandChat.STATE_CHATTING;
import static randchat.handler.RandChat.STATE_SEARCHING;
import static randchat.handler.RandChat.STATE_SEARCH_KEY;

/*
* Background Task to handle search and notify the user for a match
* */
public class Search extends Thread {

    private static final String driver = "org.postgresql.Driver";
    private static String user, pwd, url;
    private final Message message;
    private final TelegramLongPollingBot bot;

    public Search(Message message, TelegramLongPollingBot bot) throws URISyntaxException {

        URI dbUri = new URI(System.getenv("DATABASE_URL"));
        user = dbUri.getUserInfo().split(":")[0];
        pwd = dbUri.getUserInfo().split(":")[1];
        url = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();

        this.message = message;
        this.bot = bot;

    }

    @Override
    public void run() {
        if (Base.hasConnection()) {
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Stranger searcher = Stranger.findFirst("id = ?", message.getFrom().getId());
        if (searcher != null && (Integer) searcher.get("state") == STATE_SEARCH_KEY) {
            if ((Integer) searcher.get("state") != STATE_SEARCHING) {
                BotUtils.sendMessage(EmojiParser.parseToUnicode(":telescope: Looking for a chat mate..."), bot, message.getChatId(), BotUtils.backHomeOptions());
            }
            searcher.set("state", STATE_SEARCHING).saveIt();
        }
        if (searcher != null && (Integer) searcher.get("state") == STATE_SEARCHING) {
            int partnerSex;
            if (searcher.getInteger("sex") == 0) partnerSex =1; // searcher is female
            else partnerSex = 0;// searcher is male
            Stranger partner = Stranger.findFirst("current_partner = ? and id != ? and is_in_chat = ? and sex = ?", 0, message.getFrom().getId(), false, partnerSex);
            if (partner != null) {
                searcher.set("is_in_chat", true).set("current_partner", partner.get("id")).set("state", STATE_CHATTING).saveIt();
                partner.set("is_in_chat", true).set("current_partner", message.getFrom().getId()).set("state", STATE_CHATTING).saveIt();
                BotUtils.sendMessage("You have been matched with " + partner.get("name"), bot, message.getChatId(), BotUtils.chatOptions());//notify the searcher
                BotUtils.sendMessage("You have been matched with " + searcher.get("name"), bot, Long.parseLong(partner.get("chat_id").toString()), BotUtils.chatOptions());//notify the partner
                BotUtils.log(BotUtils.LogLevel.INFO, "#users_matched: " + message.getFrom().getId() + " With " + partner.get("id"), bot);
            } else {
                while (!(boolean) Stranger.findFirst("id = ?", message.getFrom().getId()).get("is_in_chat")) {//search every 5 seconds for a partner
                    Base.close();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    run();
                }
            }
        }
    }
}
