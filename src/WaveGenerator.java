import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class WaveGenerator implements WaveFunction {
    private final JComponent cmp = new JPanel(new GridBagLayout());
    private Waveform waveform = Waveform.SINE;
    private double amp = 1.0;
    private double freq = 440.0;
    private long j = 0L;

    private final JComboBox<Waveform> waveformEditor
        = new JComboBox<Waveform>(Waveform.values());
    private final JSpinner ampEditor
        = new JSpinner(new SpinnerNumberModel(amp, -1.0, 1.0, 0.1));
    private final JSpinner freqEditor
        = new JSpinner(new SpinnerNumberModel(freq, 0.1, 20000.0, 1.0));


    private static enum Waveform {
        SINE {
            @Override
            double apply(double phase) {
                return Math.sin(phase * 2.0 * Math.PI);
            }
        },
        SQUARE {
            @Override
            double apply(double phase) {
                return phase < 0.5 ? 1.0 : -1.0;
            }
        },
        TRIANGLE {
            @Override
            double apply(double phase) {
                if (phase < 0.25) {
                    return phase * 4.0;
                } else if (phase < 0.75) {
                    return 2.0 - phase * 4.0;
                } else {
                    return phase * 4.0 - 4.0;
                }
            }
        },
        SAWTOOTH {
            @Override
            double apply(double phase) {
                return phase < 0.5 ? phase * 2.0 : phase * 2.0 - 2.0;
            }
        },
        CONST {
            @Override
            double apply(double phase) {
                return 1.0;
            }
        },
        RANDOM {
            @Override
            double apply(double phase) {
                return Math.random() * 2.0 - 1.0;
            }
        };

        abstract double apply(double phase);
    }


    public WaveGenerator() {
        waveformEditor.setSelectedIndex(0);
        waveformEditor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                waveform = (Waveform) waveformEditor.getSelectedItem();
            }
        });

        ampEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) ampEditor.getValue();
                amp = value.doubleValue();
            }
        });

        freqEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) freqEditor.getValue();
                freq = value.doubleValue();
            }
        });

        GridBagConstraints key = new GridBagConstraints();
        GridBagConstraints value = new GridBagConstraints();

        value.fill = GridBagConstraints.HORIZONTAL;
        value.gridwidth = GridBagConstraints.REMAINDER;
        value.weightx = 1.0;

        cmp.add(new JLabel("Waveform"), key);
        cmp.add(waveformEditor, value);

        value.weightx = 0.0;
        cmp.add(new JLabel("Amp"), key);
        cmp.add(ampEditor, value);

        cmp.add(new JLabel("Freq"), key);
        cmp.add(freqEditor, value);
    }


    @Override
    public JComponent getComponent() {
        return cmp;
    }

    @Override
    public void reset() {
        j = 0L;
    }

    @Override
    public int apply(double[] buf, int off, int len) {
        int len2 = len / 2;
        double t = 48000.0 / freq;
        double phase;
        double value;

        for (int i = 0; i < len2; i++) {
            phase = (i + j) % t / t;
            value = waveform.apply(phase) * amp;
            buf[off + i * 2] += value;
            buf[off + i * 2 + 1] += value;
        }

        j += len2;
        return len2 * 2;
    }
}
