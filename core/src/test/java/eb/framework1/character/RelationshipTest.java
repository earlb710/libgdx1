package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the {@link Relationship} class and the relationship lists on
 * {@link Profile} and {@link NpcCharacter}.
 *
 * <p>Specifically validates:
 * <ul>
 *   <li>Opinion calculation from target charisma on first meeting.</li>
 *   <li>{@code addOrUpdateRelationship} / {@code getRelationship} /
 *       {@code getRelationships} on {@link Profile}.</li>
 *   <li>{@code addOrUpdateRelationship} / {@code getRelationship} /
 *       {@code getRelationships} on {@link NpcCharacter}.</li>
 *   <li>Bilateral recording via {@link Relationship#recordMeeting}.</li>
 * </ul>
 */
public class RelationshipTest {

    // =========================================================================
    // Relationship.forFirstMeeting — opinion based on charisma
    // =========================================================================

    @Test
    public void forFirstMeeting_neutralCharisma_opinionIsZero() {
        Relationship r = Relationship.forFirstMeeting("id1", "Alice", 5);
        assertEquals(0, r.getOpinion());
    }

    @Test
    public void forFirstMeeting_maxCharisma_opinionIsPositive() {
        Relationship r = Relationship.forFirstMeeting("id1", "Alice", 10);
        assertTrue("opinion should be positive for high charisma", r.getOpinion() > 0);
        assertEquals(50, r.getOpinion()); // (10 - 5) * 10 = 50
    }

    @Test
    public void forFirstMeeting_minCharisma_opinionIsNegative() {
        Relationship r = Relationship.forFirstMeeting("id1", "Bob", 1);
        assertTrue("opinion should be negative for low charisma", r.getOpinion() < 0);
        assertEquals(-40, r.getOpinion()); // (1 - 5) * 10 = -40
    }

    @Test
    public void forFirstMeeting_aboveNeutralCharisma_positiveOpinion() {
        Relationship r = Relationship.forFirstMeeting("id1", "Carol", 7);
        assertEquals(20, r.getOpinion()); // (7 - 5) * 10 = 20
        assertTrue(r.isPositive());
        assertFalse(r.isNegative());
    }

    @Test
    public void forFirstMeeting_belowNeutralCharisma_negativeOpinion() {
        Relationship r = Relationship.forFirstMeeting("id1", "Dave", 3);
        assertEquals(-20, r.getOpinion()); // (3 - 5) * 10 = -20
        assertFalse(r.isPositive());
        assertTrue(r.isNegative());
    }

    @Test
    public void forFirstMeeting_storesTargetIdAndName() {
        Relationship r = Relationship.forFirstMeeting("npc-42", "Eve Smith", 8);
        assertEquals("npc-42", r.getTargetId());
        assertEquals("Eve Smith", r.getTargetName());
    }

    // =========================================================================
    // Relationship.adjustOpinion
    // =========================================================================

    @Test
    public void adjustOpinion_positiveDelta_increasesOpinion() {
        Relationship r = Relationship.forFirstMeeting("id1", "Alice", 5); // opinion = 0
        r.adjustOpinion(15);
        assertEquals(15, r.getOpinion());
        assertTrue(r.isPositive());
    }

    @Test
    public void adjustOpinion_negativeDelta_decreasesOpinion() {
        Relationship r = Relationship.forFirstMeeting("id1", "Bob", 10); // opinion = 50
        r.adjustOpinion(-30);
        assertEquals(20, r.getOpinion());
    }

    @Test
    public void adjustOpinion_canCrossZero() {
        Relationship r = Relationship.forFirstMeeting("id1", "Carol", 6); // opinion = 10
        r.adjustOpinion(-20);
        assertEquals(-10, r.getOpinion());
        assertTrue(r.isNegative());
    }

    // =========================================================================
    // Profile — addOrUpdateRelationship / getRelationship / getRelationships
    // =========================================================================

    @Test
    public void profile_noRelationshipsOnCreation() {
        Profile p = new Profile("Player", "M", "Normal");
        assertTrue(p.getRelationships().isEmpty());
        assertNull(p.getRelationship("anything"));
    }

    @Test
    public void profile_addRelationship_retrievableById() {
        Profile p = new Profile("Player", "M", "Normal");
        Relationship r = Relationship.forFirstMeeting("npc-1", "Alice", 8);
        p.addOrUpdateRelationship(r);

        Relationship found = p.getRelationship("npc-1");
        assertNotNull(found);
        assertEquals(20, found.getOpinion());
    }

    @Test
    public void profile_addMultipleRelationships_allRetrievable() {
        Profile p = new Profile("Player", "M", "Normal");
        p.addOrUpdateRelationship(Relationship.forFirstMeeting("npc-1", "Alice", 7));
        p.addOrUpdateRelationship(Relationship.forFirstMeeting("npc-2", "Bob",   3));

        assertEquals(2, p.getRelationships().size());
        assertEquals(20,  p.getRelationship("npc-1").getOpinion());
        assertEquals(-20, p.getRelationship("npc-2").getOpinion());
    }

    @Test
    public void profile_updateExistingRelationship_replacesEntry() {
        Profile p = new Profile("Player", "M", "Normal");
        p.addOrUpdateRelationship(Relationship.forFirstMeeting("npc-1", "Alice", 5));
        assertEquals(0, p.getRelationship("npc-1").getOpinion());

        // Replace with an improved opinion
        Relationship updated = new Relationship("npc-1", "Alice", 40);
        p.addOrUpdateRelationship(updated);

        assertEquals(1, p.getRelationships().size()); // still one entry
        assertEquals(40, p.getRelationship("npc-1").getOpinion());
    }

    @Test
    public void profile_getRelationships_isUnmodifiable() {
        Profile p = new Profile("Player", "M", "Normal");
        p.addOrUpdateRelationship(Relationship.forFirstMeeting("npc-1", "Alice", 5));

        try {
            p.getRelationships().add(Relationship.forFirstMeeting("npc-2", "Bob", 5));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // =========================================================================
    // NpcCharacter — addOrUpdateRelationship / getRelationship / getRelationships
    // =========================================================================

    private NpcCharacter makeNpc(String id, String name, int charisma) {
        return new NpcCharacter.Builder()
                .id(id).fullName(name).gender("F")
                .attribute(CharacterAttribute.CHARISMA, charisma)
                .build();
    }

    @Test
    public void npc_noRelationshipsOnCreation() {
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 7);
        assertTrue(npc.getRelationships().isEmpty());
        assertNull(npc.getRelationship("anything"));
    }

    @Test
    public void npc_addRelationship_retrievableById() {
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 7);
        npc.addOrUpdateRelationship(Relationship.forFirstMeeting("player-id", "Player", 6));

        Relationship found = npc.getRelationship("player-id");
        assertNotNull(found);
        assertEquals(10, found.getOpinion()); // (6 - 5) * 10 = 10
    }

    @Test
    public void npc_updateExistingRelationship_replacesEntry() {
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 7);
        npc.addOrUpdateRelationship(Relationship.forFirstMeeting("player-id", "Player", 5));
        assertEquals(0, npc.getRelationship("player-id").getOpinion());

        npc.addOrUpdateRelationship(new Relationship("player-id", "Player", -30));

        assertEquals(1, npc.getRelationships().size());
        assertEquals(-30, npc.getRelationship("player-id").getOpinion());
    }

    @Test
    public void npc_getRelationships_isUnmodifiable() {
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 7);
        try {
            npc.getRelationships().add(Relationship.forFirstMeeting("player-id", "Player", 5));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // =========================================================================
    // Relationship.recordMeeting — bilateral first-meeting entry
    // =========================================================================

    @Test
    public void recordMeeting_playerGetsEntryForNpc() {
        Profile player = buildPlayerWithCharisma(6);
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 8);

        Relationship.recordMeeting(player, npc.getId(), npc.getFullName(),
                npc.getAttribute(CharacterAttribute.CHARISMA), npc);

        // Player's entry: based on NPC's charisma (8) → (8-5)*10 = 30
        Relationship fromPlayer = player.getRelationship("npc-1");
        assertNotNull("Player should have a relationship entry for the NPC", fromPlayer);
        assertEquals(30, fromPlayer.getOpinion());
    }

    @Test
    public void recordMeeting_npcGetsEntryForPlayer() {
        Profile player = buildPlayerWithCharisma(6);
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 8);

        Relationship.recordMeeting(player, npc.getId(), npc.getFullName(),
                npc.getAttribute(CharacterAttribute.CHARISMA), npc);

        // NPC's entry: based on player's charisma (6) → (6-5)*10 = 10
        Relationship fromNpc = npc.getRelationship(player.getCharacterId());
        assertNotNull("NPC should have a relationship entry for the player", fromNpc);
        assertEquals(10, fromNpc.getOpinion());
    }

    @Test
    public void recordMeeting_secondCallDoesNotOverwriteExistingEntry() {
        Profile player = buildPlayerWithCharisma(6);
        NpcCharacter npc = makeNpc("npc-1", "Alice Smith", 8);

        Relationship.recordMeeting(player, npc.getId(), npc.getFullName(),
                npc.getAttribute(CharacterAttribute.CHARISMA), npc);

        // Adjust the relationship manually to simulate game progress
        player.getRelationship("npc-1").adjustOpinion(50);

        // Record meeting again (e.g. second encounter trigger)
        Relationship.recordMeeting(player, npc.getId(), npc.getFullName(),
                npc.getAttribute(CharacterAttribute.CHARISMA), npc);

        // The adjusted value (30 + 50 = 80) should be preserved
        assertEquals(80, player.getRelationship("npc-1").getOpinion());
    }

    @Test
    public void recordMeeting_nullNpc_playerStillGetsEntry() {
        Profile player = buildPlayerWithCharisma(5);

        // contactNpc = null (NPC object not available, e.g. case contact)
        Relationship.recordMeeting(player, "case-contact-1", "Bob Jones",
                Relationship.NEUTRAL_CHARISMA, null);

        Relationship fromPlayer = player.getRelationship("case-contact-1");
        assertNotNull("Player should still get an entry when NPC object is absent", fromPlayer);
        assertEquals(0, fromPlayer.getOpinion()); // neutral charisma → opinion 0
    }

    @Test
    public void recordMeeting_higherCharismaNpc_higherOpinionFromPlayer() {
        Profile player = buildPlayerWithCharisma(5);
        NpcCharacter lowChaNpc  = makeNpc("npc-lo", "Low Cha",  2);
        NpcCharacter highChaNpc = makeNpc("npc-hi", "High Cha", 9);

        Relationship.recordMeeting(player, lowChaNpc.getId(), lowChaNpc.getFullName(),
                lowChaNpc.getAttribute(CharacterAttribute.CHARISMA), lowChaNpc);
        Relationship.recordMeeting(player, highChaNpc.getId(), highChaNpc.getFullName(),
                highChaNpc.getAttribute(CharacterAttribute.CHARISMA), highChaNpc);

        assertTrue("High-charisma NPC should produce higher player opinion",
                player.getRelationship("npc-hi").getOpinion()
                        > player.getRelationship("npc-lo").getOpinion());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Profile buildPlayerWithCharisma(int charisma) {
        java.util.Map<String, Integer> attrs = new java.util.HashMap<>();
        attrs.put(CharacterAttribute.CHARISMA.name(), charisma);
        return new Profile("Player", "M", "Normal", attrs);
    }
}
