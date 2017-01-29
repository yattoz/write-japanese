package dmeeuwis.nakama.data;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

public class AndroidInputStreamGenerator implements AssetFinder.InputStreamGenerator {

    AssetManager asm;

    public AndroidInputStreamGenerator(AssetManager asm){
        this.asm = asm;
    }

    @Override
    public InputStream fromPath(String path) throws IOException {
        return this.asm.open(path);
    }
}
