package bg.deck.santaseservice.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String KING = "KING";
    public static final String QUEEN = "QUEEN";
    public static final String NOTIFY_GAME_DESTINATION = "/topic/game/%s/%s";
    public static final String NOTIFY_GAME_SEARCH_DESTINATION = "/topic/game/%s";
    public static final String ROLE = "role";
    public static final String USERNAME = "username";
    public static final String BEARER = "Bearer ";
    public static final String WEB_SOCKET_ENDPOINT = "/ws-game";
    public static final String TOKEN_PARAM = "token";
    public static final String TOPIC = "/topic";
    public static final String APP = "/app";
    public static final String LOCALHOST = "http://localhost:3000";
    public static final String DECK_BG = "https://deck.bg";
    public static final String DECK_BG_CONFIRM_EMAIL = "https://deck.bg/api/auth/confirm-email?token=";
    public static final String DECK_BG_SUCCESS_CONFIRMATION = "https://deck.bg/confirmation-success";
    public static final String DECK_BG_CONFIRMATION_INVALID = "https://deck.bg/confirmation-invalid";
    public static final String DECK_BG_EMAIL = "no.reply.deck.bg@gmail.com";
    public static final String DECK_BG_PERSONAL = "DECK.BG";
    public static final String DECK_BG_EMAIL_SUBJECT = "Потвърди своя профил в DECK.BG";
    public static final String EMAIL_USERNAME = "{{USERNAME}}";
    public static final String EMAIL_CONFIRMATION_LINK = "{{CONFIRMATION_LINK}}";
    public static final String EMAIL_CONFIRMATION_TEMPLATE = "/templates/email-confirmation.html";
    public static final String USER = "USER";
    public static final String PROD = "prod";
    public static final String REAL_IP = "X-Real-IP";
    public static final String FORGOT_PASSWORD_SUBJECT = "Възстановяване на парола за DECK.BG";
    public static final String DECK_BG_FORGOT_PASSWORD = "https://deck.bg/api/auth/reset-password?token=";
    public static final String FORGOT_PASSWORD_TEMPLATE = "/templates/forgot-password.html";
}
