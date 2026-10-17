package lt.oneman.sync;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OneManSettingsPanelTest
{
    @Test public void savesMaskedKeyUsingExistingConfigKeysOnlyWhenRequested() throws Exception
    {
        ConfigManager configs = mock(ConfigManager.class);
        SwingUtilities.invokeAndWait(() -> {
            OneManSettingsPanel panel = new OneManSettingsPanel(configs, new OneManSyncConfig(){});
            JPasswordField key = find(panel, JPasswordField.class);
            assertNotNull(key); assertNotEquals(0, key.getEchoChar());
            key.setText("test-only-key");
            JSpinner interval = find(panel, JSpinner.class); interval.setValue(8);
            JCheckBox enabled = find(panel, JCheckBox.class);
            assertFalse("Uploading remains opt-in", enabled.isSelected());
            enabled.setSelected(true);
            verifyNoInteractions(configs);
            saveButton(panel).doClick();
            verify(configs).setConfiguration("one-man-sync", "syncKey", "test-only-key");
            verify(configs).setConfiguration("one-man-sync", "intervalMinutes", 8);
            verify(configs).setConfiguration("one-man-sync", "enabled", true);
            verifyNoMoreInteractions(configs);
        });
    }

    private static JButton saveButton(Container parent)
    {
        for(Component child : parent.getComponents()) {
            if(child instanceof JButton && "Save sync settings".equals(((JButton)child).getText())) return (JButton)child;
            if(child instanceof Container) { JButton found=saveButton((Container)child); if(found!=null) return found; }
        }
        return null;
    }

    private static <T extends Component> T find(Container parent, Class<T> type)
    {
        for(Component child : parent.getComponents()) {
            if(type.isInstance(child)) return type.cast(child);
            if(child instanceof Container) {
                T found = find((Container)child, type); if(found != null) return found;
            }
        }
        return null;
    }
}
