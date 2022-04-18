package de.itlobby.discoverj.listeners;

public class ListenerStateProvider {
    private static ListenerStateProvider instance;
    private SettingsSavedListener settingsSavedListener;
    private MultipleSelectionListener multipleSelectionListener;
    private ParentKeyDeletedListener parentKeyDeletedListener;

    private ListenerStateProvider() {
    }

    public static ListenerStateProvider getInstance() {
        if (instance == null) {
            instance = new ListenerStateProvider();
        }

        return instance;
    }

    public SettingsSavedListener getSettingsSavedListener() {
        return settingsSavedListener;
    }

    public void setSettingsSavedListener(SettingsSavedListener settingsSavedListener) {
        this.settingsSavedListener = settingsSavedListener;
    }

    public MultipleSelectionListener getMultipleSelectionListnener() {
        return multipleSelectionListener;
    }

    public void setMultipleSelectionListnener(MultipleSelectionListener multipleSelectionListener) {
        this.multipleSelectionListener = multipleSelectionListener;
    }

    public ParentKeyDeletedListener getParentKeyDeletedListener() {
        return parentKeyDeletedListener;
    }

    public void setParentKeyDeletedListener(ParentKeyDeletedListener parentKeyDeletedListener) {
        this.parentKeyDeletedListener = parentKeyDeletedListener;
    }
}
