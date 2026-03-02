package eb.framework1;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the client introduction dialogue, detective's standard reply, and
 * a list of follow-up question labels for the {@link MeetPopup}.
 *
 * <p>Speech style is influenced by:
 * <ul>
 *   <li><strong>Gender</strong> ({@code "M"} / {@code "F"}) — used for pronouns
 *       and light tonal adjustments.</li>
 *   <li><strong>Charisma</strong> (1–10) — higher values produce polished,
 *       eloquent opening lines; lower values result in simpler wording.</li>
 *   <li><strong>Empathy</strong> (1–10) — higher values give more emotional,
 *       personal language.</li>
 *   <li><strong>Nervousness</strong> (1–10) — higher values add hesitation
 *       markers and shorter sentences.</li>
 * </ul>
 *
 * <p>This class has <strong>no libGDX dependency</strong> and can be used in
 * plain-Java unit tests.
 */
public final class ClientIntroductionGenerator {

    // All static — no instance state needed.
    private ClientIntroductionGenerator() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds a first-person introduction from the client to the detective.
     *
     * @param clientName  full name of the client; must not be blank
     * @param gender      {@code "M"} or {@code "F"} (case-insensitive)
     * @param charisma    1–10 eloquence / social confidence
     * @param empathy     1–10 emotional expressiveness
     * @param nervousness 1–10 anxiety level
     * @param caseContext short description of the problem (may be empty)
     * @return multi-sentence introduction string
     */
    public static String buildIntroduction(
            String clientName,
            String gender,
            int    charisma,
            int    empathy,
            int    nervousness,
            String caseContext) {

        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = "Unknown";
        }
        boolean isMale    = !"F".equalsIgnoreCase(gender);
        boolean polished  = charisma   >= 7;
        boolean emotional = empathy    >= 7;
        boolean anxious   = nervousness >= 7;

        StringBuilder sb = new StringBuilder();

        // --- Opening sentence ---
        if (anxious) {
            sb.append("I… I'm ").append(clientName).append(". ")
              .append("Thank you for seeing me — I wasn't sure you'd be able to.");
        } else if (polished) {
            sb.append("Good day. My name is ").append(clientName).append(". ")
              .append("I appreciate you taking the time to meet with me.");
        } else {
            sb.append("Hi. I'm ").append(clientName).append(". ")
              .append("Thanks for meeting me.");
        }

        // --- Emotional / personal layer ---
        if (emotional && anxious) {
            sb.append(" This situation has been weighing on me heavily — ")
              .append(isMale ? "he has" : "she has")
              .append(" been through a great deal already.");
        } else if (emotional) {
            sb.append(" I'll be honest — this whole thing has hit me harder than I expected.");
        }

        // --- Case context ---
        String ctx = (caseContext != null) ? caseContext.trim() : "";
        if (!ctx.isEmpty()) {
            sb.append("\n\n");
            if (anxious) {
                sb.append("The reason I'm here… ");
            } else if (polished) {
                sb.append("I am here because ");
            } else {
                sb.append("So the thing is — ");
            }
            sb.append(ctx);
            if (!ctx.endsWith(".") && !ctx.endsWith("?") && !ctx.endsWith("!")) {
                sb.append(".");
            }
        } else {
            sb.append("\n\nI need your help with a personal matter.");
        }

        // --- Closing ask ---
        if (anxious) {
            sb.append(" I really hope you can help me.");
        } else if (polished) {
            sb.append(" I trust that your expertise will prove invaluable.");
        } else {
            sb.append(" I'm hoping you can look into this for me.");
        }

        return sb.toString();
    }

    /**
     * Builds the detective's short standard reply to the client's introduction.
     *
     * @param clientName full name of the client
     * @return reply text
     */
    public static String buildDetectiveReply(String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) clientName = "there";
        String first = firstName(clientName);
        return "Thank you, " + first + ". I'm listening. "
                + "Take your time and tell me everything — even the smallest detail can matter. "
                + "Nothing you say leaves this room.";
    }

    /**
     * Returns the fixed list of follow-up question labels the player can ask.
     *
     * @return unmodifiable list of question strings
     */
    public static List<String> buildQuestions() {
        List<String> qs = new ArrayList<>();
        qs.add("What exactly do you need me to do?");
        qs.add("Tell me more about the subject.");
        qs.add("How long has this been going on?");
        qs.add("Is there anyone else who knows about this?");
        return java.util.Collections.unmodifiableList(qs);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Extracts the first word of a full name to use as a given name. */
    static String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "there";
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }
}
