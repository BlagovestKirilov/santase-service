package bg.deck.santaseservice.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String KING = "KING";
    public static final String QUEEN = "QUEEN";
    public static final String NOTIFY_GAME_DESTINATION = "/topic/game/%s/%s";
    public static final String NOTIFY_GAME_SEARCH_DESTINATION = "/topic/game/%s";
    /**
     * Search topic namespaced by game, so a user queued for табла is not woken by
     * a Santase match. The legacy destination above is published to as well for
     * one release, so a client mid-deploy does not miss its match.
     */
    public static final String NOTIFY_SEARCH_BY_GAME_DESTINATION = "/topic/search/%s/%s";
    public static final String ROLE = "role";
    public static final String USERNAME = "username";
    public static final String BEARER = "Bearer ";
    public static final String WEB_SOCKET_ENDPOINT = "/ws-game";
    public static final String TOKEN = "token";
    public static final String TOPIC = "/topic";
    public static final String APP = "/app";
    public static final String LOCALHOST = "http://localhost:3000";
    public static final String DECK_BG = "https://deck.bg";
    public static final String DECK_BG_CONFIRM_EMAIL = "https://deck.bg/api/auth/confirm-email?token=";
    public static final String DECK_BG_SUCCESS_CONFIRMATION = "https://deck.bg/confirmation-success";
    public static final String DECK_BG_INVALID_LINK = "https://deck.bg/invalid";
    public static final String DECK_BG_EMAIL = "no.reply.deck.bg@gmail.com";
    /** The brand as it is written everywhere else: DECK in capitals, .bg in lower case. */
    public static final String DECK_BG_PERSONAL = "DECK.bg";
    public static final String DECK_BG_EMAIL_SUBJECT = "Потвърди своя профил в DECK.bg";
    public static final String EMAIL_USERNAME = "{{USERNAME}}";
    public static final String EMAIL_CONFIRMATION_LINK = "{{CONFIRMATION_LINK}}";
    public static final String EMAIL_CONFIRMATION_TEMPLATE = "/templates/email-confirmation.html";
    public static final String USER = "USER";
    public static final String PROD = "prod";
    public static final String CF_CONNECTING_IP = "CF-Connecting-IP";
    public static final String FORGOT_PASSWORD_SUBJECT = "Нова парола за DECK.bg";
    public static final String DECK_BG_FORGOT_PASSWORD = "https://deck.bg/reset-password?token=";
    public static final String FORGOT_PASSWORD_TEMPLATE = "/templates/forgot-password.html";
    public static final String DELETION_SUBJECT = "Изтриване на профила в DECK.bg";
    /**
     * The deletion link goes to a page on the site, which asks the person to
     * confirm and then POSTs the token. It used to point straight at the API
     * endpoint, which deleted the account on GET — so any mail scanner or
     * link prefetcher that opened the message (Outlook Safe Links, antivirus,
     * some mobile mail clients) destroyed the account with nobody clicking.
     */
    public static final String DECK_BG_DELETE_ACCOUNT = "https://deck.bg/confirm-deletion?token=";
    public static final String DELETION_TEMPLATE = "/templates/user-deletion.html";
}
