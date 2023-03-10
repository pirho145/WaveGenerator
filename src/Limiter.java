import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Limiter implements WaveFunction {
    private final JComponent cmp = new JPanel(new GridBagLayout());
    private double limit = 1.0;
    private final JSpinner limitEditor
        = new JSpinner(new SpinnerNumberModel(limit, 0.0, 1.0, 0.1));


    public Limiter() {
        limitEditor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number value = (Number) limitEditor.getValue();
                limit = value.doubleValue();
            }
        });

        GridBagConstraints c = new GridBagConstraints();

        cmp.add(new JLabel("Limit"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        cmp.add(limitEditor, c);
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
        int end = off + len;
        double value;

        for (int i = off; i < end; i++) {
            value = buf[i];
            if (value >= limit) {
                buf[i] = limit;
            } else if (value < -limit) {
                buf[i] = -limit;
            }
        }

        return len;
    }
}
