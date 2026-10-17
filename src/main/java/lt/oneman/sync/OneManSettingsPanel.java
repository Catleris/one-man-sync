package lt.oneman.sync;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;

/** Uses the same saved keys as RuneLite's standard OneMan configuration. */
final class OneManSettingsPanel extends JPanel
{
    OneManSettingsPanel(ConfigManager configs, OneManSyncConfig config)
    {
        super(new BorderLayout(0, 6));
        JPanel fields = new JPanel(new GridBagLayout());
        addField(fields, new JLabel("OneMan Sync settings"));
        JCheckBox enabled = new JCheckBox("Enable website sync", config.enabled());
        addField(fields, enabled);
        addField(fields, new JLabel("Sync Key"));
        JPasswordField key = new JPasswordField(config.syncKey());
        key.setName("syncKey"); addField(fields, key);
        addField(fields, new JLabel("Full sync interval (minutes)"));
        JSpinner interval = new JSpinner(new SpinnerNumberModel(config.intervalMinutes(), 2, 30, 1));
        interval.setName("intervalMinutes"); addField(fields, interval);
        JTextArea warning = text("Website sync sends your IP address and account progress to oneman.lt, a third-party server not controlled or verified by RuneLite developers. Leave disabled for local hubs only.");
        warning.setRows(7); addField(fields, warning);
        JButton save = new JButton("Save sync settings"); addField(fields, save);
        JLabel status = new JLabel(" "); addField(fields, status);
        save.addActionListener(e -> {
            try { interval.commitEdit(); }
            catch (java.text.ParseException error) { status.setText("Enter an interval from 2 to 30."); return; }
            char[] secret = key.getPassword();
            try {
                configs.setConfiguration("one-man-sync", "syncKey", new String(secret).trim());
                configs.setConfiguration("one-man-sync", "intervalMinutes", interval.getValue());
                configs.setConfiguration("one-man-sync", "enabled", enabled.isSelected());
                status.setText("Sync settings saved.");
            } finally { Arrays.fill(secret, '\0'); }
        });
        JButton account = new JButton("Open OneMan account");
        account.addActionListener(e -> LinkBrowser.browse("https://oneman.lt/account.php")); addField(fields, account);
        JPanel contents = new JPanel(new BorderLayout(0, 8)); contents.add(fields, BorderLayout.NORTH);
        contents.add(text("All original Slayer, notification and other settings remain in RuneLite Configuration (wrench) → Plugins → OneMan Sync. Return from Plugin Hub to the installed plugin list to open them."), BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(contents, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setMinimumSize(new Dimension(0, 0)); add(scroll, BorderLayout.CENTER);
    }

    private static void addField(JPanel panel, JComponent component)
    {
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0; row.gridy = panel.getComponentCount(); row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL; row.anchor = GridBagConstraints.NORTHWEST;
        row.insets = new Insets(0, 0, 5, 0); panel.add(component, row);
    }

    private static JTextArea text(String value)
    {
        JTextArea area = new JTextArea(value); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false);
        return area;
    }
}
