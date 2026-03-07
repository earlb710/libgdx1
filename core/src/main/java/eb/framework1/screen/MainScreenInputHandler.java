package eb.framework1.screen;

import eb.framework1.popup.*;
import eb.framework1.shop.ShopItem;
import eb.framework1.ui.MapViewState;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Handles all touch/mouse/keyboard input events for {@link MainScreen}.
 *
 * <p>Extracted from the anonymous {@code InputAdapter} that was previously inlined
 * inside {@code MainScreen.setupInput()}.  Keeping input logic in its own class
 * makes each piece easier to find and reason about in isolation.
 *
 * <p>Input-specific drag and tap state (previously scattered across MainScreen
 * fields) is now encapsulated here.  All other shared state is accessed via a
 * reference to the owning {@link MainScreen} instance; fields and methods that
 * need to be visible from this class carry package-private visibility in
 * MainScreen.
 */
class MainScreenInputHandler extends InputAdapter {

    // -------------------------------------------------------------------------
    // Input tracking state (moved from MainScreen)
    // -------------------------------------------------------------------------

    /** True while a finger/mouse button is held down in the info area or over a popup. */
    boolean infoAreaPressed = false;
    /** Screen-coordinate position where the current info-area press started. */
    float   infoTouchStartX, infoTouchStartY;
    /** Scroll offset at the moment the current info drag began. */
    float   infoScrollDragStartScrollY, infoScrollDragStartScrollX;

    /** True while the user is dragging the map. */
    boolean isDragging = false;
    /** Screen position where the current drag started. */
    float   dragStartX, dragStartY;
    /** Map offset at the moment the current drag started. */
    float   dragStartOffsetX, dragStartOffsetY;

    /** Timestamp of the last map tap (for double-click detection). */
    long lastMapTapTimeMs  = 0L;
    /** Map cell that was tapped last time (for double-click detection). */
    int  lastMapTapCellX   = -1;
    /** Map cell that was tapped last time (for double-click detection). */
    int  lastMapTapCellY   = -1;

    // -------------------------------------------------------------------------

    private final MainScreen screen;

    MainScreenInputHandler(MainScreen screen) {
        this.screen = screen;
    }

    // =========================================================================
    // Touch events
    // =========================================================================

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        int flippedY = screen.state.screenHeight - screenY;

