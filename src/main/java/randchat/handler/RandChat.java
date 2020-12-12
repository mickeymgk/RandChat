package randchat.handler;

import com.vdurmont.emoji.EmojiParser;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import randchat.BotConfig;
import randchat.BotUtils;
import randchat.database.CreateTable;
import randchat.model.Invite;
import randchat.model.Stranger;
import randchat.service.Search;

public class RandChat extends TelegramLongPollingBot {

    private String url, user, pwd;
    private static final String driver = "org.postgresql.Driver";
    private static final int STATE_REGISTERING = 1;
    private static final int STATE_PROFILE = 2;
    public static final int STATE_SEARCH_KEY = 3;
    public static final int STATE_SEARCHING = 4;
    public static final int STATE_CHATTING = 5;
    private static final int STATE_MAIN_MENU = 6;
    private static final int STATE_CHANGING_NAME = 7;

    public RandChat() throws URISyntaxException, SQLException {

        URI dbUri = new URI(System.getenv("DATABASE_URL"));
        user = dbUri.getUserInfo().split(":")[0];
        pwd = dbUri.getUserInfo().split(":")[1];
        url = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();

        //TODO: Replace this with a local postgresql server if you want to run the bot locally

        initDatabase();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().startsWith("/start ")) {
                introUser(update.getMessage().getFrom().getId(), update.getMessage().getChatId());
                updateCoupon(update.getMessage());
            } else if (update.getMessage().getText().matches("/start")) {
                introUser(update.getMessage().getFrom().getId(), update.getMessage().getChatId());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":man: I'm Male"))) {
                handleGender(1, update.getMessage());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":woman: I'm Female"))) {
                handleGender(0, update.getMessage());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":telescope: Search"))) {
                try {
                    search(update.getMessage());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (update.getMessage().getText().matches("/list_users")) {
                handleAdmin(update.getMessage());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":x: Leave Chat"))) {
                quitChat(update.getMessage());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":notebook: Rules"))) {
                handleRule(update.getMessage().getChatId());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":back: Main Menu"))) {
                handleBack(update.getMessage().getChatId());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":envelope: Invites"))) {
                handleInviteLink(update.getMessage().getChatId(), update.getMessage().getFrom().getId());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":bust_in_silhouette: Profile"))) {
                handleProfile(update.getMessage().getChatId(), update.getMessage().getFrom().getId());
            } else if (update.getMessage().getText().matches(EmojiParser.parseToUnicode(":pencil: Edit Name"))) {
                editName(update.getMessage().getChatId(), update.getMessage().getFrom().getId());
            } else handleConversations(update.getMessage());
        }
    }

    private void handleProfile(Long chatId, Integer userId) {
        if (!Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        String name = (String) Stranger.findFirst("id = ?", userId).get("name");
        Integer gender = (Integer) Stranger.findFirst("id = ?", userId).get("sex");
        if (name != null && gender != null) {
            if (gender == 0) {
                BotUtils.sendMessage("Here is Your Profile detail:\n\nName: " + "**" + name + "**" + "\nGender: **Female**" + "\nLooking for: **Male**", this, chatId, BotUtils.profileOptions());
            } else {
                BotUtils.sendMessage("Here is Your Profile detail:\nName: " + name + "\nGender: **Male**" + "\nLooking for: **Female**", this, chatId, BotUtils.profileOptions());
            }
        }
        Base.close();
    }

    private void editName(Long chatId, Integer userId) {
        if (!Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Stranger stranger = Stranger.findFirst("id = ?", userId);
        if (stranger != null) {
            stranger.set("state", STATE_CHANGING_NAME).saveIt();
            BotUtils.sendMessage("Enter Your new Name:", this, chatId, BotUtils.backHomeOptions());
        }
        Base.close();
    }

    private void updateCoupon(Message message) {
        if (!Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Stranger inviter = Stranger.findFirst("id = ?", Long.parseLong(message.getText().replace("/start ", "")));
        Stranger invitee = Stranger.findFirst("id = ?", message.getFrom().getId());
        Invite invite = Invite.findFirst("inviter = ? and invitee = ?", inviter, invitee);
        if (invite == null) {
            final Invite tmp = new Invite();
            tmp.set("inviter", inviter).set("invitee", invitee).insert();
            final int currentCoupons = Invite.find("inviter = ?", inviter).size();
            inviter.set("coupons", currentCoupons);
            inviter.saveIt();
        } else {
            BotUtils.log(BotUtils.LogLevel.INFO, "#user_reinvited" + inviter.get("id"), this);
        }
        BotUtils.log(BotUtils.LogLevel.INFO, "#user_invited" + inviter.get("id"), this);
        Base.close();
    }

    private void handleInviteLink(Long chatId, Integer userId) {
        if (!Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        try {
            Base.open(driver, url, user, pwd);
            final int currentCoupons = Invite.find("inviter = ?", userId).size();
            BotUtils.sendMessage("You invited " + currentCoupons + " User(s) and here is your invite link is " + "https://telegram.me/EthioTalksBot?start=" + userId, this, chatId, BotUtils.mainOptions());
            Base.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void introUser(Integer userId, Long chatId) {
        if (Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Stranger e = Stranger.findFirst("id = ?", userId);
        if (e == null) {
            Stranger stranger = new Stranger();
            stranger.set("id", userId);
            stranger.set("name", "Anonymous");
            stranger.set("current_partner", 0);
            stranger.set("is_in_chat", false);
            stranger.set("coupons", 0);
            stranger.set("sex", -1);
            stranger.set("state", STATE_REGISTERING);
            stranger.set("chat_id", chatId);
            if (stranger.insert()) {
                BotUtils.log(BotUtils.LogLevel.INFO, "#users_registered: " + userId, this);
                BotUtils.sendMessage("Alright! you're now registered as Anonymous, please select your gender, you are...?", this, chatId, BotUtils.genderOptions());
            } else
                BotUtils.log(BotUtils.LogLevel.ERROR, "#failed_registration:" + userId, this);
        } else {
            BotUtils.sendMessage(EmojiParser.parseToUnicode("I think I know you before :smile: What do you want to do next?"), this, chatId, BotUtils.mainOptions());
        }
        Base.close();
    }

    private void search(Message message) throws URISyntaxException {
        if (Base.hasConnection()) {
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Integer state = Stranger.findFirst("id = ?", message.getFrom().getId()).getInteger("state");
        if (state != STATE_SEARCHING) {
            Search search = new Search(message, this);
            search.start();
        } else {
            BotUtils.sendMessage(EmojiParser.parseToUnicode("You're already looking for a partner... :telescope: "), this, message.getChatId(), BotUtils.mainOptions());
        }
        Base.close();
    }

    private void handleGender(int gender, Message message) {
        if (Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        if (gender == 0) {// female
            Stranger stranger = Stranger.findFirst("id = ?", message.getFrom().getId());
            if (stranger != null) {
                stranger.set("sex", 0).set("state", STATE_SEARCH_KEY).saveIt();
                Base.close();
                BotUtils.sendMessage("Alright! you can now start looking for Male partners", this, message.getChatId(), BotUtils.searchOption());
            } else
                BotUtils.log(BotUtils.LogLevel.ERROR, "#gender_error unable to set gender of the user", this);
        } else {//male
            Stranger stranger = Stranger.findFirst("id = ?", message.getFrom().getId());
            if (stranger != null) {
                stranger.set("sex", 1).set("state", STATE_SEARCH_KEY).saveIt();
                Base.close();
                BotUtils.sendMessage("Alright! you can now start looking for Female partners", this, message.getChatId(), BotUtils.searchOption());
            } else
                BotUtils.log(BotUtils.LogLevel.ERROR, "#gender_error unable to set gender of the user", this);
        }
        Base.close();
    }


    private void handleAdmin(Message message) {
        if (Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        LazyList<Model> strangers = Stranger.findAll();
        for (Model stranger : strangers) {
            BotUtils.sendMessage(stranger.get("name").toString() + stranger.get("tag"), this, message.getChatId(), null);
        }
        Base.close();
    }

    private void handleConversations(Message message) {
        if (Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
//        Stranger receiver = Stranger.findFirst("current_partner = ?", message.getFrom().getId());
        Stranger sender = Stranger.findFirst("id = ?", message.getFrom().getId());// used for handling name change
        if (sender != null && (Integer) sender.get("state") == STATE_CHANGING_NAME) {
            sender.set("name", message.getText()).set("state", STATE_MAIN_MENU).saveIt();
            BotUtils.sendMessage("Renamed successfully! what do you want to do next?", this, message.getChatId(), BotUtils.mainOptions());
            BotUtils.log(BotUtils.LogLevel.INFO, "#name_update from " + message.getFrom().getFirstName(), this);
        }
        if (sender != null) {
            System.out.println("sending message to " + sender.get("id") + "from " + sender.get("current_partner"));
            BotUtils.sendMessage(message.getText(), this, Long.parseLong(sender.get("current_partner").toString()), null);
        }
        Base.close();
    }

    private void quitChat(Message message) {
        if (!Base.hasConnection()) {
            System.out.println("THERE WAS A CONNECTION");
            Base.close();
        }
        Base.open(driver, url, user, pwd);
        Stranger partner = Stranger.findFirst("current_partner = ?", message.getFrom().getId());
        Stranger currentUser = Stranger.findFirst("id = ?", message.getFrom().getId());
        if (partner != null) {
            partner.set("current_partner", 0).set("is_in_chat", false).set("state", STATE_SEARCH_KEY).saveIt();
            BotUtils.sendMessage("Your Partner, " + partner.get("name") + " Left the chat. What do you want to do next?", this,  Long.parseLong(partner.get("chat_id").toString()), BotUtils.mainOptions());
        }
        if (currentUser != null) {
            currentUser.set("current_partner", 0).set("is_in_chat", false).set("state", STATE_SEARCH_KEY).saveIt();
            BotUtils.sendMessage("You left the current chat, What do you want to do next?", this, message.getChatId(), BotUtils.mainOptions());
        }
        Base.close();
    }

    private void handleRule(Long chatId) {
        BotUtils.sendMessage("These are the rules:\n**1. Respect your partners**\n**2. No Promotions**\n**3. No pictures because they are not supported by the bot yet **" +
                "\nANY MOVE AGAINST THE LAW WILL RESULT IN PERMANENT BAN!!", this, chatId, BotUtils.backHomeOptions());
    }

    private void handleBack(Long chatId) {
        BotUtils.sendMessage("What do you want to do next?", this, chatId, BotUtils.mainOptions());
    }

    private void initDatabase() throws SQLException {
        final Connection con = DriverManager.getConnection(url, user, pwd);
        final Statement stm = con.createStatement();
        try {
            stm.executeUpdate(CreateTable.strangers);
            stm.executeUpdate(CreateTable.invites);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stm!=null) con.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.BOT_USER;
    }

    @Override
    public String getBotToken() {
        return BotConfig.BOT_TOKEN;
    }
}
