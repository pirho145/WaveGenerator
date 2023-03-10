import javax.swing.JComponent;

public interface WaveFunction {
    JComponent getComponent();
    void reset();
    int apply(double[] buf, int off, int len);
}
