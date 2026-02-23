# Font Size and Profile Limit Fixes

## Issues Addressed

### 1. Profile Summary Screen Font Issues
**Problem**: Labels (like "Gender:") were bigger than values (like "Male"), causing visual inconsistency and text overlapping.

**Root Cause**: Labels were using `subtitleFont` (30dp) while values used `bodyFont` (22dp).

**Solution**:
- Changed all labels to use `bodyFont` (22dp) instead of `subtitleFont`
- This ensures labels and values are the same size
- Maintains consistent visual hierarchy throughout the summary screen

### 2. Insufficient Spacing Between Attribute Heading and Values
**Problem**: Not enough space between the "Attributes:" heading and the attribute values below it.

**Solution**:
- Increased spacing before "Attributes:" section from 100px to 120px
- Increased spacing between "Attributes:" heading and first value from 60px to 80px
- Provides better visual separation and readability

### 3. Create Profile Button Text Overflow
**Problem**: The text "+ Create New Profile" was larger than the button, causing overflow.

**Root Cause**: Button text was using `buttonFont` (30dp subtitle font).

**Solution**:
- Changed button text to use `font` (bodyFont at 22dp)
- Text now fits properly within the button boundaries

### 4. Profile Limit Implementation
**Problem**: No limit on the number of profiles that could be created.

**Solution**:
- Added `MAX_PROFILES = 5` constant in ProfileManager
- Added validation in `createProfile()` and `addProfile()` methods
- Throws `IllegalArgumentException` when trying to create more than 5 profiles
- Added `canCreateNewProfile()` method to check if limit is reached
- ProfileSelectionScreen now:
  - Shows the button only when under the limit
  - Displays "Maximum profiles (5) reached" message when at limit
  - Prevents clicking when at maximum

## Technical Details

### Font Hierarchy (After Fix)
```
ProfileLoadSummaryScreen:
- Title: titleFont (40dp) - "Profile Summary"
- Labels: bodyFont (22dp) - "Character:", "Gender:", etc.
- Values: bodyFont (22dp) - "John", "Male", etc.
```

### Spacing Updates
```
Before "Attributes:" section: 120px (was 100px)
After "Attributes:" heading: 80px (was 60px)
Between attribute lines: 50px (unchanged)
```

### ProfileManager API Additions
```java
public static final int MAX_PROFILES = 5;

public boolean canCreateNewProfile() {
    return profiles.size() < MAX_PROFILES;
}

public int getMaxProfiles() {
    return MAX_PROFILES;
}
```

## Files Modified

1. **ProfileLoadSummaryScreen.java**
   - Updated `drawSummary()` method to use consistent fonts
   - Adjusted spacing for better layout

2. **ProfileSelectionScreen.java**
   - Changed "Create New Profile" button font
   - Added conditional rendering based on profile count
   - Added message display when at maximum profiles

3. **ProfileManager.java**
   - Added MAX_PROFILES constant
   - Added profile count validation
   - Added helper methods for checking limits

## User Experience Improvements

- **Consistent Typography**: All profile data now displays with uniform font sizes
- **Better Readability**: Increased spacing makes information easier to scan
- **Clear Limits**: Users are informed when they've reached the maximum number of profiles
- **Prevents Errors**: System gracefully handles profile limit without crashes
