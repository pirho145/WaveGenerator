import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class App {
    private static final Color FG = new Color(0x00ff00, false);
    private static final Color BG = new Color(0x008000, false);
    private static final Color BG2 = new Color(0x006000, false);

    private final String[] funcNames = {
        "Generator",
        "Amplifier",
        "Limiter",
        "Low-Pass Filter",
        "High-Pass Filter",
        "Load",
        "Save"
    };
    private final FunctionSupplier[] funcSuppliers = {
        WaveGenerator::new,
        Amplifier::new,
        Limiter::new,
        LowPassFilter::new,
        HighPassFilter::new,
        LoadFunction::new,
        SaveFunction::new
    };

    private final SourceDataLine line;
    private final int lineOff;
    private final double[] buf = new double[8192];
    private int start = 0;
    private int end = 0;
    private final byte[] bbuf = new byte[buf.length * 2];
    private int channel = 0;
    private double zoomX = 1.0;
    private double zoomY = 1.0;
    private double volume = 0.05;
    private boolean play = true;
    private final NamedFunction[] functions = new NamedFunction[16];
    private int funcLen = 0;
    private final double[] flagBuf = new double[buf.length];
    private double trigger = 0.0;

    private final Timer timer = new Timer(32, e -> updateWave());

    private final JFrame frame;
    private final JDialog dialog;
    private final JComponent waveField = new JComponent() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            int mid = height / 2;
            double r = (mid - 1) * -zoomY;

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);

            g.setColor(BG2);
            for (int x = 1; x < width; x += 48) {
                g.drawLine(x, 0, x, height);
            }

            g.setColor(BG);
            for (int x = 1; x < width; x += 480) {
                g.drawLine(x, 0, x, height);
            }

            g.setColor(FG);
            g.drawRect(0, 0, width - 1, height - 1);

            g.translate(1, mid);
            g.setColor(BG);
            g.drawLine(0, 0, width, 0);

            g.setColor(Color.WHITE);
            int x1 = 0;
            int y1 = (int) (buf[start] * r);
            int x2;
            int y2;
            for (int i = start + 2; i < end; i += 2) {
                x2 = (i - start) / 2;
                x2 *= zoomX;
                y2 = (int) (buf[i] * r);
                g.drawLine(x1, y1, x2, y2);
                x1 = x2;
                y1 = y2;
            }
        }
    };
    private final JList<NamedFunction> funcList = new JList<NamedFunction>();
    private final JScrollPane optionPane = new JScrollPane();
    private final JButton addButton = new JButton("Add");
    private final JButton insertButton = new JButton("Insert");
    private final JPopupMenu addMenu = new JPopupMenu("Add");
    private final JPopupMenu insertMenu = new JPopupMenu("Insert");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton upButton = new JButton("Up");
    private final JButton downButton = new JButton("Down");
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton resetButton = new JButton("Reset");
    private final JSpinner volumeEditor
        = new JSpinner(new SpinnerNumberModel(volume, 0.0, 1.0, 0.01));
    private final JCheckBox playCheckBox = new JCheckBox("Sound", play);
    private final JSpinner channelEditor
        = new JSpinner(new SpinnerNumberModel(channel, 0, 1, 1));
    private final JSpinner zoomXEditor
        = new JSpinner(new SpinnerNumberModel(zoomX, 0.0, 1000.0, 0.1));
    private final JSpinner zoomYEditor
        = new JSpinner(new SpinnerNumberModel(zoomY, 0.0, 1000.0, 0.1));
    private final JSpinner triggerEditor
        = new JSpinner(new SpinnerNumberModel(trigger, -1.0, 1.0, 0.1));


    @FunctionalInterface
    private static interface FunctionSupplier extends Supplier<WaveFunction> {
    }

    private static class NamedFunction {
        final String name;
        final WaveFunction func;

        NamedFunction(String name, WaveFunction func) {
            this.name = name;
            this.func = func;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class LoadFunction implements WaveFunction {
        private final JComponent cmp = new JPanel();

        @Override
        public JComponent getComponent() {
            return cmp;
        }

        @Override
        public void reset() {
            for (int i = 0; i < flagBuf.length; i++) {
                flagBuf[i] = 0.0;
            }
        }

        @Override
        public int apply(double[] buf, int off, int len) {
            for (int i = 0; i < len; i++) {
                buf[off + i] += flagBuf[i];
            }

            return len;
        }
    }

    private class SaveFunction implements WaveFunction {
        private final JComponent cmp = new JPanel();

        @Override
        public JComponent getComponent() {
            return cmp;
        }

        @Override
        public void reset() {
        }

        @Override
        public int apply(double[] buf, int off, int len) {
            System.arraycopy(buf, off, flagBuf, 0, len);
            return len;
        }
    }


    public App(String frameTitle, String dialogTitle) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, false);
        line = AudioSystem.getSourceDataLine(format);
        line.open();
        lineOff = line.getBufferSize() - bbuf.length;

        frame = new JFrame(frameTitle);
        dialog = new JDialog(frame, dialogTitle, false);

        funcList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                NamedFunction func = funcList.getSelectedValue();
                if (func != null) {
                    optionPane.setViewportView(func.func.getComponent());
                } else {
                    optionPane.setViewportView(null);
                }
            }
        });

        JMenuItem menuItem;
        for (int i = 0; i < funcNames.length; i++) {
            final int index = i;
            menuItem = new JMenuItem(funcNames[i]);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addFunction(index);
                }
            });
            addMenu.add(menuItem);
        }

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point point = addButton.getLocation();
                addMenu.show(dialog, point.x, point.y);
            }
        });

        for (int i = 0; i < funcNames.length; i++) {
            final int index = i;
            menuItem = new JMenuItem(funcNames[i]);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    insertFunction(index);
                }
            });
            insertMenu.add(menuItem);
        }

        insertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point point = insertButton.getLocation();
                insertMenu.show(dialog, point.x, point.y);
            }
        });

        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUpFunction();
            }
        });

        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDownFunction();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteFunction();
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        volumeEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) volumeEditor.getValue();
                volume = value.doubleValue();
            }
        });

        playCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                play = playCheckBox.isSelected();
            }
        });

        channelEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) channelEditor.getValue();
                channel = value.intValue();
            }
        });

        zoomXEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) zoomXEditor.getValue();
                zoomX = value.doubleValue();
            }
        });

        zoomYEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) zoomYEditor.getValue();
                zoomY = value.doubleValue();
            }
        });

        triggerEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) triggerEditor.getValue();
                trigger = value.doubleValue();
            }
        });

        frame.add(waveField, BorderLayout.CENTER);
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dialog.setVisible(true);
            }
        });

        dialog.setLayout(new GridLayout(2, 1));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panel.add(new JScrollPane(funcList), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weighty = 0.0;
        panel.add(addButton, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(insertButton, c);

        JPanel panel2 = new JPanel(new GridLayout(1, 3));
        panel2.add(deleteButton);
        panel2.add(upButton);
        panel2.add(downButton);

        c.weightx = 0.0;
        panel.add(panel2, c);

        dialog.add(panel);

        panel = new JPanel(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panel.add(optionPane, c);

        panel2 = new JPanel(new GridLayout(1, 3));
        panel2.add(startButton);
        panel2.add(stopButton);
        panel2.add(resetButton);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        panel.add(panel2, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        panel.add(new JLabel("Channel"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(channelEditor, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 1.0;
        panel.add(new JLabel("Zoom"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(zoomXEditor, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(zoomYEditor, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 0.0;
        panel.add(new JLabel("Trigger"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(triggerEditor, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        panel.add(playCheckBox, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(volumeEditor, c);

        dialog.add(panel);

        addFunction(0);
    }

    private void updateList(int index) {
        NamedFunction[] arr = new NamedFunction[funcLen];
        System.arraycopy(functions, 0, arr, 0, funcLen);
        funcList.setListData(arr);

        if (index >= 0) {
            funcList.setSelectedIndex(index);
        }
    }

    private void addFunction(int i) {
        functions[funcLen] = new NamedFunction(funcNames[i], funcSuppliers[i].get());
        funcList.setSelectedIndex(funcLen);
        funcLen++;
        updateList(funcLen - 1);
    }

    private void insertFunction(int i) {
        int index = funcList.getSelectedIndex();
        if (index >= 0) {
            System.arraycopy(functions, index, functions, index + 1, funcLen - index);
            functions[index] = new NamedFunction(funcNames[i], funcSuppliers[i].get());
            funcLen++;
            updateList(index);
        } else {
            addFunction(i);
        }
    }

    private void deleteFunction() {
        int index = funcList.getSelectedIndex();
        if (index >= 0) {
            funcLen--;
            if (index < funcLen) {
                System.arraycopy(functions, index + 1, functions, index, funcLen - index);
            } else {
                index = funcLen - 1;
            }
            updateList(index);
        }
    }

    private void swapFunction(int i) {
        NamedFunction func = functions[i];
        functions[i] = functions[i + 1];
        functions[i + 1] = func;
    }

    private void moveUpFunction() {
        int index = funcList.getSelectedIndex();
        if (index >= 1) {
            swapFunction(index - 1);
            updateList(index - 1);
        }
    }

    private void moveDownFunction() {
        int index = funcList.getSelectedIndex();
        if (index >= 0 && index + 1 < funcLen) {
            swapFunction(index);
            updateList(index + 1);
        }
    }

    private void updateWave() {
        int len = (line.available() - lineOff) / 2 & 0x7ffffffe;
        if (len >= 2) {
            for (int i = 0; i < len; i += 2) {
                buf[i] = 0.0;
                buf[i + 1] = 0.0;

                for (int j = 0; j < funcLen; j++) {
                    functions[j].func.apply(buf, i, 2);
                }
            }

            start = channel;
            end = len;
            for (int i = start + 2; i < len; i += 2) {
                if (buf[i - 2] < trigger && buf[i] >= trigger) {
                    start = i - 2;
                    break;
                }
            }

            waveField.repaint();

            if (play) {
                double tmp;
                int value;

                for (int i = 0; i < end; i++) {
                    tmp = buf[i];
                    if (tmp >= 1.0) {
                        tmp = 1.0;
                    } else if (tmp < -1.0) {
                        tmp = -1.0;
                    }
                    value = (int) (tmp * volume * 32768.0);
                    if (value >= 32768) {
                        value = 32767;
                    }

                    bbuf[i * 2] = (byte) value;
                    bbuf[i * 2 + 1] = (byte) (value >> 8);
                }

                line.write(bbuf, 0, end * 2);
            }
        }
    }

    private void start() {
        line.start();
        timer.start();
    }

    private void stop() {
        timer.stop();
        line.stop();
        line.flush();
    }

    private void reset() {
        for (int i = 0; i < funcLen; i++) {
            functions[i].func.reset();
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    public JDialog getDialog() {
        return dialog;
    }


    public static void main(String[] args) throws Exception {
        App app = new App("Wave Generator", "Controller");
        JFrame frame = app.getFrame();
        JDialog dialog = app.getDialog();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 640);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        dialog.setSize(320, 640);
        dialog.setLocationByPlatform(true);
        dialog.setVisible(true);
    }
}
