import com.rikodev.music.android.dna.render.OrganismCamera;
public class CameraCheck {
 public static void main(String[] args) {
  OrganismCamera c=new OrganismCamera(); c.drag(90,999); c.scale(100);
  for(int i=0;i<240;i++)c.step(1f/60);
  if(Math.abs(c.getYaw()-90)>.01 || c.getPitch()>75 || c.getZoom()>2.5)throw new AssertionError();
  c.fling(999,999);c.cancel();float y=c.getYaw();for(int i=0;i<120;i++)c.step(1f/60);
  if(c.getYaw()!=y)throw new AssertionError("cancel");
  c.pulse();c.step(.05f);if(c.getPulse()<=0||c.getPulse()>=1)throw new AssertionError("pulse");
  c.scale(Float.NaN);c.step(Float.NaN);if(!Float.isFinite(c.getZoom()))throw new AssertionError();
  c.reset();for(int i=0;i<240;i++)c.step(1f/60);if(Math.abs(c.getZoom()-1)>.001)throw new AssertionError();
  System.out.println("PASS camera bounds, smoothing, cancel, pulse decay, invalid inputs, reset");
 }
}
