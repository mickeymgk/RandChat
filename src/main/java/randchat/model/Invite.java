package randchat.model;

import org.javalite.activejdbc.Model;

public class Invite extends Model {
    static {
        validatePresenceOf("id", "inviter", "invitee");
    }
}