        // Resting popup blocks all normal interaction while visible
        // (checked before isWalking so the OK button works during Traveling result)
        if (screen.restingPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Walking animation blocks all input
        if (screen.state.isWalking) { return true; }

        // Quit confirmation overlay blocks all interaction
        if (screen.quitConfirming) { return true; }

        // Save-done popup blocks all interaction until OK is tapped
        if (screen.saveDoneVisible) {
            return blockForPopup(screenX, screenY);
        }

        // Any look-around popup blocks normal interaction
        if (screen.lookAroundPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Tiredness popup blocks all normal interaction until dismissed
        if (screen.tirednessPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Discovery popup blocks all normal interaction until dismissed
        if (screen.discoveryPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Service result popup blocks all normal interaction until dismissed
        if (screen.serviceResultPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Stash popup blocks all normal interaction until dismissed
        if (screen.stashPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Put-in-stash popup blocks all normal interaction until dismissed
        if (screen.putInStashPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Confirm drop popup blocks all normal interaction until dismissed
        if (screen.confirmDropPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Hotel reception popup blocks all normal interaction until dismissed
        if (screen.hotelReceptionPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Gym instructor popup blocks all normal interaction until dismissed
        if (screen.gymInstructorPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Shop popup blocks all normal interaction until dismissed
        if (screen.shopPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Email popup blocks all normal interaction until dismissed
        if (screen.emailPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Phone popup blocks all normal interaction until dismissed
        if (screen.phonePopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Note popup blocks all normal interaction until dismissed
        if (screen.notePopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Meet popup blocks all normal interaction until dismissed
        if (screen.meetPopup.isVisible()) {
            return blockForPopup(screenX, screenY);
        }

        // Left-click with context menu visible – record position for tap detection
        if (screen.contextMenu.isVisible()) {
            dragStartX = screenX;
            dragStartY = screenY;
            return true;
        }

        if (flippedY > screen.state.screenHeight - MapViewState.INFO_BAR_HEIGHT) {
            // Top info bar — treat as tap target (not map drag)
            infoAreaPressed = true;
            infoTouchStartX = screenX;
            infoTouchStartY = screenY;
            isDragging      = false;
        } else if (flippedY > screen.state.infoAreaHeight) {
            isDragging       = true;
            dragStartX       = screenX;
            dragStartY       = screenY;
            dragStartOffsetX = screen.state.mapOffsetX;
            dragStartOffsetY = screen.state.mapOffsetY;
            infoAreaPressed  = false;
        } else {
            infoAreaPressed = true;
            infoTouchStartX = screenX;
            infoTouchStartY = screenY;
            infoScrollDragStartScrollY = screen.state.infoScrollY;
            infoScrollDragStartScrollX = screen.state.infoScrollX;
            isDragging      = false;
        }
        return true;
    }

    /**
     * Records the start of a touch press in the info area and returns {@code true}
     * to consume the event.  Called when a blocking popup is visible and all
     * other interaction must be suppressed until the popup is dismissed.
     */
    private boolean blockForPopup(int screenX, int screenY) {
        infoAreaPressed = true;
        infoTouchStartX = screenX;
        infoTouchStartY = screenY;
        isDragging      = false;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        int flippedY = screen.state.screenHeight - screenY;

        // Resting popup: only the OK button (RESULT phase) can dismiss it
        // (checked before isWalking so the OK button works during Traveling result)
        if (screen.restingPopup.isVisible()) {
            if (infoAreaPressed && !screen.restingPopup.isAnimating()) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) screen.restingPopup.onTap(screenX, flippedY);
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Walking animation blocks all input
        if (screen.state.isWalking) {
            infoAreaPressed = false;
            isDragging = false;
            return true;
        }

        // Quit confirmation overlay – handle Yes/No only
        if (screen.quitConfirming) {
            if (screenX >= screen.quitYesBtnX && screenX <= screen.quitYesBtnX + screen.quitYesBtnW
                    && flippedY >= screen.quitYesBtnY && flippedY <= screen.quitYesBtnY + screen.quitYesBtnH) {
                com.badlogic.gdx.Gdx.app.log("MainScreen", "Quit confirmed");
                com.badlogic.gdx.Gdx.app.exit();
            } else if (screenX >= screen.quitNoBtnX && screenX <= screen.quitNoBtnX + screen.quitNoBtnW
                    && flippedY >= screen.quitNoBtnY && flippedY <= screen.quitNoBtnY + screen.quitNoBtnH) {
                screen.quitConfirming = false;
            }
            infoAreaPressed = false;
            isDragging = false;
            return true;
        }

        // Save-done popup – only the OK button dismisses it
        if (screen.saveDoneVisible) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS
                        && screenX >= screen.saveDoneOkBtnX && screenX <= screen.saveDoneOkBtnX + screen.saveDoneOkBtnW
                        && flippedY >= screen.saveDoneOkBtnY && flippedY <= screen.saveDoneOkBtnY + screen.saveDoneOkBtnH) {
                    screen.saveDoneVisible = false;
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        if (screen.lookAroundPopup.isResults()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) screen.lookAroundPopup.onTap(screenX, flippedY);
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }
        if (screen.lookAroundPopup.isAnimating()) {
            infoAreaPressed = false;
            isDragging = false;
            return true;
        }

        // Tiredness popup: only the OK button can dismiss it
        if (screen.tirednessPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) screen.tirednessPopup.onTap(screenX, flippedY);
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Discovery popup: only the OK button can dismiss it
        if (screen.discoveryPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) screen.discoveryPopup.onTap(screenX, flippedY);
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Service result popup: only the OK button can dismiss it
        if (screen.serviceResultPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) screen.serviceResultPopup.onTap(screenX, flippedY);
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Stash popup: Close, Take, or Put in Stash button
        if (screen.stashPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.stashPopup.onTap(screenX, flippedY);
                    if (result >= 0) screen.handleTakeFromStash(result);
                    else if (result == StashPopup.RESULT_PUT_IN_STASH)
                        screen.putInStashPopup.show();
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Put-in-stash popup: stash the selected item or close
        if (screen.putInStashPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.putInStashPopup.onTap(screenX, flippedY);
                    if (result >= 0) screen.handleEquipDrop(result);
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Confirm drop popup: Yes drops/stashes the item; No cancels
        if (screen.confirmDropPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.confirmDropPopup.onTap(screenX, flippedY);
                    if (result == ConfirmPopup.RESULT_YES) {
                        screen.handleEquipDrop(screen.confirmDropPopup.getPendingIdx());
                    }
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Note popup: checkbox toggles or Save / Cancel
        if (screen.notePopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.notePopup.onTap(screenX, flippedY);
                    if (result == NotePopup.RESULT_CONFIRM) {
                        screen.state.noteIncludeTime     = screen.notePopup.isIncludeTime();
                        screen.state.noteIncludeLocation = screen.notePopup.isIncludeLocation();
                        com.badlogic.gdx.Gdx.input.setOnscreenKeyboardVisible(false);
                        screen.submitNoteFromPopup();
                    } else if (result == NotePopup.RESULT_CANCEL) {
                        com.badlogic.gdx.Gdx.input.setOnscreenKeyboardVisible(false);
                    }
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Hotel reception popup: night selection or cancel
        if (screen.hotelReceptionPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int nights = screen.hotelReceptionPopup.onTap(screenX, flippedY);
                    if (nights >= 1) {
                        screen.handleHotelCheckIn(nights,
                                screen.hotelReceptionPopup.getDiscountedTotal(nights),
                                screen.hotelReceptionPopup.getStaminaBonus());
                    }
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Gym instructor popup: training option or cancel
        if (screen.gymInstructorPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int option = screen.gymInstructorPopup.onTap(screenX, flippedY);
                    if (option >= 0) screen.handleGymTraining(option);
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Shop popup: buy an item or close
        if (screen.shopPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int idx = screen.shopPopup.onTap(screenX, flippedY);
                    if (idx >= 0 && idx < screen.shopItems.size()) {
                        screen.handleShopPurchase(screen.shopItems.get(idx), screen.shopPopup.getLastQuantity());
                    }
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Email popup: Accept, Decline, or Close
        if (screen.emailPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.emailPopup.onTap(screenX, flippedY);
                    if (result >= 0) screen.handleEmailAccepted(result);
                    // Persist statuses after every interaction so re-opens restore marks
                    screen.todaysEmailStatuses = screen.emailPopup.getStatuses();
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Phone popup: tap a contact to mark as called, or tap power button to close
        if (screen.phonePopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.phonePopup.onTap(screenX, flippedY);
                    if (result >= 0) screen.handleContactPhoned(result);
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // NPC Annotation popup: close button
        // Meet popup: tap a question button, accept/reject, or close button
        if (screen.meetPopup.isVisible()) {
            if (infoAreaPressed) {
                float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
                if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                    int result = screen.meetPopup.onTap(screenX, flippedY);
                    if (result == MeetPopup.RESULT_ACCEPTED) {
                        screen.handleMeetingAccepted();
                    } else if (result == MeetPopup.RESULT_REJECTED) {
                        screen.handleMeetingRejected();
                    }
                    // RESULT_CLOSED: popup hides but the appointment and
                    // pendingCase are preserved so the player can reopen
                    // the meeting by tapping the Meet button again.
                }
                infoAreaPressed = false;
            }
            isDragging = false;
            return true;
        }

        // Left-click: handle context menu first
        if (screen.contextMenu.isVisible()) {
            float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
            if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                int idx = screen.contextMenu.onTap(screenX, flippedY);
                if (idx >= 0 && idx < screen.contextMenuActions.size()) {
                    screen.contextMenuActions.get(idx).run();
                }
            }
            screen.contextMenu.dismiss();
            isDragging = false;
            infoAreaPressed = false;
            return true;
        }

        if (isDragging && flippedY > screen.state.infoAreaHeight) {
            float d = Vector2.len(screenX - dragStartX, screenY - dragStartY);
            if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                if (screen.state.helpVisible) screen.state.helpVisible = false;
                screen.selectCellAt(screenX, flippedY);
                // Double-click detection: same cell tapped twice within time window
                long    now      = System.currentTimeMillis();
                boolean sameCell = screen.state.selectedCellX == lastMapTapCellX
                                && screen.state.selectedCellY == lastMapTapCellY;
                if (now - lastMapTapTimeMs < MainScreen.DOUBLE_CLICK_MS && sameCell
                        && !screen.lookAroundPopup.isVisible()) {
                    screen.contextMenu.dismiss();
                    screen.buildContextMenu(screenX, flippedY);
                    com.badlogic.gdx.Gdx.app.log("MainScreen", "Double-click menu at " + screenX + "," + flippedY
                            + " items=" + screen.contextMenuItems.size());
                    lastMapTapTimeMs = 0L;  // reset so triple-click doesn't re-trigger
                    lastMapTapCellX  = -1;
                    lastMapTapCellY  = -1;
                } else {
                    lastMapTapTimeMs = now;
                    lastMapTapCellX  = screen.state.selectedCellX;
                    lastMapTapCellY  = screen.state.selectedCellY;
                }
            }
        }
        if (infoAreaPressed) {
            float d = Vector2.len(screenX - infoTouchStartX, screenY - infoTouchStartY);
            if (d < MainScreen.TAP_THRESHOLD_PIXELS) {
                if (screen.state.unitInteriorOpen) {
                    // Office popup covers the info panel — only office buttons are active.
                    // Map-navigation buttons (Look Around, Move To, etc.) must be suppressed
                    // to prevent click-through to the info panel behind the popup.
                    screen.checkUnitExitButtonClick(screenX, flippedY);
                    screen.checkRestButtonClick(screenX, flippedY);
                    screen.checkSleepButtonClick(screenX, flippedY);
                    screen.checkOpenStashButtonClick(screenX, flippedY);
                    screen.checkCheckEmailsButtonClick(screenX, flippedY);
                    screen.checkOpenPhoneButtonClick(screenX, flippedY);
                    screen.checkSaveButtonClick(screenX, flippedY);
                } else {
                    screen.checkTabClick(screenX, flippedY);
                    screen.checkMoveToButtonClick(screenX, flippedY);
                    screen.checkLookAroundButtonClick(screenX, flippedY);
                    screen.checkGoToOfficeButtonClick(screenX, flippedY);
                    screen.checkGoToHotelRoomButtonClick(screenX, flippedY);
                    screen.checkEquipDropButtonClick(screenX, flippedY);
                    screen.checkHelpButtonClick(screenX, flippedY);
                    screen.checkAddNoteButtonClick(screenX, flippedY);
                    screen.checkServiceButtonClick(screenX, flippedY);
                    screen.checkImprovementButtonClick(screenX, flippedY);
                }
                screen.checkTabClick(screenX, flippedY);
                screen.checkExpandButtonClick(screenX, flippedY);
                screen.checkAppointmentButtonClick(screenX, flippedY);
                screen.checkDevModeButtonClick(screenX, flippedY);
            }
            infoAreaPressed = false;
        }
        isDragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (screen.contextMenu.isVisible()) {
            // Only dismiss if the finger has moved far enough to be a real drag.
            // Tiny jitter during a tap should not cancel the menu before touchUp fires.
            float dragDistance = Vector2.len(screenX - dragStartX, screenY - dragStartY);
            if (dragDistance >= MainScreen.TAP_THRESHOLD_PIXELS) {
                screen.contextMenu.dismiss();
            }
            return true;
        }
        if (isDragging) {
            float cs = screen.state.getCellSize();
            screen.state.mapOffsetX = dragStartOffsetX - (screenX - dragStartX) / cs;
            screen.state.mapOffsetY = dragStartOffsetY - (screenY - dragStartY) / cs;
            screen.state.clampMapOffset();
            return true;
        }
        if (infoAreaPressed && !screen.lookAroundPopup.isVisible() && !screen.discoveryPopup.isVisible()) {
            // screenY is 0 at top, increases downward
            // Drag up (screenY decreases) → reveal content below → increase infoScrollY
            float dy = infoTouchStartY - screenY;
            float dx = infoTouchStartX - screenX;
            screen.state.infoScrollY = MathUtils.clamp(
                    infoScrollDragStartScrollY + dy, 0f, screen.state.infoMaxScrollY);
            screen.state.infoScrollX = MathUtils.clamp(
                    infoScrollDragStartScrollX + dx, 0f, screen.state.infoMaxScrollX);
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        screen.contextMenu.dismiss();
        if (screen.state.unitInteriorOpen) {
            screen.state.infoScrollY = MathUtils.clamp(
                    screen.state.infoScrollY + amountY * 20f, 0f, screen.state.infoMaxScrollY);
            return true;
        }
        if (screen.discoveryPopup.isVisible()) {
            screen.discoveryPopup.scroll(amountY * 20f);
            return true;
        }
        if (screen.shopPopup.isVisible()) {
            screen.shopPopup.scroll(amountY * 20f);
            return true;
        }
        float old = screen.state.zoomLevel;
        screen.state.zoomLevel = MathUtils.clamp(screen.state.zoomLevel - amountY * MainScreen.ZOOM_SPEED, MainScreen.MIN_ZOOM, MainScreen.MAX_ZOOM);
        if (old != screen.state.zoomLevel) screen.state.clampMapOffset();
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        screen.updateCursorCell(screenX, screen.state.screenHeight - screenY);
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        if (screen.notePopup.isVisible()) return screen.notePopup.keyTyped(character);
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (screen.notePopup.isVisible()) return screen.notePopup.keyDown(keycode);
        return false;
    }
}
