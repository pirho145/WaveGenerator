import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Amplifier implements WaveFunction {
    private final JComponent cmp = new JPanel(new GridBagLayout());
    private double amp = 1.0;
    private double amp0 = 1.0;
    private double amp1 = 1.0;
    private final JSpinner ampEditor
        = new JSpinner(new SpinnerNumberModel(amp, 0.0, 1000.0, 0.1));
    private final JSpinner ampEditor0
        = new JSpinner(new SpinnerNumberModel(amp0, 0.0, 1000.0, 0.1));
    private final JSpinner ampEditor1
        = new JSpinner(new SpinnerNumberModel(amp1, 0.0, 1000.0, 0.1));


    public Amplifier() {
        ampEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) ampEditor.getValue();
                amp = value.doubleValue();
            }
        });

        ampEditor0.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) ampEditor0.getValue();
                amp0 = value.doubleValue();
            }
        });

        ampEditor1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) ampEditor1.getValue();
                amp1 = value.doubleValue();
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        cmp.add(ampEditor, c);

        c.gridwidth = 1;
        c.weightx = 1.0;
        cmp.add(ampEditor0, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        cmp.add(ampEditor1, c);
    }


    @Override
    public JComponent getComponent() {
        return cmp;
    }

    @Override
    public void reset() {
    }

    @Override
    public int apply(double[] buf, int off, int len) {
        int len2 = len & 0x7ffffffe;
        int end = off + len2;

        for (int i = off; i < end; i += 2) {
            buf[i] *= amp * amp0;
            buf[i + 1] *= amp * amp1;
        }

        return len2;
    }
}
