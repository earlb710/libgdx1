package eb.framework1.character;

import eb.framework1.city.*;
import eb.framework1.generator.*;
import eb.framework1.investigation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates fully-populated NPC characters complete with skills and a daily
 * schedule.
 *
 * <p>NpcGenerator wraps {@link CharacterGenerator} and enriches each generated
 * character with:
 * <ul>
 *   <li><strong>Skills</strong> — one or more {@link NpcSkill} values inferred
 *       from the NPC's occupation string.</li>
 *   <li><strong>Schedule</strong> — a {@link NpcSchedule} describing where the
 *       NPC is at each hour of the day, with real map coordinates when a
 *       {@link CityMap} is provided.</li>
 * </ul>
 *
 * <p>When a {@link CityMap} is supplied, the generator locates real buildings
 * on the map whose category matches the NPC's primary skill, updating the
 * NPC's {@code homeAddress}, {@code workplaceAddress}, and
 * {@code frequentLocations} accordingly.  If no matching building is found (or
 * no city map is supplied), schedule entries fall back to descriptive text
 * without map coordinates ({@code cellX}/{@code cellY} = {@code -1}).
 *
 * <h3>Example</h3>
 * <pre>
 *   NpcGenerator gen = new NpcGenerator(nameGenerator, new Random(42));
 *
 *   // Without a city map — coordinates will be -1
 *   NpcCharacter suspect = gen.generateSuspect(CaseType.FRAUD, null);
 *
 *   // With a real city map so the schedule has valid coordinates
 *   CityMap city = new CityMap(42L);
 *   NpcCharacter client = gen.generateClient(CaseType.THEFT, city);
 *
 *   List&lt;NpcSkill&gt;   skills   = client.getSkills();
 *   NpcSchedule       schedule = client.getSchedule();
 *   NpcScheduleEntry  at14     = schedule.getEntryForHour(14); // 2 PM
 * </pre>
 */
public class NpcGenerator {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The in-game base year from which birthdates are derived. */
    static final int GAME_YEAR = 2050;

    /** Days per month (non-leap year). February capped at 28 so that dates remain
     *  valid regardless of whether the birth year is a leap year. */
    private static final int[] DAYS_IN_MONTH = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    private final CharacterGenerator charGen;
    private final Random             random;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a generator using the given name source and a default {@link Random}.
     *
     * @param nameGen name generator used to produce NPC names; must not be {@code null}
     */
    public NpcGenerator(PersonNameGenerator nameGen) {
        this(nameGen, new Random());
    }

