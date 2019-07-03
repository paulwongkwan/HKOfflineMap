package mememe.hkofflinemap.MapStyle;

import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;

import java.io.InputStream;

/**
 * Created by Paul Wong on 17/04/21.
 */

public class ElevateStyle implements XmlRenderTheme {

    private final String path = "/assets/Elevate.xml";
    public XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback;

    public ElevateStyle(XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback) {
        this.xmlRenderThemeMenuCallback = xmlRenderThemeMenuCallback;
    }

    public ElevateStyle(){}

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return xmlRenderThemeMenuCallback;
    }

    /**
     * @return the prefix for all relative resource paths.
     */
    @Override
    public String getRelativePathPrefix() {
        return "";
    }

    @Override
    public InputStream getRenderThemeAsStream() {
        return getClass().getResourceAsStream(this.path);
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {

    }
}
