package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps building definition IDs (and fallback categories) to the services they offer.
 *
 * <h3>Gym overuse mechanic</h3>
 * <p>The fitness centre grants a chance to increase {@link CharacterAttribute#STRENGTH}.
 * Working out multiple times on the same in-game day has diminishing — and eventually
 * negative — returns:</p>
 * <ul>
 *   <li>1st workout of the day: 40 % chance +1 STRENGTH</li>
 *   <li>2nd workout: 20 % chance +1 STRENGTH, 10 % chance −1 STRENGTH</li>
 *   <li>3rd+ workout: no gain, 30 % chance −1 STRENGTH (over-training)</li>
 * </ul>
 * <p>Daily workout count is tracked via two profile attributes:
 * {@code GYM_DATE} (current game date as an YYYYMMDD integer) and
 * {@code GYM_USES} (workouts on that date).</p>
 */
final class BuildingServices {

    // -------------------------------------------------------------------------
    // Service IDs (used by MainScreen to dispatch execution logic)
    // -------------------------------------------------------------------------

    static final String SVC_HOTEL_RECEPTION  = "hotel_reception";
    static final String SVC_GYM_INSTRUCTOR  = "gym_instructor";
    static final String SVC_GYM_WORKOUT     = "gym_workout";      // legacy; unused in menus
    static final String SVC_BUY_MEAL        = "buy_meal";
    static final String SVC_BUY_COFFEE     = "buy_coffee";
    static final String SVC_FINE_DINING    = "fine_dining";
    static final String SVC_BUY_MEDICINE   = "buy_medicine";
    static final String SVC_DOCTOR         = "see_doctor";
    static final String SVC_LIBRARY_STUDY  = "library_study";
    static final String SVC_ATTEND_CLASS   = "attend_class";
    static final String SVC_ENTERTAINMENT  = "entertainment";
    static final String SVC_ATTEND_SERVICE = "attend_service";
    static final String SVC_HAIRCUT        = "haircut";

    // -------------------------------------------------------------------------
    // Profile attribute keys for gym tracking
    // -------------------------------------------------------------------------

    /** Stored as YYYYMMDD integer – the in-game date of the last workout. */
    static final String ATTR_GYM_DATE = "GYM_DATE";

    /** Number of workouts performed on {@link #ATTR_GYM_DATE}. */
    static final String ATTR_GYM_USES = "GYM_USES";

    // -------------------------------------------------------------------------
    // Gym training option IDs (passed to MainScreen.handleGymTraining)
    // -------------------------------------------------------------------------

    /** Self-guided strength training (cheap, moderate chance). */
    static final int GYM_OPT_STRENGTH_SELF = 0;
    /** Personal-trainer strength session (expensive, high chance). */
    static final int GYM_OPT_STRENGTH_PT   = 1;
    /** Self-guided stamina training (cheap, moderate chance). */
    static final int GYM_OPT_STAMINA_SELF  = 2;
    /** Personal-trainer stamina session (expensive, high chance). */
    static final int GYM_OPT_STAMINA_PT    = 3;

    // Self-guided: $15, 90 min; PT: $60, 60 min
    static final int   GYM_COST_SELF  =  15;
    static final int   GYM_COST_PT    =  60;
    static final int   GYM_TIME_SELF  =  90;   // minutes
    static final int   GYM_TIME_PT    =  60;   // PT is more focused
    /** Base chance of attribute gain for self-guided training (first session of the day). */
    static final float GYM_CHANCE_SELF = 0.40f;
    /** Base chance of attribute gain for personal-trainer session. */
    static final float GYM_CHANCE_PT   = 0.70f;

    /** Chance multiplier applied to the base success chance on the 2nd gym session of the day. */
    static final float GYM_OVERUSE_SECOND_CHANCE_MULT = 0.50f;
    /** Setback risk added on the 2nd gym session of the day. */
    static final float GYM_OVERUSE_SECOND_RISK        = 0.10f;
    /** Setback risk on the 3rd+ gym session of the day (no gain possible). */
    static final float GYM_OVERUSE_THIRD_RISK         = 0.30f;

    // -------------------------------------------------------------------------
    // Profile attribute keys for hotel tracking
    // -------------------------------------------------------------------------

    /** Extra stamina added on each full-8h sleep while checked in (0 = not checked in). */
    static final String ATTR_HOTEL_BONUS  = "HOTEL_BONUS";

    /** Remaining prepaid nights (decremented by 1 after each qualifying sleep). */
    static final String ATTR_HOTEL_NIGHTS = "HOTEL_NIGHTS";

    // -------------------------------------------------------------------------
    // Hotel tier data
    // -------------------------------------------------------------------------

    /** Stamina bonus awarded on a full 8-hour sleep at each hotel tier. */
    static final int HOTEL_BONUS_BUDGET   =  5;
    static final int HOTEL_BONUS_BUSINESS = 10;
    static final int HOTEL_BONUS_LUXURY   = 20;

    /** Nightly rate for each hotel tier. */
    static final int HOTEL_COST_BUDGET   =  50;
    static final int HOTEL_COST_BUSINESS = 120;
    static final int HOTEL_COST_LUXURY   = 300;

    // -------------------------------------------------------------------------
    // Primary API
    // -------------------------------------------------------------------------

    /**
     * Returns the list of services available at the given building.
     * Returns an empty list if the building has no services or is null.
     */
    static List<BuildingService> getServices(Building building) {
        if (building == null || building.getDefinition() == null) {
            return Collections.emptyList();
        }
        String id  = building.getDefinition().getId();
        String cat = building.getDefinition().getCategory();

        switch (id) {
            // ---- Hospitality ------------------------------------------------
            case "hotel_budget":
                return list(new BuildingService(SVC_HOTEL_RECEPTION,
                        "Talk to Reception", "Check in for a night's stay.", 0, 0));
            case "hotel_business":
                return list(new BuildingService(SVC_HOTEL_RECEPTION,
                        "Talk to Reception", "Check in for a night's stay.", 0, 0));
            case "hotel_luxury":
                return list(new BuildingService(SVC_HOTEL_RECEPTION,
                        "Talk to Reception", "Check in for a night's stay.", 0, 0));

            // ---- Fitness Centre ---------------------------------------------
            case "gym_fitness_center":
                return list(new BuildingService(SVC_GYM_INSTRUCTOR,
                        "Talk to Instructor", "Get professional training advice.", 0, 0));

            // ---- Food & Drink -----------------------------------------------
            case "fast_food_restaurant":
                return list(new BuildingService(SVC_BUY_MEAL,
                        "Grab a Bite", "Quick meal at the counter.",
                        8, 20));
            case "coffee_shop":
                return list(new BuildingService(SVC_BUY_COFFEE,
                        "Buy a Coffee", "A hot cup to sharpen your senses.",
                        5, 10));
            case "restaurant_casual":
                return list(new BuildingService(SVC_BUY_MEAL,
                        "Have a Meal", "Sit-down meal with decent food.",
                        15, 30));
            case "restaurant_fine_dining":
                return list(new BuildingService(SVC_FINE_DINING,
                        "Fine Dining", "An exquisite dining experience.",
                        60, 60));

            // ---- Pharmacy ---------------------------------------------------
            case "pharmacy":
                return list(new BuildingService(SVC_BUY_MEDICINE,
                        "Buy Medicine", "Pick up over-the-counter supplies.",
                        20, 10));

            // ---- Medical ----------------------------------------------------
            case "medical_clinic":
                return list(new BuildingService(SVC_DOCTOR,
                        "See a Doctor", "Receive medical attention.",
                        80, 60));
            case "urgent_care":
                return list(new BuildingService(SVC_DOCTOR,
                        "Urgent Care", "Fast-track medical treatment.",
                        150, 45));
            case "hospital_small":
            case "hospital_large":
                return list(new BuildingService(SVC_DOCTOR,
                        "Medical Treatment", "Full hospital care.",
                        200, 120));

            // ---- Education --------------------------------------------------
            case "library":
                return list(new BuildingService(SVC_LIBRARY_STUDY,
                        "Study", "Read and research at the library.",
                        0, 90));
            case "community_college":
                return list(new BuildingService(SVC_ATTEND_CLASS,
                        "Attend a Class", "Enrol in a short course.",
                        40, 120));

            // ---- Entertainment ----------------------------------------------
            case "movie_theater":
                return list(new BuildingService(SVC_ENTERTAINMENT,
                        "Watch a Film", "Kick back and watch a movie.",
                        12, 120));
            case "bowling_alley":
                return list(new BuildingService(SVC_ENTERTAINMENT,
                        "Go Bowling", "Roll a few frames.",
                        15, 60));
            case "nightclub":
                return list(new BuildingService(SVC_ENTERTAINMENT,
                        "Night Out", "Dance the night away.",
                        30, 120));
            case "sports_arena":
                return list(new BuildingService(SVC_ENTERTAINMENT,
                        "Watch a Game", "Cheer on the local team.",
                        25, 180));

            // ---- Religious --------------------------------------------------
            case "church":
            case "mosque":
            case "synagogue":
                return list(new BuildingService(SVC_ATTEND_SERVICE,
                        "Attend Service", "Spend time in quiet reflection.",
                        0, 60));

            // ---- Hair Salon -------------------------------------------------
            case "hair_salon":
                return list(new BuildingService(SVC_HAIRCUT,
                        "Get a Haircut", "Look your best.",
                        25, 30));

            default:
                return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Gym mechanic helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the number of gym workouts the player has already done today.
     *
     * @param profile   the current character profile
     * @param todayDate today's in-game date as YYYYMMDD (e.g. 20500102)
     */
    static int gymUsesToday(Profile profile, int todayDate) {
        int storedDate = profile.getAttribute(ATTR_GYM_DATE);
        if (storedDate != todayDate) return 0;
        return profile.getAttribute(ATTR_GYM_USES);
    }

    /**
     * Records one gym workout for today, resetting the counter if the date has changed.
     *
     * @param profile   the current character profile
     * @param todayDate today's in-game date as YYYYMMDD
     */
    static void recordGymUse(Profile profile, int todayDate) {
        int storedDate = profile.getAttribute(ATTR_GYM_DATE);
        int uses = (storedDate == todayDate) ? profile.getAttribute(ATTR_GYM_USES) : 0;
        profile.setAttribute(ATTR_GYM_DATE, todayDate);
        profile.setAttribute(ATTR_GYM_USES, uses + 1);
    }

    /**
     * Converts a "YYYY-MM-DD HH:MM" game-time string to an YYYYMMDD integer
     * (e.g. "2050-01-02 13:20" → 20500102).  Returns 0 for null/invalid input.
     */
    static int gameDateInt(String gameDateTime) {
        if (gameDateTime == null || gameDateTime.isEmpty()) return 0;
        try {
            String datePart = gameDateTime.split(" ")[0];
            return Integer.parseInt(datePart.replace("-", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Hotel tier helpers
    // -------------------------------------------------------------------------

    /** Returns the nightly rate for the hotel building, or 0 if not a hotel. */
    static int getHotelNightlyCost(Building building) {
        if (building == null || building.getDefinition() == null) return 0;
        switch (building.getDefinition().getId()) {
            case "hotel_budget":   return HOTEL_COST_BUDGET;
            case "hotel_business": return HOTEL_COST_BUSINESS;
            case "hotel_luxury":   return HOTEL_COST_LUXURY;
            default:               return 0;
        }
    }

    /** Returns the per-night stamina bonus for the hotel building, or 0 if not a hotel. */
    static int getHotelStaminaBonus(Building building) {
        if (building == null || building.getDefinition() == null) return 0;
        switch (building.getDefinition().getId()) {
            case "hotel_budget":   return HOTEL_BONUS_BUDGET;
            case "hotel_business": return HOTEL_BONUS_BUSINESS;
            case "hotel_luxury":   return HOTEL_BONUS_LUXURY;
            default:               return 0;
        }
    }

    /** Returns a human-readable room type label for the hotel building. */
    static String getHotelRoomType(Building building) {
        if (building == null || building.getDefinition() == null) return "Standard Room";
        switch (building.getDefinition().getId()) {
            case "hotel_budget":   return "Budget Room";
            case "hotel_business": return "Comfortable Room";
            case "hotel_luxury":   return "Luxury Suite";
            default:               return "Standard Room";
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<BuildingService> list(BuildingService svc) {
        List<BuildingService> result = new ArrayList<>(1);
        result.add(svc);
        return result;
    }

    private BuildingServices() {} // static utility — not instantiable
}