    /**
     * Creates a generator with an explicit {@link Random} instance.
     * Pass a seeded {@code Random} for reproducible output.
     *
     * @param nameGen name generator; must not be {@code null}
     * @param random  random source; {@code null} is replaced by {@code new Random()}
     */
    public NpcGenerator(PersonNameGenerator nameGen, Random random) {
        this.charGen = new CharacterGenerator(nameGen, random);
        this.random  = random != null ? random : new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates a client NPC enriched with skills and a daily schedule.
     *
     * @param caseType the type of case being investigated; must not be {@code null}
     * @param cityMap  optional city map used to resolve real building locations;
     *                 may be {@code null}
     * @return a fully populated {@link NpcCharacter}
     */
    public NpcCharacter generateClient(CaseType caseType, CityMap cityMap) {
        return enrich(charGen.generateClient(caseType), cityMap);
    }

    /**
     * Generates a victim NPC enriched with skills and a daily schedule.
     *
     * @param caseType the type of case being investigated; must not be {@code null}
     * @param cityMap  optional city map; may be {@code null}
     * @return a fully populated {@link NpcCharacter}
     */
    public NpcCharacter generateVictim(CaseType caseType, CityMap cityMap) {
        return enrich(charGen.generateVictim(caseType), cityMap);
    }

    /**
     * Generates a suspect NPC with the default personality profile, skills and
     * a daily schedule.
     *
     * @param caseType the type of case being investigated; must not be {@code null}
     * @param cityMap  optional city map; may be {@code null}
     * @return a fully populated {@link NpcCharacter}
     */
    public NpcCharacter generateSuspect(CaseType caseType, CityMap cityMap) {
        return generateSuspect(caseType, PersonalityProfile.DEFAULT, cityMap);
    }

    /**
     * Generates a suspect NPC with a specific personality profile, skills and
     * a daily schedule.
     *
     * @param caseType the type of case being investigated; must not be {@code null}
     * @param profile  personality profile; {@code null} defaults to
     *                 {@link PersonalityProfile#DEFAULT}
     * @param cityMap  optional city map; may be {@code null}
     * @return a fully populated {@link NpcCharacter}
     */
    public NpcCharacter generateSuspect(CaseType caseType, PersonalityProfile profile,
                                        CityMap cityMap) {
        return enrich(charGen.generateSuspect(caseType, profile), cityMap);
    }

    // -------------------------------------------------------------------------
    // Enrichment
    // -------------------------------------------------------------------------

    /**
     * Infers skills, builds a schedule, and reconstructs the NPC with the
     * enriched data.
     */
    private NpcCharacter enrich(NpcCharacter base, CityMap cityMap) {
        List<NpcSkill> skills   = inferSkills(base.getOccupation());
        NpcSchedule    schedule = buildSchedule(base, skills, cityMap);

        // Derive birthdate from age: birth year = GAME_YEAR − age, random month/day
        String birthdate = buildBirthdate(base.getAge());

        // Extract updated home and work addresses from the generated schedule
        String homeAddress = base.getHomeAddress();
        String workAddress = base.getWorkplaceAddress();
        String leisureAddress = "";

        for (NpcScheduleEntry entry : schedule.getEntries()) {
            boolean isHome = NpcScheduleEntry.HOME.equals(entry.activityType)
                    || NpcScheduleEntry.SLEEP.equals(entry.activityType);
            if (isHome && !entry.locationName.isEmpty() && homeAddress.isEmpty()) {
                homeAddress = entry.locationName;
            }
            if (NpcScheduleEntry.WORK.equals(entry.activityType)
                    && !entry.locationName.isEmpty()) {
                workAddress = entry.locationName;
            }
            if ((NpcScheduleEntry.LEISURE.equals(entry.activityType)
                    || NpcScheduleEntry.SHOPPING.equals(entry.activityType))
                    && !entry.locationName.isEmpty() && leisureAddress.isEmpty()) {
                leisureAddress = entry.locationName;
            }
        }

        NpcCharacter.Builder builder = new NpcCharacter.Builder()
                .id(base.getId())
                .fullName(base.getFullName())
                .gender(base.getGender())
                .age(base.getAge())
                .occupation(base.getOccupation())
                .spriteKey(base.getSpriteKey())
                .physicalDescription(base.getPhysicalDescription())
                .homeAddress(homeAddress)
                .workplaceAddress(workAddress)
                .phoneNumber(base.getPhoneNumber())
                .email(base.getEmail())
                .cooperativeness(base.getCooperativeness())
                .honesty(base.getHonesty())
                .nervousness(base.getNervousness())
                .personalityProfile(base.getPersonalityProfile())
                .attributes(base.getAttributes())
                .frequentLocations(base.getFrequentLocations())
                .skills(skills)
                .schedule(schedule)
                .birthdate(birthdate);
        // tracked defaults to false; callers can set it explicitly after generation

        // Add the evening leisure/shopping location as a frequent location
        if (!leisureAddress.isEmpty()) {
            builder.addFrequentLocation(leisureAddress);
        }

        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Skill inference
    // -------------------------------------------------------------------------

    /**
     * Infers a list of {@link NpcSkill} values from an occupation string.
     *
     * <p>The occupation string is matched case-insensitively against keywords.
     * Multiple skills are possible; the list always contains at least one
     * element (fallback: {@link NpcSkill#FREELANCER}).
     *
     * <p>This method is package-private so it can be exercised directly in
     * unit tests.
     *
     * @param occupation NPC occupation string; may be {@code null} or empty
     * @return a non-null, non-empty list of inferred skills
     */
    static List<NpcSkill> inferSkills(String occupation) {
        List<NpcSkill> skills = new ArrayList<>();
        if (occupation == null || occupation.trim().isEmpty()) {
            skills.add(NpcSkill.FREELANCER);
            return skills;
        }
        String occ = occupation.toLowerCase();

        // Medical
        if (occ.contains("nurse") || occ.contains("doctor") || occ.contains("medic")
                || occ.contains("health") || occ.contains("social worker")
                || occ.contains("dentist") || occ.contains("therapist")) {
            skills.add(NpcSkill.MEDICAL_PROFESSIONAL);
        }
        // Education
        if (occ.contains("teacher") || occ.contains("educator") || occ.contains("instructor")
                || occ.contains("student") || occ.contains("tutor")
                || occ.contains("professor") || occ.contains("lecturer")) {
            skills.add(NpcSkill.EDUCATOR);
        }
        // Law enforcement
        if (occ.contains("police") || occ.contains("detective") || occ.contains("officer")
                || occ.contains("law enforcement") || occ.contains("security")) {
            skills.add(NpcSkill.LAW_ENFORCEMENT);
        }
        // Government
        if (occ.contains("politician") || occ.contains("government") || occ.contains("official")
                || occ.contains("civil servant") || occ.contains("mayor")
                || occ.contains("judge") || occ.contains("attorney")) {
            skills.add(NpcSkill.GOVERNMENT_WORKER);
        }
        // Hospitality / food service
        if (occ.contains("chef") || occ.contains("waiter") || occ.contains("waitress")
                || occ.contains("hotel") || occ.contains("concierge")
                || occ.contains("bartender") || occ.contains("barista")
                || occ.contains("personal trainer")) {
            skills.add(NpcSkill.HOSPITALITY_WORKER);
        }
        // Entertainment
        if (occ.contains("entertainer") || occ.contains("performer") || occ.contains("artist")
                || occ.contains("musician") || occ.contains("actor")
                || occ.contains("comedian") || occ.contains("public figure")) {
            skills.add(NpcSkill.ENTERTAINER);
        }
        // Retail / commercial
        if (occ.contains("retail") || occ.contains("shop") || occ.contains("cashier")
                || occ.contains("clerk") || occ.contains("store") || occ.contains("homeowner")
                || occ.contains("landlord") || occ.contains("property owner")) {
            skills.add(NpcSkill.SHOP_CLERK);
        }
        // Industrial / manual labour
        if (occ.contains("labour") || occ.contains("driver") || occ.contains("mechanic")
                || occ.contains("factory") || occ.contains("warehouse")
                || occ.contains("contractor") || occ.contains("plumber")
                || occ.contains("electrician")) {
            skills.add(NpcSkill.LABORER);
        }
        // Research / engineering
        if (occ.contains("researcher") || occ.contains("engineer") || occ.contains("scientist")
                || occ.contains("analyst") || occ.contains("product manager")
                || occ.contains("data")) {
            skills.add(NpcSkill.RESEARCHER);
        }
        // Office / business / white-collar
        if (occ.contains("accountant") || occ.contains("financial") || occ.contains("executive")
                || occ.contains("manager") || occ.contains("director")
                || occ.contains("business owner") || occ.contains("investor")
                || occ.contains("journalist") || occ.contains("sales")
                || occ.contains("professional") || occ.contains("colleague")
                || occ.contains("associate") || occ.contains("retiree")
                || occ.contains("small business")) {
            skills.add(NpcSkill.OFFICE_WORKER);
        }
        // Homemaker / domestic / relationship roles
        if (occ.contains("spouse") || occ.contains("parent") || occ.contains("sibling")
                || occ.contains("partner") || occ.contains("relative")
                || occ.contains("friend") || occ.contains("neighbour")
                || occ.contains("acquaintance") || occ.contains("ex-partner")
                || occ.contains("fiancé") || occ.contains("family")
                || occ.contains("homemaker")) {
            skills.add(NpcSkill.HOMEMAKER);
        }
        // Freelancer / unemployed / ambiguous
        if (occ.contains("freelancer") || occ.contains("unemployed")
                || occ.contains("self-employed")) {
            skills.add(NpcSkill.FREELANCER);
        }

        if (skills.isEmpty()) {
            skills.add(NpcSkill.FREELANCER);
        }
        return skills;
    }

    // -------------------------------------------------------------------------
    // Schedule generation
    // -------------------------------------------------------------------------

    /**
     * Generates a 24-hour daily schedule for an NPC.
     *
     * <p>The schedule has the following structure:
     * <ol>
     *   <li>00:00–06:00 — {@link NpcScheduleEntry#SLEEP} at home</li>
     *   <li>06:00–(workStart) — {@link NpcScheduleEntry#HOME} (morning routine)</li>
     *   <li>(workStart)–(workEnd) — {@link NpcScheduleEntry#WORK} at the
     *       skill-matched building; replaced by HOME + SHOPPING for
     *       {@link NpcSkill#HOMEMAKER}</li>
     *   <li>(workEnd)–21:00 — {@link NpcScheduleEntry#LEISURE} or
     *       {@link NpcScheduleEntry#SHOPPING}</li>
     *   <li>21:00–22:00 — {@link NpcScheduleEntry#HOME} (evening)</li>
     *   <li>22:00–24:00 — {@link NpcScheduleEntry#SLEEP}</li>
     * </ol>
     *
     * <p>When {@code cityMap} is non-null, real buildings are located by
     * category and their map coordinates are stored in the entries.
     */
    private NpcSchedule buildSchedule(NpcCharacter npc, List<NpcSkill> skills,
                                      CityMap cityMap) {
        // Determine primary skill — prefer any skill other than HOMEMAKER/FREELANCER
        NpcSkill primarySkill = null;
        for (NpcSkill s : skills) {
            if (s != NpcSkill.HOMEMAKER && s != NpcSkill.FREELANCER) {
                primarySkill = s;
                break;
            }
        }
        if (primarySkill == null && !skills.isEmpty()) {
            primarySkill = skills.get(0);
        }
        if (primarySkill == null) {
            primarySkill = NpcSkill.FREELANCER;
        }

        int workStart = primarySkill.getWorkStartHour();
        int workEnd   = primarySkill.getWorkEndHour();

        // --- Resolve locations from city map (or use placeholder text) ---
        String homeName  = npc.getHomeAddress().isEmpty() ? "Home" : npc.getHomeAddress();
        int    homeCellX = -1, homeCellY = -1;

        String workName  = npc.getWorkplaceAddress().isEmpty()
                ? primarySkill.getDisplayName() + " Workplace"
                : npc.getWorkplaceAddress();
        int    workCellX = -1, workCellY = -1;

        String leisureName  = "Local Area";
        int    leisureCellX = -1, leisureCellY = -1;

        if (cityMap != null) {
            // Home building (residential)
            List<int[]> homeCells = findCellsByCategories(cityMap,
                    new String[]{"residential"});
            if (!homeCells.isEmpty()) {
                int[] hc    = homeCells.get(random.nextInt(homeCells.size()));
                homeCellX   = hc[0];
                homeCellY   = hc[1];
                homeName    = cityMap.getCell(homeCellX, homeCellY)
                        .getBuilding().getDisplayName();
            }

            // Work building — category from primary skill
            List<int[]> workCells = findCellsByCategories(cityMap,
                    primarySkill.getWorkBuildingCategories());
            if (!workCells.isEmpty()) {
                int[] wc    = workCells.get(random.nextInt(workCells.size()));
                workCellX   = wc[0];
                workCellY   = wc[1];
                workName    = cityMap.getCell(workCellX, workCellY)
                        .getBuilding().getDisplayName();
            }

            // Leisure building (entertainment or commercial)
            List<int[]> leisureCells = findCellsByCategories(cityMap,
                    new String[]{"entertainment", "commercial"});
            if (!leisureCells.isEmpty()) {
                int[] lc     = leisureCells.get(random.nextInt(leisureCells.size()));
                leisureCellX = lc[0];
                leisureCellY = lc[1];
                leisureName  = cityMap.getCell(leisureCellX, leisureCellY)
                        .getBuilding().getDisplayName();
            }
        }

        // --- Build entries ---
        List<NpcScheduleEntry> entries = new ArrayList<>();

        // 1. Night sleep 00:00–06:00
        entries.add(new NpcScheduleEntry(0, 6,
                NpcScheduleEntry.SLEEP, homeName, homeCellX, homeCellY));

        // 2. Morning routine at home (06:00–workStart)
        if (workStart > 6) {
            entries.add(new NpcScheduleEntry(6, workStart,
                    NpcScheduleEntry.HOME, homeName, homeCellX, homeCellY));
        }

        // 3. Work block (or homemaker variant)
        boolean isHomemaker = (primarySkill == NpcSkill.HOMEMAKER);
        if (isHomemaker) {
            // Homemakers stay home then go shopping in the middle of the day
            int shopHour = workStart + (workEnd - workStart) / 2;
            if (workStart < shopHour) {
                entries.add(new NpcScheduleEntry(workStart, shopHour,
                        NpcScheduleEntry.HOME, homeName, homeCellX, homeCellY));
            }
            entries.add(new NpcScheduleEntry(shopHour, workEnd,
                    NpcScheduleEntry.SHOPPING, leisureName, leisureCellX, leisureCellY));
        } else {
            entries.add(new NpcScheduleEntry(workStart, workEnd,
                    NpcScheduleEntry.WORK, workName, workCellX, workCellY));
        }

        // 4. Evening leisure or shopping (workEnd–21:00)
        if (workEnd < 21) {
            String eveningActivity = random.nextBoolean()
                    ? NpcScheduleEntry.LEISURE
                    : NpcScheduleEntry.SHOPPING;
            entries.add(new NpcScheduleEntry(workEnd, 21,
                    eveningActivity, leisureName, leisureCellX, leisureCellY));
        }

        // 5. Evening home routine (21:00–22:00)
        entries.add(new NpcScheduleEntry(21, 22,
                NpcScheduleEntry.HOME, homeName, homeCellX, homeCellY));

        // 6. Night sleep (22:00–24:00)
        entries.add(new NpcScheduleEntry(22, 24,
                NpcScheduleEntry.SLEEP, homeName, homeCellX, homeCellY));

        return new NpcSchedule(entries);
    }

    // -------------------------------------------------------------------------
    // Birthdate helpers
    // -------------------------------------------------------------------------

    /**
     * Derives a {@code "YYYY-MM-DD"} birthdate string from an age in years.
     *
     * <p>The birth year is {@code GAME_YEAR − age}.  Month and day are chosen
     * randomly so that every NPC born in the same year has a plausible, unique
     * birthday.
     *
     * @param age age in years (non-negative); 0 produces birth year == GAME_YEAR
     * @return birthdate string in {@code "YYYY-MM-DD"} format
     */
    String buildBirthdate(int age) {
        int birthYear = GAME_YEAR - Math.max(0, age);
        int month     = 1 + random.nextInt(12);          // 1–12
        int day       = 1 + random.nextInt(DAYS_IN_MONTH[month - 1]); // 1–28/30/31
        return String.format("%04d-%02d-%02d", birthYear, month, day);
    }

    // -------------------------------------------------------------------------
    // City-map helpers
    // -------------------------------------------------------------------------

    /**
     * Returns all city-map cells whose building belongs to one of the given
     * category IDs.
     *
     * @param cityMap    the map to search; must not be {@code null}
     * @param categories building category IDs to match (e.g. {@code "commercial"})
     * @return a (possibly empty) list of {@code [x, y]} coordinate pairs
     */
    private List<int[]> findCellsByCategories(CityMap cityMap, String[] categories) {
        List<int[]> result = new ArrayList<>();
        Set<String> catSet = new HashSet<>(Arrays.asList(categories));
        Cell[][] cells = cityMap.getCells();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = cells[x][y];
                if (cell.hasBuilding()) {
                    String cat = cell.getBuilding().getCategory();
                    if (cat != null && catSet.contains(cat)) {
                        result.add(new int[]{x, y});
                    }
                }
            }
        }
        return result;
    }
}
