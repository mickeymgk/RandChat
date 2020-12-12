package randchat.model;

import org.javalite.activejdbc.Model;

public class Stranger extends Model {

    static {
        validatePresenceOf(
                "id",
                "name",
                "current_partner",
                "is_in_chat",
                "coupons",
                "sex",
                "state",
                "chat_id");
    }

}
