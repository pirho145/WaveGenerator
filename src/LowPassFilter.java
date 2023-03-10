import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LowPassFilter implements WaveFunction {
    private final JComponent cmp = new JPanel(new GridBagLayout());
    private double level = 1.0;
    private final JSpinner levelEditor
        = new JSpinner(new SpinnerNumberModel(level, 0.0, 1000.0, 1.0));
    private double[] pres = new double[2];


    public LowPassFilter() {
        levelEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) levelEditor.getValue();
                level = value.doubleValue();
            }
        });

        GridBagConstraints c = new GridBagConstraints();

        cmp.add(new JLabel("Level"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        cmp.add(levelEditor, c);

        reset();
    }


    @Override
    public JComponent getComponent() {
        return cmp;
    }

    @Override
    public void reset() {
        for (int i = 0; i < pres.length; i++) {
            pres[i] = 0.0;
        }
    }

    @Override
    public int apply(double[] buf, int off, int len) {
        int len2 = len & 0x7ffffffe;
        int end = off + len2;
        double value;

        for (int i = off; i < end; i += 2) {
            for (int j = 0; j < pres.length; j++) {
                value = (pres[j] * level + buf[i + j]) / (level + 1.0);
                pres[j] = value;
                buf[i + j] = value;
            }
        }

        return len2;
    }
}
