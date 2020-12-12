package randchat.database;

public class CreateTable {

    public static final String strangers = "CREATE TABLE IF NOT EXISTS strangers " +
            "(id INTEGER PRIMARY KEY NOT NULL, " +
            "name TEXT NOT NULL, " +
            "current_partner INTEGER, " +
            "is_in_chat BOOLEAN NOT NULL, " +
            "coupons INT NOT NULL, " +
            "sex INT, " +
            "state INT, " +
            "chat_id BIGINT NOT NULL);";

    public static final String invites = "CREATE TABLE IF NOT EXISTS invites " +
            "(id SERIAL PRIMARY KEY, " +
            "inviter INTEGER NOT NULL, " +
            "invitee INTEGER NOT NULL);";

}
