package com.busfleet.bot;

import com.busfleet.model.BusModel;

/**
 * Сессия пользователя для пошагового ввода.
 */
public class UserSession {
    public enum Step {
        NONE,
        ADD_MODEL,
        ADD_STATE_NUMBER,
        ADD_MILEAGE,
        REMOVE_STATE_NUMBER,
        UPDATE_SELECT_BUS,
        UPDATE_MODE,
        UPDATE_VALUE,
        SEARCH_NUMBER
    }

    private Step step = Step.NONE;
    private BusModel selectedModel;
    private String stateNumber;
    private String updateMode; // "set" or "add"

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public BusModel getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(BusModel selectedModel) {
        this.selectedModel = selectedModel;
    }

    public String getStateNumber() {
        return stateNumber;
    }

    public void setStateNumber(String stateNumber) {
        this.stateNumber = stateNumber;
    }

    public String getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(String updateMode) {
        this.updateMode = updateMode;
    }

    public void reset() {
        step = Step.NONE;
        selectedModel = null;
        stateNumber = null;
        updateMode = null;
    }
}
