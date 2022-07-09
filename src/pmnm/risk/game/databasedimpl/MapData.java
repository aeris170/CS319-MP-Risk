package pmnm.risk.game.databasedimpl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;

import com.google.common.collect.ImmutableList;
import com.pmnm.risk.map.MapConfig;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Data
@ToString(includeFieldNames = true)
public final class MapData implements Serializable {

	private static final long serialVersionUID = 3162242848938299845L;
	
	@Getter
	@NonNull
	private final MapConfig config;
	
	@Getter
	@NonNull
	private final transient BufferedImage backgroundImage;
	
	@NonNull
	private final ImmutableList<pmnm.risk.game.databasedimpl.ContinentData> continents;
	public Iterable<pmnm.risk.game.databasedimpl.ContinentData> getContinents() {
		return continents;
	}

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageIO.write(backgroundImage, "png", out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        in.defaultReadObject();
        Field f = MapData.class.getDeclaredField("backgroundImage");
        f.setAccessible(true);
        f.set(this, ImageIO.read(in));
        f.setAccessible(false);
    }
}