package eb.framework1;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GameSave} — construction, snapshot accuracy, and state restoration.
 * These tests are pure-Java and require no libGDX runtime.
 */
public class GameSaveTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Profile makeProfile() {
        Map<String, Integer> attrs = new HashMap<>();
        attrs.put(CharacterAttribute.INTELLIGENCE.name(), 5);
        attrs.put(CharacterAttribute.STAMINA.name(), 3);
        Profile p = new Profile("Alice", "Female", "Normal", "woman1", attrs, 2051, 999L);
        p.setMoney(1500);
        p.setGameDateTime("2051-03-15 09:30");
        p.setCurrentStamina(25);
        return p;
    }

    private Building makeBuilding(String name) {
        List<Improvement> imps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            imps.add(new Improvement("Imp " + i, 1, i));
        }
        return new Building(name, imps);
    }

    // -------------------------------------------------------------------------
    // Snapshot – profile fields
    // -------------------------------------------------------------------------

    @Test
    public void testSnapshotCapturesCharacterName() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 2, 3, 4, 5);
        assertEquals("Alice", s.getCharacterName());
    }

    @Test
    public void testSnapshotCapturesGenderDifficultyIcon() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals("Female", s.getGender());
        assertEquals("Normal", s.getDifficulty());
        assertEquals("woman1", s.getCharacterIcon());
    }

    @Test
    public void testSnapshotCapturesMoney() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(1500, s.getMoney());
    }

    @Test
    public void testSnapshotCapturesGameDateTime() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals("2051-03-15 09:30", s.getGameDateTime());
    }

    @Test
    public void testSnapshotCapturesGameDate() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(2051, s.getGameDate());
    }

    @Test
    public void testSnapshotCapturesRandSeed() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(999L, s.getRandSeed());
    }

    @Test
    public void testSnapshotCapturesCurrentStamina() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(25, s.getCurrentStamina());
    }

    @Test
    public void testSnapshotCapturesAttributes() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);
        assertEquals(5, (int) s.getAttributes().get(CharacterAttribute.INTELLIGENCE.name()));
        assertEquals(3, (int) s.getAttributes().get(CharacterAttribute.STAMINA.name()));
    }

    // -------------------------------------------------------------------------
    // Snapshot – position fields
    // -------------------------------------------------------------------------

    @Test
    public void testSnapshotCapturesCharacterPosition() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 7, 11, 0, 0);
        assertEquals(7,  s.getCharCellX());
        assertEquals(11, s.getCharCellY());
    }

    @Test
    public void testSnapshotCapturesHomePosition() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 5, 9);
        assertEquals(5, s.getHomeCellX());
        assertEquals(9, s.getHomeCellY());
    }

    // -------------------------------------------------------------------------
    // Snapshot – map discovery state
    // -------------------------------------------------------------------------

    @Test
    public void testSnapshotCapturesBuildingDiscovery() {
        CityMap map = new CityMap(12345L);
        // Discover the first building we find
        int dx = -1, dy = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && dx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && dx < 0; y++) {
                if (map.getCell(x, y).hasBuilding()) { dx = x; dy = y; }
            }
        }
        assertTrue("Map must have at least one building cell", dx >= 0);
        map.getCell(dx, dy).getBuilding().discover();

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map, 0, 0, 0, 0);

        boolean[] disc = s.getBuildingDiscovered();
        assertTrue("Discovered building must be flagged in save",
                disc[dx * CityMap.MAP_SIZE + dy]);
    }

    @Test
    public void testSnapshotUndiscoveredBuildingIsFalse() {
        CityMap  map = new CityMap(12345L);
        Profile  p   = makeProfile();
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);

        boolean[] disc = s.getBuildingDiscovered();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                assertFalse("No building should be discovered in fresh map at (" + x + "," + y + ")",
                        disc[x * CityMap.MAP_SIZE + y]);
            }
        }
    }

    @Test
    public void testSnapshotCapturesImprovementDiscovery() {
        CityMap map = new CityMap(12345L);
        int bx = -1, by = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && bx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && bx < 0; y++) {
                if (map.getCell(x, y).hasBuilding()) { bx = x; by = y; }
            }
        }
        assertTrue("Map must have at least one building cell", bx >= 0);
        Building b = map.getCell(bx, by).getBuilding();
        b.getImprovements().get(0).discover();
        b.getImprovements().get(2).discover();

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map, 0, 0, 0, 0);

        boolean[] iDisc = s.getImprovementDiscovered();
        int base = (bx * CityMap.MAP_SIZE + by) * 4;
        assertTrue("Improvement 0 should be flagged",  iDisc[base]);
        assertFalse("Improvement 1 should not be flagged", iDisc[base + 1]);
        assertTrue("Improvement 2 should be flagged",  iDisc[base + 2]);
        assertFalse("Improvement 3 should not be flagged", iDisc[base + 3]);
    }

    @Test
    public void testSnapshotCapturesBuildingOwned() {
        CityMap map = new CityMap(12345L);
        int ox = -1, oy = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && ox < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && ox < 0; y++) {
                if (map.getCell(x, y).hasBuilding()) { ox = x; oy = y; }
            }
        }
        assertTrue("Map must have at least one building cell", ox >= 0);
        map.getCell(ox, oy).getBuilding().setOwned(true);

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map, 0, 0, 0, 0);

        boolean[] owned = s.getBuildingOwned();
        assertTrue("Owned building should be flagged in save",
                owned[ox * CityMap.MAP_SIZE + oy]);
    }

    // -------------------------------------------------------------------------
    // applyToProfile
    // -------------------------------------------------------------------------

    @Test
    public void testApplyToProfileRestoresMoney() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = new Profile("Alice", "Female", "Normal", "woman1", new HashMap<>(), 2051, 999L);
        s.applyToProfile(p2);
        assertEquals(1500, p2.getMoney());
    }

    @Test
    public void testApplyToProfileRestoresGameDateTime() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = new Profile("Alice", "Female", "Normal");
        s.applyToProfile(p2);
        assertEquals("2051-03-15 09:30", p2.getGameDateTime());
    }

    @Test
    public void testApplyToProfileRestoresStamina() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = new Profile("Alice", "Female", "Normal");
        s.applyToProfile(p2);
        assertEquals(25, p2.getCurrentStamina());
    }

    @Test
    public void testApplyToProfileRestoresAttributes() {
        Profile p   = makeProfile();
        CityMap map = new CityMap(12345L);
        GameSave s  = GameSave.from(p, map, 0, 0, 0, 0);

        Profile p2 = new Profile("Alice", "Female", "Normal");
        s.applyToProfile(p2);
        assertEquals(5, p2.getAttribute(CharacterAttribute.INTELLIGENCE.name()));
        assertEquals(3, p2.getAttribute(CharacterAttribute.STAMINA.name()));
    }

    // -------------------------------------------------------------------------
    // applyToMap
    // -------------------------------------------------------------------------

    @Test
    public void testApplyToMapRestoresBuildingDiscovery() {
        CityMap map1 = new CityMap(12345L);
        int bx = -1, by = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && bx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && bx < 0; y++) {
                if (map1.getCell(x, y).hasBuilding()) { bx = x; by = y; }
            }
        }
        assertTrue("Map must have at least one building cell", bx >= 0);
        map1.getCell(bx, by).getBuilding().discover();

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map1, 0, 0, 0, 0);

        // Apply to a fresh identical map
        CityMap map2 = new CityMap(12345L);
        assertFalse("Fresh map building should be undiscovered",
                map2.getCell(bx, by).getBuilding().isDiscovered());
        s.applyToMap(map2);
        assertTrue("After apply, building should be discovered",
                map2.getCell(bx, by).getBuilding().isDiscovered());
    }

    @Test
    public void testApplyToMapRestoresImprovementDiscovery() {
        CityMap map1 = new CityMap(12345L);
        int bx = -1, by = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && bx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && bx < 0; y++) {
                if (map1.getCell(x, y).hasBuilding()) { bx = x; by = y; }
            }
        }
        assertTrue("Map must have at least one building cell", bx >= 0);
        map1.getCell(bx, by).getBuilding().getImprovements().get(1).discover();

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map1, 0, 0, 0, 0);

        CityMap map2 = new CityMap(12345L);
        s.applyToMap(map2);
        assertFalse("Improvement 0 should remain undiscovered",
                map2.getCell(bx, by).getBuilding().getImprovements().get(0).isDiscovered());
        assertTrue("Improvement 1 should be discovered after apply",
                map2.getCell(bx, by).getBuilding().getImprovements().get(1).isDiscovered());
        assertFalse("Improvement 2 should remain undiscovered",
                map2.getCell(bx, by).getBuilding().getImprovements().get(2).isDiscovered());
    }

    @Test
    public void testApplyToMapRestoresBuildingOwnership() {
        CityMap map1 = new CityMap(12345L);
        int ox = -1, oy = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && ox < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && ox < 0; y++) {
                if (map1.getCell(x, y).hasBuilding()) { ox = x; oy = y; }
            }
        }
        assertTrue("Map must have at least one building cell", ox >= 0);
        map1.getCell(ox, oy).getBuilding().setOwned(true);

        Profile  p = makeProfile();
        GameSave s = GameSave.from(p, map1, 0, 0, 0, 0);

        CityMap map2 = new CityMap(12345L);
        assertFalse("Fresh map building should not be owned",
                map2.getCell(ox, oy).getBuilding().isOwned());
        s.applyToMap(map2);
        assertTrue("After apply, building should be owned",
                map2.getCell(ox, oy).getBuilding().isOwned());
    }

    @Test
    public void testApplyToMapLeavesUndiscoveredUntouched() {
        CityMap  map1 = new CityMap(12345L);
        Profile  p    = makeProfile();
        GameSave s    = GameSave.from(p, map1, 0, 0, 0, 0); // no discoveries

        CityMap map2 = new CityMap(12345L);
        s.applyToMap(map2);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map2.getCell(x, y);
                if (cell.hasBuilding()) {
                    assertFalse("Building at (" + x + "," + y + ") should remain undiscovered",
                            cell.getBuilding().isDiscovered());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Full round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testFullRoundTrip() {
        // Build a game state with some discoveries
        CityMap map1 = new CityMap(12345L);
        int bx = -1, by = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && bx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && bx < 0; y++) {
                if (map1.getCell(x, y).hasBuilding()) { bx = x; by = y; }
            }
        }
        assertTrue("Map must have at least one building cell", bx >= 0);
        map1.getCell(bx, by).getBuilding().discover();
        map1.getCell(bx, by).getBuilding().getImprovements().get(0).discover();

        Profile p = makeProfile();
        GameSave s = GameSave.from(p, map1, bx, by, 1, 2);

        // Restore into fresh objects
        Profile p2 = new Profile("Alice", "Female", "Normal", "woman1",
                new HashMap<>(), 2051, 999L);
        s.applyToProfile(p2);
        CityMap map2 = new CityMap(12345L);
        s.applyToMap(map2);

        // Verify profile
        assertEquals(1500, p2.getMoney());
        assertEquals("2051-03-15 09:30", p2.getGameDateTime());
        assertEquals(25, p2.getCurrentStamina());

        // Verify position
        assertEquals(bx, s.getCharCellX());
        assertEquals(by, s.getCharCellY());
        assertEquals(1,  s.getHomeCellX());
        assertEquals(2,  s.getHomeCellY());

        // Verify map
        assertTrue("Building should be discovered after round-trip",
                map2.getCell(bx, by).getBuilding().isDiscovered());
        assertTrue("Improvement 0 should be discovered after round-trip",
                map2.getCell(bx, by).getBuilding().getImprovements().get(0).isDiscovered());
        assertFalse("Improvement 1 should remain undiscovered",
                map2.getCell(bx, by).getBuilding().getImprovements().get(1).isDiscovered());
    }

    // -------------------------------------------------------------------------
    // SaveGameManager data conversion (no Gdx required)
    // -------------------------------------------------------------------------

    @Test
    public void testToDataAndFromDataRoundTrip() {
        Profile  p   = makeProfile();
        CityMap  map = new CityMap(12345L);
        int bx = -1, by = -1;
        for (int x = 0; x < CityMap.MAP_SIZE && bx < 0; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE && bx < 0; y++) {
                if (map.getCell(x, y).hasBuilding()) { bx = x; by = y; }
            }
        }
        assertTrue("Map must have at least one building cell", bx >= 0);
        map.getCell(bx, by).getBuilding().discover();

        GameSave original = GameSave.from(p, map, bx, by, 1, 2);

        // Use package-private conversion helpers
        SaveGameManager.SaveData data       = SaveGameManager.toData(original);
        GameSave                 restored   = SaveGameManager.fromData(data);

        assertEquals(original.getCharacterName(), restored.getCharacterName());
        assertEquals(original.getMoney(),          restored.getMoney());
        assertEquals(original.getGameDateTime(),   restored.getGameDateTime());
        assertEquals(original.getCurrentStamina(), restored.getCurrentStamina());
        assertEquals(original.getRandSeed(),       restored.getRandSeed());
        assertEquals(original.getCharCellX(),      restored.getCharCellX());
        assertEquals(original.getHomeCellY(),      restored.getHomeCellY());

        // Verify discovery flag survives conversion
        int idx = bx * CityMap.MAP_SIZE + by;
        assertTrue("Building-discovered flag must survive round-trip",
                restored.getBuildingDiscovered()[idx]);
    }

    @Test
    public void testAttributesMapIsUnmodifiableInSave() {
        Profile  p   = makeProfile();
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);
        try {
            s.getAttributes().put("HACK", 99);
            fail("GameSave attributes map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testGetterArraysReturnCopies() {
        Profile  p   = makeProfile();
        CityMap  map = new CityMap(12345L);
        GameSave s   = GameSave.from(p, map, 0, 0, 0, 0);

        boolean[] copy = s.getBuildingDiscovered();
        copy[0] = true;
        assertFalse("Mutating returned array must not affect GameSave",
                s.getBuildingDiscovered()[0]);
    }
}
