import com.rikodev.music.android.audiofx.visualizer.MdnaSpectrumMath;
/** Independent DFT oracle supplies actual Hann-windowed PCM powers to production scaling. */
public class MdnaSpectrumCheck {
    private static float measure(int n, double amplitude) {
        double total = 0, windowPower = 0;
        double[] samples = new double[n];
        for(int i=0;i<n;i++) {
            double w=.5*(1-Math.cos(2*Math.PI*i/(n-1)));
            windowPower+=w*w;samples[i]=amplitude*Math.sin(2*Math.PI*32*i/n)*w;
        }
        for(int k=1;k<n/2;k++) {
            double real=0,imag=0;
            for(int i=0;i<n;i++){double a=2*Math.PI*k*i/n;real+=samples[i]*Math.cos(a);imag-=samples[i]*Math.sin(a);}
            total+=real*real+imag*imag;
        }
        return MdnaSpectrumMath.level(total,n,windowPower);
    }
    public static void main(String[] args) {
        float loud=measure(1024,.8),quiet=measure(1024,.08);
        if(loud < quiet*3.15 || loud>=1 || measure(1024,0)!=0)throw new AssertionError("loudness/silence");
        if(Math.abs(loud-measure(2048,.8))>.0001)throw new AssertionError("FFT size");
        if(Math.abs(loud-Math.sqrt(.8/Math.sqrt(2)))>.0001)throw new AssertionError("absolute RMS");
        float[] bands=new float[40];bands[4]=loud;
        if(Math.abs(MdnaSpectrumMath.envelope(bands,0,40)-loud)>.0001)throw new AssertionError("band power");
        bands[4]=Float.NaN;
        if(MdnaSpectrumMath.envelope(bands,0,40)!=0)throw new AssertionError("finite");
        System.out.println("PASS PCM DFT oracle: absolute RMS, 10x amplitude contrast, FFT-size invariance, silence, band power, finite guards");
    }
}
